#!/usr/bin/env python3
"""DB全動画（videos.js経由ではなくvideosテーブル直読み）の公開状態をoEmbedで確認し、非公開/削除を private_videos.json に書き出す。
月次DB更新→ build_catalog.py の前に回すと「さがす」から非公開動画が消える。
実行: python3 scripts/check_public.py（約2分・APIキー不要）"""
import json, urllib.request, concurrent.futures, datetime, os, sqlite3
d=os.path.dirname(__file__)
DB=os.path.expanduser("~/Claude/ogatore-growth/data/ogatore.db")
con=sqlite3.connect(DB)
ids=sorted({r[0] for r in con.execute("SELECT video_id FROM videos").fetchall()})
def check(vid):
    try:
        urllib.request.urlopen(f"https://www.youtube.com/oembed?url=https%3A//www.youtube.com/watch%3Fv%3D{vid}&format=json",timeout=10)
        return vid,200
    except urllib.error.HTTPError as e: return vid,e.code
    except Exception: return vid,0
with concurrent.futures.ThreadPoolExecutor(8) as ex:
    res=dict(ex.map(check,ids))
bad=sorted([v for v,c in res.items() if c!=200 and c!=0])
json.dump({"checked":str(datetime.date.today()),"method":"oembed 403","ids":bad},
          open(os.path.join(d,"private_videos.json"),"w"),ensure_ascii=False,indent=1)
print(f"公開{sum(1 for c in res.values() if c==200)} / 非公開等{len(bad)} → private_videos.json 更新。build_catalog.py を再実行してください")
