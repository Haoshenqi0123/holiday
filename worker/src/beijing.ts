/**
 * 北京时间(UTC+8)日期工具。
 * Worker 的本地时区不固定(始终是 UTC),所有"今天/明天/昨天"都必须按北京时间计算。
 * 做法是给当前时间戳 +8 小时,再用 getUTC* 系列方法读取日历字段。
 */

export const CST_OFFSET_MS = 8 * 60 * 60 * 1000;

/** 返回"已平移"到北京时间的时间,对结果使用 getUTC* 方法即得北京时间的年月日 */
export function beijingNow(): Date {
  return new Date(Date.now() + CST_OFFSET_MS);
}

export function pad2(n: number): string {
  return n < 10 ? `0${n}` : String(n);
}

function beijingDateString(shifted: Date): string {
  const y = shifted.getUTCFullYear();
  const m = pad2(shifted.getUTCMonth() + 1);
  const d = pad2(shifted.getUTCDate());
  return `${y}-${m}-${d}`;
}

export function todayBeijing(): string {
  return beijingDateString(beijingNow());
}

export function tomorrowBeijing(): string {
  const d = beijingNow();
  d.setUTCDate(d.getUTCDate() + 1);
  return beijingDateString(d);
}

export function yesterdayBeijing(): string {
  const d = beijingNow();
  d.setUTCDate(d.getUTCDate() - 1);
  return beijingDateString(d);
}

/** 把 yyyy-MM-dd 拆成数字年月日 */
export function splitDate(dateStr: string): { year: number; month: number; day: number } {
  const [y, m, d] = dateStr.split("-").map(Number);
  return { year: y, month: m, day: d };
}

/** 某年 1-based 月份的天数 */
export function daysInMonth(year: number, month: number): number {
  return new Date(Date.UTC(year, month, 0)).getUTCDate();
}

export function isValidDate(year: number, month: number, day: number): boolean {
  if (month < 1 || month > 12) return false;
  return day >= 1 && day <= daysInMonth(year, month);
}

/** 星期几,0 = 周日,6 = 周六(与 Date#getUTCDay 一致) */
export function weekdayOf(dateStr: string): number {
  const { year, month, day } = splitDate(dateStr);
  return new Date(Date.UTC(year, month - 1, day)).getUTCDay();
}

export function isWeekend(dateStr: string): boolean {
  const wd = weekdayOf(dateStr);
  return wd === 0 || wd === 6;
}

/** 枚举某年全年(或某个月)的所有日期,格式 yyyy-MM-dd,升序 */
export function enumerateDates(year: number, month?: number): string[] {
  const out: string[] = [];
  const from = month ?? 1;
  const to = month ?? 12;
  for (let m = from; m <= to; m++) {
    const last = daysInMonth(year, m);
    for (let d = 1; d <= last; d++) {
      out.push(`${year}-${pad2(m)}-${pad2(d)}`);
    }
  }
  return out;
}
