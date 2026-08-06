package ci;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** データテーブルとモデル。ゲームのルールの土台。 */
public class Core {

    // =============== 書式 ===============
    public static String yen(double v) {
        long n = Math.round(v);
        String s = String.format("%,d", Math.abs(n));
        return (n < 0 ? "-¥" : "¥") + s;
    }
    public static String big(double v) {
        double a = Math.abs(v);
        if (a >= 1e12) return String.format("%s%.2f兆", v < 0 ? "-" : "", a / 1e12);
        if (a >= 1e8)  return String.format("%s%.2f億", v < 0 ? "-" : "", a / 1e8);
        if (a >= 1e4)  return String.format("%s%.0f万", v < 0 ? "-" : "", a / 1e4);
        return yen(v);
    }
    public static String pct(double v) { return String.format("%.1f%%", v * 100); }
    public static String stars(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 5; i++) b.append(i < n ? "★" : "☆");
        return b.toString();
    }
    public static float clamp(float v, float a, float b) { return v < a ? a : v > b ? b : v; }

    // =============== 客層 ===============
    public static class Seg {
        public final String name, emo; public final float sens, spend; public final int[] likes; public final int col;
        Seg(String n, String e, float s, float sp, int col, int... l) {
            name = n; emo = e; sens = s; spend = sp; this.col = col; likes = l;
        }
    }
    // likes は PRODUCTS のインデックス
    public static final Seg[] SEGS = {
        new Seg("学生",      "🎓", 1.75f, 0.80f, 0xFF7FB0C9, 2, 8, 9, 1),
        new Seg("ビジネス",  "💼", 0.62f, 1.32f, 0xFFD99A4E, 0, 4, 11, 1),
        new Seg("ファミリー","👨‍👩‍👧", 1.15f, 1.15f, 0xFF7FC97F, 12, 9, 3, 6),
        new Seg("シニア",    "👴", 1.05f, 0.95f, 0xFFC9A37F, 0, 3, 10),
        new Seg("感度層",    "✨", 0.78f, 1.38f, 0xFFC98AC9, 7, 8, 6, 5),
    };

    // =============== 商品 ===============
    public static class Prod {
        public final String name, emo; public final int type; // 0=ドリンク 1=フード
        public final int fair, baseCost; public final boolean bean; public final int temp; // 0none 1hot 2cold
        Prod(String n, String e, int t, int fair, int c, boolean bean, int temp) {
            name = n; emo = e; type = t; this.fair = fair; baseCost = c; this.bean = bean; this.temp = temp;
        }
    }
    public static final Prod[] PRODUCTS = {
        new Prod("ドリップコーヒー", "☕", 0, 320, 35, true, 1),
        new Prod("カフェラテ",       "🥛", 0, 420, 55, true, 1),
        new Prod("アイスコーヒー",   "🧊", 0, 350, 38, true, 2),
        new Prod("紅茶",             "🫖", 0, 340, 30, false, 1),
        new Prod("エスプレッソ",     "🤎", 0, 300, 30, true, 1),
        new Prod("カプチーノ",       "🌫", 0, 450, 55, true, 1),
        new Prod("カフェモカ",       "🍫", 0, 490, 70, true, 1),
        new Prod("抹茶ラテ",         "🍵", 0, 520, 75, false, 1),
        new Prod("フローズンラテ",   "🥤", 0, 580, 85, true, 2),
        new Prod("クッキー",         "🍪", 1, 200, 35, false, 0),
        new Prod("クロワッサン",     "🥐", 1, 280, 60, false, 0),
        new Prod("サンドイッチ",     "🥪", 1, 480, 120, false, 0),
        new Prod("チーズケーキ",     "🍰", 1, 520, 110, false, 0),
    };

    // =============== 業態 ===============
    public static class Concept {
        public final String name, emo, desc;
        public final float cap, build, rent, appeal, food;
        public final float[] segMul; // SEGS と同順
        Concept(String n, String e, float cap, float build, float rent, float appeal, float food,
                float[] seg, String d) {
            name = n; emo = e; this.cap = cap; this.build = build; this.rent = rent;
            this.appeal = appeal; this.food = food; segMul = seg; desc = d;
        }
    }
    public static final Concept[] CONCEPTS = {
        new Concept("スタンダード", "☕", 1.00f, 1.00f, 1.00f, 1.00f, 1.00f,
            new float[]{1,1,1,1,1}, "どの客層にも無難に対応できる基本形。"),
        new Concept("テイクアウト特化", "🥤", 1.50f, 0.75f, 0.70f, 0.90f, 0.55f,
            new float[]{1.20f,1.35f,0.70f,0.75f,1.00f}, "回転率が高く家賃も安い。滞在客には弱い。"),
        new Concept("大型ラウンジ", "🛋", 0.85f, 1.70f, 1.45f, 1.30f, 1.45f,
            new float[]{1.00f,0.85f,1.40f,1.35f,1.05f}, "ゆったり滞在。フードが強く家族と年配客に刺さる。"),
        new Concept("ドライブスルー", "🚗", 1.35f, 1.30f, 1.10f, 1.05f, 0.85f,
            new float[]{0.80f,1.10f,1.40f,1.10f,0.85f}, "車客を捕まえる。郊外の物件でのみ出店可。"),
        new Concept("ロースタリー旗艦店", "🏭", 0.90f, 2.60f, 1.30f, 1.75f, 1.20f,
            new float[]{0.85f,1.10f,1.00f,0.95f,1.65f}, "焙煎設備を備えた旗艦店。自社焙煎所が必要。"),
    };

    // =============== 設備(星で選ぶ) ===============
    public static class Equip {
        public final String cat, name; public final int stars, price; public final float cap, qual;
        Equip(String c, String n, int s, int p, float cap, float q) {
            cat = c; name = n; stars = s; price = p; this.cap = cap; qual = q;
        }
    }
    public static final String[] EQUIP_CATS = {"エスプレッソマシン", "コーヒーメーカー", "ブレンダー", "オーブン"};
    public static final Equip[][] EQUIPS = {
        { new Equip("エスプレッソマシン","入門機",1,180000,0.85f,0.85f),
          new Equip("エスプレッソマシン","標準機",2,420000,1.00f,1.00f),
          new Equip("エスプレッソマシン","業務用",3,950000,1.18f,1.12f),
          new Equip("エスプレッソマシン","高性能機",4,2100000,1.34f,1.24f),
          new Equip("エスプレッソマシン","最上位機",5,4800000,1.52f,1.38f) },
        { new Equip("コーヒーメーカー","簡易型",1,90000,0.88f,0.86f),
          new Equip("コーヒーメーカー","標準型",2,220000,1.00f,1.00f),
          new Equip("コーヒーメーカー","大容量型",3,520000,1.16f,1.10f),
          new Equip("コーヒーメーカー","全自動",4,1150000,1.30f,1.20f),
          new Equip("コーヒーメーカー","精密抽出",5,2600000,1.46f,1.34f) },
        { new Equip("ブレンダー","家庭用",1,40000,0.92f,0.88f),
          new Equip("ブレンダー","標準",2,110000,1.00f,1.00f),
          new Equip("ブレンダー","業務用",3,260000,1.12f,1.08f),
          new Equip("ブレンダー","静音高出力",4,560000,1.24f,1.16f),
          new Equip("ブレンダー","最上位",5,1250000,1.38f,1.26f) },
        { new Equip("オーブン","小型",1,70000,0.94f,0.90f),
          new Equip("オーブン","標準",2,180000,1.00f,1.00f),
          new Equip("オーブン","石窯風",3,430000,1.10f,1.10f),
          new Equip("オーブン","高性能",4,900000,1.20f,1.20f),
          new Equip("オーブン","最上位",5,1900000,1.32f,1.30f) },
    };

    // =============== 都市・地区・物件 ===============
    public static class City {
        public final String name; public final int idx;
        public float rentIdx, wageIdx, priceIdx;   // 都市ごとの物価
        public float awareness = 8;                // 都市別ブランド認知
        public boolean unlocked;
        public int entryCost;
        public City(int i, String n, float r, float w, float p, int cost, boolean un) {
            idx = i; name = n; rentIdx = r; wageIdx = w; priceIdx = p; entryCost = cost; unlocked = un;
        }
    }
    public static class Area {
        public final String name; public final int city; public final float[] seg; public final boolean suburb;
        public Area(String n, int c, float[] s, boolean sub) { name = n; city = c; seg = s; suburb = sub; }
    }
    public static class Prop {
        public final int area; public final String label;
        public final int foot;      // 1日の人通り
        public final int rent;      // 月額ではなく日額
        public final int seats;     // 最大席数
        public int owner = -1;      // -1空き 0自社 1..競合
        public Prop(int a, String l, int f, int r, int s) { area = a; label = l; foot = f; rent = r; seats = s; }
    }

    // =============== 焙煎会社(都市ごと) ===============
    public static class Roaster {
        public final String name; public final int city, stars, pricePerCup;
        public Roaster(String n, int c, int s, int p) { name = n; city = c; stars = s; pricePerCup = p; }
    }

    // =============== 銀行 ===============
    public static class Bank {
        public final String name; public final float spread; public final int term, minRating;
        public final boolean bullet; public final String desc;
        public Bank(String n, float sp, int t, int mr, boolean b, String d) {
            name = n; spread = sp; term = t; minRating = mr; bullet = b; desc = d;
        }
    }
    public static final String[] RATINGS = {"D","CCC","B","BB","BBB","A","AA","AAA"};
    public static final Bank[] BANKS = {
        new Bank("スタンダード銀行",   0.014f, 180, 0, false, "短期・小口。審査が緩く即日実行。"),
        new Bank("みなと信用金庫",     0.022f, 365, 1, false, "1年もの。金利は高いが枠が取りやすい。"),
        new Bank("第一都市銀行",       0.009f, 730, 3, false, "2年の設備資金。BB以上。"),
        new Bank("協調シンジケート団", 0.005f, 1095, 5, false, "銀行団の大型協調融資。A以上＋資産。"),
        new Bank("社債の発行",         0.007f, 1095, 4, true,  "3年後に一括償還。上場かつBBB以上。"),
    };

    // =============== 本社の部署と幹部 ===============
    public static class Dept {
        public final String name, emo, execTitle, deleg, desc;
        public final int cost, upkeep;
        public Dept(String n, String e, String t, String d, int c, int u, String desc) {
            name = n; emo = e; execTitle = t; deleg = d; cost = c; upkeep = u; this.desc = desc;
        }
    }
    /** 0=取締役室 1=人事部 2=経理部 3=商品開発部 4=宣伝部 5=技術部 */
    public static final Dept[] DEPTS = {
        new Dept("取締役室", "🏛", "CEO室", "経営", 8000000, 14000,
            "役員報酬と配当を決め、各幹部を招聘できるようになる。全部署の前提。"),
        new Dept("人事部", "📚", "CHRO", "採用と育成",  12000000, 20000,
            "店長の自動採用・研修・福利厚生。委任すると欠員を自動で埋める。"),
        new Dept("経理部", "🧮", "CFO", "財務",        15000000, 24000,
            "財務三表の閲覧、融資の管理と返済。委任すると資金繰りを自動化。"),
        new Dept("商品開発部", "🔬", "CPO", "商品",     20000000, 30000,
            "商品開発プロジェクトと全店の価格統一。委任すると自動で新商品を出す。"),
        new Dept("宣伝部", "📣", "CMO", "マーケ",       18000000, 28000,
            "都市別の広告出稿と市場シェアの把握。委任すると認知度を自動維持。"),
        new Dept("技術部", "💻", "CTO", "技術",         25000000, 36000,
            "会員プログラム・モバイル注文・自社アプリ。委任すると自動で改良。"),
    };

    // =============== 技術部のプロダクト ===============
    public static class Tech {
        public final String name, emo, desc; public final int cost, days;
        public Tech(String n, String e, int c, int d, String desc) {
            name = n; emo = e; cost = c; days = d; this.desc = desc;
        }
    }
    public static final Tech[] TECHS = {
        new Tech("POSシステム", "🖥", 8000000, 30, "本部の統制が効く。管理能力+2。"),
        new Tech("会員プログラム", "🎫", 22000000, 40, "常連が固定化し全店の集客+10%。"),
        new Tech("モバイル注文", "📱", 30000000, 45, "提供能力+16%。行列を減らす。"),
        new Tech("自社アプリ", "📲", 45000000, 55, "認知度の低下が半減。以後バージョンアップ可。"),
        new Tech("セルフレジ", "🤖", 35000000, 40, "店舗人件費-13%。"),
        new Tech("需要予測AI", "🧠", 90000000, 70, "人員が常に最適化され廃棄も減る。"),
    };

    // =============== 競合 ===============
    public static class Rival {
        public final String name, emo, desc; public final int person; public double cap; public float power;
        public boolean acquired; public int myShare; public int cd; public int defended;
        public Rival(String n, String e, float pw, double cap, int p, String d) {
            name = n; emo = e; power = pw; this.cap = cap; person = p; desc = d;
        }
    }

    // =============== 人物(店長・幹部) ===============
    public static class Person {
        public String name;
        public float hr, logic, product;   // 人事 / 論理 / 商品管理
        public int salary;
        public int assign = -1;            // -1=待機, 0..=店舗ID, -100-n=幹部ポストn
        public int days;                   // 勤続日数(成長)
        public float morale = 70;
        public int city = 0;
        public float avg() { return (hr + logic + product) / 3f; }
    }

    public static final String[] SURNAME = {"佐藤","鈴木","高橋","田中","伊藤","渡辺","山本","中村","小林",
        "加藤","吉田","山田","佐々木","山口","松本","井上","木村","林","斎藤","清水","阿部","森","池田","橋本"};
    public static final String[] GIVEN = {"健一","美咲","大輔","由紀","翔太","彩","拓也","恵","直樹","愛",
        "俊介","千夏","涼","麻衣","和也","沙織","悠斗","里奈","浩二","舞","将","詩織","隼人","真央"};

    public static Person newPerson(Random r, int city) {
        Person p = new Person();
        p.name = SURNAME[r.nextInt(SURNAME.length)] + " " + GIVEN[r.nextInt(GIVEN.length)];
        p.hr = 25 + r.nextInt(50) + (r.nextFloat() < 0.12f ? 18 : 0);
        p.logic = 25 + r.nextInt(50) + (r.nextFloat() < 0.12f ? 18 : 0);
        p.product = 25 + r.nextInt(50) + (r.nextFloat() < 0.12f ? 18 : 0);
        p.hr = clamp(p.hr, 20, 95); p.logic = clamp(p.logic, 20, 95); p.product = clamp(p.product, 20, 95);
        p.city = city;
        p.salary = (int) (3000 + p.avg() * 150);
        p.salary = p.salary / 100 * 100;
        return p;
    }

    // =============== 店舗 ===============
    public static class Store {
        public int id, prop;
        public int concept;
        public int[] equip = {1, 1, 0, 0};   // 各カテゴリの星-1 index
        public int interior = 1;             // 1..5
        public int staff = 2;
        public float skill = 25, morale = 70, rep = 3.0f;
        public int manager = -1;             // Person index
        public int autonomy = 0;             // 0=本部管理 1=一部委任 2=全面委任
        public int priceMode = 0;            // -1 低 / 0 標準 / +1 高
        public boolean ownedProp;
        public int closedDays;
        // 当日結果
        public int visitors, lost, potential;
        public double rev, profit, avgPrice;
        public float sat = 3, share;
        public float[] segMix = new float[SEGS.length];
        public List<Double> hist = new ArrayList<>();
        public String name = "";
    }

    // =============== 融資 ===============
    public static class Loan {
        public String name; public double principal, balance; public float rate;
        public int term, daysLeft; public boolean bullet; public int missed;
    }

    // =============== 商品開発プロジェクト ===============
    public static class DevProj {
        public String code; public int type;      // 0ドリンク 1フード
        public int targetPrice; public int weeks, weeklyBudget;
        public int daysLeft, totalDays; public double spent;
    }

    // =============== 開発済み商品 ===============
    public static class MenuItem {
        public int prodIdx; public int stars; public int price; public boolean on = true;
        public float fresh = 1f;
    }
}
