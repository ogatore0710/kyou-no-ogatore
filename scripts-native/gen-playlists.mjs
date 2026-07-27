#!/usr/bin/env node
// TASK-C2-2026-07-28-search-playlists-and-fullwidth-space.md §1: 「再生リスト」タブの中身が
// Web版のPLAYLISTS(手動キュレーションのYouTubeプレイリスト16本・index.html:3498-3521)と
// 別物(catalog.jsonの454本を絞り込みなしで並べただけ)だった欠落を埋めるためのコード生成。
// gen-voices.mjsと同じ方針(手書きパーサでなくJSエンジン自身に値を確定させる。手で書き写すと
// プレイリストIDやi.ytimg.comの長いURLで転記ミスが起きうるため)。Web版ファイルは読むだけ・無変更。
//
// 出力: scripts-native/out/playlists.json
// 使い方: node scripts-native/gen-playlists.mjs
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";

const indexSrc = readFileSync(new URL("../index.html", import.meta.url), "utf8");

// gen-voices.mjs extractConstStatementと同一の抽出方式(波かっこ/角かっこの深さ追跡で1文だけ切り出す)。
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

const stmt = extractConstStatement(indexSrc, "PLAYLISTS");
const fn = new Function(stmt + "\nreturn PLAYLISTS;");
const raw = fn();

if (!Array.isArray(raw) || raw.length !== 3) {
  throw new Error(`PLAYLISTSグループ数が3でない(実測${Array.isArray(raw) ? raw.length : "配列でない"})`);
}

const groups = raw.map((gr) => {
  if (typeof gr.g !== "string" || !gr.g) throw new Error(`グループ名が欠落: ${JSON.stringify(gr)}`);
  if (!Array.isArray(gr.items) || gr.items.length === 0) throw new Error(`グループ「${gr.g}」のitemsが空`);
  return {
    group: gr.g,
    items: gr.items.map(([id, title, desc, thumb]) => {
      if (!id || !title || !desc) throw new Error(`項目にid/title/descの欠落: ${JSON.stringify([id, title, desc, thumb])}`);
      return { id, title, desc, thumb: thumb || null };
    }),
  };
});

const total = groups.reduce((n, g) => n + g.items.length, 0);
if (total !== 16) throw new Error(`PLAYLISTS総数が16でない(実測${total})`);

mkdirSync(new URL("./out/", import.meta.url), { recursive: true });
writeFileSync(new URL("./out/playlists.json", import.meta.url), JSON.stringify(groups, null, 2));
console.log(`playlists.json 生成: ${groups.length}グループ・計${total}件`);
