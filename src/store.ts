/**
 * 年度数据存取:隔离实例内存缓存 -> Workers KV -> 百度接口,三级递进。
 * KV 是必需依赖:没有 KV 时每次请求都会出网抓百度,百度侧有 IP 频控,
 * 因此宁可返回 503 也不允许无缓存裸奔(见 MissingKVError)。
 */
import { fetchYearFromBaidu, type YearHolidayData } from "./baidu";
import { weekdayOf } from "./beijing";

export interface Env {
  HOLIDAY_KV?: KVNamespace;
  UPDATE_KEY?: string;
}

/** 未绑定 KV 时由数据层抛出,调用方据此拒绝出网抓取 */
export class MissingKVError extends Error {
  constructor() {
    super("HOLIDAY_KV 未绑定");
    this.name = "MissingKVError";
  }
}

export type DataSource = "memory" | "kv" | "baidu" | null;

export interface YearResult {
  data: YearHolidayData | null;
  source: DataSource;
}

const KV_PREFIX = "holiday:";
/** 有数据的年份,内存缓存 1 小时 */
const MEM_TTL_MS = 60 * 60 * 1000;
/** 百度抓取成功但无数据(通常次年安排未公布),内存缓存 10 分钟,避免反复出网 */
const NEG_TTL_MS = 10 * 60 * 1000;

const memCache = new Map<string, { data: YearHolidayData | null; ts: number; positive: boolean }>();

function kvKey(year: number): string {
  return `${KV_PREFIX}${year}`;
}

/** 手动刷新后失效对应年份的内存缓存 */
export function invalidateMem(year: number): void {
  memCache.delete(kvKey(year));
}

function memGet(year: number): YearHolidayData | null | undefined {
  const hit = memCache.get(kvKey(year));
  if (!hit) return undefined;
  const ttl = hit.positive ? MEM_TTL_MS : NEG_TTL_MS;
  if (Date.now() - hit.ts > ttl) {
    memCache.delete(kvKey(year));
    return undefined;
  }
  return hit.data;
}

function memPut(year: number, data: YearHolidayData | null): void {
  memCache.set(kvKey(year), { data, ts: Date.now(), positive: data !== null });
}

/**
 * 获取某年数据,forceRefresh 为 true 时强制重新抓取(用于手动更新和定时任务)。
 * 返回 null 表示该年暂无数据(未公布)或暂时抓取失败,调用方自行降级。
 */
export async function getYearData(
  env: Env,
  year: number,
  forceRefresh = false
): Promise<YearResult> {
  const kv = env.HOLIDAY_KV;
  if (!kv) throw new MissingKVError();

  if (!forceRefresh) {
    const mem = memGet(year);
    if (mem !== undefined) {
      return { data: mem, source: mem === null ? null : "memory" };
    }
    const fromKv = await readKv(kv, year);
    if (fromKv !== undefined) {
      memPut(year, fromKv);
      return { data: fromKv, source: "kv" };
    }
  }

  const fresh = await fetchYearFromBaidu(year);
  const empty = fresh === null || (fresh.holidays.length === 0 && fresh.workdays.length === 0);
  if (empty) {
    // 抓取失败或该年数据未公布:优先回退 KV 中已有的旧数据,不要用空结果覆盖
    const fromKv = await readKv(kv, year);
    if (fromKv !== undefined) {
      memPut(year, fromKv);
      return { data: fromKv, source: "kv" };
    }
    memPut(year, null);
    return { data: null, source: null };
  }

  memPut(year, fresh);
  await writeKv(kv, fresh);
  return { data: fresh, source: "baidu" };
}

async function readKv(kv: KVNamespace, year: number): Promise<YearHolidayData | undefined> {
  try {
    const data = await kv.get<YearHolidayData>(kvKey(year), "json");
    if (!data) return undefined;
    // 空数据没有缓存价值,视为未命中
    if (data.holidays.length === 0 && data.workdays.length === 0) return undefined;
    return data;
  } catch (e) {
    // 读失败降级为现抓,但必须留痕:静默降级会让「KV 其实没通」看起来一切正常
    console.error("KV 读取失败,本次降级为现抓:", kvKey(year), e);
    return undefined;
  }
}

async function writeKv(kv: KVNamespace, data: YearHolidayData): Promise<void> {
  try {
    // KV 最终一致 + 每天写入次数极少,直接覆盖即可
    await kv.put(kvKey(data.year), JSON.stringify(data));
  } catch (e) {
    // 写失败不阻塞响应:免费版每天 1000 次写,额度耗尽时接口仍须可用
    console.error("KV 写入失败:", kvKey(data.year), e);
  }
}

export interface StatusSets {
  holidays: Set<string>;
  workdays: Set<string>;
}

export function toStatusSets(data: YearHolidayData): StatusSets {
  return { holidays: new Set(data.holidays), workdays: new Set(data.workdays) };
}

/**
 * 单日状态:0 普通工作日 1 周末 2 补班工作日 3 法定节假日。
 * data 为 null(该年无数据)时返回 null,由调用方决定降级策略。
 */
export function statusOf(dateStr: string, sets: StatusSets | null): 0 | 1 | 2 | 3 | null {
  if (sets === null) return null;
  if (sets.holidays.has(dateStr)) return 3;
  if (sets.workdays.has(dateStr)) return 2;
  const wd = weekdayOf(dateStr);
  return wd === 0 || wd === 6 ? 1 : 0;
}
