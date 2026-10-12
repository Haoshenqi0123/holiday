/**
 * 按环境变量 HOLIDAY_KV_ID 生成实际部署用的配置 wrangler.gen.jsonc。
 *
 * wrangler 4.x 既不支持在配置文件里做 ${VAR} 插值,deploy 也没有 --kv 参数,
 * 所以只能构建时生成。好处是仓库里永远不会出现任何人的 namespace id。
 */
import { readFileSync, writeFileSync } from "node:fs";

const BASE = "wrangler.jsonc";
const OUT = "wrangler.gen.jsonc";
const ANCHOR = '"kv_namespaces": []';

const id = (process.env.HOLIDAY_KV_ID ?? "").trim();

const src = readFileSync(BASE, "utf8");
if (!src.includes(ANCHOR)) {
  console.error(`${BASE} 里找不到锚点 ${ANCHOR}，请检查模板是否被改动。`);
  process.exit(1);
}

if (id && !/^[0-9a-f]{32}$/.test(id)) {
  console.error(
    `HOLIDAY_KV_ID 应是 KV 命名空间的 Namespace ID(32 位十六进制)，当前长度 ${id.length}。`
  );
  process.exit(1);
}

const injected = `"kv_namespaces": [\n    { "binding": "HOLIDAY_KV", "id": "${id}" }\n  ]`;
writeFileSync(OUT, id ? src.replace(ANCHOR, injected) : src);

console.log(
  id
    ? `已绑定 KV -> ${OUT}`
    : `未设置 HOLIDAY_KV_ID，${OUT} 保持无 KV 绑定（内存缓存 + 现抓百度）`
);
