// オフライン精度ループのワークフロースクリプトを生成する。
// 現行マッチャ(norm/scoreIntents/redFlagHit)とスリムKBを埋め込み、
// 生成・診断だけをFableエージェントに任せる（判定は決定論的にスクリプト内）。
import { readFileSync, writeFileSync } from "node:fs";

const src = readFileSync(new URL("./data.mjs", import.meta.url), "utf8");
const KB = new Function(src.replace(/export const/g, "var") + "\nreturn {KB,CATALOG};")().KB;

const slim = {
  intents: KB.intents.map((it) => ({ id: it.id, chip: it.chip, kw: it.kw || [], safety: !!it.safety })),
  redflags: (KB.redFlags && KB.redFlags.kw) || [],
};

const DATA = JSON.stringify(slim);
const ROUNDS = Number(process.env.ROUNDS || 10);
const THEMES = process.env.THEMES
  ? JSON.parse(process.env.THEMES)
  : ['デスクワークで肩・首・肩甲骨','腰まわり・骨盤・お尻','脚/膝/ふくらはぎ/むくみ','姿勢(反り腰/猫背/巻き肩)',
     '睡眠/自律神経/頭痛/目の疲れ','産後・年代・更年期など慢性の事情','手/腕/手首/末端の冷え','全身のだるさ/運動が苦手/続かない',
     '開脚/柔軟/前屈ができない','生活動作(抱っこ/立ち仕事/スマホ/階段)'];

const script = String.raw`export const meta = {
  name: 'soudan-accuracy-loop',
  description: 'オフライン精度ループ: Fableで難問生成→現行マッチャで判定→ミス診断→kw処方→非退行ゲート。10周。',
  phases: [
    { title: 'Warmup', detail: 'ベースライン測定' },
    { title: 'Loop', detail: '10周: 生成→判定→処方→ゲート' },
    { title: 'Finalize', detail: '最終再測定・レビュー整理' },
  ],
}

const SLIM = __DATA__

// ---- 現行マッチャ（index.html 忠実移植・決定論的） ----
function norm(s){ s=String(s==null?'':s).toLowerCase();
  s=s.replace(/[Ａ-Ｚａ-ｚ０-９]/g,function(c){return String.fromCharCode(c.charCodeAt(0)-0xFEE0)});
  s=s.replace(/[ァ-ヶ]/g,function(c){return String.fromCharCode(c.charCodeAt(0)-0x60)});
  return s.replace(/[^0-9a-zぁ-ゖー一-鿿々]/g,''); }
// 可変KB（周回で kw が増える）
const KB = SLIM.intents.map(function(it){ return { id:it.id, chip:it.chip, safety:it.safety, kw:(it.kw||[]).slice() }; })
const RF = (SLIM.redflags||[]).map(norm).filter(function(k){ return k.length>=2 })
const byId = {}; KB.forEach(function(it){ byId[it.id]=it })
function redFlag(n){ for(var i=0;i<RF.length;i++){ if(n.indexOf(RF[i])>=0) return true } return false }
function score(n){ var out=[]; for(var a=0;a<KB.length;a++){ var it=KB[a],sc=0,fh=-1;
    for(var j=0;j<it.kw.length;j++){ var k=norm(it.kw[j]); if(k&&n.indexOf(k)>=0){ sc+=k.length; if(fh<0)fh=j } }
    if(sc>0) out.push({id:it.id,chip:it.chip,score:sc,fh:fh}) }
  out.sort(function(x,y){ return (y.score-x.score)||(x.fh-y.fh) }); return out }
function landing(text){ var n=norm(text); if(redFlag(n)) return {type:'redflag'};
  var s=score(n); if(!s.length) return {type:'none'}; return {type:'hit',id:s[0].id,chip:s[0].chip} }

// ---- 非退行ゲート付き kw 追加 ----
var regset = []   // {text, expectId} 確定済み正着地。ここが壊れたら却下
var keptKw = {}   // intentId -> [追加kw]
function alreadyHas(it,k){ for(var i=0;i<it.kw.length;i++){ if(norm(it.kw[i])===k) return true } return false }
function tryAdd(intentId, kwList, caseText){
  var it=byId[intentId]; if(!it) return {ok:false,reason:'no-intent'}
  var added=[]
  for(var i=0;i<kwList.length;i++){
    var raw=String(kwList[i]||''); var k=norm(raw)
    if(k.length<2) continue
    if(alreadyHas(it,k)) continue
    // 汎用すぎる語(全インテントで乱発しそう)は弾く: 2文字以下のひらがなのみ等
    if(/^[ぁ-ゖー]{1,2}$/.test(k)) continue
    it.kw.push(raw); added.push(raw)
  }
  if(!added.length) return {ok:false,reason:'nothing-new'}
  // 対象ケースが intentId に着地するか
  var lc=landing(caseText)
  if(!(lc.type==='hit'&&lc.id===intentId)){ for(var r=0;r<added.length;r++) it.kw.pop(); return {ok:false,reason:'no-fix'} }
  // 既存の確定正着地を壊さないか
  for(var g=0; g<regset.length; g++){ var rg=regset[g]; var lg=landing(rg.text);
    if(!(lg.type==='hit'&&lg.id===rg.expectId)){ for(var r2=0;r2<added.length;r2++) it.kw.pop(); return {ok:false,reason:'regress'} } }
  // 合格
  keptKw[intentId]=(keptKw[intentId]||[]).concat(added)
  regset.push({text:caseText, expectId:intentId})
  return {ok:true, added:added}
}

// ---- 全ケース集約（最終再測定・回帰テスト化用） ----
var allCases = []   // {text, expectId|null, verdict}

// ---- エージェントのスキーマ ----
var GEN_SCHEMA = { type:'object', properties:{ cases:{ type:'array', items:{ type:'string' } } }, required:['cases'] }
var JUDGE_SCHEMA = { type:'object', properties:{ results:{ type:'array', items:{ type:'object',
  properties:{ i:{type:'integer'}, verdict:{type:'string', enum:['ok','miss_none','miss_wrong','over_redflag','new_topic']},
    targetIntentId:{type:'string'}, kw:{ type:'array', items:{type:'string'} } },
  required:['i','verdict','targetIntentId','kw'] } } }, required:['results'] }

var THEMES = __THEMES__

// 判定用: intent一覧を1度だけ文字列化
function intentList(){ return KB.map(function(it){ return '- '+it.id+' : '+it.chip+(it.safety?' [安全]':'') }).join('\n') }

function genPrompt(r){ return '尾形(オガトレ)というストレッチ系YouTuberの「からだ相談チャット」に、視聴者が実際に打ちそうな日本語の短い相談文を作る。\n'+
  'ラウンド'+r+'のテーマ: '+THEMES[(r-1)%THEMES.length]+'\n'+
  '30個、短く自然に、バラして: 口語・かな/タイポ・複合(2部位)・個別事情(産後/年齢/仕事)・動作+部位(階段で膝の外)・ふわっとした訴え。\n'+
  '教科書的な言い方は避け、生活者が本当に打つ言い方で。医療の激痛/しびれ等の緊急ワードは入れない(それは別処理)。\n'+
  '{cases:[...30 strings...]} で返す。' }

function judgePrompt(r, landed){
  var lines = landed.map(function(x,idx){ var l=x.land;
    var s = l.type==='hit' ? ('着地→'+l.chip+'('+l.id+')') : (l.type==='none'?'着地→該当なし':'着地→赤旗(受診案内)');
    return idx+'. 「'+x.t+'」 '+s; }).join('\n')
  return '相談チャットの現行マッチャ(部分一致・正規化)の判定を診断する。マッチは「正規化した相談文に、各インテントのkw(短い部分文字列)が含まれるか」で決まる。\n\n'+
    '【インテント一覧(id:chip)】\n'+intentList()+'\n\n'+
    '【今回の相談文と現行の着地】\n'+lines+'\n\n'+
    '各iについて判定して results で返す:\n'+
    '- verdict: ok(妥当な着地) / miss_none(該当なしだが本当はあるインテントに行くべき) / miss_wrong(着地先が的外れで別インテントが正しい) / over_redflag(赤旗に飛んだが慢性で受診案内は過剰) / new_topic(既存インテントに無い新しい悩み)\n'+
    '- miss_none/miss_wrong の時: targetIntentId=正しい既存インテントのid、kw=その相談文に実在する短く特徴的な部分文字列を1〜3個(その部位に固有で、他の悩みを奪わない語。全身/がちがち等の汎用語は禁止)\n'+
    '- ok/over_redflag/new_topic の時: targetIntentId=""、kw=[]\n'+
    'kwは相談文中に実際に出てくる表記で(正規化前でよい)。' }

// ================= 実行 =================
phase('Warmup')
log('スリムKB: intents='+KB.length+' redflags='+RF.length)

phase('Loop')
for (var r=1; r<=__ROUNDS__; r++){
  var gen = await agent(genPrompt(r), { label:'gen-r'+r, phase:'Round '+r, schema:GEN_SCHEMA, model:'fable', effort:'medium' })
  var cases = ((gen&&gen.cases)||[]).filter(function(x){ return typeof x==='string' && x.trim() }).slice(0,30)
  if(!cases.length){ log('R'+r+': 生成ゼロ・スキップ'); continue }
  var landed = cases.map(function(t){ return { t:t, land:landing(t) } })
  var judge = await agent(judgePrompt(r, landed), { label:'judge-r'+r, phase:'Round '+r, schema:JUDGE_SCHEMA, model:'fable', effort:'high' })
  var results = (judge&&judge.results)||[]
  var okN=0, fixedN=0, revN=0, triedN=0
  // ok件を先に確定正着地として登録（後続の非退行ゲートの土台にする）
  for(var a=0;a<results.length;a++){ var res=results[a]; var c=landed[res.i]; if(!c) continue;
    if(res.verdict==='ok' && c.land.type==='hit'){ regset.push({text:c.t, expectId:c.land.id}); allCases.push({text:c.t, expectId:c.land.id, verdict:'ok'}); okN++ } }
  // 修正系
  for(var b=0;b<results.length;b++){ var rs=results[b]; var cc=landed[rs.i]; if(!cc) continue;
    if(rs.verdict==='miss_none'||rs.verdict==='miss_wrong'){ triedN++;
      var t=tryAdd(rs.targetIntentId, rs.kw||[], cc.t);
      if(t.ok){ fixedN++; allCases.push({text:cc.t, expectId:rs.targetIntentId, verdict:rs.verdict}) }
      else { allCases.push({text:cc.t, expectId:rs.targetIntentId||null, verdict:rs.verdict+':'+t.reason}) }
    } else if(rs.verdict==='over_redflag'){ revN++; allCases.push({text:cc.t, expectId:null, verdict:'over_redflag'}) }
    else if(rs.verdict==='new_topic'){ revN++; allCases.push({text:cc.t, expectId:null, verdict:'new_topic'}) }
  }
  var cum=0; for(var k in keptKw) cum+=keptKw[k].length
  log('R'+r+' ['+THEMES[(r-1)%THEMES.length]+'] cases='+cases.length+' ok='+okN+' fixed='+fixedN+'/'+triedN+' review='+revN+' 累計追加kw='+cum)
}

phase('Finalize')
// 最終再測定: expectId を持つケースを、元KB(KB0)と最終KBで比較
function landWith(intentsArr, text){ var n=norm(text);
  for(var i=0;i<RF.length;i++){ if(n.indexOf(RF[i])>=0) return '__redflag__' }
  var best=null,bs=0,bf=1e9;
  for(var a=0;a<intentsArr.length;a++){ var it=intentsArr[a],sc=0,fh=-1;
    for(var j=0;j<it.kw.length;j++){ var kk=norm(it.kw[j]); if(kk&&n.indexOf(kk)>=0){ sc+=kk.length; if(fh<0)fh=j } }
    if(sc>0 && (sc>bs || (sc===bs&&fh<bf))){ bs=sc; bf=fh; best=it.id } }
  return best }
var KB0 = SLIM.intents.map(function(it){ return { id:it.id, kw:(it.kw||[]).slice() } })
var fixable = allCases.filter(function(c){ return c.expectId })
var base=0, fin=0
for(var i=0;i<fixable.length;i++){ if(landWith(KB0, fixable[i].text)===fixable[i].expectId) base++; if(landWith(KB, fixable[i].text)===fixable[i].expectId) fin++ }
var totalKw=0; for(var k2 in keptKw) totalKw+=keptKw[k2].length

return {
  rounds: 10,
  totalCases: allCases.length,
  fixableCases: fixable.length,
  baselineCorrect: base,
  finalCorrect: fin,
  baselinePct: fixable.length? Math.round(base/fixable.length*100):0,
  finalPct: fixable.length? Math.round(fin/fixable.length*100):0,
  keptKwCount: totalKw,
  keptKw: keptKw,
  reviewNewTopic: allCases.filter(function(c){ return c.verdict==='new_topic' }).map(function(c){ return c.text }),
  reviewOverRedflag: allCases.filter(function(c){ return c.verdict==='over_redflag' }).map(function(c){ return c.text }),
  regressionSet: fixable.map(function(c){ return {text:c.text, expectId:c.expectId} }),
}
`.replace("__DATA__", DATA).replace("__THEMES__", JSON.stringify(THEMES)).replace("__ROUNDS__", String(ROUNDS));

const out = process.env.OUT || "accuracy-loop.wf.mjs";
writeFileSync(new URL("./" + out, import.meta.url), script);
console.log(out + " 生成 bytes=" + script.length + " rounds=" + ROUNDS + " themes=" + THEMES.length + " intents=" + slim.intents.length);
