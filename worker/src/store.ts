/**
 * 年度数据存取:隔离实例内存缓存 -> Workers KV -> 百度接口,三级递进。
 * 对应 Java 版 HolidayDateService + MySQL 的角色:数据落到 KV 后,
 * 绝大多数请求不再依赖百度,只有首次访问或定时刷新才会出网。
 */
import { fetchYearFromBaidu, type YearHolidayData } from "./baidu";
import { weekdayOf } from "./beijing";

export interface Env {
  HOLIDAY_KV?: KVNamespace;
  UPDATE_KEY?: string;
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
  if (!forceRefresh) {
    const mem = memGet(year);
    if (mem !== undefined) {
      return { data: mem, source: mem === null ? null : "memory" };
    }
    const fromKv = await readKv(env, year);
    if (fromKv !== undefined) {
      memPut(year, fromKv);
      return { data: fromKv, source: "kv" };
    }
  }

  const fresh = await fetchYearFromBaidu(year);
  const empty = fresh === null || (fresh.holidays.length === 0 && fresh.workdays.length === 0);
  if (empty) {
    // 抓取失败或该年数据未公布:优先回退 KV 中已有的旧数据,不要用空结果覆盖
    const fromKv = await readKv(env, year);
    if (fromKv !== undefined) {
      memPut(year, fromKv);
      return { data: fromKv, source: "kv" };
    }
    memPut(year, null);
    return { data: null, source: null };
  }

  memPut(year, fresh);
  await writeKv(env, fresh);
  return { data: fresh, source: "baidu" };
}

async function readKv(env: Env, year: number): Promise<YearHolidayData | undefined> {
  if (!env.HOLIDAY_KV) return undefined;
  try {
    const data = await env.HOLIDAY_KV.get<YearHolidayData>(kvKey(year), "json");
    if (!data) return undefined;
    // 空数据没有缓存价值,视为未命中
    if (data.holidays.length === 0 && data.workdays.length === 0) return undefined;
    return data;
  } catch {
    return undefined;
  }
}

async function writeKv(env: Env, data: YearHolidayData): Promise<void> {
  if (!env.HOLIDAY_KV) return;
  try {
    // KV 最终一致 + 每天写入次数极少,直接覆盖即可
    await env.HOLIDAY_KV.put(kvKey(data.year), JSON.stringify(data));
  } catch {
    // 写失败不影响本次响应,下次请求会重试
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
