# 法定节假日查询 API

不依赖服务器:HTTP 服务、定时抓取、数据缓存全部跑在 Cloudflare Workers 边缘网络上,免费套餐即可长期运行。由本项目早期的 Java(Spring Boot + MySQL) 版迁移而来,数据源同样是百度 opendata 接口。

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

## 代码结构

| 文件 | 职责 |
| --- | --- |
| `src/index.ts` | fetch 路由 + 每天一次的定时任务 |
| `src/baidu.ts` | 抓百度 opendata,解析节假日 / 补班日 |
| `src/store.ts` | 内存缓存 + KV 读写,状态计算 |
| `src/beijing.ts` | 北京时间与日期工具 |
| `wrangler.jsonc` | Worker 配置:KV 绑定、cron |

数据模型比建表简单:一年只存两份列表(法定节假日 `holidays`、需要补班的周末 `workdays`),键名 `holiday:{year}`;普通工作日/周末由星期几直接推导,无需逐日入库。

## 本地开发

```bash
npm install
cp .dev.vars.example .dev.vars   # 想在本地测 update 接口
npm run dev                      # http://localhost:8787,KV 用本地模拟,无需登录
```

## 部署到 Cloudflare(免费账号即可)

```bash
npx wrangler login                              # 1. 登录
npx wrangler kv namespace create HOLIDAY_KV     # 2. 创建 KV
#    把输出中的 id 填入 wrangler.jsonc 的 kv_namespaces[0].id
npx wrangler secret put UPDATE_KEY              # 3. 设置 update 接口密钥(可选)
npm run deploy                                  # 4. 部署
```

部署后即可通过 `https://holiday-api.<你的子域>.workers.dev/holiday` 访问。

### 绑定自有域名(可选)

Cloudflare 控制台 → Workers & Pages → holiday-api → Settings → Domains & Routes 添加 Custom Domain(如 `api.haoshenqi.top`),域名需已托管在该 Cloudflare 账号下。绑定后原有调用方无需任何改动。

## 运行机制

* **读取**:内存缓存(1 小时)→ KV → 百度 opendata 接口,逐层递进;数据进 KV 后,日常请求零出网。
* **定时任务**:每天北京时间 01:00(`0 17 * * *` UTC)执行:
  1. 当年数据缺失时自动补抓(首次部署无需手工初始化);
  2. 今天是节假日最后一天时重抓当年数据,兜底国务院临时调整;
  3. 每月 25 日刷新当年数据;11、12 月同时抓取次年数据。
* **成本**:免费版 KV 每天 10 万次读、1000 次写,本服务日常每天写入不超过 2 次,完全够用。

## 打赏

<img src="https://oss.haoshenqi.top/pay/IMG_2690.JPG" width = "400" height = "400" alt="支付宝" align=center>

<img src="https://oss.haoshenqi.top/pay/IMG_2692.JPG" width = "400" height = "400" alt="微信" align=center>
