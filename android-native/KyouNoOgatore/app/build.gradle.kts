// TASK build36 R-65: Gradle Kotlin DSLでは`java`がプロジェクト拡張に遮蔽されて
// `java.util.Properties`を直接参照できないため、ファイル先頭でimportする。
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// TASK build36 R-65: 署名資格情報の読み込み(androidブロックの外)。
val signingPropsFile = File(System.getProperty("user.home"), "Claude/ogatore-hub/secrets/android-signing.properties")
val signingProps = Properties().apply {
    if (signingPropsFile.exists()) signingPropsFile.inputStream().use { load(it) }
}

android {
    namespace = "jp.ogatore.kyouno"
    // TASK build36 R-64(Fable監査B-1・2026-08-07): Google Playの新規提出要件(API 35+)対応。
    // targetSdk 35はAndroid 15+でedge-to-edgeが強制されるが、本アプリはテーマの
    // statusBarColor方式のため、values-v35のwindowOptOutEdgeToEdgeEnforcementで
    // 現状の見た目を維持している(themes.xml参照・targetSdk 36までの技術負債)。
    compileSdk = 35

    defaultConfig {
        applicationId = "jp.ogatore.kyouno"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    // TASK build36 R-65(Fable監査B-2・2026-08-07): Play提出用のリリース署名(upload key)。
    // 鍵と資格情報はこのリポジトリに置かない(auto-syncで10分ごとに公開されるため)。
    // 保管場所はASC APIキーと同じ ~/Claude/ogatore-hub/secrets/(android-signing.properties+
    // ogatore-upload.keystore)。ファイルが無い環境(CI等)ではrelease署名なしにフォールバックし、
    // debugビルドには一切影響しない。鍵紛失時はPlay Consoleからアップロード鍵リセット申請可
    // (Play App Signing前提)。
    if (signingPropsFile.exists()) {
        signingConfigs {
            create("release") {
                storeFile = File(signingPropsFile.parentFile, signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            // minify(R8)はFable監査で「任意」判定のため現状維持(false)。挙動同一性を優先。
            isMinifyEnabled = false
            if (signingPropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")

    // GO-H1(ホーム画面ウィジェット・Duolingo式・本人GO 2026-07-28): Glance AppWidget。
    // 同一プロセス/パッケージ内なので、既存のfilesDir/kyono-store.jsonを直読みできる
    // (App Group相当の共有コンテナ新設は不要)。
    implementation("androidx.glance:glance-appwidget:1.1.0")

    // 安全系判定(SafetyGate/SafetyKB)のJSONデコード用。Android SDK同梱のorg.json(実機/インストゥルメンテッド
    // テスト専用のスタブでJVM単体テストでは動かない)と衝突するため使わず、kotlinx.serializationを使う。
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // ネイティブ移植 Step 2(マスタープラン§6 Step 2): エミュレータ不要のプレーンJVM JUnitユニットテスト。
    testImplementation("junit:junit:4.13.2")

    // ネイティブ移植 Step 4: CardRendererはandroid.graphics.Canvas/Bitmap(実機で実際にそのまま動く本物のAPI)
    // で実装する。プレーンJVM単体テストではandroid.jarのCanvas/Bitmapは中身の無いスタブで動かないため、
    // Robolectric(JVM上でAndroidフレームワークをシャドウ実装する定番テストライブラリ)でエミュレータ無しに
    // 実行する。Java2D/AWT代替実装で済ませてしまうと実機コードと別物になり後で丸ごと書き直しになるため、
    // 最初から実機と同じandroid.graphics APIをテストする方針にした。
    // R-64: 4.13はtargetSdk 35未対応(maxSdkVersion=34で全テストが構成エラー)のため、
    // SDK 35対応の4.14.1へ更新。
    testImplementation("org.robolectric:robolectric:4.14.1")

    // GO-H1(ホーム画面ウィジェット)・alan5指示の手段C: runGlanceAppWidgetUnitTestでComposeの
    // 実際の描画結果(どのImage/Textが出たか)までJVM単体テストで検証する。WidgetLogicTestが
    // 「どの状態が選ばれるか」を検証するのに対し、こちらは「その状態が実際に正しく描画されるか」
    // を検証する(実機配置の代替エビデンス)。
    testImplementation("androidx.glance:glance-appwidget-testing:1.1.0")
}
