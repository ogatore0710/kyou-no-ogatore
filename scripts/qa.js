#!/usr/bin/env node
const fs = require("fs");
const os = require("os");
const path = require("path");
const childProcess = require("child_process");
const vm = require("vm");

const ROOT = path.resolve(__dirname, "..");
const checks = [];
const failures = [];

function read(rel) {
  return fs.readFileSync(path.join(ROOT, rel), "utf8");
}

function exists(rel) {
  return fs.existsSync(path.join(ROOT, rel));
}

function pass(name, detail) {
  checks.push({ name, detail });
}

function fail(name, detail) {
  failures.push({ name, detail });
}

function assert(name, condition, detail) {
  if (condition) pass(name, detail);
  else fail(name, detail);
}

function parseJs(name, code) {
  try {
    new vm.Script(code, { filename: name });
    pass(`${name}: syntax`, "ok");
  } catch (err) {
    fail(`${name}: syntax`, err.message);
  }
}

function extractInlineScripts(html) {
  const scripts = [];
  const re = /<script\b([^>]*)>([\s\S]*?)<\/script>/gi;
  let match;
  while ((match = re.exec(html))) {
    const attrs = match[1] || "";
    if (!/\bsrc\s*=/.test(attrs)) scripts.push(match[2]);
  }
  return scripts;
}

function extractJsonAssignment(code, name) {
  const marker = `const ${name}=`;
  const start = code.indexOf(marker);
  if (start === -1) throw new Error(`${name} assignment not found`);
  const jsonStart = start + marker.length;
  let depth = 0;
  let inString = false;
  let escaped = false;
  for (let i = jsonStart; i < code.length; i++) {
    const ch = code[i];
    if (inString) {
      if (escaped) escaped = false;
      else if (ch === "\\") escaped = true;
      else if (ch === '"') inString = false;
      continue;
    }
    if (ch === '"') inString = true;
    else if (ch === "[" || ch === "{") depth++;
    else if (ch === "]" || ch === "}") {
      depth--;
      if (depth === 0) return JSON.parse(code.slice(jsonStart, i + 1));
    }
  }
  throw new Error(`${name} JSON parse boundary not found`);
}

function extractFunction(code, name) {
  const re = new RegExp(`function\\s+${name}\\s*\\([^)]*\\)\\s*\\{`);
  const match = re.exec(code);
  if (!match) return "";
  let depth = 1;
  let inString = false;
  let quote = "";
  let escaped = false;
  let inLineComment = false;
  let inBlockComment = false;
  const start = match.index + match[0].length;
  for (let i = start; i < code.length; i++) {
    const ch = code[i];
    const next = code[i + 1];
    if (inLineComment) {
      if (ch === "\n") inLineComment = false;
      continue;
    }
    if (inBlockComment) {
      if (ch === "*" && next === "/") {
        inBlockComment = false;
        i++;
      }
      continue;
    }
    if (inString) {
      if (escaped) escaped = false;
      else if (ch === "\\") escaped = true;
      else if (ch === quote) inString = false;
      continue;
    }
    if (ch === "/" && next === "/") {
      inLineComment = true;
      i++;
    } else if (ch === "/" && next === "*") {
      inBlockComment = true;
      i++;
    } else if (ch === '"' || ch === "'" || ch === "`") {
      inString = true;
      quote = ch;
    } else if (ch === "{") {
      depth++;
    } else if (ch === "}") {
      depth--;
      if (depth === 0) return code.slice(start, i);
    }
  }
  return "";
}

function extractConstArray(code, name) {
  const re = new RegExp(`const\\s+${name}\\s*=\\s*(\\[[\\s\\S]*?\\]);`);
  const match = code.match(re);
  if (!match) throw new Error(`${name} array not found`);
  return vm.runInNewContext(match[1]);
}

function extractFunctionNames(code) {
  const names = new Set();
  for (const match of code.matchAll(/function\s+([A-Za-z_$][\w$]*)\s*\(/g)) {
    names.add(match[1]);
  }
  return names;
}

function extractIds(html) {
  const ids = new Set();
  for (const match of html.matchAll(/\bid=["']([^"']+)["']/g)) {
    ids.add(match[1]);
  }
  return ids;
}

function extractHandlers(html) {
  const handlers = [];
  for (const match of html.matchAll(/\s(on[a-z]+)=["']([^"']+)["']/g)) {
    handlers.push({ attr: match[1], code: match[2] });
  }
  return handlers;
}

function calledGlobals(code) {
  const out = new Set();
  const skip = new Set([
    "if", "for", "while", "switch", "function", "return", "typeof",
    "Number", "String", "Boolean", "Array", "Object", "Math", "Date",
  ]);
  for (const match of code.matchAll(/(^|[^.\w$])([A-Za-z_$][\w$]*)\s*\(/g)) {
    const name = match[2];
    if (!skip.has(name)) out.add(name);
  }
  return Array.from(out);
}

function checkNoForbiddenModernSyntax(files) {
  const forbidden = [
    { label: "optional chaining", re: /\?\./ },
    { label: "nullish coalescing", re: /\?\?/ },
  ];
  for (const rel of files) {
    const code = read(rel);
    for (const item of forbidden) {
      assert(`${rel}: no ${item.label}`, !item.re.test(code), "ES2020 guard");
    }
  }
}

function checkHtml(html, cardScript) {
  const inline = extractInlineScripts(html);
  assert("index.html: inline scripts found", inline.length >= 2, `${inline.length} inline scripts`);
  parseJs("index.html inline scripts", inline.join("\n;\n"));

  const srcScripts = Array.from(html.matchAll(/<script\b([^>]*)>/gi))
    .map((m) => m[1].match(/\bsrc=["']([^"']+)["']/))
    .filter(Boolean)
    .map((m) => m[1]);
  assert("index.html: videos.js loaded", srcScripts.includes("videos.js"), srcScripts.join(", "));
  assert("index.html: app-search.js loaded", srcScripts.includes("app-search.js"), srcScripts.join(", "));
  assert("index.html: soudan-kb.js loaded", srcScripts.includes("soudan-kb.js"), srcScripts.join(", "));
  assert("index.html: boot marker", html.includes("window.__kyonoBoot=true"), "__kyonoBoot is set");
  assert("index.html: old browser note", html.includes('id="oldBrowserNote"'), "fallback node exists");

  const oldScript = inline[inline.length - 1] || "";
  const oldForbidden = /\b(const|let|class|async|await|Promise|fetch|URL)\b|=>|`/;
  assert("oldBrowserNote script: ES5-only guard", !oldForbidden.test(oldScript), "no modern syntax in final fallback");

  const main = inline[inline.length - 2] || "";
  // drawCard/ensureCardFontsはapp-card.jsへ移動済み（2026-07-17・SPLIT-PLAN.md「4. 記録カード」）。
  // 抽出元をmain(index.html)からcardScript(app-card.js)へ切り替え。ピクセル回帰の絶対ルール
  // （Math.random/Date.now/引数なしnew Date()禁止）は移動後も変わらず機械チェックする。
  const drawCard = extractFunction(cardScript, "drawCard");
  assert("drawCard: found", drawCard.length > 0, `${drawCard.length} chars`);
  assert("drawCard: no Math.random", !/Math\.random\s*\(/.test(drawCard), "card output stays reproducible");
  assert("drawCard: no Date.now", !/Date\.now\s*\(/.test(drawCard), "card output is date-driven");
  assert("drawCard: no new Date() without args", !/new\s+Date\s*\(\s*\)/.test(drawCard), "card output is not current-time-driven");

  // 2026-07-15実測監査で発見: type="number"は小数の直接入力("3.7"等)を弾かないため、
  // Math.roundで整数化してからクランプしないと「3.7日つづいてる！」のようなカードができてしまう
  const drawBragCard = extractFunction(main, "drawBragCard");
  assert("drawBragCard: found", drawBragCard.length > 0, `${drawBragCard.length} chars`);
  assert("drawBragCard: days input rounded to integer", /Math\.round\(Number\(document\.getElementById\("bragDays"\)\.value\)\)/.test(drawBragCard), "小数日数のカード化を防止");

  // 2026-07-15実測監査で発見: とどくメーター(#reach)のreach-row(5段階ボタン)が、二段重ねのFAB
  // (相談室/オガトレ通信・右下固定)と実測(390x844)でy座標が重なり、5番目のボタンの約2/3が隠れて
  // タップ不能に近い状態になっていた。quizと同様にFABを隠すことで解消（updateFabsのhide条件を機械チェック）
  const updateFabs = extractFunction(main, "updateFabs");
  assert("updateFabs: found", updateFabs.length > 0, `${updateFabs.length} chars`);
  assert("updateFabs: hides FABs on #reach (FAB重なり対策)", /currentSection===["']reach["']/.test(updateFabs), "reach-rowとFABの重なり対策");

  const ensureCardFonts = extractFunction(cardScript, "ensureCardFonts");
  assert("ensureCardFonts: timeout guard", /Promise\.race/.test(ensureCardFonts) && /2200/.test(ensureCardFonts), "font load cannot hang forever");
  const importData = extractFunction(main, "importData");
  assert("importData: size limit", /300000/.test(importData), "import payload capped");
  assert("importData: key prefix guard", /startsWith\("kyono_"\)/.test(importData), "only kyono_ keys imported");
  // 2026-07-17実測監査で発見: cnt>50だと51件目まで通ってしまうオフバイワンだったため cnt>=50 に修正。
  // 60キーのインポートを実機ヘッドレスChromeで実測し、取り込み件数が50件ちょうどであることを確認済み
  assert("importData: count guard", /cnt\s*>=\s*50/.test(importData), "key count capped (ちょうど50件までしか通さない)");
  assert("importData: value size guard", /200000/.test(importData), "individual value capped");

  // 2026-07-16実測監査対応→2026-07-17「はじめの1本ガイド」設計で移設: 唯一の再来訪装置（カレンダー通知）は
  // オンボQ3直後の吹き出しではなく「1日目クリア」直後(#calAsk・markDone側)に出す。
  // obBubble()は台本文言をtextContentで描画する安全設計（意図的にHTML埋め込み禁止）のため不変であること、
  // 新設のcalendarAskEl()がICS生成ロジックを重複実装せず既存renderIcs()の結果（#icsLink/#gcalLinkのhref）
  // を読むだけであること、リード文言/注記がtextContentのみ・リンクはhrefプロパティ代入のみ（安全設計維持）
  // であること、obPick()のanchor分岐がもうカレンダー案内を呼ばず素直にobAskQ()へ進むことを機械チェックで固定する。
  const obBubbleFn = extractFunction(main, "obBubble");
  assert("obBubble: found", obBubbleFn.length > 0, `${obBubbleFn.length} chars`);
  assert(
    "obBubble: still safe (textContent only, no innerHTML of variable text)",
    /\.textContent\s*=\s*text/.test(obBubbleFn) && !/innerHTML\s*=\s*text/.test(obBubbleFn),
    "existing textContent-only safety design preserved"
  );
  const calendarAskElFn = extractFunction(main, "calendarAskEl");
  assert("calendarAskEl: found", calendarAskElFn.length > 0, `${calendarAskElFn.length} chars`);
  assert(
    "calendarAskEl: reuses renderIcs (ICS生成ロジックの重複実装なし)",
    /renderIcs\s*\(\s*\)/.test(calendarAskElFn),
    "calls existing renderIcs() instead of rebuilding the ICS string"
  );
  assert(
    "calendarAskEl: does not duplicate ICS string generation",
    !/BEGIN:VCALENDAR/.test(calendarAskElFn),
    "no independent ICS payload built here"
  );
  assert(
    "calendarAskEl: copies href from existing settings-card links",
    /getElementById\(["']icsLink["']\)/.test(calendarAskElFn) && /getElementById\(["']gcalLink["']\)/.test(calendarAskElFn),
    "reads #icsLink/#gcalLink produced by renderIcs()"
  );
  assert(
    "calendarAskEl: lead text/note are textContent only (安全設計の維持)",
    /lead\.textContent\s*=\s*leadText/.test(calendarAskElFn) && /note\.textContent\s*=/.test(calendarAskElFn),
    "variable copy/lead text never goes through innerHTML"
  );
  assert(
    "calendarAskEl: link hrefs are property assignment only",
    /oi\.href\s*=\s*icsHref/.test(calendarAskElFn) && /og\.href\s*=\s*gcalHref/.test(calendarAskElFn),
    "href copied via property assignment, not re-embedded into innerHTML"
  );
  assert(
    "calendarAskEl: returns a DOM element (呼び出し側でリード文言だけ差し替え可能)",
    /function\s+calendarAskEl\s*\(\s*leadText\s*\)/.test(main) && /return\s+wrap\s*;/.test(calendarAskElFn),
    "caller controls placement; only leadText is parameterized"
  );
  const obPickFn = extractFunction(main, "obPick");
  assert("obPick: found", obPickFn.length > 0, `${obPickFn.length} chars`);
  assert(
    "obPick: anchor answer no longer wires a calendar bubble inline (移設済み・#calAsk側に一本化)",
    !/calendarAskEl|obCalendarBubble/.test(obPickFn),
    "anchor branch no longer calls any calendar-card renderer directly"
  );
  assert(
    "obPick: anchor answer advances straight to obAskQ (Q3直後は相槌のみ)",
    /obSay\(\[ONBOARDING_SCRIPT\.anchorAck\[c\.v\]\|\|["'][^"']*["']\]\s*,\s*obAskQ\s*\)/.test(obPickFn),
    "anchorAck bubble now calls obAskQ directly as the completion callback"
  );

  // 2026-07-17実機バグ修正: renderIcs()のDTSTART/datesがハードコードされた過去日("20260701")固定に
  // なっており、Googleカレンダー側で単発予定として扱われてしまう不具合が実機で確認された。
  // todayStr()（app-record.js・3時間オフセットJSTの「今日」）を必ず参照するよう修正し、再発防止として
  // ハードコード日付リテラルが復活していないことを機械チェックで固定する。
  const renderIcsFn = extractFunction(main, "renderIcs");
  assert("renderIcs: found", renderIcsFn.length > 0, `${renderIcsFn.length} chars`);
  assert(
    "renderIcs: DTSTART/dates derive from todayStr() (今日の日付を動的に使う)",
    /const\s+icsDate\s*=\s*todayStr\(\)\.replace\(\/-\/g,["']["']\)/.test(renderIcsFn)
      && /["']DTSTART:["']\s*\+\s*icsDate/.test(renderIcsFn)
      && /["']&dates=["']\s*\+\s*icsDate/.test(renderIcsFn),
    "ICS/Googleカレンダーリンクとも todayStr() 由来の icsDate を使う"
  );
  assert(
    "renderIcs: no hardcoded past date literal (過去日ハードコードの再発防止)",
    !/20260701/.test(renderIcsFn),
    "no fixed calendar date string baked into the function"
  );
  assert(
    "renderIcs: RRULE:FREQ=DAILY preserved (毎日の繰り返し設定)",
    /RRULE:FREQ=DAILY/.test(renderIcsFn) && /recur=RRULE:FREQ=DAILY/.test(renderIcsFn),
    "both ICS body and Google Calendar link keep the daily recurrence rule"
  );

  // 2026-07-17実装: 相談室シートのiOSソフトキーボード対応(visualViewport)。実測で踏んだ2つの回帰を機械チェックで固定する。
  // ①#soudanSheetにheightを直接pxで代入すると、子要素.sd-sheetのheight:92%解決が崩れて内容量ぶん縦に伸びる
  //   (Chromeでgetボundinglientrectがgetcomputedstyleと食い違う実行時バグ)ため、height自体は触らずtop/bottomの
  //   間接指定でブラウザに計算させる方式にした。
  // ②本アプリの「もじの大きさ：大きめ」機能(body.bigtext{zoom:1.12})が有効だと、JSで代入したpx値がzoom倍に
  //   さらに拡大されてしまう(zoomは要素のローカル座標系を拡大するため)。visualViewportの値は割り戻す必要がある。
  // また、sdVvOn/sdVvOffがopenSoudan/closeSoudanと連動して確実にon/offされること
  // (つけっぱなしでイベントリスナーが増え続けたり、閉じたシートに古いinline styleが残ったりするのを防止)も確認する。
  const sdVvHandlerFn = extractFunction(main, "sdVvHandler");
  assert("sdVvHandler: found", sdVvHandlerFn.length > 0, `${sdVvHandlerFn.length} chars`);
  assert(
    "sdVvHandler: leaves height for the browser to derive (regression: explicit px height breaks .sd-sheet's 92% resolution)",
    /style\.height\s*=\s*""/.test(sdVvHandlerFn) && !/style\.height\s*=\s*[^;]*vv\.height/.test(sdVvHandlerFn),
    "height is reset to \"\" instead of being assigned a computed pixel value"
  );
  assert(
    "sdVvHandler: compensates for body zoom (bigtext) before writing px values",
    /getComputedStyle\(document\.body\)\.zoom/.test(sdVvHandlerFn),
    "raw visualViewport px values are divided by the effective zoom factor"
  );
  const openSoudanFn = extractFunction(main, "openSoudan");
  assert("openSoudan: calls sdVvOn()", /sdVvOn\s*\(\s*\)/.test(openSoudanFn), "visualViewport監視をシートオープン時に開始");
  const closeSoudanFn = extractFunction(main, "closeSoudan");
  assert("closeSoudan: calls sdVvOff()", /sdVvOff\s*\(\s*\)/.test(closeSoudanFn), "visualViewport監視をシートクローズ時に停止");

  // 2026-07-17実装: 背面スクロールロック(position:fixed+top:-scrollY方式)。
  // body.sd-lock{overflow:hidden}(CSS)だけではiOS15以前のSafariで背面が指でスクロールできてしまう
  // (「スクロール抜け」既知バグ)ため、lockBodyScroll/unlockBodyScrollの共通ヘルパーで
  // scrollYの記憶→position:fixed適用→復元を行う。sd-lockを使う全モーダルがこの2関数経由であることを固定する
  // (個別にclassList.add/remove("sd-lock")する実装が新設されると、スクロール抜け対策が漏れたモーダルが
  // サイレントに増えてしまうため)。
  const lockBodyScrollFn = extractFunction(main, "lockBodyScroll");
  assert("lockBodyScroll: found", lockBodyScrollFn.length > 0, `${lockBodyScrollFn.length} chars`);
  assert(
    "lockBodyScroll: saves window.scrollY before locking",
    /scrollY/.test(lockBodyScrollFn),
    "開く前のスクロール位置を記憶している"
  );
  assert(
    "lockBodyScroll: applies position:fixed + top:-scrollY + width:100%",
    /style\.position\s*=\s*["']fixed["']/.test(lockBodyScrollFn) &&
      /style\.top\s*=\s*\(?-\s*\w+/.test(lockBodyScrollFn) &&
      /style\.width\s*=\s*["']100%["']/.test(lockBodyScrollFn),
    "inline styleでposition:fixed/top(負値)/width:100%を適用"
  );
  const unlockBodyScrollFn = extractFunction(main, "unlockBodyScroll");
  assert("unlockBodyScroll: found", unlockBodyScrollFn.length > 0, `${unlockBodyScrollFn.length} chars`);
  assert(
    "unlockBodyScroll: clears the inline position/top/width styles",
    /style\.position\s*=\s*""/.test(unlockBodyScrollFn) &&
      /style\.top\s*=\s*""/.test(unlockBodyScrollFn) &&
      /style\.width\s*=\s*""/.test(unlockBodyScrollFn),
    "position:fixedを解除して通常レイアウトに戻す"
  );
  assert(
    "unlockBodyScroll: restores window.scrollTo to the saved position",
    /window\.scrollTo\s*\(/.test(unlockBodyScrollFn),
    "記憶しておいたscrollYへ復元"
  );
  assert(
    "openSoudan: locks via lockBodyScroll() (not a raw sd-lock classList.add)",
    /lockBodyScroll\s*\(\s*\)/.test(openSoudanFn) && !/classList\.add\(["']sd-lock["']\)/.test(openSoudanFn),
    "共通ヘルパー経由でロック"
  );
  assert(
    "closeSoudan: unlocks via unlockBodyScroll() (not a raw sd-lock classList.remove)",
    /unlockBodyScroll\s*\(\s*\)/.test(closeSoudanFn) && !/classList\.remove\(["']sd-lock["']\)/.test(closeSoudanFn),
    "共通ヘルパー経由でアンロック"
  );
  const openDexFn = extractFunction(main, "openDex");
  assert("openDex: found", openDexFn.length > 0, `${openDexFn.length} chars`);
  assert(
    "openDex: locks via lockBodyScroll() (not a raw sd-lock classList.add)",
    /lockBodyScroll\s*\(\s*\)/.test(openDexFn) && !/classList\.add\(["']sd-lock["']\)/.test(openDexFn),
    "共通ヘルパー経由でロック"
  );
  const closeDexFn = extractFunction(main, "closeDex");
  assert("closeDex: found", closeDexFn.length > 0, `${closeDexFn.length} chars`);
  assert(
    "closeDex: unlocks via unlockBodyScroll() (not a raw sd-lock classList.remove)",
    /unlockBodyScroll\s*\(\s*\)/.test(closeDexFn) && !/classList\.remove\(["']sd-lock["']\)/.test(closeDexFn),
    "共通ヘルパー経由でアンロック"
  );
  // sd-lockの付け外しがlockBodyScroll/unlockBodyScrollの定義以外から直接行われていないこと
  // (新しいモーダルがこの2関数を経由せず直接classList.add/remove("sd-lock")するとスクロール抜け対策が漏れる)
  const rawLockSites = (main.match(/classList\.(?:add|remove)\(["']sd-lock["']\)/g) || []).length;
  assert(
    "sd-lock classList.add/remove(\"sd-lock\") appears only inside lockBodyScroll/unlockBodyScroll (2 sites)",
    rawLockSites === 2,
    `raw occurrences=${rawLockSites}`
  );

  const assetRefs = new Set();
  for (const m of html.matchAll(/\b(?:src|href)=["'](assets\/[^"']+)["']/g)) {
    assetRefs.add(m[1].split("#")[0].split("?")[0]);
  }
  const missing = Array.from(assetRefs).filter((rel) => !exists(rel));
  assert("index.html: local asset refs exist", missing.length === 0, missing.join(", ") || `${assetRefs.size} assets`);

  return srcScripts;
}

// WCAG相対輝度・コントラスト比計算（実測監査 2026-07-17: --teal(#2BB3A3)背景+白文字が
// 約2.6:1しかなくAA基準4.5:1を大きく下回っていた問題への対応）。
function relLuminance(hex) {
  const n = hex.replace("#", "");
  const r = parseInt(n.slice(0, 2), 16) / 255;
  const g = parseInt(n.slice(2, 4), 16) / 255;
  const b = parseInt(n.slice(4, 6), 16) / 255;
  const lin = (c) => (c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4));
  return 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b);
}
function contrastRatio(hexA, hexB) {
  const l1 = relLuminance(hexA);
  const l2 = relLuminance(hexB);
  const [lighter, darker] = l1 >= l2 ? [l1, l2] : [l2, l1];
  return (lighter + 0.05) / (darker + 0.05);
}

// 2026-07-17実測監査で発見: --teal(#2BB3A3)を背景に白文字(#fff)を乗せている箇所
// （「きょうやった！」ボタン・選択中タグチップ・とどくメーター選択ボタン・カレンダー実施日セル等）が
// WCAG AA基準(4.5:1)を大きく下回る約2.6:1しかコントラストが無かった。白文字が乗る背景専用の
// --teal-strong(#1E7B70)を新設し、装飾用途(ボーダー・アイコン・グラデーション等)の--tealはそのまま
// 維持しつつ、白文字の背景としてのみ--teal-strongに置き換えたことを機械チェックで固定する。
function checkContrast(html) {
  const rootMatch = /:root\s*\{([^}]*)\}/.exec(html);
  assert(":root variables found", !!rootMatch, "for --teal/--teal-strong extraction");
  const rootBody = rootMatch ? rootMatch[1] : "";
  const tealMatch = /--teal:\s*(#[0-9A-Fa-f]{6})/.exec(rootBody);
  const strongMatch = /--teal-strong:\s*(#[0-9A-Fa-f]{6})/.exec(rootBody);
  assert("--teal defined in :root", !!tealMatch, tealMatch && tealMatch[1]);
  assert("--teal-strong defined in :root", !!strongMatch, strongMatch && strongMatch[1]);

  const teal = tealMatch ? tealMatch[1] : "#2BB3A3";
  const tealStrong = strongMatch ? strongMatch[1] : null;
  assert("--teal-strong value is #1E7B70 (実測計算済みの値)", tealStrong === "#1E7B70", tealStrong);

  if (tealStrong) {
    const white = "#FFFFFF";
    const lightBg = "#FFFAF3";
    const darkBg = "#211E19";
    const ratioWhite = contrastRatio(tealStrong, white);
    const ratioLightBg = contrastRatio(tealStrong, lightBg);
    const ratioDarkBg = contrastRatio(tealStrong, darkBg);
    const ratioOldWhite = contrastRatio(teal, white);
    assert(
      "--teal-strong vs white text: AA (>=4.5:1)",
      ratioWhite >= 4.5,
      `${ratioWhite.toFixed(2)}:1`
    );
    assert(
      "--teal-strong vs light bg #FFFAF3: >=3:1 (UI非文字パーツ基準)",
      ratioLightBg >= 3,
      `${ratioLightBg.toFixed(2)}:1`
    );
    assert(
      "--teal-strong vs dark bg #211E19: >=3:1 (UI非文字パーツ基準)",
      ratioDarkBg >= 3,
      `${ratioDarkBg.toFixed(2)}:1`
    );
    assert(
      "--teal (旧・装飾用) vs white text stays below AA (置き換えの動機が再発しないことの記録)",
      ratioOldWhite < 4.5,
      `${ratioOldWhite.toFixed(2)}:1`
    );
  }

  // 白文字(color:#fff)が乗る背景として--tealが再度使われていないことを固定する
  // （装飾用途のvar(--teal)自体は維持してよいが、background:var(--teal); ... color:#fffの
  // 組み合わせだけは禁止パターンとして機械チェックする）。
  const whiteOnPlainTealPattern = /background:var\(--teal\)[^}]*color:#fff|color:#fff[^}]*background:var\(--teal\)(?!-strong)/;
  assert(
    "no background:var(--teal) (旧・低コントラスト値) paired with color:#fff remains",
    !whiteOnPlainTealPattern.test(html.replace(/var\(--teal-strong\)/g, "var(--TEALSTRONG)")),
    "white text must sit on --teal-strong, not the decorative --teal"
  );

  const requiredStrongRules = [
    ".done-btn",
    ".cal .d.done",
    ".daytoggle",
    ".chip-b.on",
    "body.dark .chip-b.on",
    ".reach-btn.on",
  ];
  for (const selector of requiredStrongRules) {
    const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    const re = new RegExp(`${escaped}\\{[^}]*background:var\\(--teal-strong\\)`);
    assert(`${selector} uses --teal-strong as background`, re.test(html), selector);
  }
}

// 2026-07-17仕様判断待ち項目の対応: オンボQ2「いちばん気になるのは？」で答えた悩みが保存されず、
// 直後のかたさチェックQ5「いちばんの悩みは？」で同じ質問を繰り返していた問題（本人YES=Q5スキップ承認済み）。
// index.html側のobGo()がOB_WORRY_TO_QUIZ変換表でオンボ語彙→クイズ語彙(WORRYキー)に変換してstartQuiz()へ渡すこと、
// "none"（とくにない）はスキップ対象外のままであること、app-quiz.js側がpresetWorry未指定時は従来どおり5問
// (worry込み)を維持すること（回帰防止が最優先）を機械チェックで固定する。
function checkOnboardingWorrySkip(mainScript, quizScript) {
  const mapMatch = /const\s+OB_WORRY_TO_QUIZ\s*=\s*\{([^}]*)\}/.exec(mainScript);
  assert("index.html: OB_WORRY_TO_QUIZ mapping found", !!mapMatch, "オンボ悩み語彙→クイズ悩み語彙の対応表");
  const mapBody = mapMatch ? mapMatch[1] : "";
  assert(
    "OB_WORRY_TO_QUIZ: covers all real onboarding worry choices (katakori/youtsuu/zenkutsu/nemuri)",
    ["katakori", "youtsuu", "zenkutsu", "nemuri"].every((k) => new RegExp(`\\b${k}\\s*:`).test(mapBody)),
    mapBody.trim()
  );
  assert(
    'OB_WORRY_TO_QUIZ: does NOT map "none" (とくにない＝実質的な悩みではないためQ5スキップ対象外)',
    !/\bnone\s*:/.test(mapBody),
    "none was intentionally excluded"
  );
  const obGoFn = extractFunction(mainScript, "obGo");
  assert("obGo: found", obGoFn.length > 0, `${obGoFn.length} chars`);
  assert(
    'obGo: quiz route excludes worry==="none" before mapping (実質的な悩みが無ければQ5は通常どおり出題)',
    /worry\s*&&\s*worry\s*!==\s*["']none["']/.test(obGoFn),
    "none guard present"
  );
  assert(
    "obGo: quiz route passes mapped presetWorry into startQuiz(...) (Q2の回答をQ5に引き継ぐ)",
    /startQuiz\s*\(\s*presetWorry\s*\)/.test(obGoFn),
    "startQuiz receives the mapped worry value, not called bare"
  );

  const startQuizFn = extractFunction(quizScript, "startQuiz");
  assert("app-quiz.js: startQuiz found", startQuizFn.length > 0, `${startQuizFn.length} chars`);
  assert(
    "startQuiz: accepts presetWorry parameter",
    /function\s+startQuiz\s*\(\s*presetWorry\s*\)/.test(quizScript),
    "signature carries the optional preset"
  );
  assert(
    "startQuiz: presetWorry falls back to null (単独起動/未指定時はQ5を出す既定へ)",
    /state\.worry\s*=\s*presetWorry\s*\|\|\s*null/.test(startQuizFn),
    "no preset -> state.worry stays null, same as before"
  );

  const activeQuestionsFn = extractFunction(quizScript, "activeQuestions");
  assert("app-quiz.js: activeQuestions found", activeQuestionsFn.length > 0, `${activeQuestionsFn.length} chars`);
  assert(
    'activeQuestions: only filters out Q5(worry) when state.presetWorry is set, otherwise keeps all 5 (回帰防止)',
    /state\.presetWorry\s*\?\s*QUESTIONS\.filter\(\s*q\s*=>\s*q\.k\s*!==\s*["']worry["']\s*\)\s*:\s*QUESTIONS/.test(
      activeQuestionsFn
    ),
    "default (no preset) path returns the untouched QUESTIONS array"
  );

  const answerFn = extractFunction(quizScript, "answer");
  assert("app-quiz.js: answer found", answerFn.length > 0, `${answerFn.length} chars`);
  assert(
    "answer: uses activeQuestions().length to decide when to finish (Q5スキップ時にashi直後で結果画面へ)",
    /state\.qi\s*>=\s*activeQuestions\(\)\.length/.test(answerFn),
    "handles both the 4-question preset path and the normal 5-question path"
  );
}

// はじめの1本ガイド（2026-07-17・Fable設計・PO承認済み）: オンボ完走→通算0日の初回ユーザーだけに、
// かたさチェック結果画面/ホーム「あなた用」を①1本だけに絞った能動ガイドを重ねる。新しい仕組みは作らず
// 既存のpendingNudge/markDone後UIフローに一言ずつ重ねる方式のため、フラグ配線・各画面の分岐条件・
// 復帰ナッジの拡張・カレンダー案内の移設先(#calAsk)を機械チェックで固定する。
function checkFirstDayGuide(html, mainScript, quizScript, recordScript) {
  const fdActiveFn = extractFunction(mainScript, "fdActive");
  assert("fdActive: found", fdActiveFn.length > 0, `${fdActiveFn.length} chars`);
  assert(
    'fdActive: gates on kyono_fd==="go" AND total===0 (完了/未開始では発動しない)',
    /store\.get\(\s*["']fd["']\s*,\s*null\s*\)\s*===\s*["']go["']/.test(fdActiveFn) && /getStreakData\(\)\.total\s*===\s*0/.test(fdActiveFn),
    "both conditions required"
  );

  const obGoFn = extractFunction(mainScript, "obGo");
  assert(
    "obGo: sets kyono_fd=\"go\" only for quiz/today routes on a fresh (total===0) user, before obClose(true)",
    /\(r===["']quiz["']\|\|r===["']today["']\)\s*&&\s*getStreakData\(\)\.total===0\s*&&\s*!store\.get\(["']fd["']\)\)\s*store\.set\(["']fd["'],\s*["']go["']\)[\s\S]{0,20}obClose\(true\)/.test(obGoFn),
    "soudan route is excluded (not in the r===quiz||r===today condition); does not double-set once fd exists"
  );
  assert(
    "obGo: does not start the guide for the soudan route",
    !/r===["']soudan["'][\s\S]{0,40}store\.set\(["']fd["']/.test(obGoFn),
    "soudan explicitly out of scope per design"
  );

  // かたさチェック結果画面(app-quiz.js showResult): guideをfdActive()経由(typeofガード)で判定し、
  // ①だけをfd-heroで主役化・rxHeadとバッジ文言を差し替え・3本連続再生ボタンとrotate-noteを隠し・
  // rTourBtn/obTourAfterQuizを強制falseにすること。guide=false時は既存ロジックのまま(else節)であることを固定する。
  const showResultFn = extractFunction(quizScript, "showResult");
  assert("showResult: found", showResultFn.length > 0, `${showResultFn.length} chars`);
  assert(
    "showResult: reads guide via typeof-guarded fdActive() (読み込み順ガード)",
    /const\s+guide\s*=\s*\(typeof\s+fdActive\s*===\s*["']function["']\)\s*&&\s*fdActive\(\)/.test(showResultFn),
    "cross-file forward reference guarded as required by design"
  );
  assert(
    "showResult: guide forces obTourAfterQuiz=false and hides rTourBtn",
    /if\s*\(\s*guide\s*\)\s*obTourAfterQuiz\s*=\s*false/.test(showResultFn) && /tb\.classList\.toggle\(["']hidden["'],\s*guide\s*\|\|/.test(showResultFn),
    "guide users never see the tour continuation button"
  );
  assert(
    "showResult: rxHead text differs in guide (まずはこの1本から) vs normal (fixed?)",
    /まずはこの1本から！②③はあしたからでOKだよ/.test(showResultFn) && /おすすめの3本: まずは/.test(showResultFn),
    "both branches present"
  );
  assert(
    "showResult: guide branch wraps rx[0] in .fd-hero with the hero badge label",
    /class="fd-hero"[\s\S]{0,40}videoCard\(rx\[0\],\s*["']きょうはこれ1本でOK！["']\)/.test(showResultFn),
    "hero card uses the ① video and the replacement label"
  );
  assert(
    "showResult: guide branch still renders ②③ (rx.slice(1), not hidden/grayed)",
    /rx\.slice\(1\)\.map/.test(showResultFn),
    "②③ remain visible per design (do not hide/gray them out)"
  );
  assert(
    "showResult: guide branch omits the '3本つづけて再生する' continuous-playback button",
    (function () {
      const guideBlock = (/if\s*\(\s*guide\s*\)\s*\{([\s\S]*?)\}\s*else\s*\{/.exec(showResultFn) || [])[1] || "";
      return guideBlock.indexOf("3本つづけて再生する") === -1 && /videoCard\(rx\[0\]/.test(guideBlock);
    })(),
    "button only exists in the non-guide (else) branch"
  );
  assert(
    "showResult: rotate-note (#rRotateNote) hidden only in guide",
    /getElementById\(["']rRotateNote["']\)[\s\S]{0,20}classList\.toggle\(["']hidden["'],\s*guide\)/.test(showResultFn),
    "3日ごと入れ替わりの注記もguide中は隠す"
  );
  assert(
    "showResult: clears any leftover #rDoneNudge content on (re)render",
    /getElementById\(["']rDoneNudge["'][\s\S]{0,80}classList\.add\(["']hidden["']\)/.test(showResultFn),
    "prevents a stale nudge banner surviving into a fresh result screen"
  );

  // ホーム「あなた用」(index.html renderToday): fdActive()中は①だけ・バッジ/注記付き・連続再生リンク非表示。
  const renderTodayFn = extractFunction(mainScript, "renderToday");
  assert("renderToday: found", renderTodayFn.length > 0, `${renderTodayFn.length} chars`);
  assert(
    "renderToday: guards fdActive() with typeof (読み込み順ガード)",
    /\(typeof\s+fdActive\s*===\s*["']function["']\)\s*&&\s*fdActive\(\)/.test(renderTodayFn),
    "same defensive pattern as showResult"
  );
  assert(
    "renderToday: guide branch shows only rx[0] with the hero badge + tomorrow note, no continuous-playback extra",
    /きょうはこれ1本でOK！[\s\S]{0,40}vHTML\(V\[rx\[0\]\],null\)[\s\S]{0,120}②と③はあしたからでだいじょうぶ😊/.test(renderTodayFn),
    "matches result-screen wording for consistency"
  );

  // markDone(app-record.js): guideは fdActive() ではなく生の store.get("fd")==="go" で判定（完了マーク前に評価する必要がある
  // ため。fdActive()はtotal===0が条件で、markDone内では既にtotalがインクリメント済みで使えない）。
  // 完了マークはstore.set("fd",1)。cheer文言はms(節目)優先→guide専用文言→通常文言の順。
  const markDoneFn = extractFunction(recordScript, "markDone");
  assert("markDone: found", markDoneFn.length > 0, `${markDoneFn.length} chars`);
  assert(
    'markDone: captures guide via raw store.get("fd")==="go" BEFORE marking complete (fdActive()はtotal===0前提のため使えない)',
    /const\s+guide\s*=\s*store\.get\(\s*["']fd["']\s*,\s*null\s*\)\s*===\s*["']go["']/.test(markDoneFn),
    "guide captured before store.set(\"fd\",1)"
  );
  assert(
    'markDone: marks the guide complete with store.set("fd",1) only when guide was active',
    /if\s*\(\s*guide\s*\)\s*\{\s*store\.set\(["']fd["'],\s*1\)/.test(markDoneFn),
    "fd flips go -> 1 on the first successful record"
  );
  assert(
    "markDone: sets #memoInput placeholder only in guide (no other forced focus/required behavior)",
    /mi\.placeholder\s*=\s*["']例: 肩がかるくなった気がする😊["']/.test(markDoneFn),
    "placeholder-only nudge, per design (no forced input)"
  );
  assert(
    "markDone: milestone(ms) cheer takes priority; guide-specific cheer only in the non-milestone else-if branch",
    /if\s*\(\s*ms\s*\)\s*\{[\s\S]*?\}\s*else\s+if\s*\(\s*guide\s*\)\s*\{/.test(markDoneFn),
    "guide text can never override a milestone celebration"
  );
  assert(
    "markDone: guide cheer uses the exact fixed copy (1日目クリア／メモ促しの2行のみ・シンプル化済み)",
    /🎉 1日目クリア！ナイスご自愛！/.test(markDoneFn)
      && /よかったら下に✍️きょうのひとことをどうぞ からだの感じをひとことでOK（あとからでもいいよ）/.test(markDoneFn),
    "wording matches the approved design verbatim"
  );
  assert(
    "markDone: guide cheer branch no longer embeds the tour button/使い方タブ案内 inline (moved to #tourAsk・2026-07-17 PO実機フィードバック)",
    (function () {
      const guideBlock = (/\}\s*else\s+if\s*\(\s*guide\s*\)\s*\{([\s\S]*?)\}\s*else\s*\{/.exec(markDoneFn) || [])[1] || "";
      return guideBlock.indexOf("cheerTourBtn") === -1 && guideBlock.indexOf("使い方タブ") === -1;
    })(),
    "cheer text itself stays to the 2-line copy; the tour ask lives in its own #tourAsk block below"
  );
  assert(
    "markDone: calendar card renders into #calAsk exactly once, gated on total===1 && !calseen, then sets calseen (2026-07-17改行入り文言)",
    /st\.total===1\s*&&\s*!store\.get\(["']calseen["']\)[\s\S]{0,300}calendarAskEl\(["']明日も同じ時間に会いましょう。\\nカレンダーに毎日の合図を入れておく？["']\)[\s\S]{0,80}store\.set\(["']calseen["'],\s*1\)/.test(markDoneFn),
    "matches the exact 2-line lead copy (\\n) from the approved design and the one-time guard"
  );
  assert(
    "markDone: tour-ask card renders into #tourAsk exactly when guide is true (使い方ツアー案内・カレンダー案内カードの直後)",
    /if\s*\(\s*guide\s*\)\s*\{[\s\S]{0,80}getElementById\(["']tourAsk["'][\s\S]{0,400}cheerTourBtn[\s\S]{0,200}obOpenTour\(\)[\s\S]{0,300}cheerTourSkipBtn/.test(markDoneFn),
    "renders the tour button + skip button into #tourAsk, styled like the calendar ask card"
  );
  assert(
    "markDone: #tourAsk skip button clears #tourAsk innerHTML (あとで＝カード自体を消す)",
    /cheerTourSkipBtn["'][\s\S]{0,150}getElementById\(\\?['"]tourAsk\\?['"]\)\.innerHTML\s*=\s*\\?['"]\\?['"]/.test(markDoneFn),
    "skip removes the whole card, not just the buttons"
  );

  const renderStreakFn = extractFunction(recordScript, "renderStreak");
  assert("renderStreak: found", renderStreakFn.length > 0, `${renderStreakFn.length} chars`);
  assert(
    "renderStreak: clears #calAsk and #tourAsk when today is not yet recorded (再訪日に前回の残骸を残さない)",
    /!did\)\{[\s\S]{0,300}getElementById\(["']calAsk["'][\s\S]{0,200}getElementById\(["']tourAsk["']/.test(renderStreakFn),
    "stale calendar/tour-ask card content is wiped alongside the existing cheer-clearing logic"
  );

  // checkDoneNudge(index.html): 既存の早期return群は全て維持。#resultが表示中なら#rDoneNudgeへ、
  // ホーム表示中なら従来どおりcheer文言へ（片方だけが実行される=早期returnで分岐）。
  const checkDoneNudgeFn = extractFunction(mainScript, "checkDoneNudge");
  assert("checkDoneNudge: found", checkDoneNudgeFn.length > 0, `${checkDoneNudgeFn.length} chars`);
  assert(
    "checkDoneNudge: all pre-existing early returns preserved (pendingNudge/day-boundary/already-done guards)",
    /if\s*\(!d\)\s*return;/.test(checkDoneNudgeFn)
      && /if\s*\(d\s*!==\s*todayStr\(\)\)\s*return;/.test(checkDoneNudgeFn)
      && /if\s*\(getStreakData\(\)\.dates\.includes\(todayStr\(\)\)\)\s*return;/.test(checkDoneNudgeFn),
    "regression guard: none of the original bail-out conditions were altered"
  );
  assert(
    "checkDoneNudge: when #result is visible, populates #rDoneNudge and returns before touching home's #cheer",
    /!resultEl\.classList\.contains\(["']hidden["']\)\)\{[\s\S]{0,600}rDoneNudge[\s\S]{0,600}return;\s*\}/.test(checkDoneNudgeFn),
    "result-screen branch is mutually exclusive with the home cheer branch"
  );
  assert(
    "checkDoneNudge: rDoneNudge button routes home, scrolls #doneBtn into view, and reuses nudge-pulse",
    /rDoneNudgeBtn[\s\S]{0,300}goHome\(\)[\s\S]{0,200}nudge-pulse[\s\S]{0,100}scrollIntoView\(\{block:["']center["']\}\)/.test(checkDoneNudgeFn),
    "reuses the existing 'make the button impossible to miss' pattern"
  );
  assert(
    "checkDoneNudge: home branch (cheer text + nudge-pulse) is untouched (回帰防止)",
    /おかえりなさい！おわったら下の「きょうやった！」を押してね✅/.test(checkDoneNudgeFn),
    "existing home behavior preserved verbatim"
  );

  // #todayVideoタップ検知IIFEは変更せず、#result専用の同型IIFEを追加しただけであることを確認する
  const pendingNudgeIifeCount = (mainScript.match(/kyono_pendingNudgeVideo/g) || []).length;
  assert(
    "index.html: #todayVideo tap-detection IIFE is untouched (byte-identical listener body reused for #result)",
    /getElementById\(["']todayVideo["']\)[\s\S]{0,60}addEventListener\(["']click["']/.test(mainScript),
    "original listener still attaches to #todayVideo"
  );
  assert(
    "index.html: a second tap-detection IIFE attaches the same logic to #result (rxList/worryExtraの動画リンクも対象)",
    /getElementById\(["']result["']\)[\s\S]{0,60}addEventListener\(["']click["'][\s\S]{0,400}kyono_pendingNudgeVideo/.test(mainScript),
    "new listener added without touching the #todayVideo one"
  );
  assert(
    "index.html: pendingNudgeVideo write logic appears exactly twice (once per listener, no third copy)",
    pendingNudgeIifeCount === 2,
    `found ${pendingNudgeIifeCount} occurrences (expected 2: #todayVideo + #result)`
  );

  // DOM順序（2026-07-17 PO実機フィードバック）: 「きょうやった！」後の導線は
  // cheer→#memoRow→「記録カードを画像でのこす」ボタン(#makeCardBtn)/#cardHint→#calAsk→#tourAsk、の順。
  // #calAskをカード保存ボタンより後ろに、#tourAskをさらにその後ろに置く(以前は#memoRowの直後に#calAskだけがあった)。
  assert(
    "index.html: #streakCard order is #memoRow → #makeCardBtn → #cardHint → #calAsk → #tourAsk (PO要望の並び替え・2026-07-17)",
    /id="memoRow"[\s\S]{0,900}id="makeCardBtn"[\s\S]{0,700}id="cardHint"[\s\S]{0,100}id="calAsk"[\s\S]{0,100}id="tourAsk"/.test(html),
    "記録カードを画像でのこす→カレンダー案内→使い方ツアー案内、の順で並んでいること"
  );
  assert(
    "index.html: #calAskLead has white-space:pre-line so a literal \\n in leadText renders as a line break (textContentのみの安全設計は維持)",
    /style="[^"]*white-space:pre-line[^"]*"\s+id="calAskLead"/.test(html),
    "calendarAskEl()はtextContent代入のみ・改行はCSS側で表現する設計"
  );
  assert(
    "index.html: #rDoneNudge exists inside #result, ahead of #rTourBtn",
    /id="result"[\s\S]{0,4000}id="rDoneNudge"[\s\S]{0,400}id="rTourBtn"/.test(html),
    "matches design placement"
  );
  assert(
    "index.html: .fd-hero CSS rule provides a visible pink border on the hero video card",
    /\.fd-hero \.video\{[^}]*border:2\.5px solid var\(--pink\)/.test(html),
    "hero card is visually distinguished per design"
  );
}

// 使い方ツアー(OB_TOUR_SLIDES・2026-07-17 PO実機フィードバックでブラッシュアップ)が、
// 「じまんカード・せんぱいの声・ひとことにっき」が「お楽しみ機能」ページに統合され、
// 「とどくメーター」が独自ページ(#reach)として独立した実際の構成と食い違っていないことを固定する。
// 旧文言は「とどくメーター・せんぱいの声・ひとことにっき」を一括で「このタブ」と案内しており、
// とどくメーターとお楽しみ機能が別々の導線(見てみるボタン)であることが読み取れなかった。
function checkTourSlides(mainScript, html) {
  const slidesMatch = /const OB_TOUR_SLIDES=\[([\s\S]*?)\n\];/.exec(mainScript);
  const slidesSrc = slidesMatch ? slidesMatch[1] : "";
  assert("OB_TOUR_SLIDES: found", slidesSrc.length > 0, `${slidesSrc.length} chars`);
  assert(
    "OB_TOUR_SLIDES: 7枚構成を維持(📺→✅→カード→💬→📣→📅→📖)",
    (slidesSrc.match(/\{t:/g) || []).length === 7,
    "枚数が変わっていないこと(内容のブラッシュアップのみが目的)"
  );
  assert(
    "OB_TOUR_SLIDES: 「マイ記録」スライドがとどくメーターとお楽しみ機能を別の導線として案内している(旧: 一括で「このタブ」と誤解を招く表現だった)",
    /📏とどくメーター<\/b>と<b>🎉お楽しみ機能<\/b>（じまんカード・せんぱいの声・ひとことにっき）/.test(slidesSrc),
    "実際のSECTIONS構成(reach/funが別セクション)に合わせた文言であること"
  );
  assert(
    "OB_TOUR_SLIDES: 「せんぱいの声・ひとことにっきもこのタブ」という旧・不正確な一文が復活していない",
    !/とどくメーター📏・せんぱいの声・ひとことにっき📝もこのタブ/.test(slidesSrc),
    "とどくメーターとお楽しみ機能を混同させる旧文言が再発していないこと"
  );
  // ツアーは実UIから独立した手描きモックのため、機能名/ボタン名を変更してもツアー側の文言だけ
  // 取り残されがち（過去に実際発生）。<b>で強調されたUI参照が、ツアー配列の外(=実際の画面)にも
  // 存在することを機械チェックし、リネーム時の置き去りを検知する（本人指摘「ツアーのモック画面
  // ドリフト対策」2026-07-18）。絵文字プレフィックスと「」の装飾は表記ゆれとして正規化して比較する。
  if (html && slidesMatch) {
    const arrayStart = html.indexOf("const OB_TOUR_SLIDES=[");
    const arrayEnd = html.indexOf("\n];", arrayStart) + 3;
    const rest = arrayStart >= 0 ? html.slice(0, arrayStart) + html.slice(arrayEnd) : html;
    const bolds = [...slidesSrc.matchAll(/<b>(.*?)<\/b>/g)].map((m) => m[1]);
    const coreOf = (s) => {
      const quoted = /「(.+?)」/.exec(s);
      const inner = quoted ? quoted[1] : s;
      return inner.replace(/^[^\wぁ-んァ-ヶ一-龠]+/u, "");
    };
    for (const b of new Set(bolds)) {
      const core = coreOf(b);
      assert(
        `OB_TOUR_SLIDES: UI参照「${core}」が実際の画面にも存在する`,
        core.length > 0 && rest.includes(core),
        `tour text <b>${b}</b> → core "${core}"`
      );
    }
  }
}

// ホーム画面に追加ポップアップ(#a2hsModal・初回起動ではじめてガイドの直前に出す)の静的配線チェック。
// 端末/ブラウザごとの分岐そのものの実挙動は scripts/smoke.js の7c(実機UA差し替え・7シナリオ)が担保する。
// ここでは「配線が壊れていないか」（beforeinstallpromptの早期登録・関数/DOM要素の存在・
// はじめてガイドの直前に必ず割り込む配線）だけを機械的に固定し、リファクタ時の巻き戻り事故を防ぐ。
function checkA2hsPopup(html, mainScript) {
  const bipIdx = html.indexOf('addEventListener("beforeinstallprompt"');
  const bodyIdx = html.indexOf("<body>");
  assert("beforeinstallprompt: listener registered", bipIdx !== -1, "window.addEventListener(\"beforeinstallprompt\",...) not found");
  assert(
    "beforeinstallprompt: registered early in <head> (できるだけ早い段階で保持するため)",
    bipIdx !== -1 && bodyIdx !== -1 && bipIdx < bodyIdx,
    "registration must appear before <body> so it can catch the event before user interaction"
  );
  assert(
    "beforeinstallprompt: preventDefault()して保持する(自動バナーを止めてこちらのタイミングでprompt()する)",
    /window\.addEventListener\(\s*"beforeinstallprompt"[\s\S]{0,200}?e\.preventDefault\(\)[\s\S]{0,200}?window\.__a2hsEvent\s*=\s*e/.test(html),
    "e.preventDefault() then window.__a2hsEvent = e expected"
  );

  const ids = ["a2hsModal", "a2hsBody", "a2hsBtns"];
  const missingIds = ids.filter((id) => !new RegExp(`id=["']${id}["']`).test(html));
  assert("index.html: #a2hsModal markup exists", missingIds.length === 0, missingIds.join(", ") || ids.join(", "));

  const fns = ["a2hsBoot", "a2hsShow", "a2hsClose", "a2hsIsStandalone"];
  const missingFns = fns.filter((name) => extractFunction(mainScript, name).length === 0);
  assert("a2hsBoot/a2hsShow/a2hsClose/a2hsIsStandalone: found", missingFns.length === 0, missingFns.join(", ") || fns.join(", "));

  const bootFn = extractFunction(mainScript, "a2hsBoot");
  assert(
    "a2hsBoot: standalone起動中は即座に次へ進む(ポップアップ不要)",
    /a2hsIsStandalone\(\)\)\{\s*cont\(\)/.test(bootFn),
    "standalone short-circuit missing"
  );
  assert(
    "a2hsBoot: デスクトップ(iPhone/iPad/iPod/Android以外)も即座に次へ進む",
    /!\/iPhone\|iPad\|iPod\|Android\/\.test\(ua\)\)\{\s*cont\(\)/.test(bootFn),
    "desktop short-circuit missing"
  );
  assert(
    "a2hsBoot: iOSはSafari本体とそれ以外(CriOS/FxiOS/EdgiOS/OPiOS)を区別する",
    /CriOS\|FxiOS\|EdgiOS\|OPiOS/.test(bootFn) && /iPhone\|iPad\|iPod/.test(bootFn),
    "iOS Safari vs other-browser branch missing"
  );
  assert(
    "a2hsBoot: Androidはwindow.__a2hsEventの有無でprompt分岐する",
    /window\.__a2hsEvent\)\s*a2hsShow\(\s*["']android-prompt["']/.test(bootFn) && /a2hsShow\(\s*["']android-menu["']/.test(bootFn),
    "android-prompt/android-menu branch missing"
  );

  assert(
    "はじめてガイド起動: 初回起動フローがobOpen()の前に必ずa2hsBoot()を通す(ポップアップをスキップして直接開始しない)",
    /setTimeout\(function\(\)\{\s*a2hsBoot\(obOpen\);\s*\}/.test(mainScript),
    "boot IIFE must call a2hsBoot(obOpen), not obOpen directly"
  );

  const closeFn = extractFunction(mainScript, "a2hsClose");
  assert(
    "a2hsClose: 閉じ方によらず必ずcont()(=obOpen)へ進む(進行不能防止の必須要件)",
    /if\s*\(\s*cont\s*\)\s*cont\(\)/.test(closeFn),
    "must always call the continuation, regardless of how it was closed"
  );
  assert(
    "a2hsModal: popstate(戻る操作)でも閉じられる",
    /popstate[\s\S]{0,300}?a2hsModal[\s\S]{0,200}?a2hsClose\(\)/.test(mainScript),
    "back-button close wiring missing"
  );
}

function checkSearchScript(code) {
  parseJs("app-search.js", code);
  let tags = [];
  try {
    tags = extractConstArray(code, "TAGS");
    pass("app-search.js: TAGS parsed", `${tags.length} tags`);
  } catch (err) {
    fail("app-search.js: TAGS parsed", err.message);
  }
  return tags;
}

function checkOperationalWiring(html, scripts) {
  const ids = extractIds(html);
  const definedFunctions = extractFunctionNames(scripts);
  const handlers = extractHandlers(html);
  const missingFns = [];
  for (const handler of handlers) {
    for (const name of calledGlobals(handler.code)) {
      if (!definedFunctions.has(name)) missingFns.push(`${handler.attr}:${name}`);
    }
  }
  assert("operation: inline handlers resolved", missingFns.length === 0, missingFns.slice(0, 12).join(", ") || `${handlers.length} handlers`);

  let sections = [];
  try {
    sections = extractConstArray(scripts, "SECTIONS");
    pass("operation: SECTIONS parsed", sections.join(", "));
  } catch (err) {
    fail("operation: SECTIONS parsed", err.message);
  }
  const missingSections = sections.filter((id) => !ids.has(id));
  assert("operation: routed sections exist", missingSections.length === 0, missingSections.join(", ") || `${sections.length} sections`);

  const publicTabs = ["home", "history", "playlists", "guide", "search"];
  const missingTabs = publicTabs.filter((id) => !ids.has(`tab-${id}`) || !ids.has(id));
  assert("operation: bottom tabs wired", missingTabs.length === 0, missingTabs.join(", ") || publicTabs.join(", "));

  const switchTargets = Array.from(html.matchAll(/switchTab\(["']([^"']+)["']\)/g)).map((m) => m[1]);
  const badSwitchTargets = switchTargets.filter((id) => !publicTabs.includes(id));
  assert("operation: switchTab targets valid", badSwitchTargets.length === 0, badSwitchTargets.join(", ") || `${switchTargets.length} calls`);

  const cardIds = ["doneBtn", "memoInput", "memoBtn", "cardModal", "cardMaking", "cardImg", "cardCanvas"];
  const missingCardIds = cardIds.filter((id) => !ids.has(id));
  assert("operation: record card controls exist", missingCardIds.length === 0, missingCardIds.join(", ") || cardIds.join(", "));

  const quizIds = ["qnum", "qtitle", "qArt", "qnote", "opts", "dots", "qBackBtn", "rName", "rxList"];
  const missingQuizIds = quizIds.filter((id) => !ids.has(id));
  assert("operation: quiz/result controls exist", missingQuizIds.length === 0, missingQuizIds.join(", ") || quizIds.join(", "));

  const searchIds = ["q", "ySel", "catRow", "chips", "hitCount", "filterNow", "vlist", "moreBtn", "reqBtn"];
  const missingSearchIds = searchIds.filter((id) => !ids.has(id));
  assert("operation: search controls exist", missingSearchIds.length === 0, missingSearchIds.join(", ") || searchIds.join(", "));

  const handlersByIntent = [
    ["done button", /id=["']doneBtn["'][\s\S]*?onclick=["']markDone\(\)["']/],
    ["memo save button", /id=["']memoBtn["'][\s\S]*?onclick=["']saveMemo\(\)["']/],
    ["card close button", /onclick=["']closeCard\(\)["']/],
    ["search more button", /id=["']moreBtn["'][\s\S]*?onclick=["']moreResults\(\)["']/],
    ["quiz start button", /onclick=["']startQuiz\(\)["']/],
  ];
  for (const item of handlersByIntent) {
    assert(`operation: ${item[0]} handler`, item[1].test(html), "primary action is clickable");
  }
}

function checkCatalog(code, allowedTags) {
  parseJs("videos.js", code);
  let catalog = [];
  try {
    catalog = extractJsonAssignment(code, "CATALOG");
    pass("videos.js: CATALOG parsed", `${catalog.length} videos`);
  } catch (err) {
    fail("videos.js: CATALOG parsed", err.message);
    return null;
  }
  assert("videos.js: catalog size", catalog.length >= 450, `${catalog.length} videos`);
  const ids = new Set();
  const badIds = [];
  const dupes = [];
  const emptyTags = [];
  const unknownTags = [];
  for (const item of catalog) {
    if (!item || !/^[\w-]{11}$/.test(item.id || "")) badIds.push(item && item.id);
    if (ids.has(item.id)) dupes.push(item.id);
    ids.add(item.id);
    if (!Array.isArray(item.tags) || !item.tags.length) emptyTags.push(item.id);
    for (const tag of item.tags || []) {
      if (!allowedTags.includes(tag)) unknownTags.push(`${item.id}:${tag}`);
    }
  }
  assert("videos.js: video ids valid", badIds.length === 0, badIds.slice(0, 10).join(", ") || "all ids match 11-char YouTube shape");
  assert("videos.js: video ids unique", dupes.length === 0, dupes.slice(0, 10).join(", ") || "no duplicates");
  assert("videos.js: tags present", emptyTags.length === 0, emptyTags.slice(0, 10).join(", ") || "all rows tagged");
  assert("videos.js: tags allowed", unknownTags.length === 0, unknownTags.slice(0, 10).join(", ") || "all tags are in index.html TAGS");

  if (exists("scripts/private_videos.json")) {
    const privateIds = new Set(JSON.parse(read("scripts/private_videos.json")).ids || []);
    const leaked = catalog.map((v) => v.id).filter((id) => privateIds.has(id));
    assert("videos.js: private videos excluded", leaked.length === 0, leaked.slice(0, 10).join(", ") || "none");
  }
  return ids;
}

// 2026-07-17実装(AUDIT-MEMO低優先項目対応): オガトレ通信(OBU_FEED)は運用ゼロ設計の中で唯一の
// 人力更新箇所＝投稿が止まりやすい。「未読/既読」だけでNEW📣を判定すると、初見の新規ユーザーには
// 投稿がどれだけ古くても永遠にNEWが出続ける(=更新停止が逆に目立つ見せ方になる)ため、obuHasNew()に
// 鮮度しきい値(OBU_STALE_DAYS)を追加した。regexでの存在確認だけでなく、実際にロジックを実行して
// 「未読でも古い投稿はNEWにならない／新しい投稿は従来どおりNEWになる」を固定する。
function checkObuStaleness(mainScript, html) {
  const daysBetweenBody = extractFunction(mainScript, "daysBetween");
  const obuIsLaterOrEqualBody = extractFunction(mainScript, "obuIsLaterOrEqual");
  const obuLatestBody = extractFunction(mainScript, "obuLatest");
  const obuIsStaleDateBody = extractFunction(mainScript, "obuIsStaleDate");
  const obuHasNewBody = extractFunction(mainScript, "obuHasNew");
  const staleMatch = /const\s+OBU_STALE_DAYS\s*=\s*(\d+)/.exec(mainScript);

  assert("obuHasNew: found", obuHasNewBody.length > 0, `${obuHasNewBody.length} chars`);
  assert("obuIsStaleDate: found", obuIsStaleDateBody.length > 0, `${obuIsStaleDateBody.length} chars`);
  assert(
    "obuHasNew: gates on post staleness, not just read/unread (obu_seenだけの判定に戻さない)",
    /obuIsStaleDate\s*\(\s*latest\.date\s*\)/.test(obuHasNewBody),
    "obuHasNew still calls obuIsStaleDate before falling back to obu_seen comparison"
  );
  assert("OBU_STALE_DAYS: defined as a plain number literal", !!staleMatch, "threshold constant not found or not a literal");

  if (daysBetweenBody && obuIsLaterOrEqualBody && obuLatestBody && obuIsStaleDateBody && obuHasNewBody && staleMatch) {
    const runObuHasNew = (latestDate, todayDate, seen) => {
      const code = `
        const OBU_STALE_DAYS=${staleMatch[1]};
        function daysBetween(a,b){${daysBetweenBody}}
        function obuIsLaterOrEqual(a,b){${obuIsLaterOrEqualBody}}
        function obuLatest(){${obuLatestBody}}
        function obuIsStaleDate(ds){${obuIsStaleDateBody}}
        function obuHasNew(){${obuHasNewBody}}
        obuHasNew();
      `;
      const sandbox = {
        OBU_FEED: [{ id: "post-1", date: latestDate, type: "text" }],
        store: { get: (k, d) => (k === "obu_seen" ? seen : d) },
        todayStr: () => todayDate,
      };
      return vm.runInNewContext(code, sandbox, { timeout: 2000 });
    };

    assert(
      "obuHasNew: 60日前・未読の投稿はNEW扱いにしない(息切れが目立つ見せ方を回避)",
      runObuHasNew("2026-05-01", "2026-07-17", null) === false,
      "expected false"
    );
    assert(
      "obuHasNew: 数日前・未読の投稿は従来どおりNEW扱い(回帰防止)",
      runObuHasNew("2026-07-15", "2026-07-17", null) === true,
      "expected true"
    );
    assert(
      "obuHasNew: しきい値ちょうど(OBU_STALE_DAYS日前)は古い扱い(境界値)",
      runObuHasNew("2026-06-17", "2026-07-17", null) === false,
      "expected false at the exact threshold boundary"
    );
    assert(
      "obuHasNew: 新しい投稿でも既読ならNEWでない(既読判定は維持)",
      runObuHasNew("2026-07-15", "2026-07-17", "post-1") === false,
      "expected false"
    );
  }

  const obuPostInnerBody = extractFunction(mainScript, "obuPostInner");
  assert("obuPostInner: found", obuPostInnerBody.length > 0, `${obuPostInnerBody.length} chars`);
  assert(
    "obuPostInner: applies obu-date-old class for stale posts (日付を控えめな体裁にする)",
    /obuIsStaleDate\s*\(\s*item\.date\s*\)/.test(obuPostInnerBody) && /obu-date-old/.test(obuPostInnerBody),
    "date label styling still depends on obuIsStaleDate"
  );
  assert(
    "index.html: .obu-date-old CSS rule exists (督促ではなく控えめな見た目のみ)",
    /\.obu-date\.obu-date-old\{/.test(html || ""),
    "css rule present"
  );
}

function checkObuFeed(code) {
  parseJs("obu-feed.js", code);
  let feed = [];
  try {
    feed = extractConstArray(code, "OBU_FEED");
    pass("obu-feed.js: OBU_FEED parsed", `${feed.length} posts`);
  } catch (err) {
    fail("obu-feed.js: OBU_FEED parsed", err.message);
    return;
  }

  const validTypes = ["text", "photo", "radio"];
  const ids = new Set();
  const dupes = [];
  const missingRequired = [];
  const badType = [];
  const missingImage = [];
  const missingAudioOrTitle = [];
  const missingText = [];
  const missingAssetFiles = [];
  let textPhotoCount = 0;

  for (const item of feed) {
    const label = (item && item.id) || JSON.stringify(item);
    if (!item || !item.id || !item.date || !item.type) {
      missingRequired.push(label);
      continue;
    }
    if (ids.has(item.id)) dupes.push(item.id);
    ids.add(item.id);

    if (!validTypes.includes(item.type)) {
      badType.push(`${item.id}:${item.type}`);
      continue;
    }

    if (item.type === "photo") {
      if (!item.image) missingImage.push(item.id);
      else if (!exists(item.image)) missingAssetFiles.push(`${item.id}:${item.image}`);
      textPhotoCount++;
      if (!item.text) missingText.push(item.id);
    }
    if (item.type === "radio") {
      if (!item.audio) missingAudioOrTitle.push(`${item.id}:audio missing`);
      else if (!exists(item.audio)) missingAssetFiles.push(`${item.id}:${item.audio}`);
      if (!item.title) missingAudioOrTitle.push(`${item.id}:title missing`);
    }
    if (item.type === "text") {
      textPhotoCount++;
      if (!item.text) missingText.push(item.id);
    }
  }

  assert("obu-feed.js: required fields present (id/date/type)", missingRequired.length === 0, missingRequired.slice(0, 10).join(", ") || `${feed.length} posts have id/date/type`);
  assert("obu-feed.js: ids unique", dupes.length === 0, dupes.slice(0, 10).join(", ") || "no duplicate ids");
  assert("obu-feed.js: type is text/photo/radio", badType.length === 0, badType.slice(0, 10).join(", ") || "all types valid");
  assert("obu-feed.js: photo posts have image", missingImage.length === 0, missingImage.slice(0, 10).join(", ") || "ok");
  assert("obu-feed.js: radio posts have audio+title", missingAudioOrTitle.length === 0, missingAudioOrTitle.slice(0, 10).join(", ") || "ok");
  assert("obu-feed.js: image/audio asset files exist", missingAssetFiles.length === 0, missingAssetFiles.slice(0, 10).join(", ") || "ok");
  pass(
    "obu-feed.js: text/photo posts have text (recommended)",
    missingText.length === 0
      ? `all ${textPhotoCount} text/photo posts have text`
      : `${missingText.length}/${textPhotoCount} missing text (not required): ${missingText.slice(0, 10).join(", ")}`
  );
}

function checkSw(code) {
  parseJs("sw.js", code);
  let assets = [];
  try {
    const match = code.match(/const ASSETS=(\[[^\n]+?\]);/);
    if (!match) throw new Error("ASSETS assignment not found");
    assets = JSON.parse(match[1]);
    pass("sw.js: ASSETS parsed", `${assets.length} assets`);
  } catch (err) {
    fail("sw.js: ASSETS parsed", err.message);
    return;
  }
  const missing = assets
    .filter((rel) => rel !== "./")
    .map((rel) => rel.replace(/^\.\//, "").split("?")[0])
    .filter((rel) => !exists(rel))
    // soudan-kb.jsはdev64が別途作成するファイル。未着の間だけ欠落を許す（着後は通常どおり検証される）
    .filter((rel) => rel !== "soudan-kb.js");
  assert("sw.js: cached local assets exist", missing.length === 0, missing.join(", ") || `${assets.length} cache entries`);
  assert("sw.js: soudan-kb.js in ASSETS", assets.includes("soudan-kb.js"), "obu-feed.jsと同扱い");
  assert("sw.js: soudan-kb.js in SHELL", /const SHELL=\[[^\n]*"soudan-kb\.js"/.test(code), "app shellに含まれる");
  assert("sw.js: soudan-kb.js network-first", /endsWith\("soudan-kb\.js"\)/.test(code), "no-cache再確認の対象");
  assert("sw.js: cache version named", /const C="kyono-v\d+"/.test(code), "kyono-vN");
  assert("sw.js: network-first for app shell", /cache:"no-cache"/.test(code), "no-cache request path present");
}

// オガトレ相談室の知識ベース（soudan-kb.js・dev64担当）の機械検証。
// 件数には縛りを掛けない（dev64が成長させる）。ファイル未着の間はスキップ扱い。
function checkSoudanKb(catalogIds) {
  if (!exists("soudan-kb.js")) {
    pass("soudan-kb.js: pending", "dev64作成待ち（ファイルが置かれ次第このQAが内容を検証する）");
    return;
  }
  const code = read("soudan-kb.js");
  parseJs("soudan-kb.js", code);
  let kb = null;
  try {
    kb = vm.runInNewContext(`${code}\n;SOUDAN_KB`, {}, { timeout: 2000 });
    if (!kb || typeof kb !== "object") throw new Error("SOUDAN_KB is not an object");
    pass("soudan-kb.js: SOUDAN_KB parsed", `v=${kb.v} / intents=${(kb.intents || []).length}件`);
  } catch (err) {
    fail("soudan-kb.js: SOUDAN_KB parsed", err.message);
    return;
  }

  assert("soudan-kb.js: intents array", Array.isArray(kb.intents) && kb.intents.length > 0, `${(kb.intents || []).length}件`);

  const badIntents = [];
  const badIds = [];
  const badVideos = [];
  const dupKwSameIntent = [];
  const dupKwNormSameIntent = [];
  const crossDupKw = [];
  const badFollowupRefs = [];
  const intentIds = new Set();
  const dupIntentIds = [];
  const followupIds = new Set((kb.commonFollowups || []).map((f) => f && f.id).filter(Boolean));
  const allIntentIds = new Set((kb.intents || []).map((it) => it && it.id).filter(Boolean));
  const kwOwner = new Map();

  // index.html sdNorm / soudan-ai-poc/norm.mjs の簡易移植。
  // 同一インテント内で「カタカナ/ひらがな」「大文字/小文字」だけが違うkwを検出するために使う
  // （エンジンのスコアリングは正規化後に一致文字数を合算するため、この種の重複は
  //  見た目は別語でも実質は同じ語の二重計上=近似インテントとの僅差誤答の温床になる）。
  function sdNormLite(s) {
    s = String(s == null ? "" : s).toLowerCase();
    s = s.replace(/[Ａ-Ｚａ-ｚ０-９]/g, (c) => String.fromCharCode(c.charCodeAt(0) - 0xfee0));
    s = s.replace(/[ァ-ヶ]/g, (c) => String.fromCharCode(c.charCodeAt(0) - 0x60));
    return s.replace(/[^0-9a-zぁ-ゖー一-鿿々]/g, "");
  }

  for (const it of kb.intents || []) {
    const label = (it && it.id) || JSON.stringify(it);
    // videosは0〜3本（0本=動画を勧めないデリケート枠、例: やったら痛くなった）
    if (!it || !it.id || !it.chip || !Array.isArray(it.kw) || !it.kw.length ||
        !it.empathy || !it.mitate || !Array.isArray(it.videos) || !it.keizoku) {
      badIntents.push(label);
      continue;
    }
    if (!/^[\w-]+$/.test(it.id)) badIds.push(it.id);
    if (intentIds.has(it.id)) dupIntentIds.push(it.id);
    intentIds.add(it.id);
    if (it.videos.length > 3) badVideos.push(`${it.id}: videos ${it.videos.length}本(>3)`);
    for (const v of it.videos) {
      if (!v || !/^[\w-]{11}$/.test(v.v || "")) badVideos.push(`${it.id}: ${(v && v.v) || "(空)"}(ID形式)`);
      else if (catalogIds && !catalogIds.has(v.v)) badVideos.push(`${it.id}: ${v.v}(カタログに無い)`);
    }
    const seen = new Set();
    const seenNorm = new Map();
    for (const k of it.kw) {
      if (seen.has(k)) dupKwSameIntent.push(`${it.id}: ${k}`);
      seen.add(k);
      const nk = sdNormLite(k);
      if (nk) {
        if (seenNorm.has(nk) && seenNorm.get(nk) !== k) dupKwNormSameIntent.push(`${it.id}: ${seenNorm.get(nk)}/${k}`);
        else seenNorm.set(nk, k);
      }
      if (kwOwner.has(k) && kwOwner.get(k) !== it.id) crossDupKw.push(`${k}(${kwOwner.get(k)}/${it.id})`);
      else kwOwner.set(k, it.id);
    }
    for (const fid of it.followups || []) {
      // followupsは共通followup id か 関連インテントid（相互リンク）のどちらかを指す
      if (!followupIds.has(fid) && !allIntentIds.has(fid)) badFollowupRefs.push(`${it.id}: ${fid}`);
    }
  }
  assert("soudan-kb.js: intent必須フィールド (id/chip/kw/empathy/mitate/videos/keizoku)", badIntents.length === 0, badIntents.slice(0, 10).join(", ") || `${(kb.intents || []).length}件すべて充足`);
  assert("soudan-kb.js: intent idが英数スラッグ", badIds.length === 0, badIds.slice(0, 10).join(", ") || "ok");
  assert("soudan-kb.js: intent id重複なし", dupIntentIds.length === 0, dupIntentIds.slice(0, 10).join(", ") || "ok");
  assert("soudan-kb.js: 動画ID実在(1〜3本・CATALOG照合)", badVideos.length === 0, badVideos.slice(0, 10).join(", ") || "ok");
  assert("soudan-kb.js: kw重複なし(同一インテント内)", dupKwSameIntent.length === 0, dupKwSameIntent.slice(0, 10).join(", ") || "ok");
  assert("soudan-kb.js: kw正規化後重複なし(同一インテント内・カナ/大小文字違いの二重計上防止)", dupKwNormSameIntent.length === 0, dupKwNormSameIntent.slice(0, 10).join(", ") || "ok");
  // インテント間の同一kwは順序で解決されるため警告どまり（設計§4-4: 先頭寄り優先）
  pass("soudan-kb.js: kw重複(インテント間・警告のみ)", crossDupKw.length ? `警告${crossDupKw.length}件: ${crossDupKw.slice(0, 8).join(", ")}` : "なし");
  assert("soudan-kb.js: followups参照が解決できる", badFollowupRefs.length === 0, badFollowupRefs.slice(0, 10).join(", ") || "ok");

  // 文字数規律（empathy15-30/mitate60-120/keizoku30-60字・±2字は軽微な逸脱として許容）。
  // youtsuu.mitateはM1コア(本人校正済み・本文不変の原則)で安全上重要な受診案内を含むため
  // 例外的に据え置き中（詳細: SOUDAN-QUALITY-AUDIT-2026-07-14.md）。新規の逸脱はここで検出する。
  // yubikansetsu/touhi/kaidan.mitate（2026-07-17追加）も同じ理由（受診の線引きの一文を含む）で
  // 帯を超過するが、本人監修済みの文言を変更しない前例踏襲として例外に追加（NEW-INTENTS-PROPOSAL.md②③④）。
  const LEN_BANDS = { empathy: [15, 30], mitate: [60, 120], keizoku: [30, 60] };
  const LEN_TOLERANCE = 2;
  const LEN_EXCEPTIONS = new Set(["youtsuu:mitate", "yubikansetsu:mitate", "touhi:mitate", "kaidan:mitate"]);
  const lenOffenders = [];
  for (const it of kb.intents || []) {
    if (!it || !it.id) continue;
    for (const field of Object.keys(LEN_BANDS)) {
      if (typeof it[field] !== "string") continue;
      const len = Array.from(it[field]).length;
      const [lo, hi] = LEN_BANDS[field];
      if ((len < lo - LEN_TOLERANCE || len > hi + LEN_TOLERANCE) && !LEN_EXCEPTIONS.has(`${it.id}:${field}`)) {
        lenOffenders.push(`${it.id}.${field}=${len}字(目安${lo}-${hi})`);
      }
    }
  }
  assert("soudan-kb.js: 文字数規律 (empathy15-30/mitate60-120/keizoku30-60字・±2字許容)", lenOffenders.length === 0, lenOffenders.slice(0, 10).join(", ") || `${(kb.intents || []).length}件すべて帯内(既知例外1件除く)`);

  const rf = kb.redFlags;
  assert("soudan-kb.js: redFlags(赤旗)存在", !!(rf && Array.isArray(rf.kw) && rf.kw.length && rf.answer), rf ? `kw ${(rf.kw || []).length}語` : "redFlagsが無い");

  const cr = kb.crisis;
  assert("soudan-kb.js: crisis(希死念慮)存在", !!(cr && Array.isArray(cr.kw) && cr.kw.length >= 8 && typeof cr.answer === "string" && cr.answer), cr ? `kw ${(cr.kw || []).length}語` : "crisisが無い");

  const badFu = (kb.commonFollowups || []).filter((f) =>
    !f || !f.id || !f.chip || !f.mode ||
    !["text", "shorter", "more"].includes(f.mode) ||
    (f.mode === "text" && !f.answer));
  assert("soudan-kb.js: commonFollowups妥当 (id/chip/mode、textはanswer必須)", badFu.length === 0, badFu.map((f) => (f && f.id) || "(壊れた要素)").slice(0, 10).join(", ") || `${(kb.commonFollowups || []).length}件`);

  const badSt = (kb.smalltalk || []).filter((st) => !st || !Array.isArray(st.kw) || !st.kw.length || !Array.isArray(st.replies) || !st.replies.length);
  assert("soudan-kb.js: smalltalk妥当 (kw/replies)", badSt.length === 0, badSt.length ? `${badSt.length}件が不備` : `${(kb.smalltalk || []).length}件`);
}

function checkManifest() {
  let manifest = null;
  try {
    manifest = JSON.parse(read("manifest.json"));
    pass("manifest.json: parsed", manifest.name || "ok");
  } catch (err) {
    fail("manifest.json: parsed", err.message);
    return;
  }
  assert("manifest.json: standalone", manifest.display === "standalone", manifest.display);
  const missing = (manifest.icons || []).map((icon) => icon.src).filter((src) => !exists(src));
  assert("manifest.json: icons exist", missing.length === 0, missing.join(", ") || `${(manifest.icons || []).length} icons`);
}

function checkPythonScripts() {
  const scripts = fs.readdirSync(path.join(ROOT, "scripts"))
    .filter((name) => name.endsWith(".py"))
    .map((name) => path.join("scripts", name));
  if (!scripts.length) {
    pass("python scripts: none", "");
    return;
  }
  const result = childProcess.spawnSync("python3", ["-m", "py_compile"].concat(scripts), {
    cwd: ROOT,
    encoding: "utf8",
    env: Object.assign({}, process.env, {
      PYTHONPYCACHEPREFIX: path.join(os.tmpdir(), "kyou-no-ogatore-pycache"),
    }),
  });
  assert("python scripts: syntax", result.status === 0, (result.stderr || result.stdout || `${scripts.length} files`).trim());
}

function main() {
  for (const rel of ["index.html", "videos.js", "app-search.js", "obu-feed.js", "app-quiz.js", "app-record.js", "app-card.js", "sw.js", "manifest.json"]) {
    assert(`${rel}: exists`, exists(rel), "required app file");
  }

  const shipped = ["index.html", "videos.js", "app-search.js", "obu-feed.js", "app-quiz.js", "app-record.js", "app-card.js", "sw.js"];
  if (exists("soudan-kb.js")) shipped.push("soudan-kb.js");
  checkNoForbiddenModernSyntax(shipped);

  const html = read("index.html");
  const inline = extractInlineScripts(html);
  const mainScript = inline[inline.length - 2] || "";
  const cardScript = read("app-card.js");
  checkHtml(html, cardScript);
  checkContrast(html);
  const searchScript = read("app-search.js");
  const allowedTags = checkSearchScript(searchScript);
  const quizScript = read("app-quiz.js");
  const recordScript = read("app-record.js");
  checkOperationalWiring(html, `${mainScript}\n${searchScript}\n${quizScript}\n${recordScript}\n${cardScript}`);
  checkOnboardingWorrySkip(mainScript, quizScript);
  checkFirstDayGuide(html, mainScript, quizScript, recordScript);
  checkTourSlides(mainScript);
  checkA2hsPopup(html, mainScript);
  const catalogIds = checkCatalog(read("videos.js"), allowedTags);
  checkSoudanKb(catalogIds);
  checkObuFeed(read("obu-feed.js"));
  checkObuStaleness(mainScript, html);
  checkSw(read("sw.js"));
  checkManifest();
  checkPythonScripts();

  if (failures.length) {
    console.error(`\nQA failed: ${failures.length} issue(s)`);
    for (const item of failures) {
      console.error(`- ${item.name}: ${item.detail || ""}`);
    }
    process.exit(1);
  }

  console.log(`QA passed: ${checks.length} checks`);
  for (const item of checks) {
    console.log(`- ${item.name}${item.detail ? `: ${item.detail}` : ""}`);
  }
}

main();
