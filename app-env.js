// #きょうのオガトレ PWA・環境まわり（テーマ適用・日付またぎ再描画・ホーム画面追加ポップアップ）
// 起動時に実行される配線（SW登録・envBanner・オンボ起動トリガー等）はindex.html側に残したまま、
// ここには純粋な関数定義とa2hs用の状態変数だけを置く（SPLIT-PLAN.md 5番）。

function applyTheme(){
  const p=store.get("theme","auto");
  const h=new Date().getHours();
  const osDark=window.matchMedia&&matchMedia("(prefers-color-scheme: dark)").matches;
  const dark = p==="dark" || (p==="auto" && (osDark || h>=19 || h<5));
  document.body.classList.toggle("dark",dark);
  const mc=document.querySelector('meta[name="theme-color"]');
  if(mc) mc.setAttribute("content", dark?"#211E19":"#FFD93B");
  for(const k of ["auto","light","dark"]){ const b=document.getElementById("th-"+k); if(b) b.classList.toggle("on",p===k); }
}

function refreshDay(){
  applyTheme();
  try{ renderAnchor(); }catch(e){}
  if(todayStr()!==lastDay){
    lastDay=todayStr(); state.mode=null;
    const vis=SECTIONS.find(x=>!document.getElementById(x).classList.contains("hidden"));
    if(vis==="home") renderHome();
    else if(vis==="history") renderHistory();
    try{ renderVoices(); }catch(e){} // せんぱいの声の日替わり8選も日付またぎで更新
  }
  checkDoneNudge();
}

// 実行環境の案内（アプリ内ブラウザは記録が分離される・ホーム追加のすすめ）
// 「とじる」は次の節目(7日→14日)まで。SafariタブだけだとITPで記録が消えるリスクがあるため、記録が伸びるほど再提案する
function dismissHomeHint(){
  let days=0; try{ days=(getStreakData().dates||[]).length; }catch(e){}
  store.set("homehint_next", days<7?7:(days<14?14:9999));
  store.set("homehint_done",true);
  document.getElementById("envBanner").classList.add("hidden");
}

let a2hsCont=null;
function a2hsIsStandalone(){
  try{ return navigator.standalone===true || (window.matchMedia&&matchMedia("(display-mode: standalone)").matches); }catch(e){ return false; }
}
function a2hsAddBtn(label,cls,onClick){
  const box=document.getElementById("a2hsBtns"); if(!box) return null;
  const b=document.createElement("button");
  b.className="btn "+cls;
  b.textContent=label; // 固定文言のみ・ユーザー入力は扱わない
  b.onclick=onClick;
  box.appendChild(b);
  return b;
}
// kind: "ios-safari" / "ios-other"(iOS+Safari以外) / "android-prompt"(beforeinstallprompt保持あり) / "android-menu"
function a2hsShow(kind,cont){
  const m=document.getElementById("a2hsModal"), body=document.getElementById("a2hsBody"), box=document.getElementById("a2hsBtns");
  if(!m||!body||!box){ cont(); return; } // DOM欠落時は進行不能を避けて即座に次へ
  a2hsCont=cont;
  m.setAttribute("data-a2hs-kind",kind); // 実測テスト用の分岐マーカー（表示文言には影響しない）
  body.innerHTML="";
  box.innerHTML="";
  if(kind==="ios-safari"){
    body.innerHTML="画面下部の共有ボタン（□に↑のマーク）をタップして<br>「ホーム画面に追加」を選んでね📲<br>次からアプリみたいにワンタップで開けるようになるよ😊";
    a2hsAddBtn("あとで","btn-line",function(){ a2hsClose(); });
  }else if(kind==="ios-other"){
    body.innerHTML="ホーム画面に追加するために、<b>Safari</b>で開いてくださいね🙏<br>共有メニューから「Safariで開く」を選んでみてね";
    a2hsAddBtn("あとで","btn-line",function(){ a2hsClose(); });
  }else if(kind==="android-prompt"){
    body.innerHTML="ホーム画面に追加すると、次からアプリみたいにワンタップで開けるようになるよ😊";
    a2hsAddBtn("📲 ホーム画面に追加する","btn-primary",function(){
      const ev=window.__a2hsEvent;
      window.__a2hsEvent=null;
      if(!ev||typeof ev.prompt!=="function"){ a2hsClose(); return; }
      try{
        ev.prompt();
        Promise.resolve(ev.userChoice).then(function(){ a2hsClose(); }).catch(function(){ a2hsClose(); });
      }catch(e){ a2hsClose(); }
    });
    a2hsAddBtn("あとで","btn-line",function(){ a2hsClose(); });
  }else{
    body.innerHTML="画面右上の<b>「⋮」</b>メニューから<br>「ホーム画面に追加」または「アプリをインストール」を選んでね📲";
    a2hsAddBtn("わかった","btn-primary",function(){ a2hsClose(); });
  }
  m.classList.remove("hidden");
  try{ if(!(history.state&&history.state.a2hs)) history.pushState({id:currentSection,a2hs:1},""); }catch(e){}
}
// 閉じ方（ボタン/戻る操作いずれも）を問わず必ずcont()(=obOpen)へ進む。
// obOpen()自身が独自にhistory.pushStateするため、ここでは意図的にhistory.back()を呼ばない
// （obGo()が同じ理由でobClose(true)を使っているのと同じレース防止の流儀）。
function a2hsClose(){
  const m=document.getElementById("a2hsModal");
  if(m){ m.classList.add("hidden"); m.removeAttribute("data-a2hs-kind"); }
  const cont=a2hsCont; a2hsCont=null;
  if(cont) cont();
}
// 発火判定: 既にstandalone起動中/デスクトップは案内不要で即cont()。iOS/Androidだけ文言を出し分ける。
function a2hsBoot(cont){
  try{
    if(a2hsIsStandalone()){ cont(); return; }
    const ua=navigator.userAgent||"";
    // iPadOS 13以降のSafariは既定でMacintosh系UAを名乗り、実機iPadでもタッチ操作を持つ点だけが
    // 本物のMacと違う（maxTouchPoints>1）。この判定がないとiPadユーザーに「ホーム画面に追加」の
    // 案内が一切出ない（2026-07-18発見）。
    const isIpadDesktopUA = /Macintosh/.test(ua) && navigator.maxTouchPoints>1;
    if(!/iPhone|iPad|iPod|Android/.test(ua) && !isIpadDesktopUA){ cont(); return; } // デスクトップ（ホーム画面という概念が前提と合わないため）
    if(/iPhone|iPad|iPod/.test(ua) || isIpadDesktopUA){
      // iOS上のChrome/Firefox/Edge等はUAにCriOS/FxiOS/EdgiOS/OPiOSが入りSafari本体と区別できる
      if(/CriOS|FxiOS|EdgiOS|OPiOS/.test(ua)) a2hsShow("ios-other",cont);
      else a2hsShow("ios-safari",cont);
      return;
    }
    // Android: beforeinstallpromptを保持できていればネイティブインストールダイアログを、
    // できていなければ（発火前/未対応ブラウザ）メニュー案内を出す
    if(window.__a2hsEvent) a2hsShow("android-prompt",cont);
    else a2hsShow("android-menu",cont);
  }catch(e){ cont(); }
}
