// 手元のNodeで worker.mjs を :8790 で起動して試すための薄いラッパ。
// 使い方:
//   本物: ANTHROPIC_API_KEY=sk-... node serve-local.mjs
//   モック: MOCK=1 node serve-local.mjs
// そのあと demo.html をブラウザで開く。
import { createServer } from "node:http";
import { readFileSync } from "node:fs";
import { handle } from "./worker.mjs";

const env = {
  ANTHROPIC_API_KEY: process.env.ANTHROPIC_API_KEY,
  ANTHROPIC_BASE_URL: process.env.ANTHROPIC_BASE_URL,
  MODEL: process.env.MODEL || "claude-haiku-4-5",
  MOCK: process.env.MOCK,
  DAILY_CAP: process.env.DAILY_CAP,
  MONTHLY_YEN_CAP: process.env.MONTHLY_YEN_CAP,
};
const PORT = Number(process.env.PORT || 8790);

const server = createServer(async (req, res) => {
  // CORS プリフライト
  if (req.method === "OPTIONS") {
    res.writeHead(204, {
      "access-control-allow-origin": "*",
      "access-control-allow-methods": "POST, GET, OPTIONS",
      "access-control-allow-headers": "content-type",
    });
    return res.end();
  }
  // デモHTMLを配る
  if (req.method === "GET" && (req.url === "/" || req.url === "/demo.html")) {
    try {
      const html = readFileSync(new URL("./demo.html", import.meta.url));
      res.writeHead(200, { "content-type": "text/html; charset=utf-8" });
      return res.end(html);
    } catch {
      res.writeHead(404); return res.end("demo.html not found");
    }
  }
  if (req.method === "POST" && req.url === "/chat") {
    const chunks = [];
    for await (const c of req) chunks.push(c);
    const raw = Buffer.concat(chunks).toString("utf8");
    const request = {
      method: "POST",
      headers: { get: () => null },
      json: async () => JSON.parse(raw || "{}"),
    };
    const out = await handle(request, env);
    const text = await out.text();
    res.writeHead(out.status || 200, { "content-type": "application/json; charset=utf-8", "access-control-allow-origin": "*" });
    return res.end(text);
  }
  res.writeHead(404); res.end("not found");
});

server.listen(PORT, () => {
  const mode = env.MOCK === "1" ? "MOCK（Claude未接続）" : env.ANTHROPIC_API_KEY ? `本物（${env.MODEL}）` : "キー無し→ /chat はエラー→フォールバック";
  console.log(`相談室AI試作 起動: http://localhost:${PORT}/  モード=${mode}`);
});
