// オガトレ相談室 AIレイヤー 中継役（プロキシ）。
// Cloudflare Worker としてそのまま置ける形（export default { fetch }）。
// 手元では serve-local.mjs 経由で Node からも動く。
//
// 役割:
//  第0層 赤旗の即時ふるい分け（AIに渡さない）
//  上限チェック（1日あたり回数 / 今月コスト）→ 超えたら fallback シグナル
//  第1層 Claude 呼び出し（尾形ボイス＋安全＋動画リスト限定・構造化出力・プロンプトキャッシュ）
//  第2層 動画ID実在チェック / 受診フラグ反映
//  失敗時は source:"fallback" を返し、クライアント側でパターン集に切替
import { norm, redFlagHit, redFlagAnswer, scoreIntents } from "./norm.mjs";
import { buildSystem, isRealVideo } from "./grounding.mjs";

const OUTPUT_SCHEMA = {
  type: "object",
  properties: {
    empathy: { type: "string" },
    mitate: { type: "string" },
    videoId: { type: "string" }, // 候補から1つ。無ければ ""（null相当）
    keizoku: { type: "string" },
    needsReferral: { type: "boolean" },
  },
  required: ["empathy", "mitate", "videoId", "keizoku", "needsReferral"],
  additionalProperties: false,
};

// ざっくり1ターン単価（円）。上限判定用の概算。実費は請求で確認する。
const YEN_PER_TURN = 0.3;

// --- 上限（Workerでは KV/Durable Object に置く。PoCはメモリ内） ---
const mem = { ipDay: new Map(), monthYen: 0, month: "" };
function overLimit(ip, env) {
  const dailyCap = Number(env.DAILY_CAP || 30);
  const monthYenCap = Number(env.MONTHLY_YEN_CAP || 3000);
  const now = env.__now || new Date();
  const day = now.toISOString().slice(0, 10);
  const month = day.slice(0, 7);
  if (mem.month !== month) { mem.month = month; mem.monthYen = 0; }
  if (mem.monthYen >= monthYenCap) return "month";
  const rec = mem.ipDay.get(ip);
  if (!rec || rec.day !== day) { mem.ipDay.set(ip, { day, count: 0 }); return false; }
  if (rec.count >= dailyCap) return "day";
  return false;
}
function tallyTurn(ip, env) {
  const rec = mem.ipDay.get(ip);
  if (rec) rec.count += 1;
  mem.monthYen += YEN_PER_TURN;
}

async function callClaude(system, messages, env) {
  if (env.MOCK === "1" || env.MOCK === 1) {
    // キー無しでもパイプラインを通すためのニセ応答（第2層の検証まで確認できる）
    const firstCand = (system.__candidateIds || [])[0] || "";
    return {
      empathy: "その悩み、よくあるやつだよね",
      mitate: "モック応答（本物のClaudeは未接続）。候補から1本選んだ体で返してるよ。",
      videoId: firstCand,
      keizoku: "まずは1本を2週間、気楽にどうぞ",
      needsReferral: false,
    };
  }
  const base = env.ANTHROPIC_BASE_URL || "https://api.anthropic.com";
  const res = await fetch(base + "/v1/messages", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "x-api-key": env.ANTHROPIC_API_KEY,
      "anthropic-version": "2023-06-01",
    },
    body: JSON.stringify({
      model: env.MODEL || "claude-haiku-4-5",
      max_tokens: 700,
      system: [
        { type: "text", text: system.persona },
        { type: "text", text: system.grounding, cache_control: { type: "ephemeral" } },
      ],
      messages,
      output_config: { format: { type: "json_schema", schema: OUTPUT_SCHEMA } },
    }),
  });
  if (!res.ok) throw new Error("anthropic " + res.status + " " + (await res.text()).slice(0, 300));
  const data = await res.json();
  if (data.stop_reason === "refusal") throw new Error("refusal");
  const textBlock = (data.content || []).find((b) => b.type === "text");
  if (!textBlock) throw new Error("no text block");
  return JSON.parse(textBlock.text);
}

export async function handle(request, env) {
  if (request.method !== "POST") return json({ error: "POST only" }, 405);
  let body;
  try { body = await request.json(); } catch { return json({ error: "bad json" }, 400); }
  const message = String(body.message || "").slice(0, 500);
  const history = Array.isArray(body.history) ? body.history.slice(-6) : [];
  const ip = request.headers.get("cf-connecting-ip") || body.__ip || "local";
  if (!message.trim()) return json({ error: "empty" }, 400);

  const n = norm(message);

  // 第0層: 赤旗は AI を通さず即返す
  if (redFlagHit(n)) return json(redFlagAnswer());

  // 上限: 超えたら fallback シグナル（クライアントがパターン集に切替）
  const lim = overLimit(ip, env);
  if (lim) return json({ source: "fallback", reason: lim });

  // 第1層: Claude
  const system = buildSystem(n);
  const messages = [
    ...history.map((h) => ({ role: h.role === "assistant" ? "assistant" : "user", content: String(h.content || "").slice(0, 500) })),
    { role: "user", content: message },
  ];
  let out;
  try {
    const sysForCall = { ...system, __candidateIds: system.candidateIds };
    out = await callClaude(sysForCall, messages, env);
    tallyTurn(ip, env);
  } catch (e) {
    return json({ source: "fallback", reason: "error", detail: String(e.message || e).slice(0, 200) });
  }

  // 第2層: 動画ID実在チェック（候補外/でっち上げは捨てる）
  let videoId = out.videoId || "";
  if (videoId && !(system.candidateIds.indexOf(videoId) >= 0 && isRealVideo(videoId))) videoId = "";

  return json({
    source: "ai",
    empathy: String(out.empathy || ""),
    mitate: String(out.mitate || ""),
    videoId: videoId || null,
    keizoku: String(out.keizoku || ""),
    needsReferral: !!out.needsReferral,
  });
}

function json(obj, status = 200) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "access-control-allow-origin": "*" },
  });
}

// Cloudflare Worker エントリ
export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "access-control-allow-origin": "*",
          "access-control-allow-methods": "POST, OPTIONS",
          "access-control-allow-headers": "content-type",
        },
      });
    }
    return handle(request, env);
  },
};

// クライアント側フォールバック（パターン集）— 参考用にサーバでも計算できるように公開
export function patternAnswer(message) {
  const n = norm(message);
  if (redFlagHit(n)) return redFlagAnswer();
  const top = scoreIntents(n)[0];
  if (!top) return { source: "fallback", empathy: "うまく聞き取れなかったかも。部位で言ってみて？", mitate: "", videoId: null, keizoku: "", needsReferral: false };
  const it = top.it;
  const v = (it.videos || [])[0];
  return { source: "pattern", empathy: it.empathy || "", mitate: it.mitate || "", videoId: v ? v.v : null, keizoku: it.keizoku || "", needsReferral: !!it.safety };
}
