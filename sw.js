// #きょうのオガトレ オフラインキャッシュ（https配信時のみ有効）
const C="kyono-v2";
const ASSETS=["./","index.html","videos.js","manifest.json","assets/chara.png","assets/chara-good.png","assets/chara-kaikyaku.png","assets/icon-192.png","assets/icon-512.png","assets/icon-180.png"];
self.addEventListener("install",e=>{e.waitUntil(caches.open(C).then(c=>c.addAll(ASSETS)).then(()=>self.skipWaiting()))});
self.addEventListener("activate",e=>{e.waitUntil(caches.keys().then(ks=>Promise.all(ks.filter(k=>k!==C).map(k=>caches.delete(k)))).then(()=>self.clients.claim()))});
self.addEventListener("fetch",e=>{
  const u=new URL(e.request.url);
  if(u.origin!==location.origin) return;
  e.respondWith(
    fetch(e.request).then(r=>{
      if(r.ok){ const cl=r.clone(); caches.open(C).then(c=>c.put(e.request,cl)); return r; }
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
