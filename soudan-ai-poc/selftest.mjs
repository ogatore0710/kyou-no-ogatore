// 通しテスト（キー不要・MOCKで）。赤旗・通常・動画検証・上限フォールバックを確認。
import { handle } from "./worker.mjs";
import { CATALOG } from "./data.mjs";

const CAT_IDS = new Set(CATALOG.map((c) => c.id));
function req(message, history) {
  return { method: "POST", headers: { get: () => null }, json: async () => ({ message, history, __ip: "test" }) };
}
async function call(env, message, history) {
  const res = await handle(req(message, history), env);
  return JSON.parse(await res.text());
}

let pass = 0, fail = 0;
function ok(name, cond, extra) { (cond ? pass++ : fail++); console.log((cond ? "✅" : "❌") + " " + name + (extra ? "  " + extra : "")); }

const base = { MOCK: "1", DAILY_CAP: "3", MONTHLY_YEN_CAP: "3000" };

// 1. 赤旗はAIを通さず受診案内
const rf = await call(base, "歩くと足に激痛がしてしびれもある");
ok("赤旗→受診案内(needsReferral & source=redflag)", rf.needsReferral === true && rf.source === "redflag");

// 2. 通常の悩み→ai・動画は実在カタログ内
const norm1 = await call({ ...base }, "デスクワークで肩がバキバキにこる");
ok("通常→source=ai", norm1.source === "ai", "source=" + norm1.source);
ok("動画IDが実在カタログ内 or null", norm1.videoId === null || CAT_IDS.has(norm1.videoId), "videoId=" + norm1.videoId);

// 3. でっち上げ動画は捨てられる（モックのvideoIdを候補外に差し替える経路は grounding 検証でカバー。
//    ここでは候補が空になる無関係入力で videoId=null に落ちることを確認）
const gib = await call({ ...base }, "きょうの天気とラーメンについて");
ok("無関係入力→videoIdはnull（候補なし）", gib.source !== "ai" || gib.videoId === null, "source=" + gib.source + " videoId=" + gib.videoId);

// 4. 上限（1日3回）超過→fallback
const env2 = { ...base, DAILY_CAP: "2" };
await call(env2, "肩こり");
await call(env2, "肩こり");
const third = await call(env2, "肩こり"); // 3回目は超過
ok("1日上限超過→source=fallback(reason=day)", third.source === "fallback" && third.reason === "day", "reason=" + third.reason);

console.log(`\n${pass} passed, ${fail} failed`);
process.exit(fail ? 1 : 0);
