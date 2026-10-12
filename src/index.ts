/**
 * 法定节假日查询 API —— Cloudflare Workers 版。
 *
 * 由 Java(Spring Boot + MySQL)版迁移而来,保持接口路径与响应格式兼容:
 *   GET /holiday?date=2026-05-01   查一天/一月/一年,返回 JSON 数组
 *   GET /holiday/today|tomorrow|yesterday  纯文本 "工作" / "休息"
 *   GET /holiday/update            手动刷新当年+次年数据(需 UPDATE_KEY)
 *
 * 状态值:0 普通工作日 1 周末双休 2 需要补班的工作日 3 法定节假日
 */
import {
  enumerateDates,
  isValidDate,
  splitDate,
  todayBeijing,
  tomorrowBeijing,
  weekdayOf,
  yesterdayBeijing,
} from "./beijing";
import { toStatusSets, statusOf, getYearData, invalidateMem, type Env, type YearResult } from "./store";
import type { StatusSets } from "./store";

const CORS_HEADERS: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, OPTIONS",
  "Access-Control-Allow-Headers": "Authorization",
};

function json(data: unknown, status = 200, extra: Record<string, string> = {}): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      ...CORS_HEADERS,
      ...extra,
    },
  });
}

function text(body: string, status = 200, extra: Record<string, string> = {}): Response {
  return new Response(body, {
    status,
    headers: {
      "Content-Type": "text/plain; charset=utf-8",
      "Cache-Control": "no-store",
      ...CORS_HEADERS,
      ...extra,
    },
  });
}

/** HolidayDate 的 JSON 字段顺序与 Java 版保持一致:date, year, month, day, status */
function toRow(dateStr: string, status: number): Record<string, unknown> {
  const { year, month, day } = splitDate(dateStr);
  return { date: dateStr, year, month, day, status };
}

/**
 * 取某天的状态;该年无数据时:
 *  - preferWeekendHeuristic=false 返回 null(调用方再降级)
 *  - preferWeekendHeuristic=true 按普通周末规则给出 0/1
 */
async function resolveStatus(
  env: Env,
  dateStr: string,
  preferWeekendHeuristic: boolean
): Promise<{ status: 0 | 1 | 2 | 3 | null; source: YearResult["source"]; sets: StatusSets | null }> {
  const year = Number(dateStr.slice(0, 4));
  const { data, source } = await getYearData(env, year);
  const sets = data ? toStatusSets(data) : null;
  let status = statusOf(dateStr, sets);
  if (status === null && preferWeekendHeuristic) {
    const wd = weekdayOf(dateStr);
    status = wd === 0 || wd === 6 ? 1 : 0;
  }
  return { status, source, sets };
}

/** GET /holiday?date= */
async function handleHolidayList(request: Request, env: Env, url: URL): Promise<Response> {
  const raw = (url.searchParams.get("date") ?? "").trim();
  // 原版对空参数按今天处理
  const target = raw.length > 0 ? raw : todayBeijing();

  const m = /^(\d{4})(?:-(\d{1,2})(?:-(\d{1,2}))?)?$/.exec(target);
  if (!m) {
    return json({ error: "date 参数格式应为 yyyy、yyyy-MM 或 yyyy-MM-dd,例如 2026-05-01" }, 400);
  }
  const year = Number(m[1]);
  const month = m[2] !== undefined ? Number(m[2]) : undefined;
  const day = m[3] !== undefined ? Number(m[3]) : undefined;

  if (month !== undefined && (month < 1 || month > 12)) {
    return json({ error: "月份应在 1-12 之间" }, 400);
  }
  if (month !== undefined && day !== undefined && !isValidDate(year, month, day)) {
    return json({ error: `日期不存在:${target}` }, 400);
  }

  const { data, source } = await getYearData(env, year);
  const sets = data ? toStatusSets(data) : null;

  let rows: Record<string, unknown>[];
  if (day !== undefined) {
    const status = statusOf(target, sets);
    rows = status === null ? [] : [toRow(target, status)];
  } else if (sets === null) {
    // 该年数据未公布/抓取失败,返回空数组(与原版查不到数据时一致)
    rows = [];
  } else {
    rows = enumerateDates(year, month).map((d) => toRow(d, statusOf(d, sets)!));
  }

  return json(rows, 200, {
    "Cache-Control": "public, max-age=3600",
    "X-Data-Source": source ?? "none",
  });
}

/** GET /holiday/today|tomorrow|yesterday,返回纯文本 工作/休息 */
async function handleDayText(env: Env, dateStr: string): Promise<Response> {
  const { status, source } = await resolveStatus(env, dateStr, true);
  const finalStatus = status ?? 0;
  const body = finalStatus === 1 || finalStatus === 3 ? "休息" : "工作";
  return text(body, 200, { "X-Data-Source": source ?? "heuristic" });
}

/** 恒定时间比较密钥,避免 timing 攻击 */
async function safeEqual(a: string, b: string): Promise<boolean> {
  const encoder = new TextEncoder();
  const [da, db] = await Promise.all([
    crypto.subtle.digest("SHA-256", encoder.encode(a)),
    crypto.subtle.digest("SHA-256", encoder.encode(b)),
  ]);
  const xa = new Uint8Array(da);
  const xb = new Uint8Array(db);
  let diff = 0;
  for (let i = 0; i < xa.length; i++) diff |= xa[i] ^ xb[i];
  return diff === 0;
}

/** GET /holiday/update:手动刷新当年和次年的数据 */
async function handleUpdate(request: Request, env: Env, url: URL): Promise<Response> {
  if (!env.UPDATE_KEY) {
    return text("update 接口未启用:请先执行 npx wrangler secret put UPDATE_KEY", 403);
  }
  const auth = request.headers.get("Authorization");
  const given = url.searchParams.get("key") ?? (auth?.startsWith("Bearer ") ? auth.slice(7) : "");
  if (!given || !(await safeEqual(given, env.UPDATE_KEY))) {
    return text("forbidden", 403);
  }

  const nowBeijing = new Date(Date.now() + 8 * 60 * 60 * 1000);
  const currentYear = nowBeijing.getUTCFullYear();
  const refreshed: number[] = [];
  for (const year of [currentYear, currentYear + 1]) {
    invalidateMem(year);
    const { data } = await getYearData(env, year, true);
    if (data) refreshed.push(year);
  }
  return text(`success! refreshed: ${refreshed.join(", ") || "none"}`);
}

function handleOptions(): Response {
  return new Response(null, { status: 204, headers: CORS_HEADERS });
}

/** GET /:接口说明 */
function handleIndex(): Response {
  return json({
    name: "holiday (Cloudflare Workers)",
    endpoints: {
      "/holiday?date=yyyy-MM-dd": "查一天,也支持 yyyy-MM / yyyy,返回 JSON 数组",
      "/holiday/today": '今天上班吗,纯文本 "工作"/"休息"',
      "/holiday/tomorrow": "明天上班吗",
      "/holiday/yesterday": "昨天上班吗",
      "/holiday/update": "手动刷新当年+次年数据,需 UPDATE_KEY",
    },
    status: "0 普通工作日 1 周末双休日 2 需要补班的工作日 3 法定节假日",
  });
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === "OPTIONS") return handleOptions();
    if (request.method !== "GET" && request.method !== "HEAD") {
      return json({ error: "仅支持 GET" }, 405);
    }

    const url = new URL(request.url);
    // 去掉末尾斜杠,/holiday/ 与 /holiday 等价
    const path = url.pathname.length > 1 ? url.pathname.replace(/\/+$/, "") : url.pathname;

    try {
      if (path === "/") return handleIndex();
      if (path === "/holiday") return await handleHolidayList(request, env, url);
      if (path === "/holiday/today") return await handleDayText(env, todayBeijing());
      if (path === "/holiday/tomorrow") return await handleDayText(env, tomorrowBeijing());
      if (path === "/holiday/yesterday") return await handleDayText(env, yesterdayBeijing());
      if (path === "/holiday/update") return await handleUpdate(request, env, url);
      return json({ error: `未知路径:${path}` }, 404);
    } catch (e) {
      return json({ error: "服务内部错误", detail: String(e) }, 500);
    }
  },

  /**
   * 定时任务,每天北京时间 01:00(cron: 0 17 * * *)。
   * 对应原 Java 版 ScheduledService:
   *   1. 若数据库为空,补抓当年数据(首次部署自动初始化);
   *   2. 若今天是节假日的最后一天,重新抓当年数据,兜底处理国务院临时调整;
   *   3. 每月 25 日刷新当年数据;11、12 月同时抓取次年数据。
   */
  async scheduled(_controller: ScheduledController, env: Env): Promise<void> {
    const nowBeijing = new Date(Date.now() + 8 * 60 * 60 * 1000);
    const year = nowBeijing.getUTCFullYear();
    const month = nowBeijing.getUTCMonth() + 1;
    const day = nowBeijing.getUTCDate();

    const current = await getYearData(env, year);
    if (current.data) {
      const sets = toStatusSets(current.data);
      const todayStr = todayBeijing();
      const tomorrowStr = tomorrowBeijing();
      const todayStatus = statusOf(todayStr, sets);
      let tomorrowStatus = statusOf(tomorrowStr, sets);
      if (tomorrowStatus === null && Number(tomorrowStr.slice(0, 4)) !== year) {
        // 跨年:明天属于下一年,需查下一年的数据
        const next = await getYearData(env, Number(tomorrowStr.slice(0, 4)));
        tomorrowStatus = next.data ? statusOf(tomorrowStr, toStatusSets(next.data)) : null;
      }
      if (todayStatus === 3 && tomorrowStatus !== 3) {
        // 今天是节假日最后一天,重新抓当年数据
        invalidateMem(year);
        await getYearData(env, year, true);
      }
    }

    if (day === 25) {
      invalidateMem(year);
      await getYearData(env, year, true);
      if (month >= 11) {
        invalidateMem(year + 1);
        await getYearData(env, year + 1, true);
      }
    }
  },
};
