# 法定节假日 API —— Cloudflare Workers 版

本目录是仓库根目录 Java(Spring Boot + MySQL)版本的 **Cloudflare Workers 迁移版**,完全不依赖服务器:HTTP 服务、定时任务、数据缓存全部跑在 Cloudflare 边缘网络上,免费套餐即可长期运行。

## 与 Java 版的对应关系

| Java 版 | Workers 版 |
| --- | --- |
| `HolidayController` | `src/index.ts`(fetch handler 路由) |
| `ScheduledService` 定时任务 | `src/index.ts` scheduled handler + `wrangler.jsonc` 的 cron |
| MySQL `holiday_date` 表 | Workers KV,按年存 `holiday:{year}` |
| `GetByBaidu` 抓百度数据 | `src/baidu.ts` |
| `DateUtil` / 北京时间 | `src/beijing.ts` |
| `HolidayDateServiceImpl` 状态逻辑 | `src/store.ts` |

数据模型比 SQL 简单:一年只有两份列表(法定节假日 `holidays`、需要补班的周末 `workdays`),普通工作日/周末由星期几直接推导,无需逐日入库。

## 接口(与 Java 版兼容)

```
GET /holiday?date=2026-05-01   # 也支持 2026-05、2026,返回 JSON 数组
GET /holiday/today             # 纯文本 "工作" / "休息"
GET /holiday/tomorrow
GET /holiday/yesterday
GET /holiday/update            # 手动刷新当年+次年数据,需密钥
```

`status` 含义不变:0 普通工作日、1 周末双休日、2 需要补班的工作日、3 法定节假日。

与 Java 版的细微差异:

* 参数非法时返回 400 + JSON 错误信息(Java 版返回 `null`);查不到数据的年份仍返回 `[]`。
* 响应头带 `X-Data-Source`(`memory` / `kv` / `baidu` / `heuristic`),便于排查数据来自哪一层。
* `/holiday/update` 需要 `UPDATE_KEY`(原版因防滥用直接注释掉了);支持 `?key=` 或 `Authorization: Bearer` 两种传法。
* 次年安排未公布时,`/holiday` 返回 `[]`,`today` 系列按普通周末规则回答,不会报错。
* 所有响应带 CORS 头,可直接被网页前端调用。

## 本地开发

```bash
npm install
npm run dev          # http://localhost:8787,KV 用本地模拟,无需登录
```

本地测试 update 接口:复制 `.dev.vars.example` 为 `.dev.vars`。

## 部署到 Cloudflare(免费账号即可)

```bash
npx wrangler login                              # 1. 登录
npx wrangler kv namespace create HOLIDAY_KV     # 2. 创建 KV
#    把输出中的 id 填入 wrangler.jsonc 的 kv_namespaces[0].id
npx wrangler secret put UPDATE_KEY              # 3. 设置 update 接口密钥(可选)
npm run deploy                                  # 4. 部署
```

部署完成后即可通过 `https://holiday-api.<你的子域>.workers.dev/holiday` 访问。

### 绑定自有域名(可选)

在 Cloudflare 控制台 → Workers & Pages → holiday-api → Settings → Domains & Routes 添加 Custom Domain(如 `api.haoshenqi.top`),域名需已托管在该 Cloudflare 账号下。绑定后原有调用方无需任何改动。

## 运行机制

* **读取**:内存缓存(1 小时)→ KV → 百度 opendata 接口,逐层递进;数据进 KV 后,日常请求零出网。
* **定时任务**:每天北京时间 01:00(`0 17 * * *` UTC)执行,逻辑承接原 `ScheduledService`:
  1. 当年数据缺失时自动补抓(首次部署无需手工初始化);
  2. 今天是节假日最后一天时重抓当年数据,兜底国务院临时调整;
  3. 每月 25 日刷新当年数据;11、12 月同时抓取次年数据。
* **成本**:免费版 KV 每天 10 万次读、1000 次写,本服务日常每天写入不超过 2 次,完全够用。
