// #きょうのオガトレ 検索・カタログ表示
const TAGS=["朝","夜・寝る前","座ったまま","全身","肩・肩甲骨","首・肩こり","姿勢・背中","股関節","開脚","もも裏","太もも・お尻","腰","ひざ・O脚","足首・足うら","むくみ","引き締め","筋膜・マッサージ","自律神経","スポーツ・運動前後","生活・セルフケア","解説","水族館ロケ","古民家ロケ","ショート","10分以内","その他"];
const CAT=(typeof CATALOG==="undefined")?[]:CATALOG;
let activeTag=null, searchLimit=24;
const TAG_COLOR={"朝":"a","夜・寝る前":"a","座ったまま":"a","10分以内":"a","ショート":"a","全身":"b","肩・肩甲骨":"b","首・肩こり":"b","姿勢・背中":"b","股関節":"b","開脚":"b","もも裏":"b","太もも・お尻":"b","腰":"b","ひざ・O脚":"b","足首・足うら":"b","むくみ":"c","引き締め":"c","筋膜・マッサージ":"c","自律神経":"c","スポーツ・運動前後":"c","生活・セルフケア":"c","解説":"d"};
const TAG_CATS=[
 {key:"b", name:"からだの場所", tags:["全身","肩・肩甲骨","首・肩こり","姿勢・背中","股関節","開脚","もも裏","太もも・お尻","腰","ひざ・O脚","足首・足うら"]},
 {key:"a", name:"時間・シーン", tags:["朝","夜・寝る前","座ったまま","10分以内","ショート"]},
 {key:"c", name:"目的", tags:["むくみ","引き締め","筋膜・マッサージ","自律神経","スポーツ・運動前後","生活・セルフケア"]},
 {key:"d", name:"その他", tags:["解説","水族館ロケ","古民家ロケ","その他"]},
];
let activeCat="b";
function buildChips(){
  renderCats();
  const years=[...new Set(CAT.map(v=>v.y))].filter(Boolean).sort((a,b)=>b-a);
  document.getElementById("ySel").innerHTML = `<option value="">すべての年</option>`+
    years.map(y=>`<option value="${y}">${y}年</option>`).join("");
}
function renderCats(){
  document.getElementById("catRow").innerHTML = TAG_CATS.map(c=>
    `<button class="catbtn${c.key===activeCat?" on":""}" onclick="setCat('${c.key}')">${c.name}</button>`).join("");
  const cat=TAG_CATS.find(c=>c.key===activeCat);
  document.getElementById("chips").innerHTML = cat.tags.map(t=>
    `<button class="chip chip-${TAG_COLOR[t]||"d"}${t===activeTag?" on":""}" onclick="toggleChipByName('${t}')">${t}</button>`).join("");
}
function setCat(k){ activeCat=k; renderCats(); }
function toggleChipByName(t){
  activeTag = activeTag===t ? null : t;
  renderCats(); renderSearch();
}
function currentHits(){
  const q=(document.getElementById("q").value||"").trim().toLowerCase();
  const yr=document.getElementById("ySel").value;
  return CAT.filter(v=>{
    if(activeTag && !(v.tags||[]).includes(activeTag)) return false;
    if(yr && String(v.y)!==yr) return false;
    if(!q) return true;
    const hay=(v.t+" "+(v.tags||[]).join(" ")+" "+v.y+"年").toLowerCase();
    return q.split(/\s+/).every(w=>hay.includes(w));
  });
}
function renderSearch(){ searchLimit=24; drawResults(); }
function moreResults(){ searchLimit+=48; drawResults(); }
function drawResults(){
  const hits=currentHits();
  const shown=hits.slice(0,searchLimit);
  document.getElementById("hitCount").textContent=`${hits.length}本`;
  document.getElementById("filterNow").innerHTML = activeTag
    ? `<button class="chip chip-${TAG_COLOR[activeTag]||"d"} on" onclick="toggleChipByName('${activeTag}')">${activeTag} ✕</button><span style="font-size:13px;color:var(--sub);margin-left:8px">タップで解除</span>`
    : "";
  const offlineCat=(typeof CATALOG==="undefined");
  document.getElementById("vlist").innerHTML = shown.length
    ? shown.map(v=>vHTML(v,(v.tags||[])[0])).join("")
    : `<div class="card" style="text-align:center;color:var(--sub);font-size:15px"><img src="assets/chara-2.png" alt="" style="width:84px;height:84px;object-fit:contain;display:block;margin:0 auto 6px">${offlineCat?"動画リストがよみこめませんでした📡<br>電波のよいところでもう一度ひらいてみてね":"この条件のストレッチはまだないみたい…"}</div>`;
  const more=document.getElementById("moreBtn");
  more.classList.toggle("hidden", hits.length<=searchLimit);
  more.textContent=`さらに表示（あと${Math.max(0,hits.length-searchLimit)}本）`;
  const q=(document.getElementById("q").value||"").trim();
  const kwText=[q, activeTag].filter(Boolean).join(" ");
  document.getElementById("reqMsg").innerHTML = shown.length
    ? "やりたいストレッチが見つからない？<br>オガトレに直接リクエストを送れます📮"
    : (offlineCat ? "" : "ごめんなさい まだなかった…！<br>リクエストを送ってもらえたら動画づくりの参考にします📮");
  document.getElementById("reqBtn").classList.toggle("hidden", offlineCat&&!shown.length);
  const subject=encodeURIComponent("ストレッチのリクエスト（きょうのオガトレ）");
  const body=encodeURIComponent(`こんなストレッチの動画が欲しいです：\n${kwText||"（ここに書いてください）"}\n\n--\nきょうのオガトレ「さがす」から送信`);
  document.getElementById("reqBtn").href=`mailto:kyou-no@ogatore.jp?subject=${subject}&body=${body}`;
  document.getElementById("reqBtn").textContent = kwText ? `「${kwText}」をリクエストする` : "リクエストを送る";
}
