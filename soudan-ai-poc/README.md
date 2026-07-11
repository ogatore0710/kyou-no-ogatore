# 相談室 AIレイヤー 試作（soudan-ai-poc）

オガトレ相談室に**本物のClaude**を薄くかぶせる試作。設計の全体像は 1つ上の [SOUDAN-AI-DESIGN.md](../SOUDAN-AI-DESIGN.md)。
**キーを1本差せば動く／キー無しでもモックで挙動確認できる。**

## ファイル
- `worker.mjs` … 中継役の本体。Cloudflare Worker としてそのまま置ける（`export default { fetch }`）。第0層赤旗ふるい／上限／Claude呼び出し／第2層検証／フォールバック。
- `grounding.mjs` … 候補動画の絞り込み＋システムプロンプト（人格・安全・動画限定・お手本）。
- `norm.mjs` … 正規化・赤旗判定・候補スコアリング（index.html から忠実移植）。
- `data.mjs` … KB＋カタログ（`build-data.mjs` で ../soudan-kb.js・../videos.js から再生成）。
- `serve-local.mjs` … 手元Nodeで :8790 起動するラッパ。
- `demo.html` … 挙動確認用の最小チャット。
- `selftest.mjs` … 赤旗・通常・動画検証・上限フォールバックの通しテスト。

## 動かす

### モック（キー不要・ロジック確認）
```
MOCK=1 node serve-local.mjs
# → http://localhost:8790/ をブラウザで開く
```

### 本物のClaude（Ryunosukeの端末で）
```
ANTHROPIC_API_KEY=sk-ant-... node serve-local.mjs
# モデルを変えるなら:  MODEL=claude-sonnet-5 ANTHROPIC_API_KEY=... node serve-local.mjs
```
> この作業環境ではキーに触れないため、本物のE2Eは未実行。ロジックはモックで通し済み（`node selftest.mjs`）。

### 通しテスト
```
node selftest.mjs
```

## 本番デプロイ（Cloudflare Worker の例）
1. `wrangler init` → `worker.mjs`（＋`grounding/norm/data`）を配置。
2. `wrangler secret put ANTHROPIC_API_KEY`。
3. 上限は環境変数 `DAILY_CAP`（既定30）・`MONTHLY_YEN_CAP`（既定3000）。本番の回数/コスト計上は**メモリではなく KV / Durable Object** に移す（`worker.mjs` の `mem` 部分。コメントあり）。
4. 本アプリ（index.html）側は、相談送信時にまずこの Worker を叩き、`source:"fallback"` かネットワーク失敗なら**既存の `sdScoreIntents`（パターン集）に自動で切替**。＝AIが落ちてもアプリは壊れない。

## 安全・コストの要点（詳細は設計書）
- 赤旗（激痛・しびれ・産後・診断名など51語）は**AIに渡さず**本人監修の受診案内を即返す。
- 出力は構造化JSONで受け、**動画IDが候補外/実在しなければ捨てる**（でっち上げ防止）。
- `needsReferral` が立てば受診案内を機械的に添える。
- 1日30ターン/端末・月コスト上限で**自動オフ**（青天井を構造的に防止）。
- モデル既定 = Haiku 4.5（安い・速い・この用途に十分）。`MODEL` で変更可。
