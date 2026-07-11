export const meta = {
  name: 'soudan-accuracy-loop',
  description: 'オフライン精度ループ: Fableで難問生成→現行マッチャで判定→ミス診断→kw処方→非退行ゲート。10周。',
  phases: [
    { title: 'Warmup', detail: 'ベースライン測定' },
    { title: 'Loop', detail: '10周: 生成→判定→処方→ゲート' },
    { title: 'Finalize', detail: '最終再測定・レビュー整理' },
  ],
}

const SLIM = {"intents":[{"id":"katakori","chip":"肩こり・首こり","kw":["かたこり","かたがこる","くびこり","くびがこる","かたがおもい","かたがいたい","くびがいたい","かたがばきばき","かたがはる","くびがまわらない","けんこうこつ","すまほくび","すとれーとねっく","がんせいひろう","肩こり","首こり","肩甲骨","肩が痛い","肩が重い","首が痛い","肩がガチガチ","肩がバキバキ","首がガチガチ","首がバキバキ","肩がゴリゴリ","くびがちがち","かたがちがち"],"safety":false},{"id":"youtsuu","chip":"腰痛","kw":["ようつう","こしがいたい","こしいた","こしつう","こしがおもい","こしがはる","こしがだるい","こしがつらい","そりごし","こつばん","すわりしごと","たちしごと","腰痛","腰が痛い","腰が重い","反り腰","骨盤","こし","腰","腰が","腰を","こしがぬけ","腰が抜け","ぎっくり"],"safety":false},{"id":"zenkutsu","chip":"前屈できない","kw":["ぜんくつ","たいぜんくつ","ちょうざたいぜんくつ","ゆかにつかない","てがとどかない","てがつかない","ゆびがつかない","ももうら","はむすとりんぐす","前屈","体前屈","長座体前屈","もも裏","床につかない","手が届かない","ハムストリング"],"safety":false},{"id":"kokansetsu","chip":"股関節が硬い","kw":["こかんせつ","あぐら","あぐらがかけない","あぐらがつらい","またがかたい","つけね","ちょうようきん","股関節","胡座","付け根","腸腰筋"],"safety":false},{"id":"ashikubi","chip":"しゃがめない","kw":["しゃがめない","しゃがむ","かかとがうく","かかとがつかない","あしくび","わしき","ふくらはぎ","あきれすけん","くっしん","うんこずわり","足首","踵が浮く","踵をつけ","和式","アキレス腱","屈伸"],"safety":false},{"id":"kaikyaku","chip":"開脚したい","kw":["かいきゃく","べたー","べたっ","ぺたー","ぺたっ","またわり","うちもも","ないてんきん","まえにたおれない","90ど","180ど","開脚","股割り","内もも","内転筋","前に倒れ","90度","180度"],"safety":false},{"id":"nekoze","chip":"猫背・姿勢","kw":["ねこぜ","しせい","まきがた","せなかがまるい","せすじ","くびがまえ","猫背","姿勢","巻き肩","背中が丸い","背筋"],"safety":false},{"id":"nemuri","chip":"寝つきが悪い","kw":["ねつき","ねむれない","ねれない","ねむりがあさい","ふみん","めがさめる","かいみん","じゅくすい","ぐっすり","すいみん","じりつしんけい","寝つき","寝付き","眠れない","寝れない","眠りが浅","不眠","目が覚め","快眠","熟睡","睡眠","自律神経"],"safety":false},{"id":"zutsuu","chip":"頭痛もち","kw":["ずつう","あたまがいたい","あたまがおもい","こめかみ","へんずつう","めのおく","はきけ","頭痛","頭が痛い","頭が重い","片頭痛","偏頭痛","目の奥","吐き気"],"safety":true},{"id":"hiza","chip":"ひざが気になる","kw":["ひざ","ひざがいたい","ひざうら","おさら","かいだんがつらい","oきゃく","おーきゃく","しっつう","膝","膝が痛い","膝を痛め","膝裏","膝痛","お皿","階段","o脚"],"safety":true},{"id":"zenshin","chip":"全身ガチガチ","kw":["ぜんしん","がちがち","ばきばき","かちかち","かちこち","からだがかたい","かたすぎ","うんどうぶそく","なにから","はじめかた","しょしんしゃ","全身","体が硬","身体が硬","硬すぎ","超硬い","運動不足","何から","初心者"],"safety":false},{"id":"jikan","chip":"時間がない・続かない","kw":["じかんがない","つづかない","つづけられない","みっかぼうず","いそがしい","さぼって","さぼった","めんどう","めんどくさ","やるき","あきしょう","時間がない","続かな","続けられ","三日坊主","忙し","面倒","やる気","飽き性"],"safety":false},{"id":"kikanai","chip":"効いてる気がしない","kw":["きかない","きいてない","きいてるかんじがしない","こうかがない","こうかなし","かわらない","のびてるかんじ","はんしんはんぎ","あってるか","あってるのか","どこにきいて","効かない","効いてない","効いてる感","効果がない","効果を感じ","変わらない","伸びてる感","半信半疑","合ってるか","合ってるのか","どこに効いて"],"safety":false},{"id":"itakunatta","chip":"やったら痛くなった","kw":["いたくなった","やったらいたい","いたみがでた","いたすぎ","いためた","きんにくつう","やりすぎた","つった","ひどくなった","痛くなった","やったら痛い","痛みが出た","痛すぎ","痛めた","筋肉痛","悪化"],"safety":true},{"id":"straightneck","chip":"スマホ首","kw":["すとれーとねっく","すまほくび","あたまがまえにでる","くびがまえにでる","うつむきすぎ","すまほのみすぎ","首が前に出","頭が前に出"],"safety":false},{"id":"kubifukurami","chip":"首の後ろのこぶ","kw":["くびのうしろのもりあがり","くびのつけねのもりあがり","くびのこぶ","くびのうしろがぼこ","首の膨らみ","首のこぶ","首の付け根の盛り上がり"],"safety":true},{"id":"nechigae","chip":"寝違えた","kw":["ねちがえ","ねちがえた","ねちがい","寝違え"],"safety":true},{"id":"gansei","chip":"目が疲れる","kw":["がんせいひろう","めがつかれ","めのつかれ","めのおくがおもい","めのおくがつかれ","どらいあい","ぱそこんのみすぎ","眼精疲労","目が疲れ","目の疲れ"],"safety":false},{"id":"kuishibari","chip":"食いしばり","kw":["くいしばり","くいしばって","はぎしり","あごがだるい","あごがつかれる","こめかみがいたい","食いしばり","歯ぎしり","顎がだるい"],"safety":true},{"id":"tenki","chip":"天気・気圧で不調","kw":["きあつ","てんきつう","てんきがわるいと","あめのひはあたまが","ばいう","つゆだる","気圧","天気痛","梅雨","低気圧"],"safety":false},{"id":"makura","chip":"枕が合わない?","kw":["まくらがあわない","まくらがたかい","まくらなし","あさおきるとくびがいたい","枕"],"safety":false},{"id":"kenkohagashi","chip":"肩甲骨はがし","kw":["けんこうこつはがし","けんこうこつはがしってきくの","肩甲骨はがし"],"safety":false},{"id":"kenkogori","chip":"肩甲骨ゴリゴリ","kw":["ごりごり","けんこうこつがなる","かたがなる","ゴリゴリ"],"safety":false},{"id":"shijukata","chip":"四十肩・五十肩?","kw":["しじゅうかた","ごじゅうかた","うでがあがらない","かたがあがらない","四十肩","五十肩","腕が上がらない","肩が上がらない"],"safety":true},{"id":"makikata","chip":"巻き肩","kw":["まきがた","まきかた","かたがまえにはいる","かたがうちまき","てのこうがまえをむく","巻き肩","巻肩"],"safety":false},{"id":"senakate","chip":"背中で手が組めない","kw":["せなかでてがくめない","せなかでてがとどかない","せなかでてをくむ","ばんざいがつらい","背中で手"],"safety":true},{"id":"tekubi","chip":"手首がつらい","kw":["てくびがいたい","てくびがつらい","けんしょうえん","おやゆびがいたい","手首","腱鞘炎","親指が痛い"],"safety":true},{"id":"senakahari","chip":"背中が張る","kw":["せなかがはる","せなかがいたい","せなかがおもい","せなかがばきばき","けんこうこつのあいだ","背中が張","背中が痛","肩甲骨の間","せなかがこる","背中がこ","せなかがこって"],"safety":false},{"id":"sebone","chip":"背骨ガチガチ","kw":["せぼねがかたい","せぼねをほぐしたい","せすじがのびない","せなかをそらすといたい","ぶりっじができない","ブリッジ","背骨"],"safety":false},{"id":"sorigoshi","chip":"反り腰","kw":["そりごし","こしがそってる","こしとかべのすきま","反り腰","腰が反って"],"safety":false},{"id":"nekosori","chip":"猫背も反り腰も","kw":["ねこぜとそりごし","そりごしとねこぜ","ねこぜでそりごし","すわるとねこぜたつとそりごし","猫背と反り腰"],"safety":false},{"id":"kotsuban2","chip":"骨盤のゆがみ","kw":["こつばんのゆがみ","こつばんがゆがんでる","こつばんきょうせい","ゆがみ","骨盤矯正","骨盤の歪み","歪み"],"safety":false},{"id":"swayback","chip":"スウェイバック","kw":["すうぇいばっく","こしが2まえにすらいど","ねこぜでもそりごしでもない","スウェイバック"],"safety":false},{"id":"asagoshi","chip":"朝、腰が痛い","kw":["あさこしがいたい","あさおきるとこしがいたい","ねおきのこしがいたい","おきるときこしが","朝起きると腰","寝起きの腰"],"safety":false},{"id":"oshirikori","chip":"お尻がガチガチ","kw":["おしりがかたい","おしりがこる","おしりのこり","おしりがいたい","りじょうきん","すわるとおしりが","梨状筋","お尻が痛い","お尻が硬い"],"safety":true},{"id":"momomae","chip":"前ももの張り","kw":["まえもも","もものまえ","まえももがはる","ももまえがはる","だいたいしとうきん","前もも","もも前","前ももが張る","太ももの前"],"safety":false},{"id":"sotohari","chip":"太ももの外張り","kw":["そともも","そとはり","もものそとがわ","そとももがはる","ちょうけいじんたい","外もも","外張り","太ももの外"],"safety":false},{"id":"fukurahagi","chip":"ふくらはぎガチガチ","kw":["ふくらはぎがかたい","ふくらはぎがはる","ふくらはぎがぱんぱん","ふくらはぎがだるい","ひふくきん","ふくらはぎの張り","ふくらはぎが硬い"],"safety":false},{"id":"shishamo","chip":"ふくらはぎ太め?","kw":["ふくらはぎがふとい","ししゃも","あしくびがふとい","ふくらはぎをほそく","ふくらはぎが太い","ふくらはぎ太","ふくらはぎふとい","ふくらはぎがふとくみえる"],"safety":false},{"id":"komuragaeri","chip":"足がつる","kw":["あしがつる","こむらがえり","ふくらはぎがつる","よなかにつる","よくつる","足がつる","こむら返り","攣る"],"safety":true},{"id":"mukumi","chip":"脚のむくみ","kw":["むくみ","むくむ","あしがぱんぱん","ゆうがたになるとあしが","くつがきつくなる","浮腫","あしをほそく","脚を細","きゃくやせ","あしやせ"],"safety":true},{"id":"yubimukumi","chip":"指がむくむ","kw":["ゆびがむくむ","てがむくむ","ゆびわがきつい","てのむくみ","ゆびのむくみ","指がむくむ","手がむくむ"],"safety":true},{"id":"hie","chip":"冷えがつらい","kw":["ひえしょう","ひえがつらい","てあしがつめたい","あしさきがつめたい","おんかつ","冷え性","冷え症","手足が冷た","温活","冷え"],"safety":false},{"id":"fuyu","chip":"寒いと固まる","kw":["ふゆになるとかたまる","さむいとかたまる","さむくてちぢこまる","さむいひ","寒いと","冬になると"],"safety":false},{"id":"natsu","chip":"冷房で不調","kw":["くーらーでひえる","れいぼうでひえる","なつばて","れいぼうびえ","クーラー","冷房","夏バテ"],"safety":false},{"id":"ashiura","chip":"足裏の疲れ","kw":["あしのうらがつかれる","あしうら","そくてい","つちふまず","うきゆび","あしのゆび","足裏","足の裏","足底","土踏まず","浮き指"],"safety":false},{"id":"henpeisoku","chip":"扁平足かも","kw":["へんぺいそく","あーちがない","扁平足"],"safety":true},{"id":"gaihanboshi","chip":"外反母趾ぎみ","kw":["がいはんぼし","おやゆびのつけねがいたい","外反母趾"],"safety":true},{"id":"sune","chip":"すねが張る","kw":["すねがはる","すねがだるい","すねのそとがわ","しんすぷりんと","すねが張る","脛"],"safety":true},{"id":"okyaku","chip":"O脚・X脚","kw":["がにまた","うちまた","えっくすきゃく","おーきゃくをなおしたい","o脚矯正","x脚","がに股","内股","O脚矯正","X脚","o脚","o脚なおし","おーきゃく","おきゃく","o脚を"],"safety":true},{"id":"kokansetsunaru","chip":"股関節が鳴る","kw":["こかんせつがなる","こかんせつのおと","またがぽきぽき","こかんせつがぽきぽき","股関節が鳴る","股関節の音"],"safety":true},{"id":"sokeibu","chip":"付け根の詰まり","kw":["そけいぶ","つけねがつまる","つけねがいたい","あしのつけね","鼠径部","付け根が痛い","付け根の詰まり"],"safety":true},{"id":"hizanaru","chip":"ひざが鳴る","kw":["ひざがなる","ひざがぽきぽき","ひざのおと","膝が鳴る","膝の音"],"safety":true},{"id":"senchou","chip":"お尻の骨のあたり","kw":["せんちょうかんせつ","おしりのほねがいたい","びこつ","こしとおしりのさかいめ","仙腸関節","尾骨"],"safety":true},{"id":"ashidaru","chip":"脚が重だるい","kw":["あしがだるい","あしがおもい","脚がだるい","足がだるい","脚が重い"],"safety":true},{"id":"deskwork","chip":"デスクワークで","kw":["ですくわーく","すわりっぱなし","ざいたくわーく","てれわーく","いちにちすわって","ぱそこんさぎょう","じゅけんべんきょう","べんきょうで","デスクワーク","座りっぱなし","在宅ワーク","テレワーク","パソコン作業","受験勉強"],"safety":false},{"id":"tachishigoto","chip":"立ち仕事で","kw":["たちしごと","たちっぱなし","いちにちたって","れじうち","立ち仕事","立ちっぱなし"],"safety":false},{"id":"unten","chip":"運転が長い","kw":["うんてんがながい","うんてんで","ちょうきょりうんてん","とらっく","くるまで","運転","長距離"],"safety":false},{"id":"dakko","chip":"抱っこで肩腰が","kw":["だっこ","あかちゃんをだっこ","こそだてで","いくじで","おんぶ","抱っこ","育児","子育て"],"safety":true},{"id":"oyako","chip":"子どもと一緒に","kw":["おやこで","こどもといっしょ","こどもでもできる","しょうがくせいでも","かぞくで","親子","子どもと","子供と"],"safety":false},{"id":"ashihayaku","chip":"足を速くしたい","kw":["あしをはやく","かけっこ","そくりょく","うんどうかいまえ","たいいくさい","足を速く","運動会","徒競走"],"safety":false},{"id":"koureisha","chip":"高齢の親にも","kw":["こうれいのおや","おやにすすめたい","りょうしんに","しにあ","ろうごのために","60だいでも","70だいでも","高齢","シニア","老後","両親に","親に","おやに","親にすすめ","ははに","ちちに"],"safety":true},{"id":"undomae","chip":"運動前にやるなら","kw":["うんどうまえ","うんどうのまえ","しあいまえ","じゅんびうんどう","うぉーみんぐあっぷ","運動前","準備運動","ウォームアップ","試合前"],"safety":false},{"id":"undogo","chip":"運動後のケア","kw":["うんどうご","うんどうのあと","くーるだうん","しあいのあと","れんしゅうのあと","運動後","クールダウン","練習後"],"safety":false},{"id":"running","chip":"ランニングする人","kw":["らんにんぐ","まらそん","じょぎんぐ","はしるまえ","うぉーきんぐ","ランニング","マラソン","ジョギング","ウォーキング"],"safety":false},{"id":"golf","chip":"ゴルフのために","kw":["ごるふ","すいんぐ","ひきょりをのばしたい","ゴルフ","スイング","飛距離"],"safety":false},{"id":"neoki","chip":"寝起きがつらい","kw":["ねおきがかたい","あさからだがかたい","ねおきがだるい","あさがつらい","あさおきられない","寝起き","朝がつらい","朝は体が硬い"],"safety":false},{"id":"rajio","chip":"ラジオ体操がわり","kw":["らじおたいそう","たいそうがわり","ラジオ体操","体操がわり"],"safety":false},{"id":"netamama","chip":"寝たままやりたい","kw":["ねたまま","ねながら","ふとんのなかで","よこになったまま","おきあがらずに","寝たまま","寝ながら","布団の中"],"safety":false},{"id":"nagara","chip":"ながらでやりたい","kw":["ながらで","てれびをみながら","ながらすとれっち","すきまじかん","ながらでできる","スキマ時間"],"safety":false},{"id":"shakitto","chip":"シャキッとしたい","kw":["ねむけざまし","しゃきっとしたい","ごごのねむけ","しごとちゅうねむい","あたまをすっきり","しゅうちゅうできない","眠気","集中できない","目を覚ましたい"],"safety":false},{"id":"tsukare","chip":"疲れが取れない","kw":["つかれがとれない","つかれやすい","まんせいひろう","ひろうかん","ねてもつかれ","疲れが取れない","疲れやすい","疲労","寝ても疲れ"],"safety":true},{"id":"darui","chip":"だるい・やる気ない","kw":["だるい","からだがおもい","やるきがでない","きだるい","なにもしたくない","倦怠感","けんたいかん","体が重い"],"safety":true},{"id":"stress","chip":"ストレス・イライラ","kw":["すとれす","いらいら","りらっくすしたい","きもちをおちつけ","こころがつかれた","きんちょうしやすい","ストレス","イライラ","リラックス","緊張"],"safety":false},{"id":"kokyuasa","chip":"呼吸が浅い気がする","kw":["こきゅうがあさい","いきがあさい","いきがすいづらい","しんこきゅうができない","むねがひらかない","呼吸が浅い","息が浅い","深呼吸"],"safety":true},{"id":"yasetai","chip":"痩せたい","kw":["やせたい","だいえっと","しぼうをおとしたい","たいじゅうをへらしたい","ほっそりしたい","痩せたい","ダイエット","脂肪"],"safety":false},{"id":"pokkori","chip":"下っ腹ぽっこり","kw":["したっぱら","ぽっこりおなか","おなかをひきしめ","くびれ","おなかがでてきた","下っ腹","ぽっこり","お腹を引き締め"],"safety":false},{"id":"ninoude","chip":"二の腕すっきり","kw":["にのうで","うでをほそく","ふりそでにく","二の腕","振り袖"],"safety":false},{"id":"hipup","chip":"ヒップアップ","kw":["おしりのたるみ","ひっぷあっぷ","おしりをあげたい","びしり","お尻のたるみ","ヒップアップ","美尻"],"safety":false},{"id":"haminiku","chip":"背中のハミ肉","kw":["はみにく","せなかのにく","わきのにく","ぶらのうえのにく","ハミ肉","背中の肉","わき肉"],"safety":false},{"id":"kogao","chip":"顔のむくみ","kw":["かおのむくみ","こがお","ふぇいすらいん","かおがぱんぱん","かおのたるみ","小顔","顔のむくみ","フェイスライン"],"safety":false},{"id":"nijuago","chip":"二重あご","kw":["にじゅうあご","あごのたるみ","あごにく","二重あご","二重アゴ","あご痩せ"],"safety":false},{"id":"sakotsu","chip":"鎖骨・首まわり","kw":["さこつ","うもれさこつ","くびをながくみせたい","でこるて","くびまわりをすっきり","鎖骨","デコルテ","首を長く"],"safety":false},{"id":"seiri","chip":"生理前のつらさ","kw":["せいりつう","せいりまえ","せいりちゅう","ぴーえむえす","pms","げっけいつう","生理","月経","PMS"],"safety":true},{"id":"kounenki","chip":"更年期のゆらぎ","kw":["こうねんき","更年期"],"safety":true},{"id":"nyoumore","chip":"骨盤底筋を鍛える","kw":["にょうもれ","こつばんていきん","くしゃみでもれ","ちょいもれ","尿もれ","尿漏れ","骨盤底筋"],"safety":true},{"id":"benpi","chip":"お腹すっきりしない","kw":["べんぴ","おつうじ","ちょうかつ","おなかがはる","便秘","腸活","お通じ"],"safety":false},{"id":"sayuusa","chip":"左右差がある","kw":["さゆうさ","かたほうだけこる","みぎがわだけ","ひだりがわだけ","かたがわだけ","左右差","片側だけ","右だけ","左だけ"],"safety":false},{"id":"umaretsuki","chip":"生まれつき硬い?","kw":["うまれつきかたい","もともとかたい","せんてんてきにかたい","こどものころからかたい","いでんでかたい","生まれつき","遺伝"],"safety":false},{"id":"shincho","chip":"姿勢でスタイルUP","kw":["しんちょうをたかく","せをたかくみせたい","せがちぢんだ","すたいるをよくしたい","身長","スタイル"],"safety":false},{"id":"wakibara","chip":"わき腹が伸びない","kw":["わきばら","たいそくがかたい","わきがのびない","よこっぱらがはる","脇腹","体側","わき腹"],"safety":false},{"id":"mainichi","chip":"毎日やっていい?","kw":["まいにちやっていい","やりすぎ","いちにちなんかい","まいにちやると","毎日やって","何回まで","やり過ぎ"],"safety":false},{"id":"byou","chip":"何秒伸ばせばいい?","kw":["なんびょうのばす","なんぷんのばす","どれくらいのばせば","20びょう","30びょう","何秒","何分伸ば"],"safety":false},{"id":"iki","chip":"呼吸のコツは?","kw":["いきをとめる","こきゅうほう","いきをはきながら","いきかたのこつ","呼吸法","息を止め","呼吸のコツ","こきゅうはどう","呼吸はどう","いきはどう"],"safety":false},{"id":"itaihodo","chip":"痛いほど効くの?","kw":["いたいほうがきく","いたきもちいい","どこまでのばしていい","がまんしてのばす","痛いほど","イタ気持ちいい","我慢して伸ば"],"safety":false},{"id":"handou","chip":"反動はつけていい?","kw":["はんどうをつけ","ばうんどさせ","ゆらしながらのばす","反動","バウンド"],"safety":false},{"id":"ofuro","chip":"お風呂の前後どっち?","kw":["おふろのまえ","おふろのあと","にゅうよくご","おふろあがり","ゆあがりに","お風呂","入浴後","風呂上がり"],"safety":false},{"id":"shokuji","chip":"食後にやっていい?","kw":["しょくごにやって","しょくぜんとしょくご","たべたあとすぐ","ごはんのあと","食後","食前","食べてすぐ"],"safety":false},{"id":"osake","chip":"飲んだ日はどうする?","kw":["おさけをのんだひ","いんしゅご","よっぱらって","ふつかよい","のみかいのあと","飲んだ日","二日酔い","飲酒"],"safety":false},{"id":"dougu","chip":"道具は要るの?","kw":["きぐはいる","どうぐはいる","すとれっちぽーる","ふぉーむろーらー","まっとはいる","たおるでできる","道具","器具","ポール","ローラー","マット"],"safety":false},{"id":"junban","chip":"順番はあるの?","kw":["じゅんばんはある","どのじゅんばんで","めにゅーのくみかた","くみあわせかた","順番","メニューの組み"],"safety":false},{"id":"kawaru","chip":"いつ柔らかくなる?","kw":["どれくらいでやわらかく","なんしゅうかんで","いつごろかわる","こうかがでるまで","効果が出るまで","何週間","いつ柔らかく"],"safety":false},{"id":"nenrei","chip":"何歳からでも?","kw":["なんさいからでも","いまさらはじめても","おそすぎる","としだから","もうねんだから","何歳からでも","遅すぎ","今さら"],"safety":false},{"id":"tsuiteikenai","chip":"ついていけない…","kw":["ついていけない","ぽーずができない","どうがのとおりにできない","おなじうごきができない","ついていけません","ポーズができない"],"safety":false},{"id":"kintore","chip":"筋トレとどっち?","kw":["きんとれとどっち","きんとれもしたほうがいい","きんとれとすとれっち","筋トレ"],"safety":false},{"id":"yoga","chip":"ヨガとちがうの?","kw":["よがとちがい","よがとどっち","ぴらてぃす","ヨガ","ピラティス"],"safety":false},{"id":"seitai","chip":"整体とどっちがいい?","kw":["せいたいとどっち","まっさーじにいっても","ほぐしにいっても","せいこついん","かいろぷらくてぃっく","整体","整骨院","マッサージ店"],"safety":false},{"id":"kouka","chip":"そもそも何に効く?","kw":["すとれっちのこうか","なんのためにやる","いみあるの","なにがかわるの","ストレッチの効果","意味ある"],"safety":false},{"id":"ninki","chip":"人気の動画おしえて","kw":["にんきのどうが","おすすめどうが","いちばんみられてる","ばずったどうが","だいひょうさく","人気動画","おすすめの動画","代表作"],"safety":false},{"id":"kyounani","chip":"今日どれやろう?","kw":["きょうはどれ","どれをやれば","どうがをえらべない","まよってえらべない","どれからやれば","どれがいいか"],"safety":false},{"id":"check","chip":"硬さをチェックしたい","kw":["かたさちぇっく","どれくらいかたいか","じゅうなんせいてすと","かたさをはかりたい","柔軟性チェック","硬さチェック","柔軟性テスト"],"safety":false},{"id":"tanoshiku","chip":"楽しくやりたい","kw":["たのしくやりたい","あきないほうほう","おんげー","おんがくにあわせて","だんすすとれっち","音ゲー","ダンス","楽しく"],"safety":false},{"id":"ase","chip":"汗かきたくない","kw":["あせをかきたくない","あせかかずに","めいくがくずれる","汗をかきたくない","汗かかない"],"safety":false},{"id":"suwarikata","chip":"正しい座り方は?","kw":["すわりかたをしりたい","ただしいすわりかた","いすのすわりかた","すわりかたのこつ","座り方"],"safety":false},{"id":"tachikata","chip":"正しい立ち方は?","kw":["たちかたをしりたい","ただしいたちかた","たちかたのこつ","じゅうしん","立ち方","重心"],"safety":false},{"id":"arukikata","chip":"歩き方のコツは?","kw":["あるきかたをしりたい","ただしいあるきかた","あるきかたのこつ","歩き方"],"safety":false},{"id":"hiji","chip":"肘がつらい","kw":["ひじがいたい","てにすひじ","ごるふひじ","ひじのつかいすぎ","肘","テニス肘"],"safety":true},{"id":"ikarigata","chip":"肩に力が入る","kw":["いかりがた","かたがすくむ","かたにちからがはいる","かたがあがってる","いかり肩","肩に力"],"safety":false},{"id":"kubinaru","chip":"首を鳴らしちゃう","kw":["くびをならす","くびをぼきぼき","くびがなる","ぼきぼきならす","首を鳴らす","首ぽきぽき"],"safety":true}],"redflags":["げきつう","はげしいいたみ","しびれ","まひ","ちからがはいらない","こっせつ","だっきゅう","ぎっくり","つよくぶつけた","しゅじゅつ","じゅつご","にゅういん","にんしん","にんぷ","さんご","へるにあ","きょうさくしょう","ざこつしんけいつう","はつねつ","ねつがある","めまい","激痛","痺れ","麻痺","骨折","脱臼","手術","術後","入院","妊娠","妊婦","産後","狭窄症","坐骨神経痛","転倒","発熱","力が入らない","にくばなれ","ねんざ","だぼく","むちうち","こつそしょうしょう","ねつっぽい","きゅうにいたみだした","肉離れ","捻挫","打撲","むち打ち","骨粗しょう症","腫れ","急に痛"]}

// ---- 現行マッチャ（index.html 忠実移植・決定論的） ----
function norm(s){ s=String(s==null?'':s).toLowerCase();
  s=s.replace(/[Ａ-Ｚａ-ｚ０-９]/g,function(c){return String.fromCharCode(c.charCodeAt(0)-0xFEE0)});
  s=s.replace(/[ァ-ヶ]/g,function(c){return String.fromCharCode(c.charCodeAt(0)-0x60)});
  return s.replace(/[^0-9a-zぁ-ゖー一-鿿々]/g,''); }
// 可変KB（周回で kw が増える）
const KB = SLIM.intents.map(function(it){ return { id:it.id, chip:it.chip, safety:it.safety, kw:(it.kw||[]).slice() }; })
const RF = (SLIM.redflags||[]).map(norm).filter(function(k){ return k.length>=2 })
const byId = {}; KB.forEach(function(it){ byId[it.id]=it })
function redFlag(n){ for(var i=0;i<RF.length;i++){ if(n.indexOf(RF[i])>=0) return true } return false }
function score(n){ var out=[]; for(var a=0;a<KB.length;a++){ var it=KB[a],sc=0,fh=-1;
    for(var j=0;j<it.kw.length;j++){ var k=norm(it.kw[j]); if(k&&n.indexOf(k)>=0){ sc+=k.length; if(fh<0)fh=j } }
    if(sc>0) out.push({id:it.id,chip:it.chip,score:sc,fh:fh}) }
  out.sort(function(x,y){ return (y.score-x.score)||(x.fh-y.fh) }); return out }
function landing(text){ var n=norm(text); if(redFlag(n)) return {type:'redflag'};
  var s=score(n); if(!s.length) return {type:'none'}; return {type:'hit',id:s[0].id,chip:s[0].chip} }

// ---- 非退行ゲート付き kw 追加 ----
var regset = []   // {text, expectId} 確定済み正着地。ここが壊れたら却下
var keptKw = {}   // intentId -> [追加kw]
function alreadyHas(it,k){ for(var i=0;i<it.kw.length;i++){ if(norm(it.kw[i])===k) return true } return false }
function tryAdd(intentId, kwList, caseText){
  var it=byId[intentId]; if(!it) return {ok:false,reason:'no-intent'}
  var added=[]
  for(var i=0;i<kwList.length;i++){
    var raw=String(kwList[i]||''); var k=norm(raw)
    if(k.length<2) continue
    if(alreadyHas(it,k)) continue
    // 汎用すぎる語(全インテントで乱発しそう)は弾く: 2文字以下のひらがなのみ等
    if(/^[ぁ-ゖー]{1,2}$/.test(k)) continue
    it.kw.push(raw); added.push(raw)
  }
  if(!added.length) return {ok:false,reason:'nothing-new'}
  // 対象ケースが intentId に着地するか
  var lc=landing(caseText)
  if(!(lc.type==='hit'&&lc.id===intentId)){ for(var r=0;r<added.length;r++) it.kw.pop(); return {ok:false,reason:'no-fix'} }
  // 既存の確定正着地を壊さないか
  for(var g=0; g<regset.length; g++){ var rg=regset[g]; var lg=landing(rg.text);
    if(!(lg.type==='hit'&&lg.id===rg.expectId)){ for(var r2=0;r2<added.length;r2++) it.kw.pop(); return {ok:false,reason:'regress'} } }
  // 合格
  keptKw[intentId]=(keptKw[intentId]||[]).concat(added)
  regset.push({text:caseText, expectId:intentId})
  return {ok:true, added:added}
}

// ---- 全ケース集約（最終再測定・回帰テスト化用） ----
var allCases = []   // {text, expectId|null, verdict}

// ---- エージェントのスキーマ ----
var GEN_SCHEMA = { type:'object', properties:{ cases:{ type:'array', items:{ type:'string' } } }, required:['cases'] }
var JUDGE_SCHEMA = { type:'object', properties:{ results:{ type:'array', items:{ type:'object',
  properties:{ i:{type:'integer'}, verdict:{type:'string', enum:['ok','miss_none','miss_wrong','over_redflag','new_topic']},
    targetIntentId:{type:'string'}, kw:{ type:'array', items:{type:'string'} } },
  required:['i','verdict','targetIntentId','kw'] } } }, required:['results'] }

var THEMES = ['デスクワークで肩・首・肩甲骨','腰まわり・骨盤・お尻','脚/膝/ふくらはぎ/むくみ','姿勢(反り腰/猫背/巻き肩)',
  '睡眠/自律神経/頭痛/目の疲れ','産後・年代・更年期など慢性の事情','手/腕/手首/末端の冷え','全身のだるさ/運動が苦手/続かない',
  '開脚/柔軟/前屈ができない','生活動作(抱っこ/立ち仕事/スマホ/階段)']

// 判定用: intent一覧を1度だけ文字列化
function intentList(){ return KB.map(function(it){ return '- '+it.id+' : '+it.chip+(it.safety?' [安全]':'') }).join('\n') }

function genPrompt(r){ return '尾形(オガトレ)というストレッチ系YouTuberの「からだ相談チャット」に、視聴者が実際に打ちそうな日本語の短い相談文を作る。\n'+
  'ラウンド'+r+'のテーマ: '+THEMES[(r-1)%THEMES.length]+'\n'+
  '30個、短く自然に、バラして: 口語・かな/タイポ・複合(2部位)・個別事情(産後/年齢/仕事)・動作+部位(階段で膝の外)・ふわっとした訴え。\n'+
  '教科書的な言い方は避け、生活者が本当に打つ言い方で。医療の激痛/しびれ等の緊急ワードは入れない(それは別処理)。\n'+
  '{cases:[...30 strings...]} で返す。' }

function judgePrompt(r, landed){
  var lines = landed.map(function(x,idx){ var l=x.land;
    var s = l.type==='hit' ? ('着地→'+l.chip+'('+l.id+')') : (l.type==='none'?'着地→該当なし':'着地→赤旗(受診案内)');
    return idx+'. 「'+x.t+'」 '+s; }).join('\n')
  return '相談チャットの現行マッチャ(部分一致・正規化)の判定を診断する。マッチは「正規化した相談文に、各インテントのkw(短い部分文字列)が含まれるか」で決まる。\n\n'+
    '【インテント一覧(id:chip)】\n'+intentList()+'\n\n'+
    '【今回の相談文と現行の着地】\n'+lines+'\n\n'+
    '各iについて判定して results で返す:\n'+
    '- verdict: ok(妥当な着地) / miss_none(該当なしだが本当はあるインテントに行くべき) / miss_wrong(着地先が的外れで別インテントが正しい) / over_redflag(赤旗に飛んだが慢性で受診案内は過剰) / new_topic(既存インテントに無い新しい悩み)\n'+
    '- miss_none/miss_wrong の時: targetIntentId=正しい既存インテントのid、kw=その相談文に実在する短く特徴的な部分文字列を1〜3個(その部位に固有で、他の悩みを奪わない語。全身/がちがち等の汎用語は禁止)\n'+
    '- ok/over_redflag/new_topic の時: targetIntentId=""、kw=[]\n'+
    'kwは相談文中に実際に出てくる表記で(正規化前でよい)。' }

// ================= 実行 =================
phase('Warmup')
log('スリムKB: intents='+KB.length+' redflags='+RF.length)

phase('Loop')
for (var r=1; r<=10; r++){
  var gen = await agent(genPrompt(r), { label:'gen-r'+r, phase:'Round '+r, schema:GEN_SCHEMA, model:'fable', effort:'medium' })
  var cases = ((gen&&gen.cases)||[]).filter(function(x){ return typeof x==='string' && x.trim() }).slice(0,30)
  if(!cases.length){ log('R'+r+': 生成ゼロ・スキップ'); continue }
  var landed = cases.map(function(t){ return { t:t, land:landing(t) } })
  var judge = await agent(judgePrompt(r, landed), { label:'judge-r'+r, phase:'Round '+r, schema:JUDGE_SCHEMA, model:'fable', effort:'high' })
  var results = (judge&&judge.results)||[]
  var okN=0, fixedN=0, revN=0, triedN=0
  // ok件を先に確定正着地として登録（後続の非退行ゲートの土台にする）
  for(var a=0;a<results.length;a++){ var res=results[a]; var c=landed[res.i]; if(!c) continue;
    if(res.verdict==='ok' && c.land.type==='hit'){ regset.push({text:c.t, expectId:c.land.id}); allCases.push({text:c.t, expectId:c.land.id, verdict:'ok'}); okN++ } }
  // 修正系
  for(var b=0;b<results.length;b++){ var rs=results[b]; var cc=landed[rs.i]; if(!cc) continue;
    if(rs.verdict==='miss_none'||rs.verdict==='miss_wrong'){ triedN++;
      var t=tryAdd(rs.targetIntentId, rs.kw||[], cc.t);
      if(t.ok){ fixedN++; allCases.push({text:cc.t, expectId:rs.targetIntentId, verdict:rs.verdict}) }
      else { allCases.push({text:cc.t, expectId:rs.targetIntentId||null, verdict:rs.verdict+':'+t.reason}) }
    } else if(rs.verdict==='over_redflag'){ revN++; allCases.push({text:cc.t, expectId:null, verdict:'over_redflag'}) }
    else if(rs.verdict==='new_topic'){ revN++; allCases.push({text:cc.t, expectId:null, verdict:'new_topic'}) }
  }
  var cum=0; for(var k in keptKw) cum+=keptKw[k].length
  log('R'+r+' ['+THEMES[(r-1)%10]+'] cases='+cases.length+' ok='+okN+' fixed='+fixedN+'/'+triedN+' review='+revN+' 累計追加kw='+cum)
}

phase('Finalize')
// 最終再測定: expectId を持つケースを、元KB(KB0)と最終KBで比較
function landWith(intentsArr, text){ var n=norm(text);
  for(var i=0;i<RF.length;i++){ if(n.indexOf(RF[i])>=0) return '__redflag__' }
  var best=null,bs=0,bf=1e9;
  for(var a=0;a<intentsArr.length;a++){ var it=intentsArr[a],sc=0,fh=-1;
    for(var j=0;j<it.kw.length;j++){ var kk=norm(it.kw[j]); if(kk&&n.indexOf(kk)>=0){ sc+=kk.length; if(fh<0)fh=j } }
    if(sc>0 && (sc>bs || (sc===bs&&fh<bf))){ bs=sc; bf=fh; best=it.id } }
  return best }
var KB0 = SLIM.intents.map(function(it){ return { id:it.id, kw:(it.kw||[]).slice() } })
var fixable = allCases.filter(function(c){ return c.expectId })
var base=0, fin=0
for(var i=0;i<fixable.length;i++){ if(landWith(KB0, fixable[i].text)===fixable[i].expectId) base++; if(landWith(KB, fixable[i].text)===fixable[i].expectId) fin++ }
var totalKw=0; for(var k2 in keptKw) totalKw+=keptKw[k2].length

return {
  rounds: 10,
  totalCases: allCases.length,
  fixableCases: fixable.length,
  baselineCorrect: base,
  finalCorrect: fin,
  baselinePct: fixable.length? Math.round(base/fixable.length*100):0,
  finalPct: fixable.length? Math.round(fin/fixable.length*100):0,
  keptKwCount: totalKw,
  keptKw: keptKw,
  reviewNewTopic: allCases.filter(function(c){ return c.verdict==='new_topic' }).map(function(c){ return c.text }),
  reviewOverRedflag: allCases.filter(function(c){ return c.verdict==='over_redflag' }).map(function(c){ return c.text }),
  regressionSet: fixable.map(function(c){ return {text:c.text, expectId:c.expectId} }),
}
