/**
 * 按环境变量 HOLIDAY_KV_ID 生成实际部署用的配置 wrangler.gen.jsonc。
 *
 * KV 是必需项：没有缓存层时每个请求都会回源抓百度，而百度按 IP 频控，
 * 所以这里在 deploy 路径上直接拒绝无 KV 的部署（运行时侧 src/store.ts
 * 也会在没有绑定时抛 MissingKVError，绝不降级抓取）。
 *
 * wrangler 4.x 既不支持配置内 ${VAR} 插值，deploy 也没有 --kv 参数（均实测），
 * 只能构建时生成；好处是 namespace id 永远不进版本库，fork 者用的是自己的 KV。
 *
 * 本地开发传 --dev：wrangler dev 的 KV 是本地模拟、不校验 id，未配置时用占位 id。
 */
import { readFileSync, writeFileSync } from "node:fs";

const BASE = "wrangler.jsonc";
const OUT = "wrangler.gen.jsonc";
const ANCHOR = '"kv_namespaces": []';
const MOCK_ID = "0".repeat(32);

let id = (process.env.HOLIDAY_KV_ID ?? "").trim();
const isDev = process.argv.includes("--dev");

const src = readFileSync(BASE, "utf8");
if (!src.includes(ANCHOR)) {
  console.error(`${BASE} 里找不到锚点 ${ANCHOR}，请检查模板是否被改动。`);
  process.exit(1);
}

if (!id) {
  if (isDev) {
    id = MOCK_ID;
    console.log(`未设置 HOLIDAY_KV_ID，本地 dev 使用占位 id（wrangler 的本地 KV 模拟不校验它）`);
  } else {
    console.error(
      [
        "必须设置 HOLIDAY_KV_ID = KV 命名空间的 Namespace ID（32 位十六进制）。",
        "本 Worker 强制依赖 KV：缺了缓存层，每个请求都会回源抓百度，而百度按 IP 频控。",
        "配置步骤见 DEPLOY.md；本地开发请用 npm run dev。",
      ].join("\n")
    );
    process.exit(1);
  }
}

if (!/^[0-9a-f]{32}$/.test(id)) {
  console.error(
    `HOLIDAY_KV_ID 应是 KV 命名空间的 Namespace ID(32 位十六进制)，当前长度 ${id.length}。` +
      `注意别填成 KV 的名字，ID 在 KV 的 Settings 标签页。`
  );
  process.exit(1);
}

const injected = `"kv_namespaces": [\n    { "binding": "HOLIDAY_KV", "id": "${id}" }\n  ]`;
writeFileSync(OUT, src.replace(ANCHOR, injected));
console.log(`已绑定 KV -> ${OUT}`);
