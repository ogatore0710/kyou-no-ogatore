// data.mjs を ../soudan-kb.js + ../videos.js から再生成する。
// 使い方: node build-data.mjs
import { readFileSync, writeFileSync } from "node:fs";

function loadGlobal(path, globalName) {
  const src = readFileSync(new URL(path, import.meta.url), "utf8");
  const fn = new Function(src + "\nreturn " + globalName + ";");
  return fn();
}

const KB = loadGlobal("../soudan-kb.js", "SOUDAN_KB");
const C = loadGlobal("../videos.js", "CATALOG");
const CATALOG = C.map((v) => ({ id: v.id, t: v.t, s: v.s, tags: v.tags || [] }));

const out =
  "// 自動生成: build-data.mjs（../soudan-kb.js + ../videos.js から）\n" +
  "export const KB=" + JSON.stringify(KB) + ";\n" +
  "export const CATALOG=" + JSON.stringify(CATALOG) + ";\n";

writeFileSync(new URL("./data.mjs", import.meta.url), out);
console.log(`data.mjs 再生成: intents=${KB.intents.length} catalog=${CATALOG.length}`);
