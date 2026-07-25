// ネイティブ移植 Step 0: ../videos.js（CATALOG）と ../obu-feed.js（OBU_FEED）をJSONへコード生成する。
// どちらもWeb版の配信用ブラウザグローバル定義（`const CATALOG=[...]`/`const OBU_FEED=[...]`）で、
// soudan-ai-poc/build-data.mjs と同じ loadGlobal 手法で読むだけ（Web版無変更の原則）。
// 出力: scripts-native/out/catalog.json / scripts-native/out/obu-feed.json
// 使い方: node scripts-native/gen-catalog.mjs
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";

function loadGlobal(path, globalName) {
  const src = readFileSync(new URL(path, import.meta.url), "utf8");
  const fn = new Function(src + "\nreturn " + globalName + ";");
  return fn();
}

const CATALOG = loadGlobal("../videos.js", "CATALOG");
const OBU_FEED = loadGlobal("../obu-feed.js", "OBU_FEED");

mkdirSync(new URL("./out/", import.meta.url), { recursive: true });
writeFileSync(new URL("./out/catalog.json", import.meta.url), JSON.stringify(CATALOG, null, 2));
writeFileSync(new URL("./out/obu-feed.json", import.meta.url), JSON.stringify(OBU_FEED, null, 2));

console.log(`catalog.json 生成: ${CATALOG.length}本 / obu-feed.json 生成: ${OBU_FEED.length}件`);
