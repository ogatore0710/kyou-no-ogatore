// #きょうのオガトレ オフラインキャッシュ（https配信時のみ有効）
const C="kyono-v62";
const ASSETS=["./","index.html","videos.js","app-search.js","obu-feed.js","soudan-kb.js","app-quiz.js","app-record.js","app-card.js","app-env.js","manifest.json","assets/chara.png","assets/chara-good.png","assets/chara-kaikyaku.png","assets/card-sample.png","assets/card-sample-gold.png","assets/chara-2.png","assets/chara-3.png","assets/chara-cheer.png","assets/chara-crown.png","assets/obu-fab-photo.jpg","assets/check/q1.jpg","assets/check/q2.jpg","assets/check/meter.jpg","assets/icon-192.png","assets/icon-512.png","assets/icon-180.png","assets/fonts/banana-card.woff2?v=2","assets/fonts/mplus1p-700.woff2","assets/fonts/mplus1p-800.woff2","assets/fonts/mplus1p-900.woff2","assets/type-momo.png","assets/type-kenko.png","assets/type-yawara.png","assets/pl-asa30.jpg","assets/pl-yoru30.jpg","assets/pl-suwatta20.jpg","assets/pl-zenshin35.jpg","assets/pl-jiritsu30.jpg","assets/pl-katakori30.jpg","assets/pl-koukansetsu30.jpg","assets/pl-youtsuu30.jpg","assets/chara-cracker.png","assets/chara-congrats.png","assets/chara-hitokoto.png"];
// シェル（app本体）は必須=addAll、画像などはベストエフォート（1枚の失敗でオフライン対応全体を失わない）
// assets/obu/ 配下（写真・音声）は将来ファイルが増える想定のため事前キャッシュ対象に含めない
const SHELL=["./","index.html","videos.js","app-search.js","obu-feed.js","soudan-kb.js","app-quiz.js","app-record.js","app-card.js","app-env.js","manifest.json"];
// SHELLはcache:"reload"でHTTPキャッシュを飛ばす（デプロイ直後10分のCDNキャッシュ焼き込み防止）。旧ブラウザでRequest生成が落ちたらURLのままにフォールバック
self.addEventListener("install",e=>{e.waitUntil(caches.open(C).then(c=>{
  let shellReqs=SHELL;
  try{ shellReqs=SHELL.map(u=>new Request(u,{cache:"reload"})); }catch(err){ shellReqs=SHELL; }
  return c.addAll(shellReqs).then(()=>Promise.all(ASSETS.filter(a=>SHELL.indexOf(a)<0).map(a=>c.add(a).catch(()=>{}))));
}).then(()=>self.skipWaiting()))});
self.addEventListener("activate",e=>{e.waitUntil(caches.keys().then(ks=>Promise.all(ks.filter(k=>k!==C).map(k=>caches.delete(k)))).then(()=>self.clients.claim()))});
self.addEventListener("fetch",e=>{
  const u=new URL(e.request.url);
  if(u.origin!==location.origin) return;
  // ページ本体とデータは常にサーバーへ再確認(10分CDNキャッシュ対策・ETag再検証なので軽い)
  const isShell=(e.request.mode==="navigate"||u.pathname.endsWith("videos.js")||u.pathname.endsWith("app-search.js")||u.pathname.endsWith("obu-feed.js")||u.pathname.endsWith("soudan-kb.js")||u.pathname.endsWith("app-quiz.js")||u.pathname.endsWith("app-record.js")||u.pathname.endsWith("app-card.js")||u.pathname.endsWith("app-env.js")||u.pathname.endsWith("index.html"));
  // 画像・フォント等の静的アセットはキャッシュ優先(sw版更新で入れ替わる・体感速度↑・通信減)。YouTubeサムネはクロスオリジンなので対象外
  const isAsset=/\.(png|jpe?g|webp|gif|svg|woff2?|ttf)(\?|$)/i.test(u.pathname);
  if(!isShell && isAsset){
    e.respondWith(caches.match(e.request).then(hit=>{
      if(hit) return hit;
      return fetch(e.request).then(r=>{
        if(r&&r.ok){ const cl=r.clone(); e.waitUntil(caches.open(C).then(c=>c.put(e.request,cl)).catch(()=>{})); }
        return r;
      }).catch(()=>caches.match(e.request).then(x=>x||Response.error()));
    }));
    return;
  }
  // 旧Safari(SW初期実装)ではnavigateリクエストからのRequest生成がTypeErrorになりうる→素のリクエストにフォールバック
  let req=e.request;
  if(isShell){ try{ req=new Request(e.request, {cache:"no-cache"}); }catch(err){ req=e.request; } }
  const netThenCache=fetch(req).then(r=>{
    if(r.ok){ const cl=r.clone(); e.waitUntil(caches.open(C).then(c=>c.put(e.request,cl)).catch(()=>{})); return r; }
    // デプロイ過渡期の404などはキャッシュ済みの正常版で吸収
    return caches.match(e.request).then(hit=>hit||r);
  })
  .catch(()=>caches.match(e.request).then(r=>{
    if(r) return r;
    if(e.request.mode==="navigate") return caches.match("index.html");
    return Response.error();
  }));
  if(!isShell){ e.respondWith(netThenCache); return; }
  // シェルは「切れないが遅い」回線対策：約3秒でキャッシュ即返し（ネットは裏で更新継続＝次回反映）
  e.respondWith(new Promise(resolve=>{
    let done=false;
    const finish=r=>{ if(!done){ done=true; resolve(r); } };
    const timer=setTimeout(()=>{ caches.match(e.request).then(hit=>{ if(hit) finish(hit); }); },3000);
    netThenCache.then(r=>{ clearTimeout(timer); finish(r); }).catch(()=>{ clearTimeout(timer); });
  }));
});
