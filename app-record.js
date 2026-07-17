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
  if(!did){ const ce=document.getElementById("cheer"); if(ce) ce.innerHTML=""; }
  // 数日あいて券でもつなげない時は、古い連続を見せない（押した瞬間に消えたと誤解させない）
  if(!did && streakBrokenNow(st)) contTxt="きょうやると新しい章のスタート🌱";
  document.getElementById("totalDays").textContent = contTxt;
  const btn=document.getElementById("doneBtn");
  btn.innerHTML = did?"きょうの分完了！<br>おつかれさまでした✨":"きょうやった！";
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
  // ここまででstreak2の保存が確定したので、初めておやすみ券・章カウント・カレンダー記録を確定させる
  if(missed) tryUseFreezes(missed);
  if(newChapter){ const ch=(store.get("chapters",1))+1; store.set("chapters",ch); note=`第${ch}章のスタート！通算はぜんぶ残ってます 戻ってくる人がいちばん強い✨`; }
  // きょうの動画をカレンダー用に記録
  try{
    const vid=currentTodayId();
    let vt=""; for(const k in V){ if(V[k].id===vid){ vt=V[k].t; break; } }
    const dl=store.get("daylog",{}); dl[today]={v:vid,t:vt,c:st.count};
    const ks=Object.keys(dl).sort(); if(ks.length>400) ks.slice(0,ks.length-400).forEach(k=>delete dl[k]);
    store.set("daylog",dl);
  }catch(e){}
  const ms=MS.find(x=>x.d===st.total);
  try{ launchConfetti(ms?105:70); }catch(e){} // 節目は70粒→105粒（1.5倍）でちょっと特別に
  if(ms){
    cheerEl.innerHTML=(note?`<div style="font-weight:800;color:var(--teal);margin-bottom:4px">${note}</div>`:"")
      +`<div style="font-size:16px;font-weight:900;color:var(--pink)">🎉 ${ms.t}！（通算${st.total}日）</div>`
      +(ms.m?`<div style="margin-top:4px">${ms.m}</div>`:"")
      +(ms.q?`<div style="margin-top:8px;font-size:13px;text-align:left;background:var(--bg);border:1.5px solid var(--line);border-radius:12px;padding:9px 12px"><span style="font-weight:900;color:var(--teal)">💬 せんぱいの声</span><br><span style="color:var(--sub)">${ms.q.replace(/（先輩の声）$/,"")}</span></div>`:"")
      +`<img src="assets/chara-crown.png" alt="" style="width:72px;height:72px;object-fit:contain;display:block;margin:10px auto 0">`
      +(MILESTONE_MSG_VIDEO?`<a class="btn btn-ghost" href="https://www.youtube.com/watch?v=${MILESTONE_MSG_VIDEO}" target="_blank" rel="noopener" style="margin-top:10px;font-size:15px;display:block;text-decoration:none">🎬 尾形さんからお祝いメッセージ</a>`:"");
  } else {
    const cheers=["ナイスご自愛🎉","がんばったね！おつかれさまでした✨","その数分が体を変えます💪","イタ気持ちいい できました？😊","体は正直！ちゃんと応えてくれますよ✨","昨日の自分より1ミリ前へ🌱"];
    cheerEl.innerHTML=(note?`<div style="font-weight:800;color:var(--teal);margin-bottom:4px">${note}</div>`:"")+cheers[Math.floor(Math.random()*cheers.length)];
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

function setReach(lv){
  const arr=getReach();
  const today=todayStr();
  const best=arr.length?Math.max(...arr.map(r=>r.lv)):0;
  const i=arr.findIndex(r=>r.d===today);
  if(i>=0) arr[i].lv=lv; else arr.push({d:today,lv});
  if(arr.length>200){ const bestRec=arr.reduce((a,b)=>b.lv>=a.lv?b:a); arr.splice(0,arr.length-200); if(!arr.some(r=>r.lv>=bestRec.lv)) arr.unshift(bestRec); }
  const reach_ok=store.set("reach",arr);
  renderReach();
  const msg=document.getElementById("reachMsg");
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
