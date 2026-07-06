
## Pages連続デプロイの注意（2026-07-06 alanハブより）
短時間に連続pushすると直後のDeploy Pagesが「Deployment failed, try again later」で落ちる（GitHub側の癖・中身は正常）。対処: **失敗したら `gh run rerun <id>` で通る**（今日3回ともそれで成功）。可能ならpushは2〜3分空けるかまとめる。ハブ側もGmail通知で失敗を検知したら自動でrerunしてる。
