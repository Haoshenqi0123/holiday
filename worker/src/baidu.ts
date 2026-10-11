/**
 * 从百度 opendata 抓取全年法定节假日(对应 Java 版 GetByBaidu)。
 *
 * 接口返回 JSONP:jsonp_xxx({"Result":[{"DisplayData":{"resultData":{"tplData":{
 *   "data":[{"holidayList":["2026-01-01",...],"workDay":"2026-01-04,2026-02-28",...},...]
 * }}}]}})
 * holidayList -> 法定节假日(status 3)
 * workDay     -> 需要补班的周末(status 2),逗号分隔,可能为空字符串
 */

/** 一年的节假日数据,存 KV / 内存缓存都是这个结构 */
export interface YearHolidayData {
  year: number;
  /** 法定节假日,yyyy-MM-dd */
  holidays: string[];
  /** 需要补班的工作日,yyyy-MM-dd */
  workdays: string[];
  /** 抓取时间(ISO) */
  updatedAt: string;
}

const BAIDU_URL =
  "https://opendata.baidu.com/data/inner?tn=reserved_all_res_tn&format=json" +
  "&resource_id=52109&query=%E6%B3%95%E5%AE%9A%E8%8A%82%E5%81%87%E6%97%A5" +
  "&apiType=holidayData&cb=jsonp_1732947987298_87125";
const JSONP_CB = "jsonp_1732947987298_87125";

interface BaiduTplItem {
  holidayList?: string[];
  workDay?: string;
}

/**
 * 抓取并解析指定年份的数据。
 * 返回 null 表示请求失败(网络错误/响应格式异常);
 * 返回 holidays 和 workdays 都为空表示抓取成功但该年数据尚未公布。
 */
export async function fetchYearFromBaidu(year: number): Promise<YearHolidayData | null> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 10_000);
  try {
    const resp = await fetch(`${BAIDU_URL}&year=${year}`, {
      signal: controller.signal,
      headers: {
        "User-Agent":
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.3",
      },
    });
    if (!resp.ok) return null;
    return parseJsonp(await resp.text(), year);
  } catch {
    return null;
  } finally {
    clearTimeout(timer);
  }
}

/** 剥掉 JSONP 外壳并解析,任何一步不符合预期都返回 null */
export function parseJsonp(text: string, year: number): YearHolidayData | null {
  const start = text.indexOf("{");
  const end = text.lastIndexOf("}");
  if (start < 0 || end <= start) return null;

  let root: any;
  try {
    root = JSON.parse(text.slice(start, end + 1));
  } catch {
    return null;
  }

  const items: BaiduTplItem[] | undefined =
    root?.Result?.[0]?.DisplayData?.resultData?.tplData?.data;
  if (!Array.isArray(items)) return null;

  const holidays = new Set<string>();
  const workdays = new Set<string>();
  for (const item of items) {
    if (Array.isArray(item?.holidayList)) {
      for (const d of item.holidayList) {
        if (typeof d === "string" && d.startsWith(`${year}-`)) holidays.add(d);
      }
    }
    if (typeof item?.workDay === "string" && item.workDay.length > 0) {
      for (const d of item.workDay.split(",")) {
        const t = d.trim();
        if (t.startsWith(`${year}-`)) workdays.add(t);
      }
    }
  }
  return {
    year,
    holidays: [...holidays].sort(),
    workdays: [...workdays].sort(),
    updatedAt: new Date().toISOString(),
  };
}
