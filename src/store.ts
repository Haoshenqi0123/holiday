/**
 * 年度数据存取:隔离实例内存缓存 -> Workers KV -> 百度接口,三级递进。
 * KV 是必需依赖:没有 KV 时每次请求都会出网抓百度,百度侧有 IP 频控,
 * 因此宁可返回 503 也不允许无缓存裸奔(见 MissingKVError)。
 *
 * 两条不变量:
 *  1. 空结果也进 KV(带 TTL 的负标记),否则每个 isolate 每 10 分钟就会重复出网抓同一个空年份。
 *  2. 已过去的日期以缓存为准,永不被新抓取结果改写 —— 法定节假日不会追溯调整过去的日期,
 *     两边不一致时更可能是百度数据出问题(见 mergePreservingPast)。
 */
import { fetchYearFromBaidu, type YearHolidayData } from "./baidu";
import { todayBeijing, weekdayOf } from "./beijing";

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

/** KV 里的一行:正常年度数据,或 empty=true 的「该年暂无数据」负标记 */
type StoredYear = YearHolidayData & { empty?: boolean };

const KV_PREFIX = "holiday:";
/** 有数据的年份,内存缓存 1 小时 */
const MEM_TTL_MS = 60 * 60 * 1000;
/** 空结果的内存缓存 10 分钟;跨实例的负缓存靠 KV 里的 empty 标记 */
const NEG_TTL_MS = 10 * 60 * 1000;
/** 空标记在 KV 中的存活时长(秒) */
const TTL_FETCH_FAILED = 10 * 60;
const TTL_NOT_PUBLISHED = 60 * 60;
const TTL_PAST_YEAR = 30 * 24 * 60 * 60;

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
 * 空标记该在 KV 里存多久。
 * 抓取失败(网络/格式)只是临时故障,给最短;未公布的将来年份要等公告,给 1 小时;
 * 已经过去的年份不会再冒出新数据,给 30 天,基本不再出网。
 */
export function emptyTtlSeconds(year: number, fetchFailed: boolean): number {
  if (fetchFailed) return TTL_FETCH_FAILED;
  return year < Number(todayBeijing().slice(0, 4)) ? TTL_PAST_YEAR : TTL_NOT_PUBLISHED;
}

/**
 * 用新抓取结果更新缓存,但已过去的日期一律保留缓存值。
 * 全部日期都已过去的年份(即往年),等价于「缓存原样不动」。
 */
export function mergePreservingPast(
  fresh: YearHolidayData,
  cached: StoredYear | null | undefined,
  today: string
): YearHolidayData {
  if (!cached || cached.empty) return fresh;

  const conflicts: string[] = [];
  const mergeField = (field: "holidays" | "workdays"): string[] => {
    const cachedPast = cached[field].filter((d) => d < today);
    const freshPast = fresh[field].filter((d) => d < today);
    if (cachedPast.join() !== freshPast.join()) {
      conflicts.push(`${field} 缓存=[${cachedPast}] 百度=[${freshPast}]`);
    }
    return [...new Set([...cachedPast, ...fresh[field].filter((d) => d >= today)])].sort();
  };

  const merged: YearHolidayData = {
    year: fresh.year,
    holidays: mergeField("holidays"),
    workdays: mergeField("workdays"),
    updatedAt: fresh.updatedAt,
  };
  if (conflicts.length > 0) {
    console.error("百度返回的过去日期与缓存不一致,已按缓存保留:", fresh.year, conflicts);
  }
  return merged;
}

/**
 * 获取某年数据,forceRefresh 为 true 时强制重新抓取(用于手动更新和定时任务)。
 * 返回 data=null 表示该年暂无数据(未公布)或暂时抓取失败,调用方自行降级。
 */
export async function getYearData(
  env: Env,
  year: number,
  forceRefresh = false
): Promise<YearResult> {
  const kv = env.HOLIDAY_KV;
  if (!kv) throw new MissingKVError();

  const mem = forceRefresh ? undefined : memGet(year);
  if (mem !== undefined) {
    return { data: mem, source: mem === null ? null : "memory" };
  }

  const cached = await readKv(kv, year);
  if (cached && !forceRefresh) {
    const data = cached.empty ? null : cached;
    memPut(year, data);
    return { data, source: data ? "kv" : null };
  }

  const fresh = await fetchYearFromBaidu(year);
  const fetchFailed = fresh === null;
  const empty =
    fetchFailed || (fresh.holidays.length === 0 && fresh.workdays.length === 0);

  if (empty) {
    // 抓取失败或该年数据未公布:有旧数据就继续用它,绝不用空结果覆盖
    if (cached && !cached.empty) {
      memPut(year, cached);
      return { data: cached, source: "kv" };
    }
    if (!cached) {
      await writeEmptyKv(kv, year, fetchFailed);
    }
    memPut(year, null);
    return { data: null, source: null };
  }

  const merged = mergePreservingPast(fresh, cached, todayBeijing());
  memPut(year, merged);
  await writeKv(kv, merged);
  return { data: merged, source: "baidu" };
}

async function readKv(kv: KVNamespace, year: number): Promise<StoredYear | undefined> {
  try {
    return (await kv.get<StoredYear>(kvKey(year), "json")) ?? undefined;
  } catch (e) {
    // 读失败降级为现抓,但必须留痕:静默降级会让「KV 其实没通」看起来一切正常
    console.error("KV 读取失败,本次降级为现抓:", kvKey(year), e);
    return undefined;
  }
}

async function writeKv(kv: KVNamespace, data: YearHolidayData): Promise<void> {
  try {
    // 不写 expirationTtl:年度数据永久有效,靠每月 25 日的定时任务覆盖更新
    await kv.put(kvKey(data.year), JSON.stringify(data));
  } catch (e) {
    // 写失败不阻塞响应:缓存是优化层,不该因为它把读路径打挂
    console.error("KV 写入失败:", kvKey(data.year), e);
  }
}

/** 把「该年暂无数据」记进 KV,让负缓存跨 isolate 生效,避免重复出网抓百度 */
async function writeEmptyKv(kv: KVNamespace, year: number, fetchFailed: boolean): Promise<void> {
  const ttl = emptyTtlSeconds(year, fetchFailed);
  const marker: StoredYear = {
    year,
    holidays: [],
    workdays: [],
    updatedAt: new Date().toISOString(),
    empty: true,
  };
  try {
    await kv.put(kvKey(year), JSON.stringify(marker), { expirationTtl: ttl });
  } catch (e) {
    console.error("KV 写入空标记失败:", kvKey(year), e);
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
