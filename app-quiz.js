// #きょうのオガトレ かたさチェック（診断・結果表示）

const TYPE_ART = {
  koka: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 96 96"><rect x="24" y="12" width="48" height="74" rx="6" fill="#D9A066" stroke="#3A3A35" stroke-width="3"/><rect x="31" y="20" width="34" height="26" rx="4" fill="#E8B87E" stroke="#B4805A" stroke-width="2"/><rect x="31" y="52" width="34" height="26" rx="4" fill="#E8B87E" stroke="#B4805A" stroke-width="2"/><circle cx="63" cy="49" r="3.5" fill="#3A3A35"/><circle cx="39" cy="33" r="1.8" fill="#3A3A35"/><circle cx="53" cy="33" r="1.8" fill="#3A3A35"/><path d="M42 39q4 2.5 8 0" stroke="#3A3A35" stroke-width="2.2" fill="none" stroke-linecap="round"/></svg>`,
  ashi: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 96 96"><ellipse cx="48" cy="54" rx="24" ry="30" fill="#4E5A6E" stroke="#3A3A35" stroke-width="3"/><ellipse cx="48" cy="60" rx="15" ry="20" fill="#FFFFFF"/><circle cx="41" cy="40" r="2.2" fill="#3A3A35"/><circle cx="55" cy="40" r="2.2" fill="#3A3A35"/><path d="M44 47l4 3 4-3z" fill="#F5A25D" stroke="#3A3A35" stroke-width="2" stroke-linejoin="round"/><path d="M34 82q4 3 9 1M62 82q-4 3-9 1" stroke="#F5A25D" stroke-width="4" fill="none" stroke-linecap="round"/><path d="M24 44q-4 8 2 14M72 44q4 8-2 14" stroke="#3A3A35" stroke-width="3" fill="none" stroke-linecap="round"/></svg>`,
  robot:`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 96 96"><rect x="24" y="26" width="48" height="42" rx="10" fill="#C7D3DE" stroke="#3A3A35" stroke-width="3"/><path d="M48 26V14" stroke="#3A3A35" stroke-width="3" stroke-linecap="round"/><circle cx="48" cy="12" r="4" fill="#FF8A70" stroke="#3A3A35" stroke-width="2.5"/><rect x="33" y="38" width="10" height="10" rx="3" fill="#FFFFFF" stroke="#3A3A35" stroke-width="2.5"/><rect x="53" y="38" width="10" height="10" rx="3" fill="#FFFFFF" stroke="#3A3A35" stroke-width="2.5"/><circle cx="38" cy="43" r="1.8" fill="#3A3A35"/><circle cx="58" cy="43" r="1.8" fill="#3A3A35"/><path d="M40 57h16" stroke="#3A3A35" stroke-width="2.5" stroke-linecap="round"/><rect x="30" y="72" width="36" height="10" rx="5" fill="#E4EAF0" stroke="#3A3A35" stroke-width="2.5"/></svg>`,
};

// ================= 診断（選択肢に理学療法士の目安つき） =================
const QUESTIONS = [
  {k:"momo", title:"立って前屈 手はどこまでいく？", note:"ひざを曲げずに ゆっくり倒れてみて",
   opts:[
    ["床にペタッとつく","手のひら全体がゆかにつく（ゆかタッチ）",0],
    ["つま先にさわれる","指先が足先〜床すれすれ（目安 0〜10cm）",1],
    ["すねの途中まで","指先がすねの中ほどで止まる（目安 10〜25cm）",2],
    ["ひざから下に行かない","指先がひざ上で止まる（目安 25cm以上）",3]]},
  {k:"koka", title:"あぐらで座ると ひざは？", note:"床に座って 足の裏どうしを合わせてみて",
   opts:[
    ["床にペタッと近い","ひざと床のすき間が こぶし1個未満",0],
    ["ちょっと浮く","すき間 こぶし1〜2個ぶん",1],
    ["山みたいに浮く","すき間 こぶし3個以上",2],
    ["そもそもあぐらがつらい","骨盤が立たず 体が後ろに倒れてしまう",3]]},
  {k:"kenko",title:"胸の前で両ひじをつけて上げると どこまで上がる？", note:"手のひらを合わせて 胸の前でひじをくっつけたまま ゆっくり上げてみて",
   opts:[
    ["頭より上まで上がる","ひじをつけたまま頭の上まで上がる",0],
    ["あごより上まで上がる","ひじをつけたままあごの高さをこえる",1],
    ["ひじはつくけど あまり上がらない","ひじはくっつくが胸〜肩の高さまでしか上がらない",2],
    ["そもそもひじがつかない","胸の前でひじをくっつけることができない",3]]},
  {k:"ashi", title:"かかとを付けたまま しゃがめる？", note:"和式トイレのポーズ 無理はしないでね",
   opts:[
    ["余裕でしゃがめる","かかとを付けたまま深くしゃがみ 保持できる",0],
    ["しゃがめるけど ぐらぐら","しゃがめるが姿勢を保てない",1],
    ["かかとが浮いちゃう","足首の曲がり（背屈）が足りないサイン",2],
    ["後ろにコロンと転がる","足首＋股関節の複合的な硬さのサイン",3]]},
  {k:"worry",title:"いちばんの悩みは？", note:"あなたに合うおすすめの仕上げに使います",
   opts:[
    ["肩こり・首こり","デスクワーク・スマホ首のお供に","katakori"],
    ["腰痛","骨盤まわりからケアします","yotsu"],
    ["疲れ・眠りの浅さ","自律神経をととのえます","tsukare"],
    ["とにかく柔らかくなりたい","王道の柔軟コースへ","yawaraka"]]},
];

// ================= タイプ（解説つき・処方はローテーション制） =================
const TYPES = {
  momo: {name:"つっぱりモモンガ", area:"もも裏",
    copy:"前屈すると、つま先がとても遠い。それはあなたの脚が長い…わけではなく、もも裏がモモンガの滑空ポーズみたいにピンとつっぱっているサイン。",
    hope:"でもモモンガも、着地すればちゃんと脚をゆるめます。もも裏は変化が出やすい場所。2週間後の前屈で、床がぐっと近くなってるはず。",
    pt:"硬いのは<b>ハムストリングス（もも裏の筋肉）</b>。ここが硬いと骨盤が後ろに倒れたまま固定され、前屈で腰だけが無理に曲がります。放っておくと<b>腰痛や座り姿勢の悪化</b>につながる場所。逆に言えば、もも裏をゆるめるだけで前屈も腰もラクになります。",
    rx:["momo7"], pool:["kaikyaku","momoKai","momoIsho","zenkutsu15","hamu10","kaikyaku2","kotsuban5","yotsu12","yotsu8","asa10","nagomi7"]},
  koka: {name:"開かずのトビラ", area:"股関節",
    copy:"あぐらでひざが山になるのは、股関節のとびらが閉まっているから。股関節の封印は解きたいですよね。",
    hope:"とびらは、毎日すこしずつ油をさせば開きます。股関節は9分の習慣がいちばん効く場所。あせらずコツコツ。",
    pt:"硬いのは<b>内もも（内転筋）とお尻（大臀筋・梨状筋）</b>。股関節を外に開く動きが制限されて、あぐら・開脚が苦手になります。股関節は体の土台なので、ここが動くと<b>歩く・座る・立つ全部がラクに</b>。腰への負担も減ります。",
    rx:["koka9"], pool:["kominka","kokaSai","koka22","koka3cho","kokaIsho","kokaPoki","kaikyaku","kaikyaku90","nagomi7","ashisuki","yotsu12"]},
  kenko:{name:"飛べないダチョウ", area:"肩甲骨",
    copy:"背中で手がつながらないのは、肩甲骨まわりの羽根が飛べないダチョウみたいに、すっかり休眠しているから。デスクワークの勲章です。",
    hope:"ダチョウの羽根だって、バサバサ動かせば血が巡ります。肩甲骨がゆるむと、肩こりも呼吸もぐっとラクに。",
    pt:"硬いのは<b>肩甲骨まわり（僧帽筋・広背筋・大胸筋など）</b>。肩甲骨の動きが小さくなると、首と肩の筋肉が代わりに働き続けて<b>肩こり・巻き肩・浅い呼吸</b>の原因に。肩甲骨を動かす習慣がつくと、背中が軽くなって姿勢も変わります。",
    // yoru15(16分)は16分以上は毎日のおすすめから外す(2026-07-18 PO方針: 視聴体験として長すぎる)
    rx:["kenko12"], pool:["asa5","kenkoIsho","kenko22","kenkoIsho2","kenko3cho","katakori","katakori8","zutsu7","suwatta8","nagomi7"]},
  ashi: {name:"棒立ちペンギン", area:"足首",
    copy:"しゃがむとかかとがプカッ あるいは後ろにコロン。それは足首がカチッと固まっている証拠。ペンギンは可愛いけど、転ぶと痛い。",
    hope:"足首がゆるむと、歩くのも立つのも軽くなります。つまむだけの簡単ストレッチから始めましょう。",
    pt:"硬いのは<b>足首の背屈（すねに向けて曲げる動き）＝ふくらはぎ・アキレス腱まわり</b>。ここが硬いと、しゃがむ動作でかかとが浮き、<b>つまずき・むくみ・ふくらはぎの張り</b>につながります。足首は毎日使う関節なので、ゆるめた効果を実感しやすい場所です。",
    rx:["ashi1"], pool:["ashi2","ashi10","ashi3cho","ashiIsho","fukura5","fukuraMassa","fukura8","ashi4","katai8st","ashisuki"]},
  robot:{name:"ガチガチロボット", area:"全身",
    copy:"全体的に、ガチガチ。でも言いかえれば、どこを伸ばしても効く「伸びしろの宝庫」ということ。",
    hope:"ロボットにも心はあります。全身をやさしくほぐす1本から始めれば、ガチガチの体もちゃんと応えてくれます。",
    pt:"特定の場所というより<b>全身が複合的に硬い状態</b>。この場合は部位を絞るより、全身をまんべんなく動かすルーティンで底上げするのが近道です。<b>どこを伸ばしても効く＝変化を感じやすい</b>ので、実はいちばん楽しいスタート地点だったりします。",
    // yaruki22(23分)・ofuro20(20分)・zenshin15(16分)は16分以上は毎日のおすすめから外す(2026-07-18 PO方針: 視聴体験として長すぎる)
    rx:["honki9"], pool:["asa10","asaBaki9","yoru9umi","yoru9ice","zenshinCho","yoru12kai","senaka5","ofuro10","nagomi7"]},
  yawara:{name:"しなやかネコ", area:"メンテナンス",
    copy:"おっと、けっこうしなやか！あなたはもう「しなやかネコ」。ここから先は、そのしなやかさを守るステージです。",
    hope:"しなやかさは資産。猫が毎朝伸びをするみたいに、朝と夜の習慣で守っていきましょう。悩みに合わせた1本もどうぞ。",
    pt:"関節の可動域は良好です。次の課題は<b>「維持」と「使い方」</b>。柔らかくても、支える筋力や毎日の習慣が崩れると体は硬さに戻ります。朝晩の軽いルーティンで可動域を守りつつ、悩みのある部位を先回りでケアしましょう。",
    rx:["asa10"], pool:["asa9shi","asaGachi5","asa3","honki9","yoru9umi","jukusui9","jiritsu10","ofuro6","choyokin10","ibuki10","nagomi7"]},
};

const WORRY = {
  katakori:{v:"katakori", label:"肩こりさんへ もう1本"},
  yotsu:   {v:"yotsu12",  label:"腰痛さんへ もう1本"},
  tsukare: {v:"jiritsu10",label:"おつかれさんへ もう1本"},
  yawaraka:null,
};

// ---- チェック質問のポーズ図解（書き下ろしSVG） ----
const QUIZ_ART=[
 `<img src="assets/check/q1.jpg" alt="立って前屈のお手本">`,
 `<img src="assets/check/q2.jpg" alt="あぐらのお手本">`,
  // Q3 胸の前で両ひじをつけて上げる（肩甲骨の硬さチェック・本人のYouTube「肩甲骨12分」動画準拠）
  // ※実写が撮れたら差し替え。あたま/あごの高さ線＝両ひじが上がる目安、ピンクの丸＝ひじをつける位置
 `<svg viewBox="0 0 300 170" fill="none" stroke-linecap="round" stroke-linejoin="round">
   <path d="M30 150h240" stroke="#E0D8C4" stroke-width="5"/>
   <g stroke="#CFC9B8" stroke-width="2.5" stroke-dasharray="2 6">
     <path d="M136 45h74M136 68h74"/>
   </g>
   <g fill="#6E6B5F" stroke="none" font-weight="800" font-size="11">
     <text x="215" y="49">あたま</text><text x="215" y="72">あご</text>
   </g>
   <path d="M112 118L106 150M132 118L138 150" stroke="#55524A" stroke-width="9"/>
   <path d="M122 118L122 80" stroke="#3A3A35" stroke-width="15"/>
   <circle cx="122" cy="58" r="13" fill="#FFE3C9" stroke="#3A3A35" stroke-width="4"/>
   <path d="M110 51q4-9 12-8q8 2 10 8q-6-3-11-2q-7 0-11 2z" fill="#3A3A35"/>
   <circle cx="118" cy="59" r="2" fill="#3A3A35"/><circle cx="126" cy="59" r="2" fill="#3A3A35"/>
   <path d="M120 65q2 2 4 0" stroke="#3A3A35" stroke-width="2.2"/>
   <path d="M108 80Q106 58 118 42" stroke="#E8B48C" stroke-width="8"/>
   <path d="M118 42Q120 30 122 20" stroke="#E8B48C" stroke-width="7"/>
   <path d="M136 80Q138 58 126 42" stroke="#E8B48C" stroke-width="8"/>
   <path d="M126 42Q124 30 122 20" stroke="#E8B48C" stroke-width="7"/>
   <circle cx="122" cy="41" r="7" fill="#E56A9A"/>
   <circle cx="122" cy="20" r="5" fill="#FFE3C9" stroke="#3A3A35" stroke-width="2"/>
   <path d="M122 18V4" stroke="#E56A9A" stroke-width="4"/>
   <path d="M116 10l6 -8l6 8" stroke="#E56A9A" stroke-width="3.5"/>
 </svg>`,
 // Q4 深くしゃがむ: かかとの浮きをズーム円で拡大
 `<svg viewBox="0 0 300 170" fill="none" stroke-linecap="round" stroke-linejoin="round">
   <path d="M30 150h240" stroke="#E0D8C4" stroke-width="5"/>
   <path d="M120 112L152 110" stroke="#55524A" stroke-width="13"/>
   <path d="M120 112L134 142" stroke="#55524A" stroke-width="11"/>
   <path d="M120 148L142 144" stroke="#3A3A35" stroke-width="6"/>
   <path d="M141 146v4" stroke="#E56A9A" stroke-width="3"/>
   <path d="M152 112Q150 88 158 76" stroke="#3A3A35" stroke-width="15"/>
   <circle cx="152" cy="60" r="15" fill="#FFE3C9" stroke="#3A3A35" stroke-width="4"/>
   <path d="M141 52q4-10 15-8q9 2 10 9q-7-3-13-2q-7 0-12 1z" fill="#3A3A35"/>
   <circle cx="146" cy="62" r="2.2" fill="#3A3A35"/>
   <path d="M156 80L118 86" stroke="#E8B48C" stroke-width="8"/>
   <circle cx="113" cy="87" r="5" fill="#E8B48C"/>
   <path d="M158 140q14 10 32 4" stroke="#CFC9B8" stroke-width="2.5" stroke-dasharray="3 4"/>
   <circle cx="224" cy="110" r="34" style="fill:var(--card)" stroke="#3A3A35" stroke-width="3.5"/>
   <path d="M204 128h40" stroke="#E0D8C4" stroke-width="4"/>
   <path d="M209 124L235 118" stroke="#3A3A35" stroke-width="6"/>
   <path d="M234 120v8" stroke="#E56A9A" stroke-width="3.5" stroke-dasharray="2 3"/>
   <path d="M229 122l5 6l5 -6" stroke="#E56A9A" stroke-width="3"/>
   <text x="224" y="98" fill="#E56A9A" font-size="12" font-weight="800" text-anchor="middle" stroke="none">かかと</text>
 </svg>`,
 // Q5 いちばんの悩み
 `<svg viewBox="0 0 300 170" fill="none" stroke="#3A3A35" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"><path d="M150 140C114 115 96 92 104 70q10-18 30-10q8 4 16 15q8-11 16-15q20-8 30 10c8 22-10 45-46 70z" fill="#FFEDF3" stroke="#E56A9A" stroke-width="5"/><path d="M132 74q7-10 17-7" stroke="#F5AFC6" stroke-width="4"/></svg>`,
];

// ---- 診断 ----
// presetWorry: オンボーディングQ2で悩みに回答済みの場合、その悩み(WORRYキー)を渡すとQ5(悩み質問)を
// スキップして4問構成にする。省略時（使い方タブ/マイ記録等からの単独起動）は従来どおり5問すべて聞く。
function startQuiz(presetWorry){
  state.qi=0; state.scores={momo:0,koka:0,kenko:0,ashi:0};
  state.worry = presetWorry || null;
  state.presetWorry = !!presetWorry; // trueならQ5(悩み)を出題しない
  state.picked={}; quizAborted=false; renderQ(); navTo("quiz");
}
// 出題対象の質問配列（presetWorry時はk:"worry"のQ5を除く）。worryは常に配列の最後尾のため、
// 除いてもmomo/koka/kenko/ashiの並び順・QUIZ_ARTのインデックス対応(0〜3)はそのまま
function activeQuestions(){ return state.presetWorry ? QUESTIONS.filter(q=>q.k!=="worry") : QUESTIONS; }
function renderQ(){
  quizRendered=true;
  // 質問ごとに履歴を1件積む（ブラウザ/スワイプの「戻る」で1問ずつ遡れるようにする）。
  // popstateでの復元時はhistory.stateのqiと一致するため重複pushしない
  try{ if(!(history.state&&history.state.id==="quiz"&&history.state.qi===state.qi)) history.pushState({id:"quiz",qi:state.qi},""); }catch(e){}
  const QS = activeQuestions();
  const q = QS[state.qi];
  document.getElementById("qnum").textContent = `Q${state.qi+1} / ${QS.length}`;
  document.getElementById("qtitle").textContent = q.title;
  document.getElementById("qArt").innerHTML = QUIZ_ART[state.qi]||"";
  document.getElementById("qnote").textContent = q.note;
  const box = document.getElementById("opts"); box.innerHTML="";
  q.opts.forEach(o=>{
    const b=document.createElement("button"); b.className="opt";
    // 「まえの質問へ」で戻ってきたとき、前に選んだ答えがわかるように枠色を付ける
    if(state.picked&&(q.k in state.picked)&&state.picked[q.k]===o[2]) b.classList.add("on");
    b.innerHTML=`${o[0]}<span class="crit">${o[1]}</span>`;
    b.onclick=()=>answer(q.k,o[2]); box.appendChild(b);
  });
  const dots=document.getElementById("dots"); dots.innerHTML="";
  QS.forEach((_,i)=>{const d=document.createElement("div");d.className="dot"+(i<=state.qi?" on":"");dots.appendChild(d)});
  document.getElementById("qBackBtn").classList.toggle("hidden", state.qi===0);
}
function answer(k,val){
  document.querySelectorAll("#opts .opt").forEach(b=>b.disabled=true);
  if(!state.picked) state.picked={};
  state.picked[k]=val;
  if(k==="worry"){ state.worry=val; finishQuiz(); return; }
  state.scores[k]=val; state.qi++;
  // presetWorry時はQ5(悩み)が配列に無いため、最後の身体質問(ashi)の直後でそのまま結果へ
  if(state.qi>=activeQuestions().length){ finishQuiz(); return; }
  renderQ();
}
function prevQ(){ if(state.qi>0){ state.qi--; renderQ(); } }

function decideType(){
  const s=state.scores;
  const total=s.momo+s.koka+s.kenko+s.ashi;
  if(total>=9) return "robot";
  if(total<=2) return "yawara";
  const order=["momo","koka","kenko","ashi"];
  let best=order[0]; for(const k of order){ if(s[k]>s[best]) best=k; }
  if(s[best]<=1) return "yawara";
  return best;
}
// クイズQ1(momo・立って前屈)の選択肢インデックス(0〜3)→とどくメーターの段位(1〜5)対応表。
// 「床につく/つま先/すねの途中/ひざ下」の4段階と「ひざ/すね/足首/つま先/ゆか」の5段階はズレがあり、
// メーター側の「足首」に相当する選択肢はクイズに無いため自動では選ばれない(仕様上の許容差)。
const REACH_FROM_MOMO=[5,4,2,1];
let quizAutoReachLv=null;
function finishQuiz(){
  const t=decideType();
  state.type={key:t, worry:state.worry, at:todayStr()};
  store.set("type", state.type);
  // 2026-07-19 本人指摘「とどくメーターとかたさチェックのQ1が前屈を二重に測っている」対応。
  // とどくメーターがまだ1件も無ければ、Q1の回答を初回記録として自動で書きこむ(A案・自動転記)。
  // 既に記録がある場合は上書きしない(ユーザーが自分で測った値を優先する)。結果画面での一言表示のため
  // quizAutoReachLvに記録し、showResult側で使い切ったらnullに戻す(再表示時に出ないように)。
  quizAutoReachLv=null;
  try{
    if(typeof getReach==="function" && typeof setReach==="function" && !getReach().length && state.picked && ("momo" in state.picked)){
      const lv=REACH_FROM_MOMO[state.picked.momo];
      setReach(lv);
      quizAutoReachLv=lv;
    }
  }catch(e){}
  showResult(state.type);
}

function currentRx(typeKey){
  const T=TYPES[typeKey];
  const need=3-T.rx.length;
  const r=rotationIndex();
  const L=T.pool.length;
  const spacing=Math.floor(L/(need===3?3:2));
  const picks=[];
  for(let i=0;i<need;i++){
    let idx=(r+i*spacing)%L;
    let tries=0;
    while((T.rx.indexOf(T.pool[idx])!==-1 || picks.indexOf(T.pool[idx])!==-1) && tries<L){
      idx=(idx+1)%L;
      tries++;
    }
    picks.push(T.pool[idx]);
  }
  return T.rx.concat(picks);
}

function showResult(saved){
  const T=TYPES[saved.key];
  // はじめの1本ガイド中（オンボ完走→通算0日の初回ユーザー）は結果画面を①1本だけの導線に絞る
  const guide=(typeof fdActive==="function")&&fdActive();
  // オンボからかたさチェックに来た人（ツアー未見）には「つづき：使い方ツアー」を出す。
  // guide時はrTourBtnを出さない＆obTourAfterQuizを強制falseにする（設計指定）
  try{
    if(guide) obTourAfterQuiz=false;
    const tb=document.getElementById("rTourBtn");
    if(tb) tb.classList.toggle("hidden", guide || !(typeof obTourAfterQuiz!=="undefined"&&obTourAfterQuiz));
  }catch(e){}
  // 復帰ナッジ(rDoneNudge)は前回表示分の残骸を持ち越さない（結果画面を新しく描画するたびクリア）
  try{ const rd=document.getElementById("rDoneNudge"); if(rd){ rd.classList.add("hidden"); rd.innerHTML=""; } }catch(e){}
  {
    const img=TYPE_IMG[saved.key];
    document.getElementById("rIllust").innerHTML = img
      ? `<img src="${img}" alt="">`
      : TYPE_ART[saved.key];
  }
  document.getElementById("rName").textContent=T.name;
  document.getElementById("rCopy").textContent=T.copy;
  document.getElementById("rHope").textContent="🌱 "+T.hope;
  document.getElementById("rPT").innerHTML=`<div class="pt-head">${ICON_PT}理学療法士のひとくち解説</div>`+T.pt;
  {
    const rn=document.getElementById("rReachNote");
    if(quizAutoReachLv){
      rn.textContent=`📏 いまの前屈「${REACH_LV[quizAutoReachLv]}」を とどくメーターにも記録したよ`;
      rn.classList.remove("hidden");
    }else{
      rn.classList.add("hidden"); rn.textContent="";
    }
    quizAutoReachLv=null;
  }
  const fixed=T.rx.length>0;
  document.getElementById("rxHead").innerHTML = guide
    ? `${ICON_RX}まずはこの1本から！②③はあしたからでOKだよ`
    : `${ICON_RX}`+(fixed?`おすすめの3本: まずは「${T.area}」から！2週間続けてみて`:`おすすめの3本: 柔らかさを守る毎日の1本をどうぞ`);
  const rx=currentRx(saved.key);
  if(guide){
    // ①だけを主役化(fd-hero)し、オガトレの一言吹き出しを添える。②③はそのまま表示（隠さない）
    document.getElementById("rxList").innerHTML =
      '<div class="sd-row oga" style="display:flex;gap:8px;margin-bottom:8px">'
        +'<img class="sd-ava" src="assets/chara-hitokoto.png" alt="">'
        +'<div class="sd-b">タップするとYouTubeがひらくよ 見おわったらこのアプリにもどってきて 下の「きょうやった！」を押してね💪</div>'
      +'</div>'
      +'<div class="fd-hero">'+videoCard(rx[0], "きょうはこれ1本でOK！")+'</div>'
      +rx.slice(1).map((vk,i)=>videoCard(vk, fixed?["②メインの1本","③しあげ"][i]:null)).join("");
  }else{
    document.getElementById("rxList").innerHTML = rx.map((vk,i)=>videoCard(vk, fixed?["①まずほぐす","②メインの1本","③しあげ"][i]:null)).join("")
      +`<a class="btn btn-ghost" style="font-size:15px;margin-top:4px" href="https://www.youtube.com/watch_videos?video_ids=${rx.map(k=>V[k].id).join(",")}" target="_blank" rel="noopener">▶ 3本続けて再生する</a>`;
  }
  try{ const rn=document.getElementById("rRotateNote"); if(rn) rn.classList.toggle("hidden", guide); }catch(e){}
  const w=WORRY[saved.worry];
  document.getElementById("worryExtra").innerHTML = (w&&w.v&&!rx.includes(w.v)) ? videoCard(w.v, "＋ "+w.label) : "";
  // 逆導線: タイプのareaに対応するインテントで相談室を開く（KB未読込なら出さない）
  const sl=document.getElementById("rSoudanLink");
  if(sl){
    if(sdKb()){
      const tid=(SOUDAN_TYPE_INTENT[saved.key]||[])[0];
      const arg=tid?"'"+tid+"'":"";
      sl.innerHTML='<a href="#" class="tapx" onclick="openSoudan('+arg+');return false" style="color:var(--tealink);font-weight:800">💬 この悩み、相談室で聞いてみる</a>';
      sl.classList.remove("hidden");
    }else{
      sl.classList.add("hidden");
    }
  }
  navTo("result");
}
