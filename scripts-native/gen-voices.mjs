#!/usr/bin/env node
// ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「じまん/声/...」行): せんぱいの声(VOICES・116件)を
// index.htmlからコード生成する。実際のYouTubeコメントに基づく引用文を含む116件を手で書き写すと
// 転記ミスが起きうるため、gen-card-data.mjs/gen-safety-kb.mjsと同じ方針(手書きパーサでなくJSエンジン
// 自身に値を確定させる)で、実ソースを実際に評価してから回収する(Web版ファイルは読むだけ・無変更)。
//
// 出力: scripts-native/out/voices.json
// 使い方: node scripts-native/gen-voices.mjs
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";

const indexSrc = readFileSync(new URL("../index.html", import.meta.url), "utf8");

// gen-card-data.mjsのextractConstStatementと同一の抽出方式(波かっこ/角かっこの深さ追跡で1文だけ切り出す)。
function extractConstStatement(src, name) {
  const markerRe = new RegExp(`const\\s+${name}\\s*=`, "m");
  const m = markerRe.exec(src);
  if (!m) throw new Error(`定数 ${name} が見つからない`);
  const start = m.index;
  let i = start + m[0].length;
  let depth = 0;
  let inStr = null;
  for (; i < src.length; i++) {
    const ch = src[i];
    if (inStr) {
      if (ch === "\\") { i++; continue; }
      if (ch === inStr) inStr = null;
      continue;
    }
    if (ch === '"' || ch === "'" || ch === "`") { inStr = ch; continue; }
    if (ch === "[" || ch === "{" || ch === "(") depth++;
    else if (ch === "]" || ch === "}" || ch === ")") depth--;
    else if (ch === ";" && depth <= 0) { i++; break; }
  }
  return src.slice(start, i);
}

const stmt = extractConstStatement(indexSrc, "VOICES");
const fn = new Function(stmt + "\nreturn VOICES;");
const voices = fn();

if (!Array.isArray(voices) || voices.length !== 116) {
  throw new Error(`VOICES件数が116でない(実測${Array.isArray(voices) ? voices.length : "配列でない"})`);
}
for (const v of voices) {
  for (const k of ["tag", "front", "vid", "q", "src"]) {
    if (typeof v[k] !== "string" || !v[k]) throw new Error(`VOICES要素にフィールド${k}が欠落: ${JSON.stringify(v)}`);
  }
}

mkdirSync(new URL("./out/", import.meta.url), { recursive: true });
writeFileSync(new URL("./out/voices.json", import.meta.url), JSON.stringify(voices, null, 2));
console.log(`voices.json 生成: ${voices.length}件`);
