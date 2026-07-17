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

function checkHtml(html) {
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
  const drawCard = extractFunction(main, "drawCard");
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

  const ensureCardFonts = extractFunction(main, "ensureCardFonts");
  assert("ensureCardFonts: timeout guard", /Promise\.race/.test(ensureCardFonts) && /2200/.test(ensureCardFonts), "font load cannot hang forever");
  const importData = extractFunction(main, "importData");
  assert("importData: size limit", /300000/.test(importData), "import payload capped");
  assert("importData: key prefix guard", /startsWith\("kyono_"\)/.test(importData), "only kyono_ keys imported");
  assert("importData: count guard", /cnt\s*>\s*50/.test(importData), "key count capped");
  assert("importData: value size guard", /200000/.test(importData), "individual value capped");

  // 2026-07-16実測監査対応: 唯一の再来訪装置（カレンダー通知）をオンボQ3「いつやる派？」の直後に接続。
  // obBubble()は台本文言をtextContentで描画する安全設計（意図的にHTML埋め込み禁止）のため不変であること、
  // 新設のobCalendarBubble()がICS生成ロジックを重複実装せず既存renderIcs()の結果（#icsLink/#gcalLinkのhref）
  // を読むだけであること、obPick()のanchor分岐がobCalendarBubble()を経て自動でobAskQ()へ進むこと（スキップ
  // チップなしでタップ有無に関わらず継続する仕様）を機械チェックで固定する。
  const obBubbleFn = extractFunction(main, "obBubble");
  assert("obBubble: found", obBubbleFn.length > 0, `${obBubbleFn.length} chars`);
  assert(
    "obBubble: still safe (textContent only, no innerHTML of variable text)",
    /\.textContent\s*=\s*text/.test(obBubbleFn) && !/innerHTML\s*=\s*text/.test(obBubbleFn),
    "existing textContent-only safety design preserved"
  );
  const obCalendarBubbleFn = extractFunction(main, "obCalendarBubble");
  assert("obCalendarBubble: found", obCalendarBubbleFn.length > 0, `${obCalendarBubbleFn.length} chars`);
  assert(
    "obCalendarBubble: reuses renderIcs (ICS生成ロジックの重複実装なし)",
    /renderIcs\s*\(\s*\)/.test(obCalendarBubbleFn),
    "calls existing renderIcs() instead of rebuilding the ICS string"
  );
  assert(
    "obCalendarBubble: does not duplicate ICS string generation",
    !/BEGIN:VCALENDAR/.test(obCalendarBubbleFn),
    "no independent ICS payload built here"
  );
  assert(
    "obCalendarBubble: copies href from existing settings-card links",
    /getElementById\(["']icsLink["']\)/.test(obCalendarBubbleFn) && /getElementById\(["']gcalLink["']\)/.test(obCalendarBubbleFn),
    "reads #icsLink/#gcalLink produced by renderIcs()"
  );
  const obPickFn = extractFunction(main, "obPick");
  assert("obPick: found", obPickFn.length > 0, `${obPickFn.length} chars`);
  assert(
    "obPick: anchor answer wires obCalendarBubble before obAskQ (Q3直後にカレンダー案内を接続)",
    /obCalendarBubble\s*\(\s*\)\s*;\s*\}\s*catch[\s\S]{0,20}\}\s*obAskQ\s*\(\s*\)\s*;/.test(obPickFn),
    "calendar card renders then flow auto-advances to the next question/route, no skip chip needed"
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

  const assetRefs = new Set();
  for (const m of html.matchAll(/\b(?:src|href)=["'](assets\/[^"']+)["']/g)) {
    assetRefs.add(m[1].split("#")[0].split("?")[0]);
  }
  const missing = Array.from(assetRefs).filter((rel) => !exists(rel));
  assert("index.html: local asset refs exist", missing.length === 0, missing.join(", ") || `${assetRefs.size} assets`);

  return srcScripts;
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
  for (const rel of ["index.html", "videos.js", "app-search.js", "obu-feed.js", "app-quiz.js", "app-record.js", "sw.js", "manifest.json"]) {
    assert(`${rel}: exists`, exists(rel), "required app file");
  }

  const shipped = ["index.html", "videos.js", "app-search.js", "obu-feed.js", "app-quiz.js", "app-record.js", "sw.js"];
  if (exists("soudan-kb.js")) shipped.push("soudan-kb.js");
  checkNoForbiddenModernSyntax(shipped);

  const html = read("index.html");
  const inline = extractInlineScripts(html);
  const mainScript = inline[inline.length - 2] || "";
  checkHtml(html);
  const searchScript = read("app-search.js");
  const allowedTags = checkSearchScript(searchScript);
  const quizScript = read("app-quiz.js");
  const recordScript = read("app-record.js");
  checkOperationalWiring(html, `${mainScript}\n${searchScript}\n${quizScript}\n${recordScript}`);
  checkOnboardingWorrySkip(mainScript, quizScript);
  const catalogIds = checkCatalog(read("videos.js"), allowedTags);
  checkSoudanKb(catalogIds);
  checkObuFeed(read("obu-feed.js"));
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
