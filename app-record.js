// #きょうのオガトレ 記録・継続（ストレージ・つづけたカウンター・とどくメーター・ひとことにっき・マイ記録カレンダー）

const store = {
  get(k,d){ try{const v=JSON.parse(localStorage.getItem("kyono_"+k)); return v==null?d:v}catch(e){return d} },
  set(k,v){ try{ localStorage.setItem("kyono_"+k, JSON.stringify(v)); return true }catch(e){ return false } }
};

// 深夜3時までは「前日」あつかい（寝るまえ派が0時を過ぎても記録が割れない）
function todayStr(){ const d=new Date(Date.now()-3*3600*1000); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,"0")}-${String(d.getDate()).padStart(2,"0")}`; }

function getStreakData(){
  let st=store.get("streak2",null);
  if(!st||typeof st!=="object"||Array.isArray(st)){
    const old=store.get("streak",null);
    st = (old&&typeof old==="object"&&!Array.isArray(old)) ? {dates: old.last?[old.last]:[], count:old.count||0, total:old.total||0} : {dates:[],count:0,total:0};
  }
  // 形の防御：破損データ(例 {"dates":5})でも起動が止まらないよう既定値に落とす
  if(!Array.isArray(st.dates)) st.dates=[];
  if(typeof st.count!=="number"||!isFinite(st.count)) st.count=0;
  if(typeof st.total!=="number"||!isFinite(st.total)) st.total=0;
  return st;
}

function renderStreak(){
  const st=getStreakData();
  document.getElementById("streakNum").textContent=st.total||0;
  const did = st.dates.includes(todayStr());
  let contTxt = st.count>=2 ? `いま${st.count}日連続` : "";
  if(!did && st.total===0) contTxt="きょう1本やると「1日目」がはじまります🌱";
  if(!did){
    const ce=document.getElementById("cheer"); if(ce) ce.innerHTML="";
    // 前回記録日の#calAsk/#a2hsAsk残骸を残さない（再訪日にきょう分がまだのときは必ず空にする）
    // ※#tourAskは2026-07-21チュートリアルv2(ツアー自動起動化)でDOMごと廃止
    const ca=document.getElementById("calAsk"); if(ca) ca.innerHTML="";
    const aa=document.getElementById("a2hsAsk"); if(aa) aa.innerHTML="";
  }
  // はじめの1本ガイド中、まだきょうの記録がついていない間は「きょうやった！」ボタンの直上に
  // 常時表示の案内を出す（動画タップ→復帰のタイミング検知に依存するcheckDoneNudge()と違い、
  // ホーム描画のたびにfdActive()＋未記録の条件だけで出る保険。記録がつく/guide終了で消える）。
  try{
    const fdNudge=document.getElementById("fdDoneStaticNudge");
    if(fdNudge){
      const showFdNudge=(typeof fdActive==="function")&&fdActive()&&!did;
      fdNudge.classList.toggle("hidden", !showFdNudge);
    }
  }catch(e){}
  // 数日あいて券でもつなげない時は、古い連続を見せない（押した瞬間に消えたと誤解させない）
  if(!did && streakBrokenNow(st)) contTxt="きょうやると新しい章のスタート🌱";
  document.getElementById("totalDays").textContent = contTxt;
  const btn=document.getElementById("doneBtn");
  btn.innerHTML = did?"きょうの分は完了！<br>おつかれさまでした😊":"きょうやった！";
  btn.classList.toggle("did",did);
  btn.disabled=did;
  const memos=store.get("memos",{});
  const row=document.getElementById("memoRow");
  row.classList.toggle("hidden", !did);
  if(did) document.getElementById("memoInput").value=memos[todayStr()]||"";
  const pn=document.getElementById("plateauNote");
  if(did) pn.innerHTML="";
  else if(st.total>=12&&st.total<=16) pn.innerHTML=`💡 いまは効果を感じにくい時期！体は変わり続けていますよ <a href="#" onclick="switchTab('history');return false" class="tapx" style="color:var(--tealink);font-weight:800">とどくメーター</a>で確かめてみて`;
  else if(st.total>=28&&st.total<=34) pn.innerHTML=`💡 1ヶ月ちかくまで来ました この時期を過ぎると変化を感じた報告がぐっと増えますよ のんびりどうぞ`;
  else pn.innerHTML="";
  applyMiniStreak();
}

function markDone(){
  const st=getStreakData();
  const today=todayStr();
  if(st.dates.includes(today)) return;
  const last=st.dates.length?st.dates[st.dates.length-1]:null;
  const gap=last?daysBetween(last,today):null;
  let note="";
  let missed=null; // gap>=2のときの欠席日リスト。券の消費確定はstreak2保存の成功後まで待つ
  let newChapter=false;
  if(gap===null){ st.count=1; }
  else if(gap<=0){ /* 端末時計の巻き戻り: 連続は減らさない */ st.count=Math.max(1,st.count); }
  else if(gap===1){ st.count++; }
  else {
    missed=[];
    for(let i=1;i<gap;i++){
      const dd=new Date(new Date(last).getTime()+i*86400000);
      missed.push(dd.toISOString().slice(0,10));
    }
    if(canBridgeFreezes(missed)){
      st.count++;
      note=`おやすみ券を${gap-1}枚つかったので連続はつながっています`;
    } else {
      missed=null;
      newChapter=true;
      st.count=1;
    }
  }
  st.total=(st.total||0)+1;
  st.dates.push(today);
  st.dates.sort();
  if(st.dates.length>1200) st.dates=st.dates.slice(-1200);
  const saved_ok=store.set("streak2",st); renderStreak();
  const cheerEl=document.getElementById("cheer");
  if(!saved_ok){ cheerEl.textContent="⚠️ この画面設定では記録がのこせないみたい プライベートモードをオフにしてもう一度どうぞ"; return; }
  // はじめの1本ガイド中の記録かどうかは、完了マーク(fd=1)を立てる前に判定しておく
  // （fdActive()はtotal===0が条件のため、この時点で既にtotal===1になっている＝markDone側では使えない）
  const guide=store.get("fd",null)==="go";
  if(guide){
    store.set("fd",1);
    // 1日目チュートリアルv2(2026-07-21): 使い方ツアーの自動起動を予約する。実際の起動は
    // fdTourMaybeStart()(index.html)が「区切り」(記録カードモーダルclose/タブ移動)で拾う
    store.set("tourpend",1);
    const mi=document.getElementById("memoInput");
    if(mi) mi.placeholder="例: 肩がかるくなった気がする😊";
  }
  // ここまででstreak2の保存が確定したので、初めておやすみ券・章カウント・カレンダー記録を確定させる
  if(missed) tryUseFreezes(missed);
  if(newChapter){ const ch=(store.get("chapters",1))+1; store.set("chapters",ch); note=`第${ch}章のスタート！通算はぜんぶ残ってます 戻ってくる人がいちばん強い✨`; }
  // きょうの動画をカレンダー用に記録
  // 「きょうの1本」でタップされた動画IDがあればそれを優先（実際に見た動画とおすすめが違うケースに対応）。
  // タップ記録がない/日付が違う場合はこれまでどおりおすすめ動画IDにフォールバック。
  try{
    let vid=currentTodayId();
    try{
      const pv=JSON.parse(sessionStorage.getItem("kyono_pendingNudgeVideo")||"null");
      if(pv&&pv.d===today&&pv.v&&/^[\w-]{11}$/.test(pv.v)) vid=pv.v;
    }catch(e2){}
    let vt=""; for(const k in V){ if(V[k].id===vid){ vt=V[k].t; break; } }
    const dl=store.get("daylog",{}); dl[today]={v:vid,t:vt,c:st.count};
    const ks=Object.keys(dl).sort(); if(ks.length>400) ks.slice(0,ks.length-400).forEach(k=>delete dl[k]);
    store.set("daylog",dl);
  }catch(e){}
  const ms=MS.find(x=>x.d===st.total);
  // あした節目予告: きょうは節目でない(ms falsy)ときだけ、通算+1がMILESTONESに乗るなら1行予告する
  // （節目名(MS.t)は出さない＝当日の新鮮味を減らさないため。判定はmarkDone本体と同じst.totalを使う）
  const tomorrowMsPreview=(!ms&&MILESTONES.includes(st.total+1))?`<div style="margin-top:6px;font-size:13px;color:var(--sub)">あしたで ${st.total+1}日目🎉 おたのしみに！</div>`:"";
  try{ launchConfetti(ms?105:70); }catch(e){} // 節目は70粒→105粒（1.5倍）でちょっと特別に
  if(ms){
    cheerEl.innerHTML=(note?`<div style="font-weight:800;color:var(--teal);margin-bottom:4px">${note}</div>`:"")
      +`<div style="font-size:16px;font-weight:900;color:var(--pink)">🎉 ${ms.t}！（通算${st.total}日）</div>`
      +(ms.m?`<div style="margin-top:4px">${ms.m}</div>`:"")
      +(ms.q?`<div style="margin-top:8px;font-size:13px;text-align:left;background:var(--bg);border:1.5px solid var(--line);border-radius:12px;padding:9px 12px"><span style="font-weight:900;color:var(--teal)">💬 せんぱいの声</span><br><span style="color:var(--sub)">${ms.q.replace(/（先輩の声）$/,"")}</span></div>`:"")
      +`<img src="assets/chara-crown.png" alt="" style="width:72px;height:72px;object-fit:contain;display:block;margin:10px auto 0">`
      +(MILESTONE_MSG_VIDEO?`<a class="btn btn-ghost" href="https://www.youtube.com/watch?v=${MILESTONE_MSG_VIDEO}" target="_blank" rel="noopener" style="margin-top:10px;font-size:15px;display:block;text-decoration:none">🎬 尾形さんからお祝いメッセージ</a>`:"");
  } else if(guide){
    // 節目とは重ならない前提（通算1日目=guideの唯一の発生タイミングはMSの最小値3より前）だが、
    // 念のため節目表示(if(ms))を優先する構造にしてある（このelse ifは節目でないときだけ通る）
    cheerEl.innerHTML=(note?`<div style="font-weight:800;color:var(--teal);margin-bottom:4px">${note}</div>`:"")
      +`<div style="font-size:16px;font-weight:900;color:var(--pink)">🎉 1日目クリア！ナイスご自愛！</div>`
      +`<img src="assets/card-sample.png" alt="" class="fd-cardpop">`
      +`<div style="margin-top:6px;font-size:14px">きょうの記録が1まい目のカードになったよ ためると<b>図鑑</b>がうまっていく📖</div>`
      +`<div style="margin-top:6px;font-size:14px">よかったら下に✍️きょうのひとことをどうぞ からだの感じをひとことでOK（あとからでもいいよ）</div>`
      +tomorrowMsPreview;
  } else {
    const cheers=["ナイスご自愛🎉","がんばったね！おつかれさまでした✨","その数分が体を変えます💪","イタ気持ちいい できました？😊","体は正直！ちゃんと応えてくれますよ✨","昨日の自分より1ミリ前へ🌱"];
    cheerEl.innerHTML=(note?`<div style="font-weight:800;color:var(--teal);margin-bottom:4px">${note}</div>`:"")+cheers[Math.floor(Math.random()*cheers.length)]+tomorrowMsPreview;
  }
  // ===== お願いカードの「1日1枚」キュー（2026-07-20監査Top1「お願い渋滞」対応・PO承認） =====
  // 旧実装は通算1日目クリアの瞬間に3枚が同時に縦積みされ、成功体験の頂点が最も混雑した画面になっていた。
  // きょうやった！直後の提案はキューにして、優先順=①ホーム画面追加（記録の安全に直結・最重要）
  // →②カレンダー の「その日の1枚」だけを出す（典型: 実機は1日目=追加・2日目=カレンダー）。
  // 使い方ツアーは2026-07-21のチュートリアルv2でキューから外し、1日目の「区切り」での自動起動に昇格
  // （fdTourMaybeStart・index.html。一度きりフラグtourseenはそちらで消費する）。
  // 一度きりフラグは従来のstoreキー（a2hs2/calseen）。出せない環境の候補（a2hs不適合環境・要素なし等）は
  // フラグだけ消費して同日中に次候補を繰り上げる（従来の「再訪のたび判定し直さない」流儀）。
  const askQueue=[
    {key:"a2hs2", can:function(){
      // ホーム画面追加「二度目のチャンス」: standalone/デスクトップ/アプリ内ブラウザ/YouTube内ブラウザでは意味がない
      const kind=(typeof a2hsKindFor==="function")?a2hsKindFor():null;
      const inApp=(typeof envIsInApp==="function")&&envIsInApp();
      const ytInApp=(typeof ytInAppDetect==="function")&&ytInAppDetect();
      return !!(document.getElementById("a2hsAsk") && kind && !inApp && !ytInApp);
    }, render:function(){
      const box=document.getElementById("a2hsAsk");
      box.innerHTML="";
      const wrap=document.createElement("div");
      wrap.style.cssText="margin-top:10px;background:var(--bg);border:1.5px solid var(--line);border-radius:14px;padding:12px 14px";
      wrap.innerHTML='<div style="font-size:14px;font-weight:800">📲 毎日つかうなら ホーム画面に追加が便利！記録もいちばん安全にのこるよ</div>'
        +'<button class="btn btn-primary" id="a2hsAskBtn" style="margin-top:8px;font-size:15px" onclick="a2hsShowForce()">やり方を見る</button>'
        +'<button class="btn btn-line" id="a2hsAskSkipBtn" style="margin-top:8px;font-size:14px" onclick="document.getElementById(\'a2hsAsk\').innerHTML=\'\'">あとで</button>';
      box.appendChild(wrap);
    }},
    {key:"calseen", can:function(){
      // カレンダー登録カード（唯一の再来訪装置）
      return !!(document.getElementById("calAsk") && typeof calendarAskEl==="function");
    }, render:function(){
      const ca=document.getElementById("calAsk");
      ca.innerHTML="";
      ca.appendChild(calendarAskEl("明日も同じ時間に会いましょう。\nカレンダーに毎日の合図を入れておく？"));
    }},
  ];
  for(const a of askQueue){
    if(store.get(a.key)) continue;
    if(a.can()){
      try{ a.render(); }catch(e){}
      store.set(a.key,1);
      break; // 1日1枚まで。次の候補はあしたのきょうやった！のあとに出る
    }
    store.set(a.key,1); // 出せない環境: フラグだけ消費して同日中に次候補へ繰り上げ
  }
}
function saveMemo(){
  const t=document.getElementById("memoInput").value.trim().slice(0,30);
  const memos=store.get("memos",{});
  if(t) memos[todayStr()]=t; else delete memos[todayStr()];
  const keys=Object.keys(memos).sort();
  if(keys.length>400) keys.slice(0,keys.length-400).forEach(k=>delete memos[k]);
  const memo_ok=store.set("memos",memos);
  document.getElementById("memoSaved").textContent = !memo_ok?"⚠️ のこせませんでした（プライベートモードかも）":(t?"メモをのこしました✍️ 記録カードにも入ります":"メモを消しました");
  const mb=document.getElementById("memoBtn");
  mb.textContent="のこしました ✓"; mb.disabled=true;
  document.getElementById("memoInput").oninput=()=>{ mb.textContent="メモをのこす"; mb.disabled=false; };
}

// silent=true: かたさチェックQ1からの自動転記(finishQuiz)用。ユーザーが自分でボタンを押したときの
// 演出(自己ベスト更新🎉・最初からすばらしい 等)は、押していないのに出ると誤解を招くため出さない
// (2026-07-20対応)。値そのものの記録・renderReach()の反映は通常どおり行う
function setReach(lv,silent){
  const arr=getReach();
  const today=todayStr();
  const best=arr.length?Math.max(...arr.map(r=>r.lv)):0;
  const i=arr.findIndex(r=>r.d===today);
  if(i>=0) arr[i].lv=lv; else arr.push({d:today,lv});
  if(arr.length>200){ const bestRec=arr.reduce((a,b)=>b.lv>=a.lv?b:a); arr.splice(0,arr.length-200); if(!arr.some(r=>r.lv>=bestRec.lv)) arr.unshift(bestRec); }
  const reach_ok=store.set("reach",arr);
  renderReach();
  const msg=document.getElementById("reachMsg");
  if(silent){ msg.textContent=""; return; }
  if(!reach_ok) msg.textContent="⚠️ のこせませんでした（プライベートモードかも）";
  else if(lv>best&&best>0) msg.innerHTML=`<b style="color:var(--pink)">🎉 自己ベスト更新！「${REACH_LV[lv]}」</b> 記録カードにも入ります`;
  else if(lv>=4&&best===0) msg.innerHTML=`<b style="color:var(--pink)">最初から「${REACH_LV[lv]}」！すばらしい</b>`;
  else msg.textContent="記録しました！じわじわ伸びていきますよ";
}
function renderReach(){
  const arr=getReach();
  const today=todayStr();
  for(let i=1;i<=5;i++){
    const on=arr.length&&arr[arr.length-1].d===today&&arr[arr.length-1].lv===i;
    document.getElementById("rlv"+i).classList.toggle("on",!!on);
  }
  const now=document.getElementById("reachNow");
  if(!arr.length){ now.textContent="まだ記録なし！まずは1回はかってみましょう"; document.getElementById("reachPrev").innerHTML=""; document.getElementById("reachTrend").innerHTML=""; return; }
  const latest=arr[arr.length-1], best=Math.max(...arr.map(r=>r.lv));
  now.innerHTML=`いまの記録: <b>${REACH_LV[latest.lv]}</b>（${latest.d.slice(5).replace("-","/")}）<br>自己ベスト: <b style="color:var(--teal)">${REACH_LV[best]}</b>`;
  // 前回比（別の日の記録が2回以上あるときだけ 段位で表現・数字プレッシャーをかけない）
  const pv=document.getElementById("reachPrev");
  if(arr.length>=2){
    const prev=arr[arr.length-2], diff=latest.lv-prev.lv;
    if(diff>0) pv.innerHTML=`前回（${REACH_LV[prev.lv]}）より<b style="color:var(--pink)">${diff}段とどくようになった！🎉</b>`;
    else if(diff===0) pv.innerHTML=`前回とおなじ「${REACH_LV[latest.lv]}」 キープも立派です！`;
    else pv.innerHTML=`体は日によってちがうもの またコツコツいきましょう🌱`;
  } else pv.innerHTML="";
  document.getElementById("reachTrend").innerHTML = arr.slice(-14).map(r=>`<div class="rbar"><div style="height:${r.lv*20}%"></div></div>`).join("");
}
// ---- ひとことにっき ----
function renderDiary(){
  const memos=store.get("memos",{});
  const keys=Object.keys(memos).sort().reverse().slice(0,7);
  document.getElementById("diaryList").innerHTML = keys.length
    ? keys.map(k=>`<div style="display:flex;gap:10px;padding:7px 0;border-bottom:1px dashed var(--line);font-size:15px"><span style="color:var(--sub);font-weight:800;flex-shrink:0">${k.slice(5).replace("-","/")}</span><span style="overflow-wrap:anywhere">${memos[k].replace(/</g,"&lt;")}</span></div>`).join("")
    : `<div style="font-size:14px;color:var(--sub)">「きょうやった！」のあとにメモをのこせます</div>`;
}

function renderHistory(){
  const st=getStreakData();
  document.getElementById("histStreak").textContent=effectiveStreakCount(st);
  document.getElementById("histTotal").textContent=st.total;
  const next=MILESTONES.find(m=>m>st.total);
  if(next){
    const ms=MS.find(x=>x.d===next);
    document.getElementById("msNote").innerHTML=`次のお祝い「<b style="color:var(--pink)">${ms.t}</b>」は通算${next}日目🌱 マイペースでどうぞ`;
    document.getElementById("msBar").style.width=Math.min(100,Math.round(st.total/next*100))+"%";
  }else{
    document.getElementById("msNote").textContent="全部の節目をたっせい！すごすぎます";
    document.getElementById("msBar").style.width="100%";
  }
  document.getElementById("histFreeze").innerHTML=`おやすみ券 のこり${freezeLeft()}枚<br>休んだ日に自動でつかわれて連続がつながります`;
  if(!calDate){ const n=new Date(); calDate=new Date(n.getFullYear(),n.getMonth(),1); }
  renderCal(); renderReach(); renderDiary(); renderIcs(); renderDexBanner();
}
function showDay(ds){
  calSelected=ds;
  renderCal();
  const log=(store.get("daylog",{}))[ds];
  const memo=(store.get("memos",{}))[ds];
  const el=document.getElementById("dayInfo");
  let html=`<b>${Number(ds.slice(5,7))}/${Number(ds.slice(8,10))} にやった記録</b>`;
  if(log&&log.v&&/^[\w-]{11}$/.test(log.v)) html+=`<div><a href="#" class="tapx daychip" onclick="toggleDayVideo(this);return false">この日の動画 <span class="daytoggle">＋</span></a><div class="hidden" style="margin-top:8px"><a href="https://www.youtube.com/watch?v=${log.v}" target="_blank" rel="noopener" class="tapx" style="color:var(--tealink);font-weight:800">▶ YouTubeでチェックする</a></div></div>`;
  if(memo) html+=`<br><svg viewBox="0 0 24 24" width="18" height="18" style="vertical-align:-3px;margin-right:4px" fill="none" stroke="#3A3A35" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20l1-4.5L15.5 5l3.5 3.5L8.5 19 4 20z" fill="#FFEDF3"/><path d="M13.5 6.5l3.5 3.5M5.3 17.9l3.2 1.2"/></svg>${memo.replace(/</g,"&lt;")}`;
  if(!log&&!memo) html+=`<br>この日は「やった！」の印だけ残っています`;
  html+=`<br><a href="#" class="tapx daychip" onclick="makeCard('${ds}');return false"><svg viewBox="0 0 24 24" style="width:18px;height:18px;vertical-align:-3px;margin-right:4px" fill="none" stroke="#177065" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="5" width="18" height="15" rx="3.5"/><circle cx="9" cy="10.5" r="1.8" fill="#177065" stroke="none"/><path d="M4.5 17.5l4.5-4 3.5 3 3.5-3.5 3.5 3.5"/></svg>この日の記録カードを見る</a>`;
  el.innerHTML=html;
  el.classList.remove("hidden");
}

function renderCal(){
  const y=calDate.getFullYear(), m=calDate.getMonth();
  document.getElementById("calTitle").textContent=`${y}年${m+1}月`;
  const done=new Set(getStreakData().dates);
  const today=todayStr();
  const first=new Date(y,m,1).getDay();
  const days=new Date(y,m+1,0).getDate();
  let html="<tr>", cell=0;
  for(let i=0;i<first;i++){ html+="<td></td>"; cell++; }
  for(let d=1;d<=days;d++){
    const ds=`${y}-${String(m+1).padStart(2,"0")}-${String(d).padStart(2,"0")}`;
    const cls=["d", done.has(ds)?"done":"", ds===today?"today":"", ds===calSelected?"sel":"", ds>today?"mute":""].join(" ");
    html+=`<td${done.has(ds)?` onclick="showDay('${ds}')" style="cursor:pointer"`:""}><span class="${cls}">${d}</span></td>`;
    if(++cell%7===0&&d<days) html+="</tr><tr>";
  }
  html+="</tr>";
  document.getElementById("calBody").innerHTML=html;
}
