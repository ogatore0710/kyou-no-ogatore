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

  const ensureCardFonts = extractFunction(main, "ensureCardFonts");
  assert("ensureCardFonts: timeout guard", /Promise\.race/.test(ensureCardFonts) && /2200/.test(ensureCardFonts), "font load cannot hang forever");
  const importData = extractFunction(main, "importData");
  assert("importData: size limit", /300000/.test(importData), "import payload capped");
  assert("importData: key prefix guard", /startsWith\("kyono_"\)/.test(importData), "only kyono_ keys imported");
  assert("importData: count guard", /cnt\s*>\s*50/.test(importData), "key count capped");
  assert("importData: value size guard", /200000/.test(importData), "individual value capped");

  const assetRefs = new Set();
  for (const m of html.matchAll(/\b(?:src|href)=["'](assets\/[^"']+)["']/g)) {
    assetRefs.add(m[1].split("#")[0].split("?")[0]);
  }
  const missing = Array.from(assetRefs).filter((rel) => !exists(rel));
  assert("index.html: local asset refs exist", missing.length === 0, missing.join(", ") || `${assetRefs.size} assets`);

  const tagMatch = main.match(/const TAGS=(\[[^\n]+?\]);/);
  assert("index.html: TAGS list found", !!tagMatch, "search tag allowlist");
  return tagMatch ? JSON.parse(tagMatch[1]) : [];
}

function checkCatalog(code, allowedTags) {
  parseJs("videos.js", code);
  let catalog = [];
  try {
    catalog = extractJsonAssignment(code, "CATALOG");
    pass("videos.js: CATALOG parsed", `${catalog.length} videos`);
  } catch (err) {
    fail("videos.js: CATALOG parsed", err.message);
    return;
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
    .filter((rel) => !exists(rel));
  assert("sw.js: cached local assets exist", missing.length === 0, missing.join(", ") || `${assets.length} cache entries`);
  assert("sw.js: cache version named", /const C="kyono-v\d+"/.test(code), "kyono-vN");
  assert("sw.js: network-first for app shell", /cache:"no-cache"/.test(code), "no-cache request path present");
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
  for (const rel of ["index.html", "videos.js", "sw.js", "manifest.json"]) {
    assert(`${rel}: exists`, exists(rel), "required app file");
  }

  checkNoForbiddenModernSyntax(["index.html", "videos.js", "sw.js"]);

  const html = read("index.html");
  const allowedTags = checkHtml(html);
  checkCatalog(read("videos.js"), allowedTags);
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
