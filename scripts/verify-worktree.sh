#!/bin/bash
# 検証専用worktreeの作成/破棄（2026-07-27 追加）
#
# 事件: iOSシミュレータはタップ自動化ができないため、起動画面を強制的に相談室へ固定する
# 一時コードを入れて目視確認したところ、even-syncの10分ごとの自動コミットに巻き込まれて
# origin/mainへpushされた(コミット2bfd59e)。今回はネイティブ(未配布)で無害だったが、
# 同じ経路で本番配信のindex.htmlに乗れば実害になる(同日朝の衝突マーカー事故が実例)。
#
# 対策: 一時コードを入れる作業は、even-syncの監視外(~/Claude の外)に作った worktree で行う。
# こうすると仮コードが共有リポジトリに乗ることが原理的になくなる。
#
# 使い方:
#   scripts/verify-worktree.sh new     # 検証用worktreeを作って場所を表示
#   scripts/verify-worktree.sh clean   # 検証用worktreeを破棄(仮コードごと消える)
#
# 注意: このworktreeでの変更は「捨てる前提」。残したい修正があるなら、
#       仮コードを外した上で本体リポジトリで作り直すこと。

set -euo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WT="/private/tmp/kyouno-verify"      # even-syncの監視対象(~/Claude/*)の外に置くのが肝
BR="verify-scratch"

case "${1:-}" in
  new)
    if [ -d "$WT" ]; then
      echo "既にあります: $WT"
      echo "作り直すなら先に: $0 clean"
      exit 0
    fi
    git -C "$REPO" worktree add -f -B "$BR" "$WT" HEAD >/dev/null
    echo "✅ 検証用worktreeを作りました"
    echo "   $WT"
    echo "   ここで仮コードを入れて、ビルド/シミュレータ確認をしてください。"
    echo "   even-syncの監視外なので、共有リポジトリには絶対に乗りません。"
    echo "   終わったら: $0 clean"
    ;;
  clean)
    if [ ! -d "$WT" ]; then
      echo "ありません(掃除済み): $WT"
      exit 0
    fi
    git -C "$REPO" worktree remove --force "$WT"
    git -C "$REPO" branch -D "$BR" 2>/dev/null || true
    echo "✅ 検証用worktreeを破棄しました(仮コードごと消えています)"
    ;;
  *)
    echo "usage: $0 {new|clean}"
    exit 1
    ;;
esac
