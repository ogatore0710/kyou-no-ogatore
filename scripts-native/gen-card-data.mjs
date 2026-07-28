// ネイティブ移植 Step 4(マスタープラン§2-4・§6 Step 4): 記録カード抽選に使う静的データテーブル
// (SEASON_CARDS/RARE_CARDS/NORMAL_CARDS/TOKU_CARDS/CARD_ROT_ORDER/CARD_THEMES/GOLD/MS/
// CARD_IMG_FROM/CARD_THEMES_V2_FROM/CARD_THEMES_V1_COUNT)を index.html / app-card.js から
// コード生成する。100件超の色/名前/日付データを手で書き写すと1件でも転記ミスが起きうるため、
// gen-safety-kb.mjs/gen-safety-fixtures.mjsと同じ方針(手書きパーサでなくJSエンジン自身に
// 値を確定させる)で、実ソースを実際に評価してから回収する(Web版ファイルは読むだけ・無変更)。
//
// 出力: scripts-native/out/card-data.json
// 使い方: node scripts-native/gen-card-data.mjs
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";

const appCardSrc = readFileSync(new URL("../app-card.js", import.meta.url), "utf8");
const indexSrc = readFileSync(new URL("../index.html", import.meta.url), "utf8");

// index.htmlは巨大な1枚のHTML(インラインscript全体を素朴にevalすると、document等が未定義の
// トップレベル副作用コードまで実行されて即エラーになる)。ここでは必要な定数宣言だけを、
// 変数名を起点にした波かっこ/角かっこの深さ追跡で1文ずつ厳密に切り出す(正規表現の手書きパーサで
// 中身をパースするのではなく、境界=文の終わりだけを機械的に見つける。値そのものは後段でJSエンジンに評価させる)。
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

const appCardConsts = ["CARD_THEMES", "GOLD", "MS", "CHARA_FILES"].map((n) => extractConstStatement(appCardSrc, n));
const indexConsts = [
  "CARD_THEMES_V1_COUNT",
  "CARD_THEMES_V2_FROM",
  "MILESTONES",
  "CARD_IMG_FROM",
  // UI/UXパリティ監査GO-1(2026-07-28): 節目お祝いメッセージ動画リンク(空文字=機能オフ)。
  // 節目カードの中身をmarkDone直後の演出として移植する際に必要。
  "MILESTONE_MSG_VIDEO",
  "SEASON_CARDS",
  "RARE_CARDS",
  "NORMAL_CARDS",
  "TOKU_CARDS",
  "CARD_ROT_ORDER",
  // Step 7a(マスタープラン§6 Step 7a・図鑑UI): getDexStatus()が参照するヒント/フレーバー文言。
  // 手写し禁止(§1-2)のため他の定数と同じくJSエンジンに実評価させて回収する。
  "DEX_TEASE",
  "DEX_FLAVOR",
  "DEX_FLAVOR_NORMAL",
  "DEX_NORMAL_TEASE",
].map((n) => extractConstStatement(indexSrc, n));

const NAMES = [
  "CARD_THEMES", "GOLD", "MS", "CHARA_FILES",
  "CARD_THEMES_V1_COUNT", "CARD_THEMES_V2_FROM", "MILESTONES", "CARD_IMG_FROM",
  "MILESTONE_MSG_VIDEO",
  "SEASON_CARDS", "RARE_CARDS", "NORMAL_CARDS", "TOKU_CARDS", "CARD_ROT_ORDER",
  "DEX_TEASE", "DEX_FLAVOR", "DEX_FLAVOR_NORMAL", "DEX_NORMAL_TEASE",
];
const body = appCardConsts.join("\n") + "\n" + indexConsts.join("\n") + "\nreturn {" + NAMES.join(",") + "};\n";
const fn = new Function(body);
const data = fn();

for (const n of NAMES) if (data[n] === undefined) throw new Error(`定数 ${n} の抽出に失敗`);

if (data.CARD_THEMES.length !== 10) throw new Error(`CARD_THEMES件数が10でない(実測${data.CARD_THEMES.length})`);
if (data.MS.length !== 17) throw new Error(`MS件数が17でない(実測${data.MS.length})`);
if (data.SEASON_CARDS.length !== 40) throw new Error(`SEASON_CARDS件数が40でない(実測${data.SEASON_CARDS.length})`);
if (data.RARE_CARDS.length !== 30) throw new Error(`RARE_CARDS件数が30でない(実測${data.RARE_CARDS.length})`);
if (data.NORMAL_CARDS.length !== 20) throw new Error(`NORMAL_CARDS件数が20でない(実測${data.NORMAL_CARDS.length})`);
if (Object.keys(data.TOKU_CARDS).length !== 16) throw new Error(`TOKU_CARDS件数が16でない(実測${Object.keys(data.TOKU_CARDS).length})`);
if (data.CARD_ROT_ORDER.length !== 50) throw new Error(`CARD_ROT_ORDER件数が50でない(実測${data.CARD_ROT_ORDER.length})`);
// cardFromRotPos(index.html:2277)はpos<NORMAL_CARDS.lengthでノーマル・それ以外をRARE_CARDSへ、
// という前提でCARD_ROT_ORDER(0〜49)を引く。プール合計が50でないと抽選プールの一部が存在しない
// カードを指すことになるため、生成時点で機械的に整合を確認する。
if (data.NORMAL_CARDS.length + data.RARE_CARDS.length !== 50) {
  throw new Error(`NORMAL_CARDS+RARE_CARDSが50でない(実測${data.NORMAL_CARDS.length + data.RARE_CARDS.length})`);
}

// UI/UXパリティ監査GO-1(2026-07-28): 節目お祝い(記録直後の紙吹雪カード)の中身がm/q未同梱のため
// 移植されず、通常日と見分けがつかない状態になっていた欠落の修正。以前は「m/qは長文お祝い
// メッセージ・先輩の声でmarkDone側=Step5aの管轄」としてd/tだけを抜き出していたが、Step5a側では
// 結局手当てされていなかった。手写し禁止(§1-2)の原則どおりm/qも同じくJSエンジンに実評価させて
// 機械抽出し、カードデータに同梱する(空文字の項目はWeb版でも元々m/qが無い節目=そのまま)。
// d=3のmだけ、Web側で折返し制御用の<span style='display:inline-block'>...</span>タグを含む。
// ネイティブはHTMLを描画しないため、タグを除いたプレーンテキストにして同梱する
// (2つのspanは元々隣接=間に空白なしなので、タグ除去だけで意味は変わらない)。
const stripHtml = (s) => (s || "").replace(/<[^>]+>/g, "");
const msSlim = data.MS.map((x) => ({ d: x.d, t: x.t, m: stripHtml(x.m), q: stripHtml(x.q) }));

mkdirSync(new URL("./out/", import.meta.url), { recursive: true });
const out = {
  CARD_THEMES: data.CARD_THEMES,
  GOLD: data.GOLD,
  MS: msSlim,
  CHARA_FILES: data.CHARA_FILES,
  CARD_THEMES_V1_COUNT: data.CARD_THEMES_V1_COUNT,
  CARD_THEMES_V2_FROM: data.CARD_THEMES_V2_FROM,
  MILESTONES: data.MILESTONES,
  CARD_IMG_FROM: data.CARD_IMG_FROM,
  MILESTONE_MSG_VIDEO: data.MILESTONE_MSG_VIDEO,
  SEASON_CARDS: data.SEASON_CARDS,
  RARE_CARDS: data.RARE_CARDS,
  NORMAL_CARDS: data.NORMAL_CARDS,
  TOKU_CARDS: data.TOKU_CARDS,
  CARD_ROT_ORDER: data.CARD_ROT_ORDER,
  DEX_TEASE: data.DEX_TEASE,
  DEX_FLAVOR: data.DEX_FLAVOR,
  DEX_FLAVOR_NORMAL: data.DEX_FLAVOR_NORMAL,
  DEX_NORMAL_TEASE: data.DEX_NORMAL_TEASE,
};
writeFileSync(new URL("./out/card-data.json", import.meta.url), JSON.stringify(out, null, 2));
console.log(
  `card-data.json 生成: CARD_THEMES=${data.CARD_THEMES.length} MS=${data.MS.length} ` +
  `SEASON_CARDS=${data.SEASON_CARDS.length} RARE_CARDS=${data.RARE_CARDS.length} NORMAL_CARDS=${data.NORMAL_CARDS.length} ` +
  `TOKU_CARDS=${Object.keys(data.TOKU_CARDS).length} CARD_ROT_ORDER=${data.CARD_ROT_ORDER.length} ` +
  `CARD_IMG_FROM=${data.CARD_IMG_FROM} CARD_THEMES_V2_FROM=${data.CARD_THEMES_V2_FROM} ` +
  `DEX_TEASE=${Object.keys(data.DEX_TEASE).length} DEX_FLAVOR=${Object.keys(data.DEX_FLAVOR).length} ` +
  `DEX_FLAVOR_NORMAL=${Object.keys(data.DEX_FLAVOR_NORMAL).length}`
);
