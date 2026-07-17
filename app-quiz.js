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
  {k:"kenko",title:"指先を鎖骨において ひじはどこまで上がる？", note:"ひじを体の前で ゆっくり上げてみて",
   opts:[
    ["眉より高く上がる","ひじが眉の高さをこえる",0],
    ["鼻くらいまで","ひじが鼻の高さで止まる",1],
    ["あごくらいまで","ひじがあごの高さで止まる",2],
    ["あごまで上がらない","ひじがあごより下で止まる",3]]},
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
    rx:["kenko12"], pool:["yoru15","asa5","kenkoIsho","kenko22","kenkoIsho2","kenko3cho","katakori","katakori8","zutsu7","suwatta8","nagomi7"]},
  ashi: {name:"棒立ちペンギン", area:"足首",
    copy:"しゃがむとかかとがプカッ あるいは後ろにコロン。それは足首がカチッと固まっている証拠。ペンギンは可愛いけど、転ぶと痛い。",
    hope:"足首がゆるむと、歩くのも立つのも軽くなります。つまむだけの簡単ストレッチから始めましょう。",
    pt:"硬いのは<b>足首の背屈（すねに向けて曲げる動き）＝ふくらはぎ・アキレス腱まわり</b>。ここが硬いと、しゃがむ動作でかかとが浮き、<b>つまずき・むくみ・ふくらはぎの張り</b>につながります。足首は毎日使う関節なので、ゆるめた効果を実感しやすい場所です。",
    rx:["ashi1"], pool:["ashi2","ashi10","ashi3cho","ashiIsho","fukura5","fukuraMassa","fukura8","ashi4","katai8st","ashisuki"]},
  robot:{name:"ガチガチロボット", area:"全身",
    copy:"全体的に、ガチガチ。でも言いかえれば、どこを伸ばしても効く「伸びしろの宝庫」ということ。",
    hope:"ロボットにも心はあります。全身をやさしくほぐす1本から始めれば、ガチガチの体もちゃんと応えてくれます。",
    pt:"特定の場所というより<b>全身が複合的に硬い状態</b>。この場合は部位を絞るより、全身をまんべんなく動かすルーティンで底上げするのが近道です。<b>どこを伸ばしても効く＝変化を感じやすい</b>ので、実はいちばん楽しいスタート地点だったりします。",
    rx:["honki9"], pool:["asa10","asaBaki9","yoru9umi","yoru9ice","yaruki22","zenshinCho","yoru12kai","ofuro20","zenshin15","senaka5","ofuro10","nagomi7"]},
  yawara:{name:"しなやかネコ", area:"メンテナンス",
    copy:"おっと、けっこうしなやか！あなたはもう「しなやかネコ」。ここから先は、そのしなやかさを守るステージです。",
    hope:"しなやかさは資産。猫が毎朝伸びをするみたいに、朝と夜の習慣で守っていきましょう。悩みに合わせた1本もどうぞ。",
    pt:"関節の可動域は良好です。次の課題は<b>「維持」と「使い方」</b>。柔らかくても、支える筋力や毎日の習慣が崩れると体は硬さに戻ります。朝晩の軽いルーティンで可動域を守りつつ、悩みのある部位を先回りでケアしましょう。",
    rx:[], pool:["asa10","asa9shi","asaGachi5","asa3","honki9","yoru9umi","jukusui9","jiritsu10","ofuro6","choyokin10","ibuki10","nagomi7"]},
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
  // Q3 肘上げ（あご/鼻/眉のレベル線）※実写が撮れたら差し替え
 `<svg viewBox="0 0 300 170" fill="none" stroke-linecap="round" stroke-linejoin="round">
   <path d="M30 150h240" stroke="#E0D8C4" stroke-width="5"/>
   <g stroke="#CFC9B8" stroke-width="2.5" stroke-dasharray="2 6">
     <path d="M168 40h66M168 54h66M168 66h66"/>
   </g>
   <g fill="#6E6B5F" stroke="none" font-weight="800" font-size="11">
     <text x="240" y="44">まゆ</text><text x="240" y="58">鼻</text><text x="240" y="70">あご</text>
   </g>
   <path d="M116 108L112 148M130 108L134 148" stroke="#55524A" stroke-width="9"/>
   <path d="M123 104L123 72" stroke="#3A3A35" stroke-width="15"/>
   <circle cx="123" cy="52" r="15" fill="#FFE3C9" stroke="#3A3A35" stroke-width="4"/>
   <path d="M110 46q5-10 15-9q9 2 11 9q-7-3-13-2q-8 0-13 2z" fill="#3A3A35"/>
   <circle cx="118" cy="53" r="2" fill="#3A3A35"/><circle cx="128" cy="53" r="2" fill="#3A3A35"/>
   <path d="M120 60q3 3 6 0" stroke="#3A3A35" stroke-width="2.2"/>
   <path d="M112 74L104 90" stroke="#E8B48C" stroke-width="7"/>
   <path d="M134 76Q150 70 152 56" stroke="#E8B48C" stroke-width="8"/>
   <path d="M152 56Q146 66 136 70" stroke="#E8B48C" stroke-width="7"/>
   <circle cx="134" cy="72" r="4" fill="#E8B48C"/>
   <circle cx="153" cy="54" r="7" stroke="#E56A9A" stroke-width="3.5"/>
   <path d="M162 50V26" stroke="#E56A9A" stroke-width="4"/>
   <path d="M156 32l6 -8l6 8" stroke="#E56A9A" stroke-width="3.5"/>
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
function finishQuiz(){
  const t=decideType();
  state.type={key:t, worry:state.worry, at:todayStr()};
  store.set("type", state.type);
  showResult(state.type);
}

function currentRx(typeKey){
  const T=TYPES[typeKey];
  const need=3-T.rx.length;
  const r=rotationIndex();
  const picks=[];
  for(let i=0;i<need;i++) picks.push(T.pool[(r+i)%T.pool.length]);
  return T.rx.concat(picks);
}

function showResult(saved){
  const T=TYPES[saved.key];
  // オンボからかたさチェックに来た人（ツアー未見）には「つづき：使い方ツアー」を出す
  try{ const tb=document.getElementById("rTourBtn"); if(tb) tb.classList.toggle("hidden",!(typeof obTourAfterQuiz!=="undefined"&&obTourAfterQuiz)); }catch(e){}
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
  const fixed=T.rx.length>0;
  document.getElementById("rxHead").innerHTML=`${ICON_RX}`+(fixed?`おすすめの3本: まずは「${T.area}」から！2週間つづけてみて`:`おすすめの3本: 柔らかさを守る毎日の1本をどうぞ`);
  const rx=currentRx(saved.key);
  document.getElementById("rxList").innerHTML = rx.map((vk,i)=>videoCard(vk, fixed?["①まずほぐす","②メインの1本","③しあげ"][i]:null)).join("")
    +`<a class="btn btn-ghost" style="font-size:15px;margin-top:4px" href="https://www.youtube.com/watch_videos?video_ids=${rx.map(k=>V[k].id).join(",")}" target="_blank" rel="noopener">▶ 3本つづけて再生する</a>`;
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
