# ☕ コーヒー帝国 (Coffee Empire) — Android版

Coffee Inc 2 にインスパイアされた、オリジナルのコーヒーチェーン経営シミュレーションゲームの Android アプリです。
(名称・アセット・コードはすべてオリジナルです)

## 📲 インストール

`dist/CoffeeEmpire-debug.apk` を Android 端末 (Android 5.0 / API 21 以上) に転送してタップするだけです。

- デバッグ署名のため、インストール時に「提供元不明のアプリ」の許可が必要です
- インターネット権限なし・完全オフラインで動作します
- セーブデータは端末内 (WebView の localStorage) に自動保存されます

## 🎮 ゲーム内容

資金 ¥5,000,000 を元手に、コーヒーチェーンを10店舗の「帝国」に育てます。

| 要素 | 内容 |
|---|---|
| 🏬 出店 | 駅前・オフィス街・商店街・大学前・住宅街・郊外モールの6立地。人通り/家賃/価格許容度/平日週末の傾向が異なる |
| 👥 スタッフ | 採用・削減・研修 (スキル)・店長雇用。足りないと行列で客を逃し評判が下がる |
| ☕ メニュー | 13商品 (ドリンク9+フード4)。価格は±10円単位で自由設定。高すぎると客離れ |
| 🔬 商品開発 | R&D 投資で新商品をアンロック |
| 🌱 豆の仕入れ | ロブスタ〜スペシャルティの4グレード。品質は満足度に、価格は原価に直結 |
| 📣 マーケティング | チラシ/SNS/インフルエンサー/TV CM でブランド認知度を上げる |
| 🏢 競合 | 2つの競合チェーンが徐々に出店し、商圏を奪い合う |
| 💰 財務 | 日次P/L・利益チャート・銀行借入 (限度1,000万・日利0.05%)。現金マイナス7日で倒産 |
| 🌦️ イベント | 天気 (猛暑はアイスが売れる等)・マシン故障・メディア掲載などのランダムイベント |
| 🏆 マイルストーン | 報酬付きの目標10種。10店舗達成でゲームクリア |

## 🛠️ ビルド方法

Android SDK は不要です。Java (JDK 11+) と curl があればビルドできます:

```bash
./build.sh
# => dist/CoffeeEmpire-debug.apk
```

スクリプトが以下を自動ダウンロードして使います:

| ツール | 役割 |
|---|---|
| [apktool](https://github.com/iBotPeaches/Apktool) | AndroidManifest/リソースのコンパイルと APK パッケージング |
| [dalvik-dx](https://mvnrepository.com/artifact/com.jakewharton.android.repackaged/dalvik-dx) | `.class` → `classes.dex` 変換 |
| [uber-apk-signer](https://github.com/patrickfav/uber-apk-signer) | zipalign + v1/v2/v3 署名 (デバッグ鍵) |
| android (stub jar) | `javac` 用のコンパイル時クラスパス |

## 📁 構成

```
coffee-empire-android/
├── src/MainActivity.java        # WebView をフルスクリーン表示するだけの薄いガワ
├── app/
│   ├── AndroidManifest.xml      # minSdk 21 / targetSdk 29
│   ├── apktool.yml
│   ├── res/                     # アプリ名・アイコン
│   └── assets/www/index.html    # ゲーム本体 (単一HTML・vanilla JS・依存なし)
├── build.sh                     # ビルドスクリプト
└── dist/CoffeeEmpire-debug.apk  # ビルド済みAPK
```

ゲームロジックはすべて `app/assets/www/index.html` にあり、PC のブラウザで直接開いてもプレイできます。
