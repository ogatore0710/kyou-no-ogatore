plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "jp.ogatore.kyouno"
    compileSdk = 34

    defaultConfig {
        applicationId = "jp.ogatore.kyouno"
        minSdk = 26
        targetSdk = 34
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
    testImplementation("org.robolectric:robolectric:4.13")
}
