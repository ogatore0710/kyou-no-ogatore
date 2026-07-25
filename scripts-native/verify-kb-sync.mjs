// ネイティブ移植 Step 0: soudan-ai-poc/data.mjs（build-data.mjsによる自動生成スナップショット）と
// ../soudan-kb.js（本番の実体）のredFlags/crisisが完全一致しているかをdeep-equalで照合する。
//
// なぜ必要か（マスタープラン§3-4手順1の注意書き）: node redflag-safety-test.mjs が実際に検証しているKBは
// 本番soudan-kb.jsではなくsoudan-ai-poc/data.mjs（norm.mjsが import { KB } from "./data.mjs" するため）。
// data.mjsの再生成(build-data.mjs)を誰かが忘れたまま111/111緑が続くと、「テストは通っているが本番の
// 赤旗定義とは違う」という気づきにくい乖離が起こる。この照合はその乖離を機械的に検知する。
//
// 件数のみの比較（kw.length等）では不十分（査読指摘）。kw/stateKw配列の中身・answer文面まで
// すべてdeep-equalで比較する。
//
// 不一致時: build-data.mjsの再生成はWeb側の生成物（soudan-ai-poc/はPages非配信だが本タスクは
// Web版の変更を一切しない原則のため）に触れる操作なので、このスクリプトは絶対に自動修復しない。
// 不一致を検出したらそのまま報告してalan5の判断を仰ぐこと（TASK-C2-2026-07-25-native-migration-step0.md 検収基準）。
//
// 使い方: node scripts-native/verify-kb-sync.mjs
import { readFileSync } from "node:fs";

function loadGlobal(path, globalName) {
  const src = readFileSync(new URL(path, import.meta.url), "utf8");
  const fn = new Function(src + "\nreturn " + globalName + ";");
  return fn();
}

function deepEqual(a, b, path = "$") {
  const diffs = [];
  if (a === b) return diffs;
  if (typeof a !== typeof b) { diffs.push(`${path}: 型が違う (${typeof a} vs ${typeof b})`); return diffs; }
  if (Array.isArray(a) || Array.isArray(b)) {
    if (!Array.isArray(a) || !Array.isArray(b)) { diffs.push(`${path}: 片方だけ配列`); return diffs; }
    if (a.length !== b.length) diffs.push(`${path}: 配列長が違う (${a.length} vs ${b.length})`);
    const n = Math.max(a.length, b.length);
    for (let i = 0; i < n; i++) diffs.push(...deepEqual(a[i], b[i], `${path}[${i}]`));
    return diffs;
  }
  if (a && b && typeof a === "object") {
    const keys = new Set([...Object.keys(a), ...Object.keys(b)]);
    for (const k of keys) diffs.push(...deepEqual(a[k], b[k], `${path}.${k}`));
    return diffs;
  }
  diffs.push(`${path}: 値が違う (${JSON.stringify(a)} vs ${JSON.stringify(b)})`);
  return diffs;
}

const { KB: dataKB } = await import("../soudan-ai-poc/data.mjs");
const liveKB = loadGlobal("../soudan-kb.js", "SOUDAN_KB");

const diffs = [
  ...deepEqual(dataKB.redFlags, liveKB.redFlags, "redFlags"),
  ...deepEqual(dataKB.crisis, liveKB.crisis, "crisis"),
];

if (diffs.length) {
  console.log("❌ data.mjs と soudan-kb.js の redFlags/crisis が不一致");
  diffs.slice(0, 50).forEach((d) => console.log("  - " + d));
  if (diffs.length > 50) console.log(`  ...ほか${diffs.length - 50}件`);
  console.log("→ build-data.mjsの再生成はWeb側の生成物操作にあたるため実行せず、alan5へ報告して判断を仰ぐこと。");
  process.exit(1);
} else {
  console.log(
    `✅ data.mjs と soudan-kb.js のredFlags/crisisは完全一致 ` +
    `(redFlags.kw=${liveKB.redFlags.kw.length} stateKw=${(liveKB.redFlags.stateKw || []).length} crisis.kw=${liveKB.crisis.kw.length})`
  );
  process.exit(0);
}
