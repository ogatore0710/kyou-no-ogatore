#!/usr/bin/env python3
"""videos.js（実際に配信中のカタログ）に載っている動画IDが、oEmbedで引き続き公開されているか確認する。
ローカルDB(~/Claude/ogatore-growth/data/ogatore.db)には依存しない＝GitHub Actions上でも動く軽量版。
月次スケジュールで実行し、非公開/削除された動画が1本でもあれば非ゼロ終了してワークフローを失敗させる
（GitHubの既定通知でオーナーへメールが飛ぶ＝「動画を探す」に死んだリンクが残ったまま気づかない事故の防止）。
本人が気づいたら scripts/update_catalog.py（要ローカルDB）でカタログを作り直す、までが対応の流れ。
実行: python3 scripts/check_catalog_public.py（約1-2分・APIキー不要）"""
import re, sys, urllib.request, urllib.error, concurrent.futures, os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def load_catalog_ids():
    with open(os.path.join(ROOT, "videos.js"), encoding="utf-8") as f:
        content = f.read()
    return sorted(set(re.findall(r'"id":"([a-zA-Z0-9_-]{11})"', content)))


def check(vid):
    url = f"https://www.youtube.com/oembed?url=https%3A//www.youtube.com/watch%3Fv%3D{vid}&format=json"
    try:
        urllib.request.urlopen(url, timeout=10)
        return vid, 200
    except urllib.error.HTTPError as e:
        return vid, e.code
    except Exception:
        return vid, 0


def main():
    ids = load_catalog_ids()
    if not ids:
        print("videos.jsから動画IDを1件も抽出できませんでした（抽出パターンの確認が必要）")
        return 1
    with concurrent.futures.ThreadPoolExecutor(8) as ex:
        results = dict(ex.map(check, ids))
    bad = sorted(v for v, code in results.items() if code != 200)
    ok = len(ids) - len(bad)
    print(f"カタログ{len(ids)}本中 公開確認{ok}本 / 非公開・削除・確認失敗{len(bad)}本")
    if bad:
        for vid in bad:
            print(f"  - https://www.youtube.com/watch?v={vid} (status={results[vid]})")
        print("\n配信中のカタログに非公開/削除された動画が含まれています。")
        print("尾形さんへ: scripts/update_catalog.py（ローカルDB要）でカタログを更新してください。")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
