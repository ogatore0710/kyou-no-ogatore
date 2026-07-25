// ネイティブ移植 Step 0: ../soudan-kb.js（ブラウザ用グローバル定義 SOUDAN_KB）を
// JSONへコード生成する。soudan-ai-poc/build-data.mjs と同じ loadGlobal 手法を踏襲し、
// soudan-kb.js自体は読むだけ（Web版無変更の原則。書き換えない）。
// 出力: scripts-native/out/soudan-kb.json（SOUDAN_KB全体・intents/redFlags/crisis/followups/smalltalk等を含む）
// 使い方: node scripts-native/gen-safety-kb.mjs
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";

function loadGlobal(path, globalName) {
  const src = readFileSync(new URL(path, import.meta.url), "utf8");
  const fn = new Function(src + "\nreturn " + globalName + ";");
  return fn();
}

const KB = loadGlobal("../soudan-kb.js", "SOUDAN_KB");

mkdirSync(new URL("./out/", import.meta.url), { recursive: true });
writeFileSync(new URL("./out/soudan-kb.json", import.meta.url), JSON.stringify(KB, null, 2));

console.log(
  `soudan-kb.json 生成: intents=${KB.intents.length} redFlags.kw=${KB.redFlags.kw.length} ` +
  `redFlags.stateKw=${(KB.redFlags.stateKw || []).length} crisis.kw=${KB.crisis.kw.length}`
);
