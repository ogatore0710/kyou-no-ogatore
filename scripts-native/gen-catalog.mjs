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

// TASK build31 R-32(本人指摘・2026-08-06): Web版videos.jsのタグに「足首」の「首」が
// 首・肩こりへ誤マッチした10本が混入している(タイトルに肩こり/頭痛/ストレートネック等の
// 首肩要素なし)。Web版無変更の原則のため、ネイティブ向け出力でのみ除去する。
// 正本の両OSバンドルcatalog.json(st入り)は直接修正済み。ここは再生成時の再混入防止。
const WRONG_NECK_TAG_IDS = new Set([
  "iXNAGygELPQ", "t3C-N5_828k", "86u3S-epkRg", "cs1A8W_HofI", "6U4fgJu0ZMw",
  "99xdVf6lPWs", "nkvn6zyYx08", "B-vdrGt8hlA", "8vftEiHldF8", "riNaWEe4qp4",
]);
for (const v of CATALOG) {
  if (WRONG_NECK_TAG_IDS.has(v.id) && Array.isArray(v.tags)) {
    v.tags = v.tags.filter((t) => t !== "首・肩こり");
  }
}

mkdirSync(new URL("./out/", import.meta.url), { recursive: true });
writeFileSync(new URL("./out/catalog.json", import.meta.url), JSON.stringify(CATALOG, null, 2));
writeFileSync(new URL("./out/obu-feed.json", import.meta.url), JSON.stringify(OBU_FEED, null, 2));

console.log(`catalog.json 生成: ${CATALOG.length}本 / obu-feed.json 生成: ${OBU_FEED.length}件`);
