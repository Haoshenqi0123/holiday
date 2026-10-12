# 部署到 Cloudflare Workers

本文只讲部署，接口定义和代码结构见 [README.md](README.md)。全程不需要服务器，免费账号即可。

## 前置条件

* 一个 Cloudflare 账号（免费套餐够用）。
* 你自己的这个仓库的 fork 或副本，代码在某个分支上（本仓库是 `cloudflare-workers`）。
* 一个属于该账号的 KV 命名空间 —— **必需**，第 3 步现场创建。

## 配置项

仓库里不含任何与具体账号相关的值，fork 之后不必改代码，但有一个变量非配不可。

| 变量 | 必需 | 作用 | 不设置时 |
| --- | --- | --- | --- |
| `HOLIDAY_KV_ID` | **是** | 你自己账号下 KV 命名空间的 Namespace ID（32 位十六进制），按年缓存节假日数据 | **构建阶段直接失败**（`link-kv.mjs` 退出码 1），部署不上去 |
| `UPDATE_KEY` | 否 | 放开 `GET /holiday/update` 手动刷新接口的口令 | 该接口固定返回 403，数据仍由每天 01:00（北京时间）的定时任务刷新 |

**KV 为什么是硬依赖**：没有缓存层时每个请求都会出网抓百度，而百度按调用方 IP 频控，入口被刷几次就会连数据源一起封掉。所以 `src/store.ts` 在缺少 KV 绑定时抛 `MissingKVError`，数据接口返回 **503** 并拒绝回源，而不是静默降级 —— 后者会让「KV 其实没配上」看起来一切正常（本项目踩过这个坑）。

`HOLIDAY_KV_ID` 的生效方式：`wrangler.jsonc` 只是模板，`npm run deploy` 先执行 `scripts/link-kv.mjs`，按该变量生成 `wrangler.gen.jsonc`（已 gitignore，id 因此不进版本库），再用它部署。之所以生成而非插值 —— 实测 wrangler 4.x 不支持配置里写 `${VAR}`，`deploy` 也没有 `--kv` 参数。本地 `npm run dev` 是唯一例外：wrangler 的 KV 是本地模拟、不校验 id，未设置时脚本自动填占位 id。

## 方式一：连 GitHub 仓库，push 自动部署（推荐）

### 1. 创建应用

dashboard → **Workers & Pages** → **Create application** → 选 **Pages/Workers** 里的仓库导入（Connect to Git），选中你的 `holiday` 仓库。

### 2. 填表单

| 字段 | 填什么 | 说明 |
| --- | --- | --- |
| 项目名称 | 你希望 Worker 叫的名字（如 `holiday`） | **决定访问域名** `<名字>.<子域>.workers.dev`。git 集成下 CI 会用这个名称覆盖 `wrangler.jsonc` 里的 `name`，两者不一致时构建日志会报 `Failed to match Worker name` 并尝试向你的仓库开 PR 修正 —— 所以建好项目后就把仓库里的 `name` 改成同一个 |
| **生产分支** | **`cloudflare-workers`**（你放 Worker 代码的那个分支） | 默认取仓库默认分支。本仓库默认分支 `master` 是 Java 版，根目录没有 `package.json`，不改必挂 |
| 构建命令 | 留空 | wrangler 自带 esbuild 打包 TS，依赖由平台自动 `npm clean-install` |
| **部署命令** | **`npm run deploy`** | 填 `npx wrangler deploy` 会跳过 `scripts/link-kv.mjs`，部署出来的 Worker 没有 KV 绑定，所有数据接口返回 **503**（这是故意的，见上文「KV 为什么是硬依赖」）—— 务必核对构建日志里那一行是 `Executing user deploy command: npm run deploy` |
| 根目录 | 留空 | 代码已在仓库根目录 |

### 3. 绑定 KV（必需，不配构建会失败）

先建命名空间：dashboard → **Storage & Databases** → **KV** → Add，名字随意（如 `holiday-data-kv`），创建后进 **Settings 标签页**复制 **Namespace ID**（32 位十六进制；别复制成名字）。注意必须和 Worker 在同一个 Cloudflare 账号下。

然后加到**构建变量**：项目 → Settings → Build → **Variables and secrets**（Production 标签）→ Add，名称 `HOLIDAY_KV_ID`，值填 Namespace ID。官方文档明确 "Build variables will not be accessible at runtime" —— 我们要的正是构建时，别加到 Settings → Variables and Secrets（那是 Worker 运行时，构建容器读不到）。

预览部署读的是 **Previews Base** 标签下的同名设置，不单独加的话预览构建会因为缺 `HOLIDAY_KV_ID` 直接失败 —— 这比让它以无 KV 形态上线更安全。

**不要用 dashboard 的 Bindings 面板代替这一步**：手工加的 `HOLIDAY_KV` 绑定会被下一次 `wrangler deploy` 用配置文件里的绑定集（当前是空的 `"kv_namespaces": []`）覆盖掉，只有落在配置生成流程里的才是持久的。

改完构建变量**不会自动重建**，去 Deployments 点 `Retry deployment`。

### 4. 可选：放开手动刷新接口

项目 → Settings → **Variables and Secrets**（运行时那个）→ Add **secret**，名称 `UPDATE_KEY`，值自己定一个强口令。之后：

```
GET /holiday/update?key=你的口令
```

### 5. 部署并验证

点 Deploy，或直接向生产分支 push 一次触发自建。构建日志里按顺序核对两行：

```
Executing user deploy command: npm run deploy          ← 不是 npx wrangler deploy
已绑定 KV -> wrangler.gen.jsonc                          ← 若构建直接失败并打「必须设置 HOLIDAY_KV_ID」，说明变量没读到
```

访问域名取自项目名（本仓库 `wrangler.jsonc` 里也是 `holiday`），即 `https://holiday.<你的子域>.workers.dev`：

```bash
curl -i "https://holiday.<你的子域>.workers.dev/holiday?date=2026-10-01"   # 应返回 status:3，且带 X-Data-Source 头
curl "https://holiday.<你的子域>.workers.dev/holiday/today"                 # 工作 / 休息
curl "https://holiday.<你的子域>.workers.dev/"                              # 接口自述
```

`X-Data-Source` 表示这次数据来自哪一层：`kv` / `baidu`（现抓）/ `memory` / `heuristic`（按星期推导）。刚部署完第一次通常是 `baidu`，**再请求一次应变为 `kv`**（同一 isolate 内会是 `memory`）。如果数据接口返回 **503** 且错误里提到 `HOLIDAY_KV`，就是绑定没挂上 —— 这是设计行为，Worker 拒绝在无缓存形态下回源百度。

> `*.workers.dev` 在中国大陆普遍被 DNS 污染（解析到非 Cloudflare 地址、连接超时），从国内验证需要代理，或直接绑下面的自有域名。

### 绑定自有域名（可选，国内可达的唯一办法）

Workers & Pages → holiday → Settings → **Domains & Routes** → Add → Custom Domain（如 `api.haoshenqi.top`），域名需已托管在同一 Cloudflare 账号下。绑定后原有调用方无需任何改动。

## 方式二：本地 wrangler 部署

不想用 git 集成时，从本机直接部署当前检出的代码：

```bash
npm install
npx wrangler login
npx wrangler kv namespace create HOLIDAY_KV           # 输出里的 id 即 HOLIDAY_KV_ID
HOLIDAY_KV_ID=<id> npm run deploy                      # 不给这个变量，link-kv.mjs 会以退出码 1 拒绝部署
npx wrangler secret put UPDATE_KEY                     # 可选
```

## 故障排查

| 现象 | 原因与处理 |
| --- | --- |
| 构建报找不到 `wrangler.jsonc` / `package.json` | 生产分支填错了，导到了 Java 版所在的 `master` |
| 日志 `Executing user deploy command: npx wrangler deploy` | 部署命令没走 npm script，`link-kv.mjs` 从未执行 → 部署出来的 Worker 没有 KV 绑定，数据接口全部 503。改成 `npm run deploy` 后 retry |
| 构建失败，日志打 `必须设置 HOLIDAY_KV_ID` | 脚本跑了但没读到变量：变量名拼错、加到了运行时变量面板（构建容器读不到）、或只加在 Previews Base 而这次是 Production |
| 数据接口返回 503、`error` 里提到 `HOLIDAY_KV` | 就是上面两种情况的结果：Worker 主动拒绝在无缓存形态下回源百度。`/` 自述页仍返回 200，方便你区分「服务活着」和「配置缺东西」 |
| 在 dashboard 的 Bindings 面板手工加了 KV，几分钟后又没了 | 下一次 `wrangler deploy` 会用仓库配置里的绑定集（空）覆盖它。只有走 `HOLIDAY_KV_ID` + 构建时生成才是持久的 |
| 日志 `Failed to match Worker name ... Overriding using the CI provided Worker name` | `wrangler.jsonc` 的 `name` 与 CF 项目名不一致。CI 以项目名为准并会尝试开 PR 修正，把仓库里的 `name` 改成项目名即可 |
| `HOLIDAY_KV_ID 应是 KV 命名空间的 Namespace ID(32 位十六进制)，当前长度 N。` | 复制了错的值（最常见：复制了 KV 的**名字**而非 ID，或带了空格/换行）。脚本会在 deploy 之前以退出码 1 拦下，构建直接失败 |
| `/holiday/update` 返回 403 | 未配 `UPDATE_KEY`（这是预期行为），或口令不对 |
| 查次年返回 `[]` | 次年安排尚未公布，属正常；公布后由定时任务或手动刷新接口取到 |
| 某年一直返回 `[]` | 先看 KV 里有没有 `holiday:{year}` 且 `empty: true` —— 那是负标记（未公布的将来年份存 1 小时、已过去的年份存 30 天、抓取失败只存 10 分钟），**删掉这个键就能立刻重试**。若 KV 里是非空数据却仍返回 `[]`，去 Workers 的 Logs 看，`store.ts` 会把 KV 读写异常和「过去日期与缓存不一致」都打出来 |
| 国内浏览器打不开 `*.workers.dev` | 该后缀在大陆普遍被 DNS 污染（解析到非 Cloudflare 地址后超时），不是服务故障。绑自有域名，或用代理验证 |
