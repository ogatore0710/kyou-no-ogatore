# index.html 分割計画

作成日: 2026-07-07

## 方針
いきなり大分割しない。`npm test` と実ブラウザQAで守れている範囲から、1回1領域ずつ切る。

分割時も以下は維持する:
- ビルド不要
- 依存ゼロ
- 古いiOS向けにES2020構文禁止
- `oldBrowserNote` は最後のES5 scriptとして残す
- `drawCard()` の再現性を壊さない

## 推奨順

### 1. 検索・カタログ表示（完了）
ファイル:
- `app-search.js`

対象:
- `TAGS`
- `CAT`
- `TAG_COLOR`
- `TAG_CATS`
- `buildChips`
- `renderCats`
- `setCat`
- `toggleChipByName`
- `currentHits`
- `renderSearch`
- `moreResults`
- `drawResults`

理由:
- `videos.js` と責務が近い
- QAで検索DOMとカタログ整合を見ている
- 記録カードやlocalStorageへの影響が少ない

### 2. かたさチェック（完了・2026-07-14）
ファイル:
- `app-quiz.js`

対象:
- `QUESTIONS`
- `TYPE_ART`
- `TYPES`
- `WORRY`
- `QUIZ_ART`
- `startQuiz`
- `renderQ`
- `answer`
- `prevQ`
- `decideType`
- `finishQuiz`
- `currentRx`
- `showResult`

理由:
- 画面として独立している
- 実ブラウザQAで5問完走を確認できる

### 3. 記録・継続（完了・2026-07-17）
ファイル:
- `app-record.js`

対象:
- `store`
- `todayStr`
- `getStreakData`
- `renderStreak`
- `markDone`
- `saveMemo`
- `setReach`
- `renderReach`
- `renderDiary`
- `renderHistory`
- `renderCal`
- `showDay`

（`renderThanks`/`sendThanks`は当初案に含まれていたが、実装時点では既に「ありがとうボタン撤去」で
index.htmlから削除済みだったため対象外。実在する12個のみ移動）

理由:
- localStorageと日付境界が絡むため、検索・チェックより後に切る
- ここを切る前に実ブラウザQAの自動化を強くしたい

### 4. 記録カード（完了・2026-07-17）
ファイル:
- `app-card.js`

対象:
- `CARD_THEMES`
- `GOLD`
- `MS`
- `CHARA_FILES`
- `ensureCardFonts`
- `makeCard`
- `drawCard`
- `shareCard`
- `downloadCard`

理由:
- 一番壊れやすい
- 画像生成・フォント・iOS共有が絡む
- 最後に切るのが安全

### 5. PWA・環境案内（完了・2026-07-18）
ファイル:
- `app-env.js`

対象:
- `applyTheme`
- `refreshDay`
- `dismissHomeHint`
- `a2hsCont` / `a2hsIsStandalone` / `a2hsAddBtn` / `a2hsShow` / `a2hsClose` / `a2hsBoot`

（service worker登録・storage persist・in-app browser banner・home screen hintの発火IIFE自体は
起動順序に関わるトップレベルの実行文のため、他の分割と同じ方針で index.html に残した。移動したのは
それらが呼び出す純粋な関数定義のみ）

理由:
- 起動順序に関わるので、他の分割が安定してから触る

## 分割時のチェック
- `npm test`
- mobile幅の実ブラウザQA
- 検索: 初期451本、`肩こり` 44本
- チェック: 5問完走、おすすめ3本
- 記録: `きょうやった！` -> 通算+1
- カード: PNG data URL生成
- 公開URL: manifest / sw / icons 200

## やらないこと
- bundler導入
- npm依存追加
- TypeScript化
- 大規模な命名変更
- `index.html` とJS分割を同時に大きく進めること
