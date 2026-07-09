// #きょうのオガトレ オガトレ部フィード（尾形さん本人の「ひとこと」「写真」「ラジオ」投稿）
// 運用: 尾形さん本人がアプリ内で投稿するのではなく、このファイルを外部で更新して配信する（動画カタログ更新と同じ運用）
// 要素の形:
//   {id:"一意なID文字列", date:"YYYY-MM-DD", type:"text"|"photo"|"radio",
//    text:"本文（type=text/photoのキャプション・200字程度想定）",
//    image:"assets/obu/xxx.jpg（type=photoの時だけ）",
//    audio:"assets/obu/xxx.mp3（type=radioの時だけ）",
//    title:"ラジオのタイトル（type=radioの時だけ）",
//    time:"HH:MM（任意項目・投稿のだいたいの時刻。省略可＝無ければ日付のみ表示）"}
const OBU_FEED=[
 {id:"20260709-01", date:"2026-07-09", type:"photo", image:"assets/obu/post-2026-07-09-01.jpg",
  text:"アプリ開発中〜！腹へった〜！やきとりくいて〜！！！"},
 {id:"20260709-02", date:"2026-07-09", type:"text", time:"18:00",
  text:"1行目だよ<script>alert(1)</script>\n2行目だよ&\"quote\"\n3行目 <b>bold?</b>"},
];
