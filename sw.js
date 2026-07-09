// #きょうのオガトレ オフラインキャッシュ（https配信時のみ有効）
const C="kyono-v12";
const ASSETS=["./","index.html","videos.js","app-search.js","obu-feed.js","manifest.json","assets/chara.png","assets/chara-good.png","assets/chara-kaikyaku.png","assets/chara-2.png","assets/chara-3.png","assets/chara-cheer.png","assets/chara-crown.png","assets/obu-fab-photo.jpg","assets/check/q1.jpg","assets/check/q2.jpg","assets/check/meter.jpg","assets/icon-192.png","assets/icon-512.png","assets/icon-180.png","assets/fonts/banana-card.woff2?v=2"];
// シェル（app本体）は必須=addAll、画像などはベストエフォート（1枚の失敗でオフライン対応全体を失わない）
// assets/obu/ 配下（写真・音声）は将来ファイルが増える想定のため事前キャッシュ対象に含めない
const SHELL=["./","index.html","videos.js","app-search.js","obu-feed.js","manifest.json"];
self.addEventListener("install",e=>{e.waitUntil(caches.open(C).then(c=>c.addAll(SHELL).then(()=>Promise.all(ASSETS.filter(a=>SHELL.indexOf(a)<0).map(a=>c.add(a).catch(()=>{}))))).then(()=>self.skipWaiting()))});
self.addEventListener("activate",e=>{e.waitUntil(caches.keys().then(ks=>Promise.all(ks.filter(k=>k!==C).map(k=>caches.delete(k)))).then(()=>self.clients.claim()))});
self.addEventListener("fetch",e=>{
  const u=new URL(e.request.url);
  if(u.origin!==location.origin) return;
  // ページ本体とデータは常にサーバーへ再確認(10分CDNキャッシュ対策・ETag再検証なので軽い)
  const req=(e.request.mode==="navigate"||u.pathname.endsWith("videos.js")||u.pathname.endsWith("app-search.js")||u.pathname.endsWith("obu-feed.js")||u.pathname.endsWith("index.html"))
    ? new Request(e.request, {cache:"no-cache"}) : e.request;
  e.respondWith(
    fetch(req).then(r=>{
      if(r.ok){ const cl=r.clone(); e.waitUntil(caches.open(C).then(c=>c.put(e.request,cl)).catch(()=>{})); return r; }
      // デプロイ過渡期の404などはキャッシュ済みの正常版で吸収
      return caches.match(e.request).then(hit=>hit||r);
    })
    .catch(()=>caches.match(e.request).then(r=>{
      if(r) return r;
      if(e.request.mode==="navigate") return caches.match("index.html");
      return Response.error();
    }))
  );
});
