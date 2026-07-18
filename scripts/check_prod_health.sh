#!/usr/bin/env bash
# 本番配信(https://kyou-no.ogatore.net/)の週次ヘルスチェック。
# デプロイ直後の確認は都度やっているが、証明書失効・CNAME事故・配信欠落・全公開事故の再発など
# 「静かに壊れる」系を継続監視できていなかった（2026-07-18 Fable設計）。
# 1つでも失敗すれば非ゼロ終了→GitHub Actionsの既定通知でオーナーへメールが飛ぶ、運用ゼロの安全網。
# 成功時は静か（ダッシュボード等は作らない）。
#
# 実行: bash scripts/check_prod_health.sh
#   リポジトリのチェックアウト済みディレクトリから実行すること（sw.jsの版数比較にリポジトリ本体を参照するため）。
set -u

BASE="https://kyou-no.ogatore.net"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FAIL=0

ok()   { echo "OK   $1"; }
bad()  { echo "FAIL $1"; FAIL=1; }

# curlでURLを取得し、"<http_code>\n<body>" の形で返す
fetch() {
  curl -sS -L --max-time 20 -w '\n%{http_code}' "$1" 2>/dev/null
}

body_of() { printf '%s' "$1" | sed '$d'; }
code_of() { printf '%s' "$1" | tail -n1; }

echo "=== 本番ヘルスチェック: $BASE ==="

# 1. index.html (ルート/) が200・HTMLとして妥当
resp="$(fetch "$BASE/")"
code="$(code_of "$resp")"; html="$(body_of "$resp")"
if [ "$code" = "200" ] && printf '%s' "$html" | grep -q "きょうのオガトレ"; then
  ok "index.html: 200・既知の文字列を含む"
else
  bad "index.html: code=$code、期待する文字列(きょうのオガトレ)が見つからない可能性"
fi

# 2. sw.js が200・kyono-v版数文字列を含む
resp="$(fetch "$BASE/sw.js")"
code="$(code_of "$resp")"; sw_body="$(body_of "$resp")"
remote_ver="$(printf '%s' "$sw_body" | grep -o 'kyono-v[0-9]\+' | head -n1)"
if [ "$code" = "200" ] && [ -n "$remote_ver" ]; then
  ok "sw.js: 200・版数($remote_ver)を検出"
else
  bad "sw.js: code=$code、版数文字列(kyono-v...)が見つからない"
fi

# 3. sw.jsの版数がリポジトリmainのsw.jsと一致（CDN焼き込み/デプロイ滞留の検知）
#    push直後のCDN過渡期の偽陽性対策として、不一致時は5分待って1回だけ再試行してから失敗にする
local_ver="$(grep -o 'kyono-v[0-9]\+' "$ROOT/sw.js" | head -n1)"
if [ -z "$local_ver" ]; then
  bad "sw.js版数比較: リポジトリ本体のsw.jsから版数を抽出できない"
elif [ "$remote_ver" = "$local_ver" ]; then
  ok "sw.js版数比較: 本番($remote_ver) == リポジトリ($local_ver)"
else
  echo "WARN sw.js版数不一致(本番=$remote_ver / リポジトリ=$local_ver)。CDN過渡期の可能性があるため5分待って再試行します..."
  sleep 300
  resp="$(fetch "$BASE/sw.js")"
  retry_ver="$(printf '%s' "$(body_of "$resp")" | grep -o 'kyono-v[0-9]\+' | head -n1)"
  if [ "$retry_ver" = "$local_ver" ]; then
    ok "sw.js版数比較: 再試行後に一致(本番=$retry_ver)"
  else
    bad "sw.js版数比較: 再試行後も不一致(本番=$retry_ver / リポジトリ=$local_ver)"
  fi
fi

# 4. manifest.json が200・JSONパース可能
resp="$(fetch "$BASE/manifest.json")"
code="$(code_of "$resp")"; manifest_body="$(body_of "$resp")"
if [ "$code" = "200" ] && printf '%s' "$manifest_body" | python3 -c "import json,sys; json.load(sys.stdin)" >/dev/null 2>&1; then
  ok "manifest.json: 200・JSONとしてパース可能"
else
  bad "manifest.json: code=$code、JSONパース失敗"
fi

# 5. index.htmlの<script src>を動的抽出し、全app系JSが本番で200
#    scripts/qa.jsのcheckDeployAllowlist()と同じ「固定リストに頼らず動的抽出する」流儀
scripts="$(printf '%s' "$html" | grep -o '<script src="[^"]*"' | sed -E 's/<script src="([^"]*)"/\1/')"
if [ -z "$scripts" ]; then
  bad "script src抽出: index.htmlから<script src>を1件も抽出できない"
else
  script_fail=0
  for s in $scripts; do
    case "$s" in
      http*) url="$s" ;;
      *) url="$BASE/$s" ;;
    esac
    scode="$(curl -sS -L --max-time 20 -o /dev/null -w '%{http_code}' "$url" 2>/dev/null)"
    if [ "$scode" != "200" ]; then
      bad "script: $s -> code=$scode"
      script_fail=1
    fi
  done
  [ "$script_fail" = "0" ] && ok "script src: 全て200 ($(printf '%s' "$scripts" | wc -l | tr -d ' ')件)"
fi

# 6. robots.txt が200・Disallowを含む（β中の検索除外の生存確認）
resp="$(fetch "$BASE/robots.txt")"
code="$(code_of "$resp")"; robots_body="$(body_of "$resp")"
if [ "$code" = "200" ] && printf '%s' "$robots_body" | grep -q "Disallow"; then
  ok "robots.txt: 200・Disallowを含む"
else
  bad "robots.txt: code=$code、Disallowが見つからない"
fi

# 7. 公開してはいけないファイルが404であること（過去に全公開事故があった箇所の再発監視・最重要）
for f in "/WORKING_NOTES.md" "/scripts/private_videos.json"; do
  code="$(curl -sS -L --max-time 20 -o /dev/null -w '%{http_code}' "$BASE$f" 2>/dev/null)"
  if [ "$code" = "404" ]; then
    ok "非公開ファイル $f: 404(想定通り非公開)"
  else
    bad "非公開ファイル $f: code=$code(404であるべき。公開事故の可能性)"
  fi
done

echo "=== 完了 ==="
if [ "$FAIL" = "1" ]; then
  echo "本番ヘルスチェックで1件以上失敗しました。"
  exit 1
fi
echo "全チェックPASS"
exit 0
