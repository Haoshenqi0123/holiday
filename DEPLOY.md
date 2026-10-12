# 部署到 Cloudflare Workers

本文只讲部署，接口定义和代码结构见 [README.md](README.md)。全程不需要服务器，免费账号即可。

## 前置条件

* 一个 Cloudflare 账号（免费套餐够用）。
* 你自己的这个仓库的 fork 或副本，代码在某个分支上（本仓库是 `cloudflare-workers`）。

## 配置项：两个可选环境变量

仓库里不含任何与具体账号相关的值，fork 之后不必改代码。

| 变量 | 作用 | 不设置时的行为 |
| --- | --- | --- |
| `HOLIDAY_KV_ID` | 你自己账号下 KV 命名空间的 Namespace ID（32 位十六进制），用于按年缓存节假日数据 | 不绑定 KV，退化为「内存缓存 + 现抓百度」。接口行为不变，只是失去跨实例缓存，以及百度抓取失败时回退旧数据的能力 |
| `UPDATE_KEY` | 放开 `GET /holiday/update` 手动刷新接口的口令 | 该接口固定返回 403，数据仍由每天 01:00（北京时间）的定时任务刷新 |

`HOLIDAY_KV_ID` 的生效方式值得说明一句：`wrangler.jsonc` 只是模板，`npm run dev` / `npm run deploy` 会先执行 `scripts/link-kv.mjs`，按该变量生成 `wrangler.gen.jsonc`（已 gitignore，所以 id 不会进版本库），再用它部署。之所以要生成而不是插值 —— 实测 wrangler 4.x 不支持在配置文件里写 `${VAR}`，`deploy` 也没有 `--kv` 参数。

## 方式一：连 GitHub 仓库，push 自动部署（推荐）

### 1. 创建应用

dashboard → **Workers & Pages** → **Create application** → 选 **Pages/Workers** 里的仓库导入（Connect to Git），选中你的 `holiday` 仓库。

### 2. 填表单

| 字段 | 填什么 | 说明 |
| --- | --- | --- |
| 项目名称 | 随意 | 只是控制台里的标签，**不决定访问域名** |
| **生产分支** | **`cloudflare-workers`**（你放 Worker 代码的那个分支） | 默认取仓库默认分支。本仓库默认分支 `master` 是 Java 版，根目录没有 `package.json`，不改必挂 |
| 构建命令 | 留空 | wrangler 自带 esbuild 打包 TS，依赖由平台自动 `npm ci` |
| **部署命令** | **`npm run deploy`** | 不要填 `npx wrangler deploy` —— 那样会跳过 `scripts/link-kv.mjs`，KV 永远绑不上 |
| 根目录 | 留空 | 代码已在仓库根目录 |

### 3. 可选：绑定 KV

先建命名空间：dashboard → **Storage & Databases** → **KV** → Add，名字随意（如 `holiday-kv`），创建后在概览页复制 **Namespace ID**。注意必须和 Worker 在同一个 Cloudflare 账号下。

然后把 `HOLIDAY_KV_ID` = 这个 ID 加到该项目的变量（Variables）里，让部署命令的 shell 能读到它。

**不要用 dashboard 的 Bindings 面板代替这一步**：手工加的 `HOLIDAY_KV` 绑定会被下一次 `wrangler deploy` 用配置文件里的绑定集覆盖掉，只有落在配置生成流程里的才是持久的。

### 4. 可选：放开手动刷新接口

项目 → Settings → **Variables and Secrets** → Add **secret**，名称 `UPDATE_KEY`，值自己定一个强口令。之后：

```
GET /holiday/update?key=你的口令
```

### 5. 部署并验证

点 Deploy，或直接向生产分支 push 一次触发自建。构建日志里应能看到 `已绑定 KV -> wrangler.gen.jsonc`（若打了 `未设置 HOLIDAY_KV_ID` 说明变量没注入到部署命令环境，见下方故障排查）。

访问域名取自 `wrangler.jsonc` 的 `"name": "holiday-api"`，即 `https://holiday-api.<你的子域>.workers.dev`：

```bash
curl -i "https://holiday-api.<你的子域>.workers.dev/holiday?date=2026-10-01"   # 应返回 status:3，且带 X-Data-Source 头
curl "https://holiday-api.<你的子域>.workers.dev/holiday/today"                 # 工作 / 休息
curl "https://holiday-api.<你的子域>.workers.dev/"                              # 接口自述
```

`X-Data-Source` 表示这次数据来自哪一层：`kv` / `baidu`（现抓）/ `memory` / `heuristic`（按星期推导）。刚部署完第一次通常是 `baidu`，之后是 `kv` 或 `memory`。

### 绑定自有域名（可选）

Workers & Pages → holiday-api → Settings → **Domains & Routes** → Add → Custom Domain（如 `api.haoshenqi.top`），域名需已托管在同一 Cloudflare 账号下。绑定后原有调用方无需任何改动。

## 方式二：本地 wrangler 部署

不想用 git 集成时，从本机直接部署当前检出的代码：

```bash
npm install
npx wrangler login
npx wrangler kv namespace create HOLIDAY_KV          # 可选，输出里的 id 即 HOLIDAY_KV_ID
HOLIDAY_KV_ID=<id> npm run deploy                     # 不需要 KV 就省掉前缀
npx wrangler secret put UPDATE_KEY                    # 可选
```

## 故障排查

| 现象 | 原因与处理 |
| --- | --- |
| 构建报找不到 `wrangler.jsonc` / `package.json` | 生产分支填错了，导到了 Java 版所在的 `master` |
| 日志出现 `未设置 HOLIDAY_KV_ID` | 变量没进部署命令的 shell 环境。检查是否加在**构建/部署变量**而非仅 Worker 运行时变量上；实在取不到就用方式二本地部署一次 |
| `HOLIDAY_KV_ID 应是 KV 命名空间的 Namespace ID(32 位十六进制)，当前长度 N。` | 复制了错的值（常见：复制到 KV 的名字而不是 ID，或带了空格/换行）。脚本会在 deploy 之前以退出码 1 拦下 |
| `/holiday/update` 返回 403 | 未配 `UPDATE_KEY`（这是预期行为），或口令不对 |
| 查次年返回 `[]` | 次年安排尚未公布，属正常；公布后由定时任务或手动刷新接口取到 |
| 百度接口异常时该年返回 `[]` | 没绑 KV 就没有旧数据兜底。要这层保险就按第 3 步绑 KV |
