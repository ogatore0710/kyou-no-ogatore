#!/usr/bin/env python3
"""ogatore.db から「さがす」タブ用の全動画カタログ(videos.js)を生成する。
企画もの（エンタメ・検証・報告系）は除外。タグはタイトルのキーワードで自動付与。
どのタグにも当たらないものは「その他」（年度セレクタで探せる）。
実行: python3 scripts/build_catalog.py
"""
import sqlite3, json, re, os

DB = os.path.expanduser("~/Claude/ogatore-growth/data/ogatore.db")
OUT = os.path.expanduser("~/Claude/kyou-no-ogatore/videos.js")

# ---- 企画もの除外（ストレッチ実技でないもの） ----
EXCLUDE = [
    r"どうなった", r"検証", r"対決", r"質問", r"Q&A", r"ルームツアー",
    r"本気で踊ってみた", r"歌って", r"記念", r"登録者", r"生配信", r"ライブ", r"お知らせ", r"報告",
    r"ラジオ体操って",
    r"ご挨拶", r"あいさつ", r"開設", r"紹介動画", r"チャンネルの", r"vlog", r"Vlog", r"VLOG",
    r"密着", r"晒", r"レビュー", r"開封", r"買ってみた", r"行ってみた", r"食べ", r"グッズ",
    r"募集", r"発表", r"公開収録", r"サブチャンネル", r"切り抜き", r"総集編?メイキング", r"メイキング",
    r"裏側", r"NG集", r"座談", r"対談", r"インタビュー", r"表彰", r"受賞", r"アンケート",
]

# ---- タグ判定（先勝ちでなく全部付与・上limit3つ） ----
RULES = [
    ("開脚",           r"開脚"),
    ("股関節",         r"股関節|あぐら"),
    ("肩・肩甲骨",     r"肩甲骨|四十肩|五十肩|肩まわり"),
    ("首・肩こり",     r"肩こり|肩コリ|首こり|首コリ|ストレートネック|スマホ首|頭痛|眼精疲労|首"),
    ("もも裏",         r"もも裏|ハムスト|前屈"),
    ("腰",             r"腰痛|反り腰|骨盤|腰"),
    ("姿勢・背中",     r"猫背|ネコ背|巻き肩|スウェイバック|姿勢|背中|背骨"),
    ("足首・足うら",   r"足首|ふくらはぎ|すね|足裏|足底|扁平足|外反母趾|足指|アキレス腱"),
    ("ひざ・O脚",      r"ひざ|膝|O脚|X脚|がに股|ガニ股"),
    ("太もも・お尻",   r"太もも|内もも|もも前|外もも|前もも|お尻|おしり|ヒップ|脚やせ|美脚|下半身|足パカ"),
    ("引き締め",       r"引き締め|音ゲー|痩せ|やせ|ダイエット|くびれ|二の腕|お腹|下っ腹|腹筋|体幹|プランク|筋トレ|ぽっこり|たるみ|小顔|尿もれ|ラジオ体操|HANDCLAP|ダンス"),
    ("むくみ",         r"むくみ|むくむ"),
    ("筋膜・マッサージ", r"筋膜|マッサージ|ローラー|ほぐす?玉"),
    ("スポーツ・運動前後", r"運動前|運動後|クールダウン|ウォーミングアップ|ランニング|ウォーキング|サッカー|野球|テニス|バレー|バスケ|ゴルフ|登山|トレッキング|部活"),
    ("全身",           r"全身"),
    ("朝",             r"朝|Morning|モーニング"),
    ("夜・寝る前",     r"夜|寝る前|睡眠|快眠|熟睡|眠り|不眠|お風呂上がり|Night"),
    ("座ったまま",     r"座ったまま|座りながら|椅子|イス|デスクワーク"),
    ("自律神経",       r"自律神経|リラックス|呼吸|ストレス|イライラ|生理"),
]

def fmt_views(v):
    if v >= 10000: return f"{v//10000}万回再生"
    if v > 0: return f"{v:,}回再生"
    return ""

def fmt_dur(sec):
    if not sec: return ""
    if sec < 60: return f"{sec}秒"
    return f"{round(sec/60)}分"

con = sqlite3.connect(DB)
rows = con.execute("""
  SELECT v.video_id, v.title, v.published_year, v.duration_sec, v.is_short, COALESCE(s.views,0)
  FROM videos v LEFT JOIN stats_lifetime s USING(video_id)
  ORDER BY COALESCE(s.views,0) DESC""").fetchall()

catalog, excluded, untagged = [], [], []
for vid, title, year, dur, is_short, views in rows:
    if not title: continue
    if any(re.search(p, title) for p in EXCLUDE):
        excluded.append(title); continue
    tags = [t for t, p in RULES if re.search(p, title)][:3]
    if dur and dur <= 600: tags.append("10分以内")
    if is_short: tags.append("ショート")
    if not [t for t in tags if t != "10分以内"]:
        tags.insert(0, "その他"); untagged.append(title)
    sub = "・".join(x for x in [f"{year}年", fmt_dur(dur), fmt_views(views)] if x)
    catalog.append({"id": vid, "t": title, "y": year, "s": sub, "tags": tags})

with open(OUT, "w") as f:
    f.write("// 自動生成: scripts/build_catalog.py（元データ=ogatore-growth/data/ogatore.db）\n")
    f.write("const CATALOG=" + json.dumps(catalog, ensure_ascii=False, separators=(",", ":")) + ";\n")

print(f"収録 {len(catalog)}本 / 除外 {len(excluded)}本 / その他 {len(untagged)}本")
print("\n--- 除外された動画（企画もの判定） ---")
for t in excluded: print(" ×", t[:70])
print("\n--- 「その他」行き（タグ判定できず・年度で探す） ---")
for t in untagged[:40]: print(" ?", t[:70])
