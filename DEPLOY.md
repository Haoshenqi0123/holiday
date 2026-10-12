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
| 项目名称 | 你希望 Worker 叫的名字（如 `holiday`） | **决定访问域名** `<名字>.<子域>.workers.dev`。git 集成下 CI 会用这个名称覆盖 `wrangler.jsonc` 里的 `name`，两者不一致时构建日志会报 `Failed to match Worker name` 并尝试向你的仓库开 PR 修正 —— 所以建好项目后就把仓库里的 `name` 改成同一个 |
| **生产分支** | **`cloudflare-workers`**（你放 Worker 代码的那个分支） | 默认取仓库默认分支。本仓库默认分支 `master` 是 Java 版，根目录没有 `package.json`，不改必挂 |
| 构建命令 | 留空 | wrangler 自带 esbuild 打包 TS，依赖由平台自动 `npm clean-install` |
| **部署命令** | **`npm run deploy`** | 填 `npx wrangler deploy` 会跳过 `scripts/link-kv.mjs`，KV 永远绑不上，而且接口照样 200、零报错，从响应上看不出问题 —— 务必核对构建日志里那一行是 `Executing user deploy command: npm run deploy` |
| 根目录 | 留空 | 代码已在仓库根目录 |

### 3. 可选：绑定 KV

先建命名空间：dashboard → **Storage & Databases** → **KV** → Add，名字随意（如 `holiday-data-kv`），创建后进 **Settings 标签页**复制 **Namespace ID**（32 位十六进制；别复制成名字）。注意必须和 Worker 在同一个 Cloudflare 账号下。

然后加到**构建变量**：项目 → Settings → Build → **Variables and secrets**（Production 标签）→ Add，名称 `HOLIDAY_KV_ID`，值填 Namespace ID。官方文档明确 "Build variables will not be accessible at runtime" —— 我们要的正是构建时，别加到 Settings → Variables and Secrets（那是 Worker 运行时，构建容器读不到）。预览部署用的是 **Previews Base** 标签下的同名设置，需要就单独加一份。

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
已绑定 KV -> wrangler.gen.jsonc                          ← 若打的是「未设置 HOLIDAY_KV_ID」说明变量没读到
```

访问域名取自项目名（本仓库 `wrangler.jsonc` 里也是 `holiday`），即 `https://holiday.<你的子域>.workers.dev`：

```bash
curl -i "https://holiday.<你的子域>.workers.dev/holiday?date=2026-10-01"   # 应返回 status:3，且带 X-Data-Source 头
curl "https://holiday.<你的子域>.workers.dev/holiday/today"                 # 工作 / 休息
curl "https://holiday.<你的子域>.workers.dev/"                              # 接口自述
```

`X-Data-Source` 表示这次数据来自哪一层：`kv` / `baidu`（现抓）/ `memory` / `heuristic`（按星期推导）。刚部署完第一次通常是 `baidu`，**再请求一次应变为 `kv`**；两次都是 `baidu` 就说明绑定没挂上。

> `*.workers.dev` 在中国大陆普遍被 DNS 污染（解析到非 Cloudflare 地址、连接超时），从国内验证需要代理，或直接绑下面的自有域名。

### 绑定自有域名（可选，国内可达的唯一办法）

Workers & Pages → holiday → Settings → **Domains & Routes** → Add → Custom Domain（如 `api.haoshenqi.top`），域名需已托管在同一 Cloudflare 账号下。绑定后原有调用方无需任何改动。

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
| 日志 `Executing user deploy command: npx wrangler deploy` | 部署命令没走 npm script，`link-kv.mjs` 从未执行 → Worker 上没有 KV 绑定。改成 `npm run deploy` 后 retry |
| 日志出现 `未设置 HOLIDAY_KV_ID` | 脚本跑了但没读到变量：变量名拼错、加到了运行时变量面板（构建读不到）、或只加在 Previews Base 而这次是 Production |
| 接口 200、Errors 0，但 KV 里始终是 0 条 | 就是上面两种情况的共同表现 —— `src/store.ts` 会把 KV 缺失/异常静默降级成「现抓百度」，所以响应上看不出差别。判据是 `X-Data-Source` 连续两次都是 `baidu`，以及 Settings → Variables and bindings 里根本没有 `HOLIDAY_KV` |
| 在 dashboard 的 Bindings 面板手工加了 KV，几分钟后又没了 | 下一次 `wrangler deploy` 会用仓库配置里的绑定集（空）覆盖它。只有走 `HOLIDAY_KV_ID` + 构建时生成才是持久的 |
| 日志 `Failed to match Worker name ... Overriding using the CI provided Worker name` | `wrangler.jsonc` 的 `name` 与 CF 项目名不一致。CI 以项目名为准并会尝试开 PR 修正，把仓库里的 `name` 改成项目名即可 |
| `HOLIDAY_KV_ID 应是 KV 命名空间的 Namespace ID(32 位十六进制)，当前长度 N。` | 复制了错的值（最常见：复制了 KV 的**名字**而非 ID，或带了空格/换行）。脚本会在 deploy 之前以退出码 1 拦下，构建直接失败 |
| `/holiday/update` 返回 403 | 未配 `UPDATE_KEY`（这是预期行为），或口令不对 |
| 查次年返回 `[]` | 次年安排尚未公布，属正常；公布后由定时任务或手动刷新接口取到 |
| 百度接口异常时该年返回 `[]` | 没绑 KV 就没有旧数据兜底。要这层保险就按第 3 步绑 KV |
| 国内浏览器打不开 `*.workers.dev` | 该后缀在大陆普遍被 DNS 污染（解析到非 Cloudflare 地址后超时），不是服务故障。绑自有域名，或用代理验证 |
