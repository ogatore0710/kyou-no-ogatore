#!/usr/bin/env bash
# F2b(TASK-C2-2026-07-29-inspection-upgrade.md フォローアップ):
# SearchViewUITests.testNoBlackBarAtBottomOfScreen(C1=タブバー下端の黒い帯の再発防止)は、
# 実測の結果システムのダークモードでないと再現しない(HANDOFF.md参照)。UIテストのプロセス
# 自体はシミュレータ内でサンドボックスされておりsimctlを呼べないため、host側(このスクリプト)
# から appearance を切り替えてから xcodebuild test を実行する。
#
# 「既定の xcodebuild test では効かない、C1の黒帯検査はこちらを使うこと」— HANDOFF.md参照。
#
# 使い方: scripts/run-darkmode-uitest.sh [DEVICE_UDID] [TEST_IDENTIFIER]
#   DEVICE_UDID省略時: 現在Bootedのシミュレータを自動選択(複数ある場合は先頭)
#   TEST_IDENTIFIER省略時: KyouNoOgatoreUITests/SearchViewUITests/testNoBlackBarAtBottomOfScreen
#
# 終了時に必ずappearanceをlightへ戻す(テストが失敗して早期リターンしても戻すため、
# 後始末はtrapで登録する。ここが今回いちばん大事な条件)。

set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_DIR="$ROOT/ios-native/KyouNoOgatore"

DEVICE_UDID="${1:-}"
TEST_ID="${2:-KyouNoOgatoreUITests/SearchViewUITests/testNoBlackBarAtBottomOfScreen}"

if [ -z "$DEVICE_UDID" ]; then
  DEVICE_UDID="$(xcrun simctl list devices | grep -m1 "Booted" | grep -oE '[0-9A-F-]{36}')"
fi

if [ -z "$DEVICE_UDID" ]; then
  echo "エラー: Bootedなシミュレータが見つからない。DEVICE_UDIDを明示的に渡してください。" >&2
  exit 1
fi

echo "対象デバイス: $DEVICE_UDID"

# 検収基準どおり: テストが落ちても・スクリプトが途中で止まっても、必ずlightへ戻す。
cleanup() {
  echo "後始末: シミュレータのappearanceをlightへ戻します"
  xcrun simctl ui "$DEVICE_UDID" appearance light
}
trap cleanup EXIT

echo "appearance を dark へ切り替え"
xcrun simctl ui "$DEVICE_UDID" appearance dark

echo "xcodebuild test を実行: $TEST_ID"
xcodebuild test \
  -project "$PROJECT_DIR/KyouNoOgatore.xcodeproj" \
  -scheme KyouNoOgatore \
  -destination "id=$DEVICE_UDID" \
  -only-testing:"$TEST_ID"
STATUS=$?

# cleanup()はEXIT trapで自動的に呼ばれる(ここでは何もしない・trapに任せる)。
exit $STATUS
