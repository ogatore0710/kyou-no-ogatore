package jp.ogatore.kyouno.safety

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// ネイティブ移植 Step 2(マスタープラン§3-4手順3): safety-fixtures.json(111件)をJUnit4 Parameterizedで
// 1件=1テストケースとしてバンドルし、全件アサートする(gradle testの結果に111件それぞれの成否が個別に出る。
// XCTestのループ内XCTAssertTrueと同じく、1件の失敗が他の残り110件の実行を止めない)。
//
// スタブ実装(全関数が「安全でない側の誤値」を返す)時点での「全赤」の定義(§3-4手順3):
//   refer/crisis/state/symptom系ケースは全件赤くなるべき(スタブは常にfalse/nullを返すため)。
//   normal/crisis-negative系は「該当なし」を期待するケースなので、スタブでも意図せず緑になる(偽緑)。
//   これは仕様どおりであり確認対象から除外する。

@Serializable
data class SafetyFixture(val input: String, val expect: String)

private fun loadSafetyFixtures(): List<SafetyFixture> {
    val stream = SafetyFixture::class.java.classLoader!!.getResourceAsStream("safety-fixtures.json")!!
    val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    return Json { ignoreUnknownKeys = true }.decodeFromString(text)
}

// 件数ガード(111件固定): fixtureファイルが壊れて0件やロード自体が失敗した場合に、
// Parameterizedのdata()が空リストを返して「0件だから全部緑」という偽の安全判定になるのを防ぐ。
class SafetyFixtureCountTest {
    @Test
    fun fixtureCountIs111() {
        assertEquals("safety-fixtures.jsonの件数が111でない", 111, loadSafetyFixtures().size)
    }
}

@RunWith(Parameterized::class)
class SafetyGateFixtureTest(private val fixture: SafetyFixture) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: [{0}]")
        fun data(): List<SafetyFixture> = loadSafetyFixtures()
    }

    // expectの意味(マスタープラン§3-4手順1):
    //   refer            → redFlagHit(norm(input)) == true
    //   normal           → redFlagHit(norm(input)) == false
    //   crisis           → crisisHit(norm(input)) == true
    //   crisis-negative  → crisisHit(norm(input)) == false
    //   state            → redFlagHit==true かつ redFlagKind=="state"
    //   symptom          → redFlagHit==true かつ redFlagKind=="symptom"
    @Test
    fun evaluatesExpectedVerdict() {
        val n = SafetyGate.norm(fixture.input)
        val ok = when (fixture.expect) {
            "refer" -> SafetyGate.redFlagHit(n)
            "normal" -> !SafetyGate.redFlagHit(n)
            "crisis" -> SafetyGate.crisisHit(n)
            "crisis-negative" -> !SafetyGate.crisisHit(n)
            "state" -> SafetyGate.redFlagHit(n) && SafetyGate.redFlagKind(n) == "state"
            "symptom" -> SafetyGate.redFlagHit(n) && SafetyGate.redFlagKind(n) == "symptom"
            else -> throw AssertionError("未知のexpect種別: ${fixture.expect}")
        }
        assertTrue("[${fixture.expect}] ${fixture.input}", ok)
    }
}
