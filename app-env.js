// #きょうのオガトレ PWA・環境まわり（テーマ適用・日付またぎ再描画・ホーム画面追加ポップアップ）
// 起動時に実行される配線（SW登録・envBanner・オンボ起動トリガー等）はindex.html側に残したまま、
// ここには純粋な関数定義とa2hs用の状態変数だけを置く（SPLIT-PLAN.md 5番）。

// アプリ内ブラウザ判定（LINE/Instagram/汎用WebView）。index.htmlの起動時IIFEが2箇所(envBanner描画・
// はじめてガイド起動判定)で使うほか、2026-07-20追加のホーム画面追加「二度目のチャンス」カードの
// 出し分けにも使うため、ここに一本化した（以前は2箇所に同じ正規表現がベタ書きされていた）。
function envIsInApp(){
  try{ return /\bLine\/|Instagram|FBAN|FBAV|; wv\)/.test(navigator.userAgent||""); }catch(e){ return false; }
}

// ================= YouTube経由の検出と脱出バナー（2026-07-20新規・A2HS導線再設計） =================
// 配布はYouTubeコミュニティ投稿のURL前提。YouTubeアプリ内ブラウザ（iOS=SFSafariViewController系／
// Android=Chromeカスタムタブ）では「ホーム画面に追加」が不可能で、UA検出（envIsInApp相当）では
// 見抜けない（特にiOSのSFVC系はUAがSafari本体とほぼ同一）。そこでdocument.referrerがYouTube由来かで
// 推測する。
// 正直な限界（WORKING_NOTESにも記帳）: iOSのSFVCはreferrerを送らない実装もあり、検出は保証されない。
// 検出できない場合の頼みの綱は配布投稿側の画像手順（docs/invite-kit.md）。
// sessionStorageに保持する理由: YouTube内ブラウザから同一WebView内でリンクをたどるとreferrerが
// 消えることがあるため、一度立てたフラグはタブが生きている間は保持する。localStorageにしないのは、
// 次にSafari/Chromeで素直に開いたとき（別セッション）に「まだYouTubeの中」と誤爆させないため。
function ytInAppDetect(){
  try{
    if(sessionStorage.getItem("kyono_ytInApp")==="1") return true;
    if(a2hsIsStandalone()) return false; // ホーム画面から起動した後は判定不要（誤爆防止）
    const ref=document.referrer||"";
    if(!ref) return false;
    let host="";
    try{ host=new URL(ref).hostname; }catch(e){ return false; }
    const isYt = host==="youtu.be" || host.endsWith(".youtu.be") || host==="youtube.com" || host.endsWith(".youtube.com");
    if(isYt){ sessionStorage.setItem("kyono_ytInApp","1"); return true; }
    return false;
  }catch(e){ return false; }
}
// YouTube脱出バナーの文言。iOS系(SFVC)は右下のコンパスマーク、Android系(カスタムタブ)は右上「⋮」→
// 「Chromeで開く」で脱出できる（invite-kit.md ①投稿文と文言統一）。
function ytInAppBannerHTML(){
  const ua=navigator.userAgent||"";
  const isIOS = /iPhone|iPad|iPod/.test(ua) || (/Macintosh/.test(ua)&&navigator.maxTouchPoints>1);
  return isIOS
    ? 'YouTubeの中でひらいてるみたい📺 <b>右下のコンパスのマーク</b>を押すと Safariでちゃんとひらけるよ（記録を守るため）'
    : 'YouTubeの中でひらいてるみたい📺 <b>右上の「⋮」→「Chromeで開く」</b>を押してね（記録を守るため）';
}

// 共有アイコン（□に↑のマーク）。iOS/Androidの共有ボタンを指す視覚アンカーとして文中に埋め込む
// インラインSVG（既存コードのアイコン流儀=viewBox 24x24・stroke 2.2・線色は文字色を継承、に合わせる）。
const SHARE_SVG='<svg viewBox="0 0 24 24" width="16" height="16" style="vertical-align:-3px" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3v11M8 7l4-4 4 4"/><path d="M6 10.5v7.5a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2v-7.5"/></svg>';

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
    // 実4ステップ+絵入り化（2026-07-20・デジタル弱者監査Top1対応）。旧文言は「共有→追加」の2ステップ
    // しかなく、実際に必要な「下にスクロール」「右上の追加」が欠落していた（invite-kit.md③と文言統一）。
    // iPadは共有ボタンの位置が画面上部のため①の文言だけ出し分ける（既存のiPad判定=isIpadDesktopUAを流用）。
    const ua=navigator.userAgent||"";
    const isIpad=/iPad/.test(ua) || (/Macintosh/.test(ua)&&navigator.maxTouchPoints>1);
    const step1=isIpad
      ? "①画面の<b>上のほう</b>の"+SHARE_SVG+"をタップ"
      : "①画面のいちばん下 まん中の"+SHARE_SVG+"をタップ";
    body.innerHTML=step1+"<br>②出てきた画面を<b>下にスクロール</b><br>③「ホーム画面に追加」をタップ<br>④右上の<b>「追加」</b>をタップ<br>これで完成🎉 次からはホームの黄色いアイコンから開くだけ😊";
    a2hsAddBtn("あとで","btn-line",function(){ a2hsClose(); });
  }else if(kind==="ios-other"){
    // 旧文言「Safariで開いてね」はiOS Chromeに存在しないメニューを案内する実行不能な指示だった
    // （2026-07-20監査Top3対応）。iOS16.4+はChromeの共有メニューから直接「ホーム画面に追加」できる。
    body.innerHTML="Chromeでもだいじょうぶ😊 右上の"+SHARE_SVG+"→「ホーム画面に追加」でOK📲<br>うまくいかないときは このページのアドレスをSafariでひらいてみてね";
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
    body.innerHTML="①画面右上の<b>「⋮」</b>メニューをタップ<br>②「ホーム画面に追加」または「アプリをインストール」を選ぶ📲";
    a2hsAddBtn("わかった","btn-primary",function(){ a2hsClose(); });
  }
  m.classList.remove("hidden");
  modalFocusOpen("a2hsModalBox");
  updateFabs(); // a2hs表示中はFABを隠す(他モーダルと同じ扱い。2026-07-20対応=表示中FABが背後に見えるバグ修正)
  try{ if(!(history.state&&history.state.a2hs)) history.pushState({id:currentSection,a2hs:1},""); }catch(e){}
}
// 閉じ方（ボタン/戻る操作いずれも）を問わず必ずcont()(=obOpen)へ進む。
// obOpen()自身が独自にhistory.pushStateするため、ここでは意図的にhistory.back()を呼ばない
// （obGo()が同じ理由でobClose(true)を使っているのと同じレース防止の流儀）。
function a2hsClose(){
  const m=document.getElementById("a2hsModal");
  if(m){ m.classList.add("hidden"); m.removeAttribute("data-a2hs-kind"); }
  updateFabs(); // 他モーダルのclose関数と同じ作法(updateFabs→modalFocusCloseの順序厳守)
  modalFocusClose();
  const cont=a2hsCont; a2hsCont=null;
  if(cont) cont();
}
// 発火判定: 既にstandalone起動中/デスクトップは案内不要で即cont()。iOS/Androidだけ文言を出し分ける。
// 環境判定の単一の正: 「ホーム画面に追加」の案内が意味を持つ環境か（表示の副作用は起こさない）。
// 2026-07-21 5視点検証(提案A・PO承認)で初回起動時ポップアップ(旧a2hsBoot)を廃止したため、
// 旧a2hsBootにあった分岐はこの関数に一本化された。呼び出し元は1日目クリア直後の再提案カード
// (markDoneのaskQueue)とa2hsShowForce()（gd-mamoriの再案内ボタン等）。
// - iPadOS 13以降のSafariは既定でMacintosh系UAを名乗り、実機iPadでもタッチ操作を持つ点だけが
//   本物のMacと違う（maxTouchPoints>1）。この判定がないとiPadユーザーに案内が一切出ない（2026-07-18発見）。
// - iOS上のChrome/Firefox/Edge等はUAにCriOS/FxiOS/EdgiOS/OPiOSが入りSafari本体と区別できる。
// - Android: beforeinstallpromptを保持できていればネイティブインストールダイアログ(android-prompt)、
//   できていなければ（発火前/未対応ブラウザ）メニュー案内(android-menu)。
function a2hsKindFor(){
  try{
    if(a2hsIsStandalone()) return null;
    const ua=navigator.userAgent||"";
    const isIpadDesktopUA = /Macintosh/.test(ua) && navigator.maxTouchPoints>1;
    if(!/iPhone|iPad|iPod|Android/.test(ua) && !isIpadDesktopUA) return null; // デスクトップ
    if(/iPhone|iPad|iPod/.test(ua) || isIpadDesktopUA){
      return /CriOS|FxiOS|EdgiOS|OPiOS/.test(ua) ? "ios-other" : "ios-safari";
    }
    return window.__a2hsEvent ? "android-prompt" : "android-menu";
  }catch(e){ return null; }
}
// a2hsBoot()の「はじめてガイド直前だけ・実質一生に一度」というゲートを迂回して、いつでも強制的に
// ポップアップを出す版（2026-07-20新設）。1日目クリア直後の再提案カード「やり方を見る」ボタンと、
// gd-mamoriカードの「もういちど かんたん案内を見る」ボタンから呼ぶ。continuationは省略可（省略時は
// 何もしない＝オンボーディングには進まない。これらの呼び出し元はオンボ完走後のユーザー向けのため）。
function a2hsShowForce(cont){
  const done=function(){ if(typeof cont==="function") cont(); };
  try{
    const kind=a2hsKindFor();
    if(!kind){ done(); return; } // standalone/デスクトップでは案内不要
    a2hsShow(kind,done);
  }catch(e){ done(); }
}
