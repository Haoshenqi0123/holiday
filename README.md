# 法定节假日查询 API

> **⚠️ 服务迁移公告 · 2026-10-12**
>
> 老接口 `http://api.haoshenqi.top/holiday` 由早期的 Java + MySQL 服务提供，**将于 2026-11-03 下线**，届时该地址不再保证任何响应。
>
> 本仓库已重写为 Cloudflare Workers 版本，不需要服务器、不需要数据库。**请 fork 本仓库并按 [DEPLOY.md](DEPLOY.md) 部署到你自己账号下的 Worker** —— 免费套餐即可，全程约十分钟：建一个 KV 命名空间、把它填进一个构建变量，其余什么都不用改（仓库里不含任何账号专属值）。
>
> 我的实例在 `https://holiday.magichouse.top`，仅作示例与对照，**不承诺可用性和长期存在**，请别把它写成生产依赖。

HTTP 服务、定时抓取、数据缓存全部跑在 Cloudflare Workers 边缘网络上,数据源是百度 opendata 接口,免费套餐即可长期运行。

## 接口

```
GET /holiday?date=2026-05-01   # 也支持 2026-05、2026,返回 JSON 数组
GET /holiday/today             # 纯文本 "工作" / "休息"
GET /holiday/tomorrow
GET /holiday/yesterday
GET /holiday/update            # 手动刷新当年 + 次年数据,需密钥
```

响应示例:

```json
[{ "date": "2026-05-01", "year": 2026, "month": 5, "day": 1, "status": 3 }]
```

`status` 含义:0 普通工作日、1 周末双休日、2 需要补班的工作日、3 法定节假日。

其他行为约定:

* 参数非法返回 400 + JSON 错误信息;该年数据未公布时 `/holiday` 返回 `[]`,`today` 系列按普通周末规则回答,不会报错。
* 响应头 `X-Data-Source` 标明数据来源:`memory`(内存缓存)/ `kv` / `baidu`(现抓)/ `heuristic`(按星期推导)。
* 所有响应带 CORS 头,网页前端可直接调用。
* `/holiday/update` 需要 `UPDATE_KEY`,支持 `?key=` 或 `Authorization: Bearer` 两种传法;未配置密钥时该接口始终返回 403。
* Worker 强制依赖 KV:没有 `HOLIDAY_KV` 绑定时数据接口返回 **503** 并拒绝回源百度(`/` 自述页仍返回 200,便于区分「服务活着」和「配置缺东西」)。

## 代码结构

| 文件 | 职责 |
| --- | --- |
| `src/index.ts` | fetch 路由 + 每天一次的定时任务 |
| `src/baidu.ts` | 抓百度 opendata,解析节假日 / 补班日 |
| `src/store.ts` | 内存缓存 + KV 读写,状态计算 |
| `src/beijing.ts` | 北京时间与日期工具 |
| `scripts/link-kv.mjs` | 按 `HOLIDAY_KV_ID` 生成本次部署用的 `wrangler.gen.jsonc` |
| `wrangler.jsonc` | 配置模板:Worker 名、cron;不含 KV id |

数据模型比建表简单:一年只存两份列表(法定节假日 `holidays`、需要补班的周末 `workdays`),键名 `holiday:{year}`;普通工作日/周末由星期几直接推导,无需逐日入库。

## 本地开发

```bash
npm install
cp .dev.vars.example .dev.vars          # 想在本地测 update 接口
npm run dev                             # http://localhost:8787,无需登录
```

dev 不需要真的 KV:wrangler 用的是本地模拟,`scripts/link-kv.mjs --dev` 在未设置 `HOLIDAY_KV_ID` 时自动填占位 id。想连自己的 namespace 测就 `HOLIDAY_KV_ID=<id> npm run dev`。

## 部署

完整步骤见 **[DEPLOY.md](DEPLOY.md)**:dashboard git 集成(表单逐项取值、生产分支那个坑)、本地 `wrangler` 部署、`HOLIDAY_KV_ID`(必需)与 `UPDATE_KEY`(可选)的配法、访问验证、故障排查。

`HOLIDAY_KV_ID` 是硬要求,不配构建直接失败 —— 无缓存形态下每个请求都会回源抓百度,而百度按调用方 IP 频控,入口被刷几次就会连数据源一起封掉。之所以用「构建时生成 `wrangler.gen.jsonc`」来注入而不是写死在仓库里:实测 wrangler 4.x 不支持配置内 `${VAR}` 插值、`deploy` 也没有 `--kv` 参数;而把某个账号的 namespace id 提交进仓库,fork 者要么改代码,要么把自己的数据写进别人的 KV。

## 运行机制

* **读取**:内存缓存(1 小时)→ KV → 百度 opendata 接口,逐层递进;数据进 KV 后,日常请求零出网。
* **空结果也进 KV**:某年确实暂无数据(通常是次年安排未公布)时,写一条带 TTL 的负标记,让「别再去抓了」这个结论跨 isolate 生效 —— 只有内存负缓存的话,每个新实例每 10 分钟就会重复出网。TTL 分三档:抓取失败 10 分钟、未公布的将来年份 1 小时、已过去的年份 30 天。
* **历史不可变**:已过去的日期永不被新抓取结果改写。法定节假日不会追溯调整过去的日期,两边不一致时更可能是百度数据出了问题 —— 此时按缓存保留并打 `console.error`,在 Workers 的 Logs 里可见。往年数据一旦入库就是终态。
* **定时任务**:每天北京时间 01:00(`0 17 * * *` UTC)执行:
  1. 当年数据缺失时自动补抓(首次部署无需手工初始化);
  2. 今天是节假日最后一天时重抓当年数据,兜底国务院临时调整;
  3. 每月 25 日刷新当年数据;11、12 月同时抓取次年数据。
* **成本**:免费版 KV 每天 10 万次读、1000 次写。写入量按上面的定时任务节奏算约 20 次/**年**,日常请求只读不写,额度用不到零头。

## 从 Java 版迁移过来

| | Java 版（2026-10 之前） | Workers 版（现在） |
| --- | --- | --- |
| 运行时 | 自建服务器 + Docker + MySQL | Cloudflare Workers 边缘，无服务器 |
| 存储 | `holiday_date` 表逐日入库 | KV 按年存两份列表，键名 `holiday:{year}` |
| 抓取 | `GetByBaidu` | `src/baidu.ts`，同一数据源 |
| 定时任务 | Spring `@Scheduled` | cron `0 17 * * *`（北京时间 01:00），逻辑等价 |

接口路径与响应字段保持兼容，但有几处行为差异，老客户端要留意：

* 只提供 `https://`，`http://api.haoshenqi.top` 那个明文入口随之下线。
* 参数非法时 Java 版返回 `null`（`HolidayController` 里 catch 后直接 `return null`），现在返回 **400 + JSON 错误信息**。
* `/holiday/update` 在 Java 版里为防滥用被整段注释掉了，这里恢复开放，但需要 `UPDATE_KEY`；不配置该变量时该接口固定返回 403。

新增能力：所有响应带 CORS 头（网页前端可直接调用）、`X-Data-Source` 头标明本次数据来自哪一层。

**为什么不再提供一个公共实例给大家共用**：无服务器架构下不存在「我替你扛流量」的中心节点 —— Workers 与 KV 的免费额度按账号计算，公共入口一旦被刷，所有使用者会同时拿到超额错误（超额只会报错、不会产生费用，所以也无法靠付费兜底）。各自账号内部署反而互不影响，这就是本仓库做到 fork 后零改动的原因。

## 打赏

用 OSS 原始域名而非 `oss.haoshenqi.top`：后者的证书 2024-06-23 已过期，浏览器会直接拒绝加载图片。

<img src="https://magichouse-pub-read.oss-cn-hangzhou.aliyuncs.com/pay/IMG_2690.JPG" width = "400" height = "400" alt="支付宝" align=center>

<img src="https://magichouse-pub-read.oss-cn-hangzhou.aliyuncs.com/pay/IMG_2692.JPG" width = "400" height = "400" alt="微信" align=center>
