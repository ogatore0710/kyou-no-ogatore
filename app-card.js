// #きょうのオガトレ 記録カード（女性向けポップ・日替わりテーマ＋節目ゴールド）
// カードのテーマ/節目データと、フォント準備・カード生成・保存/共有を担当。
// drawCard()はピクセル回帰テストの対象（qa.jsのdrawCard: no Math.random/Date.now/new Date()チェック）。
// 内部でMath.random()・Date.now()・引数なしnew Date()を使わないこと（過去日カードの再現性を壊すため絶対厳守）。

// 過去カード再現の絶対ルール: カレンダーの日付タップで過去日のカードを「同じ見た目で」再生成できる
// 仕様のため、テーマの追加は必ず【末尾に足す＋CARD_THEMES_V2_FROM以降の日付にだけ効かせる】こと。
// 先頭5色の並び・色・従来の固定ちらし配置を変えると、過去の全カードの見た目が変わってしまう。
const CARD_THEMES = [
  {name:"さくら",   bg:["#FFEDF3","#FFF7EC"], main:"#E56A9A", deco:["#F5AFC6","#FFD93B","#FF9E8E"]},
  {name:"ラベンダー",bg:["#F1EDFF","#FFEFF6"], main:"#8B7BD8", deco:["#C3B8F2","#F5AFC6","#FFD93B"]},
  {name:"ミント",   bg:["#E7F8F1","#F0F9FF"], main:"#2FA98C", deco:["#9BDFC9","#A6D8F5","#FFD93B"]},
  {name:"ピーチ",   bg:["#FFF0E6","#FFEFF4"], main:"#F08A5D", deco:["#FFC0A0","#F5AFC6","#FFE28A"]},
  {name:"レモン",   bg:["#FFF9DC","#FFEFE6"], main:"#DFA400", deco:["#FFE47A","#FFC0A0","#9BDFC9"]},
  // ▼ 2026-07-13から解禁の追加5テーマ（本人リクエスト「背景の種類ふやして」2026-07-12）
  {name:"そら",       bg:["#E9F3FF","#EFFCF6"], main:"#4E96D9", deco:["#A6D8F5","#FFD93B","#F5AFC6"]},
  {name:"いちごみるく",bg:["#FFE9EE","#FFF7F0"], main:"#E0566E", deco:["#FF9EAE","#FFE28A","#A6D8F5"]},
  {name:"ミルクティー",bg:["#F6EEE1","#FFF7EB"], main:"#A87B4F", deco:["#DDBE96","#F5AFC6","#9BDFC9"]},
  {name:"わかくさ",   bg:["#EFF8E1","#F6FCEF"], main:"#6FA832", deco:["#B4DC8C","#FFD93B","#FFC0A0"]},
  {name:"よぞら",     bg:["#2E3560","#4A3E78"], main:"#6A74C9", deco:["#8F9AE8","#FFE28A","#F5AFC6"]},
];
const GOLD = {name:"ゴールド", bg:["#FFF6D8","#FFE8B0"], main:"#B8860B", deco:["#FFD700","#FFB300","#FFDF7E"]};
const MS=[
 {d:3,  t:"『気がする』チェックポイント", m:"<span style='display:inline-block'>「ちょっと軽い気がする」</span><span style='display:inline-block'>それ気のせいじゃないですよ！</span>", q:""},
 {d:4,  t:"三日坊主 超えました", m:"多くの人がつまずく壁 もう超えました！えらい！", q:"4日目終わって三日坊主回避しました！（先輩の声）"},
 {d:7,  t:"1週間たっせい", m:"最初の壁突破です！", q:"お風呂上がりに1日1回を1週間続けただけで、長座体前屈50cm→64cmになりました（先輩の声）"},
 {d:14, t:"2週間たっせい", m:"2週間は完成じゃなくて本番のはじまり！ここからが楽しいところ", q:"2週間で完全に、では私はなかったです。でも1ヶ月以上で確実に変わりました（先輩の声）"},
 {d:21, t:"3週間たっせい", m:"", q:"ずっと三日坊主だったんだけど、初めて3週間続けられてうれしい…（先輩の声）"},
 {d:30, t:"1ヶ月たっせい", m:"目に見える変化が出はじめる時期に入りました", q:"開脚90度→2ヶ月で150度。すぐ結果が出なくても続けた人の記録です（先輩の声）"},
 {d:50, t:"50日たっせい", m:"", q:""},
 {d:66, t:"習慣化の記念日", m:"習慣が身につくまで平均66日という研究があります きょうがその日です🎉", q:""},
 {d:100,t:"100日たっせい", m:"もう体の一部ですね", q:"1日の締めくくりとして、もはや歯磨きと同じ習慣と化している（先輩の声）"},
 {d:150,t:"150日たっせい", m:"", q:""},
 {d:200,t:"200日たっせい", m:"", q:""},
 {d:300,t:"300日たっせい", m:"", q:""},
 {d:365,t:"1年たっせい", m:"1年前とはもう別の体です！ほんとうにすごい", q:""},
 {d:500,t:"500日たっせい", m:"", q:""},
 {d:730,t:"2年たっせい", m:"", q:""},
 {d:1000,t:"1000日たっせい", m:"生活の一部どころか人生の一部ですね", q:""},
 {d:1900,t:"1900日——伝説の域", m:"1900日続けた先輩は高校生から社会人になっていました あなたの記録ももう物語です✨", q:""},
];

// w=カード描画幅(px)。各イラストの「頭部が画像に占める高さの割合」がポーズごとに違う
// （クローズアップ構図 vs 全身構図）ため、幅280固定だと頭のサイズがバラバラに見える。
// chara-crown(節目・w=280)の頭部サイズを基準に、他は頭の見た目サイズが揃うよう個別に拡大。
const CHARA_FILES=[
  {file:"assets/chara-good.png", w:285},
  {file:"assets/chara-kaikyaku.png", w:310},
  {file:"assets/chara-cheer.png", w:378},
  {file:"assets/chara-cracker.png", w:334},
  {file:"assets/chara-congrats.png", w:321},
];

function ensureCardFonts(){
  const specs=['900 100px "M PLUS 1p"','800 52px "M PLUS 1p"','700 34px "M PLUS 1p"','100px "BananaNum"'];
  const work=(async()=>{
    try{ await Promise.all(specs.map(x=>document.fonts.load(x))); }catch(e){}
    for(let i=0;i<8;i++){
      try{ if(document.fonts.check('900 100px "M PLUS 1p"')&&document.fonts.check('100px "BananaNum"')) return; }catch(e){ return; }
      await new Promise(r=>setTimeout(r,150));
      try{ await Promise.all(specs.map(x=>document.fonts.load(x))); }catch(e){}
    }
  })();
  // iOSでフォントAPIが黙り込んでも必ず先へ進む保険
  return Promise.race([work, new Promise(r=>setTimeout(r,2200))]);
}
function makeCard(ds){
  const st=getStreakData();
  if(ds){
    if(!st.dates.includes(ds)) return;
    cardDate=ds;
  } else {
    if(st.total===0){
      const c=document.getElementById("cheer");
      if(c) c.textContent="まずはきょうの1本のあとに「きょうやった！」を押してみて😊";
      return;
    }
    cardDate=todayStr();
  }
  const tEl=document.getElementById("cardTitle");
  if(tEl) tEl.textContent = cardDate===todayStr() ? "きょうの記録カード" : `${Number(cardDate.slice(5,7))}/${Number(cardDate.slice(8,10))} の記録カード`;
  const tierEl=document.getElementById("cardTierNote"); if(tierEl) tierEl.textContent="";
  // タップした瞬間に開く（無反応に見せない）
  const mk=document.getElementById("cardMaking");
  if(mk){ mk.classList.remove("hidden"); mk.textContent="カードをつくってます…"; }
  const sn0=document.getElementById("cardSaveNote"); if(sn0) sn0.classList.add("hidden");
  document.getElementById("cardModal").classList.remove("hidden");
  modalFocusOpen("cardModalBox");
  updateFabs();
  // その日のカードパターン画像キーを先に決めてプリロード（画像方式=CARD_IMG_FROM以降のみキーが返る）
  let motifKey=null;
  let _pat=null, _milestone=false, _msInfo=null;
  try{
    const _st=getStreakData(), _idx=_st.dates.indexOf(cardDate);
    const _off=Math.max(0,(_st.total||0)-_st.dates.length);
    const _eff=_idx>=0?_idx+1+_off:_st.total;
    const _di=Math.floor((new Date(cardDate).getTime()+9*3600*1000)/86400000);
    _pat=cardPatternFor(cardDate,_eff,_di);
    if(_pat&&_pat.key) motifKey=_pat.key;
    _milestone=MILESTONES.includes(_eff);
    _msInfo=_milestone?MS.find(x=>x.d===_eff):null;
  }catch(e){}
  ensureCardFonts().then(()=>loadChara(()=>loadTypeIcon(state.type?state.type.key:null,()=>loadCardMotif(motifKey,()=>{
    try{
      drawCard(); if(mk) mk.classList.add("hidden");
      const tierLabel={toku:"🎉 記念日カード",season:"🌿 季節のカード",rare:"✨ レアカード",normal:"🎨 ノーマルカード"};
      if(tierEl){
        if(_pat&&tierLabel[_pat.tier]) tierEl.textContent=`${tierLabel[_pat.tier]}「${_pat.name}」`;
        else if(!_pat&&_milestone&&_msInfo) tierEl.textContent=`🎉 記念日カード「${_msInfo.t}」`;
        else tierEl.textContent="";
      }
      if((_pat&&(_pat.tier==="toku"||_pat.tier==="rare"))||(!_pat&&_milestone)){ try{ launchConfetti(90); }catch(e){} }
    }
    catch(e){ if(mk) mk.textContent="うまく作れませんでした もう一度「とじる」→開き直してみて"; }
  }))));
}
function drawCard(){
  const st=getStreakData();
  const ds=cardDate||todayStr();
  const isToday = ds===todayStr();
  const idx=st.dates.indexOf(ds);
  // dates配列が不完全（旧形式からの移行・1200件剪定）でも通算がズレないよう補正
  const off=Math.max(0,(st.total||0)-st.dates.length);
  const effTotal = idx>=0 ? idx+1+off : st.total;
  let effCount;
  const dlRec=(store.get("daylog",{})||{})[ds];
  if(isToday) effCount=st.count;
  else if(dlRec&&dlRec.c) effCount=dlRec.c; // その日の実測値（おやすみ券の橋渡し込み）
  else { effCount=1; let cur=ds; while(st.dates.includes(prevOf(cur))){ effCount++; cur=prevOf(cur); } }
  const dateIdx = Math.floor((new Date(ds).getTime()+9*3600*1000)/86400000);
  const T = state.type ? TYPES[state.type.key] : null;
  const milestone = MILESTONES.includes(effTotal);
  const msInfo = milestone ? MS.find(x=>x.d===effTotal) : null;
  // テーマ選択: CARD_IMG_FROM(2026-07-14)以降は画像方式(記念>季節>抽選)。それ未満は従来の色割り当てを完全維持（過去カード再現）。
  const pat = cardPatternFor(ds, effTotal, dateIdx); // null=従来方式(CARD_IMG_FROM未満 or 画像なし節目)
  const themeCount = dateIdx>=CARD_THEMES_V2_FROM ? CARD_THEMES.length : CARD_THEMES_V1_COUNT;
  const th = pat ? {name:pat.name, bg:pat.bg, main:(pat.main||pat.bg[1]), deco:(pat.deco||[pat.bg[1],"#FFD93B","#F5AFC6"])}
                 : (milestone ? GOLD : CARD_THEMES[dateIdx%themeCount]);
  const c=document.getElementById("cardCanvas"), ctx=c.getContext("2d");
  const F='"M PLUS 1p","Hiragino Kaku Gothic ProN",sans-serif';
  // 背景グラデ
  const g=ctx.createLinearGradient(0,0,1000,1000);
  g.addColorStop(0,th.bg[0]); g.addColorStop(1,th.bg[1]);
  ctx.fillStyle=g; ctx.fillRect(0,0,1000,1000);
  const hasMotif = pat && pat.key && cardMotifKey===pat.key && cardMotifImg;
  if(hasMotif){
    // 画像方式: 透過モチーフを全面に重ねる（中央は透明→白カードで隠れる）
    ctx.drawImage(cardMotifImg,0,0,1000,1000);
  } else if(pat){
    // ノーマル(画像なし)・画像未読込時のフォールバック=日替わり散らし（cardRand・新テーマ色）
    const rnd=cardRand(dateIdx);
    const SHAPES=["h","s","k","c","f","k","c"];
    const bands=[[30,970,25,150],[30,970,850,975],[25,75,160,850],[925,975,160,850]];
    const n=12+Math.floor(rnd()*5);
    for(let i=0;i<n;i++){
      const b=bands[Math.floor(rnd()*bands.length)];
      const x=Math.round(b[0]+rnd()*(b[1]-b[0])), y=Math.round(b[2]+rnd()*(b[3]-b[2])), sz=Math.round(10+rnd()*24);
      const sh=SHAPES[Math.floor(rnd()*SHAPES.length)], col=th.deco[i%th.deco.length];
      if(sh==="h") drawHeart(ctx,x-sz/2,y-sz/2,sz,col);
      else if(sh==="s") drawStar(ctx,x,y,sz*0.6,col);
      else if(sh==="k") drawSparkle(ctx,x,y,sz*0.7,col);
      else if(sh==="f") drawFlower(ctx,x,y,sz*0.62,col);
      else {ctx.fillStyle=col;ctx.beginPath();ctx.arc(x,y,sz,0,7);ctx.fill();}
    }
  } else {
    // ===== CARD_IMG_FROM未満 or 画像なし節目: 従来方式を1バイトも変えない（過去カード再現） =====
    const isYozora = th.name==="よぞら";
    let deco;
    if(dateIdx>=CARD_THEMES_V2_FROM && !milestone){
      const rnd=cardRand(dateIdx);
      const SHAPES=["h","s","k","c","f","k","c"]; // kとcを厚めに（にぎやかすぎ防止）
      const bands=[[30,970,25,150],[30,970,850,975],[25,75,160,850],[925,975,160,850]]; // 上/下/左/右の帯（白カードに隠れない場所）
      const n=12+Math.floor(rnd()*5); // 12〜16個
      deco=[];
      for(let i=0;i<n;i++){
        const b=bands[Math.floor(rnd()*bands.length)];
        const x=b[0]+rnd()*(b[1]-b[0]), y=b[2]+rnd()*(b[3]-b[2]);
        let sh=SHAPES[Math.floor(rnd()*SHAPES.length)];
        if(isYozora&&(sh==="h"||sh==="f")) sh=rnd()<0.5?"s":"k";
        deco.push([sh,Math.round(x),Math.round(y),Math.round(10+rnd()*24)]);
      }
    } else {
      deco=[
        ["h",95,150,34],["s",885,120,26],["k",120,860,30],["h",905,850,30],
        ["c",60,480,11],["k",940,430,24],["s",180,70,18],["c",500,60,9],
        ["h",820,300,22],["c",935,650,10],["s",70,690,16],["c",250,935,9],["k",760,945,22]
      ];
    }
    deco.forEach((d,i)=>{
      const col=th.deco[i%th.deco.length];
      if(d[0]==="h") drawHeart(ctx,d[1]-d[3]/2,d[2]-d[3]/2,d[3],col);
      else if(d[0]==="s") drawStar(ctx,d[1],d[2],d[3]*0.6,col);
      else if(d[0]==="k") drawSparkle(ctx,d[1],d[2],d[3]*0.7,col);
      else if(d[0]==="f") drawFlower(ctx,d[1],d[2],d[3]*0.62,col);
      else {ctx.fillStyle=col;ctx.beginPath();ctx.arc(d[1],d[2],d[3],0,7);ctx.fill();}
    });
    if(isYozora&&!milestone) drawMoon(ctx,152,108,44,"#FFE9A8"); // よぞらの夜だけ三日月がのぼる
    if(milestone){
      for(let i=0;i<24;i++){ctx.save();const x=(i*173)%1000,y=(i*257)%1000;ctx.translate(x,y);ctx.rotate(i*1.3);ctx.fillStyle=th.deco[i%3];ctx.fillRect(-8,-3,16,6);ctx.restore();}
    }
  }
  // 白カード（ふち＝テーマ色の点線）
  ctx.fillStyle="rgba(255,255,255,.94)"; roundRect(ctx,85,175,830,650,52); ctx.fill();
  ctx.save();ctx.strokeStyle=th.main;ctx.globalAlpha=.45;ctx.lineWidth=4;ctx.setLineDash([2,16]);ctx.lineCap="round";
  roundRect(ctx,110,200,780,600,40); ctx.stroke();ctx.restore();
  // リボン風タイトルピル
  ctx.fillStyle=th.main; roundRect(ctx,300,145,400,64,32); ctx.fill();
  ctx.textAlign="center"; ctx.fillStyle="#FFFFFF"; ctx.font="900 34px "+F;
  ctx.fillText("#きょうのオガトレ",500,190);
  // 日付バッジ（右上）
  {
    const dtxt=`${Number(ds.slice(0,4))}/${Number(ds.slice(5,7))}/${Number(ds.slice(8,10))}`;
    ctx.font="800 26px "+F;
    const dw=ctx.measureText(dtxt).width;
    ctx.save(); ctx.globalAlpha=.85; ctx.fillStyle=th.main;
    roundRect(ctx,868-dw-44,212,dw+44,52,26); ctx.fill(); ctx.restore();
    ctx.fillStyle="#FFFFFF"; ctx.fillText(dtxt,868-(dw+44)/2,247);
  }
  // 見出し（節目のときだけ）
  if(milestone){ ctx.fillStyle="#8A877D"; ctx.font="800 34px "+'"M PLUS 1p",sans-serif'; drawCrown(ctx,500,258,100,"#FFD700"); const msTxt=`${msInfo?msInfo.t:"節目たっせい"}！おめでとうございます！`; if(ctx.measureText(msTxt).width>740) ctx.font='800 28px "M PLUS 1p",sans-serif'; ctx.fillText(msTxt,500,330); }
  // 山本さん案レイアウト: 数字+日目！を1行 → タグピル行（かたさタイプ/メモ）
  const FB='"BananaNum","M PLUS 1p",sans-serif';
  const memo=(store.get("memos",{}))[ds];
  const rows=[];
  if(T) rows.push(["かたさタイプ",T.name]);
  if(memo) rows.push(["メモ",memo]);
  const shift = milestone?0:(rows.length?0:60);
  // 数字＋日目！（1行・ベースライン揃え）
  ctx.textAlign="left";
  const numTxt=String(effTotal);
  ctx.font="900 180px "+F; const numW=ctx.measureText(numTxt).width;
  ctx.font="900 84px "+F; const dayW=ctx.measureText("日目！").width;
  const bx=500-(numW+16+dayW)/2;
  const by=(milestone?520:462)+shift;
  ctx.fillStyle=th.main; ctx.font="900 180px "+F; ctx.fillText(numTxt,bx,by);
  ctx.fillStyle="#3A3A35"; ctx.font="900 84px "+F; ctx.fillText("日目！",bx+numW+16,by);
  ctx.textAlign="center";
  if(effCount>=2){ ctx.fillStyle="#8A877D"; ctx.font="700 30px "+F; ctx.fillText(`連続記録${effCount}日`,500,by+52); }
  // タグピル行
  const rowY = milestone? [648,764] : [614,738];
  // メモ/ありがとう行(row1)は右下のキャラと縦帯が重なる位置にあるため、実際に描画されるキャラ幅から
  // その行だけ避け幅を動的に確保する（かたさタイプ行=row0はキャラ上端より上にあり無関係・従来通り860固定）
  const chW = milestone?280:charaW;
  const chActive = milestone ? !!(crownImg||charaImg) : !!charaImg;
  const row1RightEdge = chActive ? (985-chW-24) : 860;
  ctx.textAlign="left";
  rows.slice(0,2).forEach((row,i)=>{
    const yc=rowY[i];
    ctx.font="28px "+FB;
    const lw=ctx.measureText(row[0]).width;
    const pw=lw+48;
    ctx.fillStyle=th.main;
    roundRect(ctx,130,yc-30,pw,60,30); ctx.fill();
    ctx.fillStyle="#FFFFFF";
    ctx.fillText(row[0],130+24,yc+10);
    // 値（幅に合わせて縮小）
    const vx=130+pw+26;
    const maxW=(i===1?row1RightEdge:860)-vx;
    let fs=46;
    ctx.fillStyle="#3A3A35";
    let txt=row[1];
    ctx.font="800 "+fs+"px "+F;
    while(ctx.measureText(txt).width>maxW && fs>32){ fs-=2; ctx.font="800 "+fs+"px "+F; }
    if(ctx.measureText(txt).width<=maxW){
      ctx.fillText(txt,vx,yc+Math.round(fs*0.36));
      if(row[0]==="かたさタイプ" && typeIconImg){
        const ih=fs+10, ar=typeIconImg.naturalWidth/typeIconImg.naturalHeight||1, iw=ih*ar;
        const ix=vx+ctx.measureText(txt).width+14, iy=yc-ih/2;
        if(ix+iw<=maxW+vx) ctx.drawImage(typeIconImg, ix, iy, iw, ih);
      }
    } else if(i===1){
      // メモ行はキャラ回避で幅が狭くなりうるため、切り捨てる前に最大3行まで許容(20文字程度を欠けさせない)
      fs=28; ctx.font="800 "+fs+"px "+F;
      const lines=wrapLines(ctx,txt,maxW,3);
      const ly0=yc-(lines.length-1)*19;
      lines.forEach((l,li)=>ctx.fillText(l,vx,ly0+li*38));
    } else {
      // 2行に分割（30px固定・2行目に収まらない分は…）
      fs=30; ctx.font="800 "+fs+"px "+F;
      let l1="", i2=0;
      while(i2<txt.length && ctx.measureText(l1+txt[i2]).width<=maxW){ l1+=txt[i2]; i2++; }
      let l2=txt.slice(i2);
      while(ctx.measureText(l2).width>maxW && l2.length>2){ l2=l2.slice(0,l2.length-2)+"…"; }
      ctx.fillText(l1,vx,yc-6);
      ctx.fillText(l2,vx,yc+32);
    }
  });
  ctx.textAlign="center";
  // 点線区切り
  if(rows.length>=2){
    const sy=(rowY[0]+rowY[1])/2;
    ctx.save(); ctx.strokeStyle=th.main; ctx.globalAlpha=.4; ctx.lineWidth=3; ctx.setLineDash([3,11]); ctx.lineCap="round";
    ctx.beginPath(); ctx.moveTo(150,sy); ctx.lineTo(850,sy); ctx.stroke(); ctx.restore();
  }
  // キャラ（日替わりローテ・節目は王冠ドヤ・右下の専用ゾーンで文字と重ねない）
  const chFig = milestone ? (crownImg||charaImg) : charaImg;
  if(chFig){
    const w=milestone?280:charaW, h=w*chFig.height/chFig.width;
    ctx.drawImage(chFig, 985-w, 998-h, w, h);
  }
  // フッター＝キャラの吹き出し
  ctx.textAlign="left";
  ctx.fillStyle=th.main;
  const fm=Number(ds.slice(5,7)), fd=Number(ds.slice(8,10));
  const COMMON=[
    "今日も一日、じぶんを大切に。ご自愛くださいね。",
    "がんばりすぎず、ほどよく。ご自愛くださいね。",
    "深呼吸ひとつぶん、じぶんをいたわる時間を。",
    "今日のあなたに、おつかれさまとご自愛を。",
    "体の声をきいて、むりせずご自愛くださいね。",
    "つづけてる自分を、ちゃんと褒めてあげてね。",
  ];
  let pool;
  if(milestone) pool=[
    "節目の日。がんばった体に、ごほうびのご自愛を。",
    "ここまでつづけた自分に、拍手とご自愛を。",
    "きょうは記念日。とびきりのご自愛を。",
  ];
  else if(fm===1&&fd<=7) pool=["今年もじぶんのペースで。ご自愛くださいね。","新年も一日一本、ゆるっといきましょう。"];
  else if(fm===12&&fd>=28) pool=["一年おつかれさま。ゆっくりご自愛くださいね。","今年もよくがんばりました。よいお年を。"];
  else if(fm===6) pool=COMMON.concat(["じめじめの季節も心は軽く。ご自愛くださいね。","雨の日は、おうちストレッチ日和です。"]);
  else if(fm===7||fm===8) pool=COMMON.concat(["暑い毎日、水分とご自愛を忘れずに。","夏バテ予防も、ストレッチとご自愛から。"]);
  else if(fm>=9&&fm<=11) pool=COMMON.concat(["季節の変わり目、ゆっくりご自愛くださいね。","実りの秋。体にもいいことを少しずつ。"]);
  else if(fm===12||fm<=2) pool=COMMON.concat(["寒い季節も、あたたかくご自愛くださいね。","湯船にゆっくり。それもご自愛のうち。"]);
  else pool=COMMON.concat(["新しい季節も、マイペースにご自愛くださいね。","春の陽気と一緒に、体もゆるめてあげてね。"]);
  let fh=0; for(const c of ds){ fh=(fh*31+c.charCodeAt(0))>>>0; }
  const fmsg=pool[fh%pool.length];
  let ffs=27; ctx.font="800 "+ffs+"px "+F;
  while(ctx.measureText(fmsg).width>560 && ffs>21){ ffs-=1; ctx.font="800 "+ffs+"px "+F; }
  const bw=ctx.measureText(fmsg).width+56;
  const bx1=Math.max(70,690-bw);
  // 吹き出し本体＋キャラへのしっぽ
  ctx.save();
  ctx.fillStyle="rgba(255,255,255,.95)";
  roundRect(ctx,bx1,900,bw,74,37); ctx.fill();
  ctx.beginPath(); ctx.moveTo(bx1+bw-6,918); ctx.lineTo(bx1+bw+26,930); ctx.lineTo(bx1+bw-6,950); ctx.closePath(); ctx.fill();
  ctx.restore();
  ctx.fillStyle=th.main;
  ctx.fillText(fmsg,bx1+28,900+37+Math.round(ffs*0.36));
  ctx.textAlign="center";
  // プレビュー表示
  document.getElementById("cardImg").src=c.toDataURL("image/png");
  document.getElementById("cardModal").classList.remove("hidden");
  updateFabs();
  canvasToBlobCompat(c, blob=>{ lastBlob=blob; });
  lastShareText=`#きょうのオガトレ ${effTotal}日目！`;
}
function downloadCard(file){
  // download属性が効かないブラウザではa.click()がblobページへの遷移になり戻れず立ち往生するため、
  // クリックせず「画像を長押しで保存」を案内する（cardImgは-webkit-touch-callout:defaultで長押し保存可）
  if(!("download" in HTMLAnchorElement.prototype)){
    const n=document.getElementById("cardSaveNote");
    if(n){ n.textContent="上のカードの画像を長押しすると保存できます📷"; n.classList.remove("hidden"); }
    return;
  }
  const a=document.createElement("a");
  a.href=URL.createObjectURL(lastBlob); a.download=file.name; a.click();
  setTimeout(()=>URL.revokeObjectURL(a.href),5000);
  // Web Share非対応環境向け: ダウンロードしただけだと無反応に見えるため一言添える
  const note=document.getElementById("cardSaveNote");
  if(note) note.classList.remove("hidden");
  markCardSaved();
}
async function shareCard(){
  // SNS投稿用: ハッシュタグ文言つき→ダメなら画像だけで開き直す（iOSは文言+画像の同時共有に失敗することがある）
  if(!lastBlob) return;
  const file=cardFile();
  if(navigator.canShare && navigator.canShare({files:[file]})){
    try{ await navigator.share({files:[file], text:lastShareText||"#きょうのオガトレ"}); markCardSaved(); return; }
    catch(e){ if(e && e.name==="AbortError") return; } // キャンセル＝保存していないのでフラグは立てない
    try{ await navigator.share({files:[file]}); markCardSaved(); }catch(e){}
    return; // 共有シートが使える端末ではダウンロードに落とさない
  }
  downloadCard(file);
}
