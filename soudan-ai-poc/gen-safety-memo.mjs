import { writeFileSync, readFileSync } from "node:fs";
const fixes = JSON.parse(readFileSync(new URL("./safety-fixes.raw.json", import.meta.url), "utf8")).result.fixes;
const title = { yakan: "① 夜間痛（痛くて眠れない）", hatsunetsu: "② 発熱の漢字形（熱がある/熱っぽい）", tento: "③ 転倒の口語（転んだ/しりもち）", hakike: "④ 吐き気を伴う頭痛" };
const state = {
  yakan: "データのみ・検証OK(危険7/7・正常5/5)",
  hatsunetsu: "データのみ・検証OK(危険8/8・正常5/5)",
  hakike: "データのみ・検証OK(危険8/8・正常5/5)",
  tento: "データ＋小エンジン改修・検証OK(危険8/8・正常5/5※寝転除去込み)",
};
const bt = String.fromCharCode(96); // `
let m = "# 相談室 赤旗の穴 4件 修正案（要・本人監修 / 2026-07-11）\n\n";
m += "> 総点検で出た相談室の安全ギャップ4件を、Fableがエビデンス＋提案で設計→alanが現行KBで決定論的に検証。\n";
m += "> 産後と同じフロー。**反映は本人監修のあと**。3件はデータ(redFlags.kw)だけ・1件(転倒)は小さなエンジン1行つき。\n\n";
m += "## ひとことで\n";
m += "- 4件とも「危険な相談→受診案内／正常な相談→巻き込まない」を実測で確認済み。\n";
m += "- ①②④ はデータ追加だけ。③転倒だけ「寝転んで」の誤爆回避に、赤旗判定前の1行(寝転/ねころ除去)が要る(解決策も実証済み)。\n";
m += "- 医学的根拠つき。安全側の許容トレードオフ(下記)も明記。\n\n---\n";
for (const key of ["yakan", "hatsunetsu", "tento", "hakike"]) {
  const f = fixes.find((x) => x.key === key);
  m += "\n## " + title[key] + "\n";
  m += "**状態**: " + state[key] + "\n\n";
  m += "**なぜ危険(エビデンス)**: " + f.evidence + "\n\n";
  m += "**足す赤旗語**: " + bt + f.addRedflagKw.join(bt + " " + bt) + bt + "\n\n";
  if (f.removeFromIntent && f.removeFromIntent.length) {
    m += "**外す**: " + f.removeFromIntent.map((r) => r.intent + ".kw から「" + r.kw + "」").join(" / ") + "\n\n";
  }
  m += "**受診になるべき例**: " + f.referCases.slice(0, 4).map((c) => "「" + c + "」").join("・") + "\n\n";
  m += "**巻き込まない例(通常回答のまま)**: " + f.normalCases.slice(0, 4).map((c) => "「" + c + "」").join("・") + "\n\n";
  m += "**設計メモ/トレードオフ**: " + (f.notes || "") + "\n\n---\n";
}
m += "\n## ③転倒の エンジン1行（要判断）\n";
m += "「寝転んで」「ねころんで」が「転んで/ころんで」を部分文字列に含むため、赤旗判定の前に正規化テキストから 寝転/ねころ/寝ころ/ねっころ/寝っこ を除去する1行が要る。実証済み: 寝転んで→通常・寝転がろうとして転んだ→受診(実転倒は取りこぼさない)。sdRedFlagHit手前に1行。安全経路のロジック変更なので監修対象。\n\n";
m += "## おまけ改善(推奨・スコープ外)\n";
m += "裸の「転倒」を外すと「転倒予防」が赤旗を通過するので、koureisha(高齢者)の kw に「転倒予防」「転倒防止」「転ばない」を足すと予防相談がシニア向け回答に着地する(loop2の\"転倒予防\"過剰赤旗もこれで解決)。\n\n";
m += "## 監修してほしい点\n";
m += "1. ①②④(データのみ)はこの語で反映してOK?\n";
m += "2. ③転倒: エンジン1行(寝転除去)を入れて口語対応する方針でOK? それとも弱い代替(で転ん/て転ん)にする?\n";
m += "3. 安全側の許容トレードオフ(例: 肩こりで吐き気→受診／過去形の痛みで眠れない→受診)はこのままでいい?\n";
writeFileSync(new URL("../SAFETY-REDFLAG-FIXES.md", import.meta.url), m);
console.log("SAFETY-REDFLAG-FIXES.md 生成: " + m.length + "字");
