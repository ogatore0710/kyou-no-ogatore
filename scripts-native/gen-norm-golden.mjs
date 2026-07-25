// ネイティブ移植 Step 0: norm()の実出力をゴールデンとして固定する（マスタープラン§3-4手順2・§3-3）。
// 「JS実出力を正とする」方針どおり、soudan-ai-poc/norm.mjs の実装を直接importして実行し、
// このスクリプト側では正規化ロジックを一切再実装しない（複製によるズレのリスクを避ける）。
//
// 3系統（プラットフォーム差が最も出やすい箇所の先回り固定）:
//   1. 合成濁点（NFC精準結合 vs NFD分解形）— 本スクリプト作成中に実際に踏んだ罠: 濁点/半濁点つきの
//      仮名を普通にソースへ打ち込むと、保存経路によってNFC(精準結合・1コードポイント)とNFD(基底文字+
//      結合文字U+3099/U+309Aの2コードポイント)のどちらで保存されるか変わってしまい、意図せず両者を
//      混同する（このファイルの旧版で実際に発生し、Edit照合が通らなくなる不具合まで起きた）。
//      norm()の挙動は2形で明確に異なる（NFDだと結合文字だけが許可リスト外として消え、基底文字が残る＝
//      濁点/半濁点が消える）ため、目視入力を一切せずString.fromCharCode(コードポイント数値)で明示的に
//      組み立てる。曖昧さの入る余地をゼロにする（§1-4「合成濁点はJS版では許可リスト削除で基底文字だけに
//      なる」の実測）。
//   2. 半角カナ — 変換されず削除される（全角ｶﾅ変換(0xFEE0シフト)・カタカナ→ひらがな変換(ァ-ヶのみ)の
//      どちらの対象にもならないため、最終の許可リストで丸ごと消える）。
//   3. 絵文字混在 — 許可リスト外なので削除され、前後の日本語だけが残る。
// 加えて§3-3の連結マッチ敵対ケース（「寝転」除去5語の単一パス選択置換で、除去後に新しい部分文字列が
// 生まれるケース）をnorm()+redFlagHit()の両方で採取する。
//
// 出力: scripts-native/out/norm-golden.json
// 使い方: node scripts-native/gen-norm-golden.mjs
import { writeFileSync, mkdirSync } from "node:fs";
import { norm, redFlagHit } from "../soudan-ai-poc/norm.mjs";

const cases = [];

// ---- 系統1: 合成濁点(NFC精準結合 vs NFD分解形)。目視入力を避けコードポイント数値から組み立てる ----
const dakutenPairs = [
  // [ラベル, NFC(精準結合・1コードポイント。がU+304C), NFD(基底かU+304B+結合濁点U+3099・2コードポイント)]
  ["ka-ga", String.fromCharCode(0x304c), String.fromCharCode(0x304b, 0x3099)],
  // [ラベル, NFC(ぱU+3071), NFD(基底はU+306F+結合半濁点U+309A)]
  ["ha-pa", String.fromCharCode(0x3071), String.fromCharCode(0x306f, 0x309a)],
  // [ラベル, NFC(ごU+3054), NFD(基底こU+3053+結合濁点U+3099)]
  ["ko-go", String.fromCharCode(0x3054), String.fromCharCode(0x3053, 0x3099)],
];
for (const [label, nfc, nfd] of dakutenPairs) {
  cases.push({ system: "composed-dakuten", form: "NFC(精準結合・1コードポイント)", label, input: nfc, inputCodepoints: [...nfc].map(c => "U+" + c.codePointAt(0).toString(16).toUpperCase()), normOutput: norm(nfc) });
  cases.push({ system: "composed-dakuten", form: "NFD(分解形・基底+結合濁点2コードポイント)", label, input: nfd, inputCodepoints: [...nfd].map(c => "U+" + c.codePointAt(0).toString(16).toUpperCase()), normOutput: norm(nfd) });
}

// ---- 系統2: 半角カナ ----
for (const input of ["ｶﾀｺﾘ", "ﾗｳﾂﾂ", "肩こりｶﾀｺﾘ"]) {
  cases.push({ system: "halfwidth-kana", input, normOutput: norm(input) });
}

// ---- 系統3: 絵文字混在 ----
for (const input of ["肩こり\u{1F622}つらい", "腰が痛い\u{1F972}助けて", "\u{1F64F}ストレッチ教えて\u{1F64F}"]) {
  cases.push({ system: "emoji-mixed", input, normOutput: norm(input) });
}

// ---- §3-3 連結マッチ敵対ケース(「寝転」除去5語=寝転|ねころ|寝ころ|ねっころ|寝っこ の単一パス選択置換で、
//      除去による前後連結が新しいkwマッチを生む/消すケース。JS実挙動(単一パスregex選択置換)を正として採取) ----
const adversarial = [
  "寝ねころ転んだ",       // 査読で指摘された例そのもの。「ねころ」除去後「寝転んだ」が残り「転んだ」でヒットするか
  "寝ころねっころ転んだ", // 二重重複のバリエーション
  "寝ねっころ転んだ",
  "寝転んでできるストレッチはありますか", // 既存fixtureにもある通常の非該当ケース(回帰確認用に同梱)
];
for (const input of adversarial) {
  cases.push({ system: "negation-concat-adversarial", input, normOutput: norm(input), redFlagHit: redFlagHit(norm(input)) });
}

mkdirSync(new URL("./out/", import.meta.url), { recursive: true });
writeFileSync(new URL("./out/norm-golden.json", import.meta.url), JSON.stringify(cases, null, 2));

console.log(`norm-golden.json 生成: ${cases.length}件`);
for (const c of cases) {
  console.log(`  [${c.system}${c.form ? "/" + c.form : ""}] ${JSON.stringify(c.input)} → norm=${JSON.stringify(c.normOutput)}` + ("redFlagHit" in c ? ` redFlagHit=${c.redFlagHit}` : ""));
}
