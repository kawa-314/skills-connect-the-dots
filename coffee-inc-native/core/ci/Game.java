package ci;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import ci.Core.*;

/** ゲーム状態とシミュレーション本体。UIから独立していてヘッドレスでも動く。 */
public class Game {
    public Random rnd = new Random(12345);

    // ---- 時間 ----
    public int day = 1;
    public int speed = 0;

    // ---- 世界 ----
    public List<City> cities = new ArrayList<>();
    public List<Area> areas = new ArrayList<>();
    public List<Prop> props = new ArrayList<>();
    public List<Roaster> roasters = new ArrayList<>();
    public List<Rival> rivals = new ArrayList<>();

    // ---- 会社 ----
    public double cash = 8000000;
    public double personal = 2000000;
    public List<Store> stores = new ArrayList<>();
    public List<Person> people = new ArrayList<>();     // 雇用済み
    public List<Person> market = new ArrayList<>();     // 人材市場
    public List<Loan> loans = new ArrayList<>();
    public List<MenuItem> menu = new ArrayList<>();
    public DevProj dev;
    public int[] roasterOf;                              // 都市ごとに契約中の焙煎会社index(-1未契約)
    public boolean[] deptOpen = new boolean[Core.DEPTS.length];
    public int[] deptExec = new int[Core.DEPTS.length];   // Person index / -1
    public boolean[] deleg = new boolean[Core.DEPTS.length];
    public boolean[] techDone = new boolean[Core.TECHS.length];
    public int techBuilding = -1, techDays = 0;
    public int appVersion = 0;
    public boolean roastery;                             // 自社焙煎所

    // ---- 財務 ----
    public String rating = "BB";
    public int missed;
    public boolean listed;
    public long shares = 0;                  // 発行済株式数
    public double ownerShare = 100;
    public double sharePrice = 0;
    public int quartersPositive;             // 上場条件: 3四半期連続の純利益プラス
    public double lastQuarterNet;
    public double policyRate = 0.02;         // 政策金利(年)
    public double inflation = 0.015;
    public double ceoSalary = 0;
    public double dividendRate = 0;

    // ---- 当日/累計 ----
    public double dRev, dCogs, dWage, dRent, dHq, dMkt, dInt, dDep, dNet, dPrincipal;
    public int dVisitors, dLost;
    public double totalRev, totalNet;
    public List<Double> profitHist = new ArrayList<>();
    public List<Double> valHist = new ArrayList<>();
    public List<String> log = new ArrayList<>();
    public double valuation = 20000000;

    // 貸借対照表用
    public double ppe;                        // 有形固定資産(簿価)
    public double accumDep;

    public boolean over; public String overReason = "";
    public int seq = 1;

    // ================= 世界生成 =================
    public Game() { build(); }

    private void addCity(String n, float rent, float wage, float price, int cost, boolean un) {
        cities.add(new City(cities.size(), n, rent, wage, price, cost, un));
    }
    private int addArea(String n, int city, float[] seg, boolean suburb) {
        areas.add(new Area(n, city, seg, suburb));
        return areas.size() - 1;
    }
    private void addProp(int area, String label, int foot, int rent, int seats) {
        props.add(new Prop(area, label, foot, rent, seats));
    }

    private void build() {
        // --- 都市 ---
        addCity("東京", 1.25f, 1.15f, 1.10f, 0, true);
        addCity("大阪", 1.00f, 1.00f, 1.00f, 250000000, false);
        addCity("福岡", 0.82f, 0.90f, 0.94f, 180000000, false);

        String[] an = {"駅前", "オフィス街", "商店街", "大学前", "住宅街", "郊外モール"};
        float[][] as = {
            {.15f,.35f,.20f,.10f,.20f}, {.05f,.70f,.05f,.05f,.15f}, {.15f,.15f,.30f,.30f,.10f},
            {.65f,.10f,.05f,.05f,.15f}, {.05f,.10f,.45f,.35f,.05f}, {.15f,.05f,.55f,.15f,.10f},
        };
        int[] baseFoot = {14000, 11000, 7000, 8000, 4200, 9000};
        int[] baseRent = {9000, 7000, 4800, 3600, 2600, 5600};
        int[] cnt = {4, 4, 5, 3, 6, 3};

        for (City c : cities) {
            for (int a = 0; a < an.length; a++) {
                int ai = addArea(an[a], c.idx, as[a], a >= 4);
                for (int k = 0; k < cnt[a]; k++) {
                    int foot = (int) (baseFoot[a] * (0.72f + rnd.nextFloat() * 0.62f) * (c.idx == 0 ? 1.15f : c.idx == 1 ? 1.0f : 0.86f));
                    int rent = (int) (baseRent[a] * (0.8f + rnd.nextFloat() * 0.5f) * c.rentIdx);
                    int seats = 20 + rnd.nextInt(60);
                    addProp(ai, an[a] + (k + 1) + "番地", foot / 10 * 10, rent / 100 * 100, seats);
                }
            }
        }

        // --- 焙煎会社(都市ごとに3社) ---
        String[][] rn = {
            {"江戸屋焙煎", "東京ロースターズ", "神田珈琲工房"},
            {"浪速ロースト", "大阪ビーンズ", "中之島珈琲"},
            {"博多焙煎所", "九州コーヒー商会", "天神ロースター"},
        };
        for (City c : cities) {
            roasters.add(new Roaster(rn[c.idx][0], c.idx, 1, 26));
            roasters.add(new Roaster(rn[c.idx][1], c.idx, 3, 52));
            roasters.add(new Roaster(rn[c.idx][2], c.idx, 5, 104));
        }
        roasterOf = new int[cities.size()];
        for (int i = 0; i < roasterOf.length; i++) roasterOf[i] = i * 3;   // 最安と契約

        // --- 競合 ---
        rivals.add(new Rival("ステラコーヒー", "🌟", 62, 10_000_000_000d, 0,
            "外資系の全国チェーン。資金力は圧倒的で業界の絶対王者。あなたが伸びれば必ず潰しに来る。"));
        rivals.add(new Rival("ドリップ&ゴー", "🚶", 40, 3_500_000_000d, 1,
            "テイクアウト特化の格安チェーン。追い詰められると全店値下げの価格戦争を仕掛ける。"));
        rivals.add(new Rival("珈琲豆善", "🫘", 32, 1_800_000_000d, 2,
            "昭和創業の老舗。腕利きの職人を抱え、優秀な人材を引き抜いてくる。"));
        rivals.add(new Rival("ねこまど珈琲", "🐱", 26, 800_000_000d, 3,
            "猫カフェ併設の新興チェーン。SNSでバズを起こしては認知度を奪っていく。"));
        for (Rival r : rivals) r.cd = 15 + rnd.nextInt(25);

        // 競合に物件を割り当て(東京の良物件を押さえている)
        List<Integer> byFoot = new ArrayList<>();
        for (int i = 0; i < props.size(); i++) if (areas.get(props.get(i).area).city == 0) byFoot.add(i);
        byFoot.sort((a, b) -> props.get(b).foot - props.get(a).foot);
        int[] give = {0, 0, 1, 0, 2, 1, 3, 1, 2, 0};
        for (int i = 0; i < give.length && i < byFoot.size(); i++) props.get(byFoot.get(i)).owner = give[i] + 1;

        // --- メニュー(初期3品) ---
        for (int i = 0; i < 3; i++) {
            MenuItem m = new MenuItem();
            m.prodIdx = i; m.stars = 2; m.price = Core.PRODUCTS[i].fair; m.on = true;
            menu.add(m);
        }
        for (int i = 0; i < deptExec.length; i++) deptExec[i] = -1;
        for (int i = 0; i < 6; i++) market.add(Core.newPerson(rnd, 0));
        log.add("1日 会社を設立しました。まずは物件を探して1号店を開きましょう。");
    }

    // ================= 便利アクセサ =================
    public City city(int i) { return cities.get(i); }
    public Area area(Prop p) { return areas.get(p.area); }
    public int cityOf(Prop p) { return areas.get(p.area).city; }
    public Prop prop(Store s) { return props.get(s.prop); }
    public Area areaOf(Store s) { return areas.get(prop(s).area); }
    public int cityOf(Store s) { return areaOf(s).city; }
    public Person mgr(Store s) { return s.manager >= 0 && s.manager < people.size() ? people.get(s.manager) : null; }
    public Person exec(int dept) { int i = deptExec[dept]; return i >= 0 && i < people.size() ? people.get(i) : null; }
    public boolean hasTech(int t) { return techDone[t]; }
    public int ratingIdx() { for (int i = 0; i < Core.RATINGS.length; i++) if (Core.RATINGS[i].equals(rating)) return i; return 2; }

    public void news(String s) { log.add(0, day + "日 " + s); if (log.size() > 80) log.remove(log.size() - 1); }

    public double avgProfit(int n) {
        if (profitHist.isEmpty()) return 0;
        int from = Math.max(0, profitHist.size() - n);
        double s = 0; int c = 0;
        for (int i = from; i < profitHist.size(); i++) { s += profitHist.get(i); c++; }
        return c == 0 ? 0 : s / c;
    }
    public double totalDebt() { double s = 0; for (Loan l : loans) s += l.balance; return s; }
    public double totalAssets() {
        double s = ppe - accumDep;
        for (Store st : stores) if (st.ownedProp) s += props.get(st.prop).rent * 2200;
        return s + cash;
    }
    public double equity() { return totalAssets() - totalDebt(); }

    // ================= 店舗の派生値 =================
    public float equipCap(Store s) {
        float m = 1;
        for (int i = 0; i < s.equip.length; i++) m *= (0.62f + Core.EQUIPS[i][s.equip[i]].cap * 0.38f);
        return m;
    }
    public float equipQual(Store s) {
        float q = 0;
        for (int i = 0; i < s.equip.length; i++) q += Core.EQUIPS[i][s.equip[i]].qual;
        return q / s.equip.length;
    }
    public float perStaff(Store s) {
        Person m = mgr(s);
        float base = 74 + s.skill * 0.9f;
        float mm = m != null ? 1 + m.logic / 300f : 0.90f;
        float tech = hasTech(2) ? 1.16f : 1f;
        return base * equipCap(s) * Core.CONCEPTS[s.concept].cap * mm * tech * menuComplexity();
    }
    /** 提供能力は人員と席数の両方で決まる(席数=物件の広さ) */
    public float capacity(Store s) {
        float byStaff = s.staff * perStaff(s);
        float bySeats = props.get(s.prop).seats * 13f * Core.CONCEPTS[s.concept].cap;
        return Math.min(byStaff, bySeats);
    }
    public float seatCap(Store s) { return props.get(s.prop).seats * 13f * Core.CONCEPTS[s.concept].cap; }
    public int idealStaff(Store s) {
        float per = Math.max(1, perStaff(s));
        float need = Math.min(Math.max(20, s.potential), seatCap(s));
        return (int) Core.clamp((float) Math.ceil(need / per), 1, 14);
    }
    public float storeAttract(Store s) {
        if (s.closedDays > 0) return 0;
        Person m = mgr(s);
        float a = 36 + s.interior * 11 + s.skill * 0.45f + s.rep * 12 + equipQual(s) * 16;
        if (m != null) a += 10 + m.product * 0.22f;
        a *= Core.CONCEPTS[s.concept].appeal;
        if (hasTech(1)) a *= 1.10f;
        if (appVersion > 0) a *= 1 + Math.min(0.15f, appVersion * 0.03f);
        return Math.max(1, a);
    }
    public float menuComplexity() {
        int on = 0; for (MenuItem m : menu) if (m.on) on++;
        return Core.clamp(1 - Math.max(0, on - 7) * 0.03f, 0.62f, 1f);
    }
    public double storeRent(Store s) {
        if (s.ownedProp) return 0;
        return props.get(s.prop).rent * Core.CONCEPTS[s.concept].rent * (1 + inflationDrift);
    }
    public double wageOf(Store s) {
        City c = cities.get(cityOf(s));
        double w = s.staff * 7900 * c.wageIdx * (1 + inflationDrift);
        if (hasTech(4)) w *= 0.87;
        return w;
    }
    public double inflationDrift = 0;

    public int menuPrice(MenuItem m, Store s) {
        double p = m.price;
        if (s != null) {
            if (s.priceMode > 0) p *= 1.15;
            else if (s.priceMode < 0) p *= 0.88;
            p *= cities.get(cityOf(s)).priceIdx;
        }
        return (int) Math.round(p);
    }

    public double beanCostPerCup(int cityIdx) {
        int ri = roasterOf[cityIdx];
        Roaster r = roasters.get(ri);
        double c = r.pricePerCup;
        if (roastery) c *= 0.72;
        return c;
    }
    public int beanStars(int cityIdx) {
        int st = roasters.get(roasterOf[cityIdx]).stars;
        if (roastery) st = Math.min(5, st + 1);
        return st;
    }

    // ================= 1日進める =================
    public void step() {
        if (over) return;
        macro();
        delegation();
        techProgress();
        devProgress();

        dRev = dCogs = dWage = dRent = dHq = dMkt = dInt = dDep = dNet = dPrincipal = 0;
        dVisitors = dLost = 0;

        // --- 需要計算: 物件ごと、客層ごと ---
        // まず地区単位で、その地区の全プレイヤー(自社+競合)の魅力を集計
        for (int ai = 0; ai < areas.size(); ai++) {
            Area ar = areas.get(ai);
            if (!cities.get(ar.city).unlocked) continue;
            List<Store> mine = new ArrayList<>();
            for (Store s : stores) if (props.get(s.prop).area == ai) mine.add(s);
            float rivalPull = 0;
            for (Prop p : props) if (p.area == ai && p.owner > 0) rivalPull += rivals.get(p.owner - 1).power;
            if (mine.isEmpty()) continue;

            City ct = cities.get(ar.city);
            float aw = 0.55f + (ct.awareness / 100f) * 1.0f;

            for (Store s : mine) {
                Prop pr = props.get(s.prop);
                float total = 0;
                double rev = 0, cogs = 0;
                int potential = 0;
                float[] mix = new float[Core.SEGS.length];
                double bean = beanCostPerCup(ar.city);
                int bstars = beanStars(ar.city);

                for (int gi = 0; gi < Core.SEGS.length; gi++) {
                    Seg g = Core.SEGS[gi];
                    float share = ar.seg[gi];
                    if (share <= 0) continue;
                    // その物件の人通りのうち、この客層のぶん
                    float pool = pr.foot * share * 0.090f * aw;   // 人通りの約9%が来店候補
                    // 魅力: 自店 vs 同地区の自店・競合
                    float mypull = storeAttract(s) * Core.CONCEPTS[s.concept].segMul[gi] * menuFit(gi, s, bstars);
                    float others = 0;
                    for (Store o : mine) if (o != s)
                        others += storeAttract(o) * Core.CONCEPTS[o.concept].segMul[gi] * menuFit(gi, o, bstars);
                    float denom = mypull + others + rivalPull * 0.9f + 22;
                    float got = pool * (mypull / Math.max(1, denom));
                    mix[gi] = got;
                    potential += got;
                    total += got;
                }
                s.potential = potential;
                if (s.closedDays > 0) {
                    s.closedDays--;
                    s.visitors = 0; s.lost = 0; s.rev = 0;
                    s.profit = -(storeRent(s) + wageOf(s));
                    dRent += storeRent(s); dWage += wageOf(s);
                    pushHist(s);
                    continue;
                }
                int cap = (int) capacity(s);
                int served = (int) Math.min(potential, cap);
                int lost = Math.max(0, potential - served);
                float ratio = potential > 0 ? (float) served / potential : 0;

                // 客層ごとの単価と原価
                double sumRev = 0, sumCogs = 0, sumCnt = 0;
                for (int gi = 0; gi < Core.SEGS.length; gi++) {
                    double n = mix[gi] * ratio;
                    if (n <= 0) continue;
                    double[] pc = priceCostFor(gi, s, bean);
                    sumRev += n * pc[0];
                    sumCogs += n * pc[1];
                    sumCnt += n;
                }
                for (int gi = 0; gi < mix.length; gi++) s.segMix[gi] = sumCnt > 0 ? (float) (mix[gi] * ratio / sumCnt) : 0;
                rev = sumRev; cogs = sumCogs;

                double wage = wageOf(s), rent = storeRent(s);
                s.visitors = served; s.lost = lost; s.rev = rev;
                s.avgPrice = served > 0 ? rev / served : 0;
                s.profit = rev - cogs - wage - rent;
                s.share = potential > 0 ? (float) served / Math.max(1, pr.foot * 0.090f) : 0;

                // 満足度
                Person m = mgr(s);
                float sat = 1.85f + bstars * 0.30f + s.skill / 100f + s.interior * 0.10f + equipQual(s) * 0.55f;
                sat += (s.morale - 60) / 160f;
                if (m != null) sat += 0.12f + m.hr / 260f;
                if (potential > 0) sat -= Math.min(0.9f, (float) lost / potential * 1.2f);
                sat -= priceStress(s) * 0.9f;
                sat = Core.clamp(sat, 1, 5);
                s.sat = sat;
                s.rep = s.rep * 0.92f + sat * 0.08f;

                // 士気と離職
                float util = cap > 0 ? (float) served / cap : 0;
                float dm = (m != null ? 0.55f : -0.2f) + (util > 0.97f ? -1.6f : util > 0.88f ? -0.3f : 0.6f);
                if (deptOpen[1]) dm += 0.35f;                 // 人事部の福利厚生
                s.morale = Core.clamp(s.morale + dm, 0, 100);
                if (s.morale < 30 && s.staff > 1 && rnd.nextFloat() < 0.06f) {
                    s.staff--; s.morale += 8;
                    news("😞 " + s.name + "でスタッフが1人退職しました");
                }

                dRev += rev; dCogs += cogs; dWage += wage; dRent += rent;
                dVisitors += served; dLost += lost;
                pushHist(s);
            }
        }

        // --- ブランド認知: 店舗数に応じた自然水準へ回帰 ---
        for (City c : cities) {
            if (!c.unlocked) continue;
            int n = 0;
            for (Store s : stores) if (cityOf(s) == c.idx) n++;
            float base = 10 + n * 2.4f + (appVersion > 0 ? 4 : 0) + (hasTech(1) ? 5 : 0);
            base = Math.min(base, 62);
            float k = (hasTech(3) ? 0.06f : 0.035f);
            if (c.awareness < base) c.awareness += (base - c.awareness) * k;
            else c.awareness -= Math.min(0.25f, (c.awareness - base) * 0.006f);
            c.awareness = Core.clamp(c.awareness, 4, 100);
        }

        // --- 店長の成長 ---
        for (Person p : people) {
            p.days++;
            if (p.days % 12 == 0) {
                p.hr = Core.clamp(p.hr + 0.6f, 0, 99);
                p.logic = Core.clamp(p.logic + 0.6f, 0, 99);
                p.product = Core.clamp(p.product + 0.7f, 0, 99);
            }
        }
        // 人材市場の入れ替え
        if (day % 4 == 0 && !market.isEmpty()) { market.remove(0); }
        while (market.size() < 6) market.add(Core.newPerson(rnd, 0));

        // --- 本社コスト ---
        double hq = 0;
        for (int i = 0; i < deptOpen.length; i++) if (deptOpen[i]) hq += Core.DEPTS[i].upkeep;
        for (Person p : people) hq += p.assign <= -100 ? p.salary * 1.8 : p.salary;
        hq += Math.pow(stores.size(), 1.25) * 2600;
        hq *= (1 + inflationDrift);
        if (hasTech(0)) hq *= 0.95;
        dHq = hq;
        dMkt = pendingMkt; pendingMkt = 0;

        // --- 減価償却 ---
        double dep = ppe * 0.00035;
        accumDep += dep; dDep = dep;

        // --- 借入 ---
        processLoans();

        double ceo = ceoSalary;
        personal += ceo;
        dNet = dRev - dCogs - dWage - dRent - dHq - dMkt - dInt - dDep - ceo;
        cash += dNet + dDep;                 // 減価償却は非現金支出
        cash -= dPrincipal;
        totalRev += dRev; totalNet += dNet;
        profitHist.add(dNet);
        if (profitHist.size() > 120) profitHist.remove(0);

        rating = calcRating();
        valuation = calcValuation();
        valHist.add(valuation);
        if (valHist.size() > 120) valHist.remove(0);
        if (listed && shares > 0) sharePrice = valuation / shares;

        // 四半期(90日)ごとに上場条件を判定
        if (day % 90 == 0) {
            double q = 0;
            int from = Math.max(0, profitHist.size() - 90);
            for (int i = from; i < profitHist.size(); i++) q += profitHist.get(i);
            lastQuarterNet = q;
            if (q > 0) quartersPositive++; else quartersPositive = 0;
            news((q > 0 ? "📈 " : "📉 ") + "第" + (day / 90) + "四半期の純利益は " + Core.big(q)
                + (q > 0 ? "(黒字" + quartersPositive + "期連続)" : "(赤字)"));
        }

        rivalAi();

        if (cash < 0) {
            negDays++;
            if (negDays == 1) news("⚠️ 現金がマイナスです。7日以内に立て直さないと倒産します。");
            if (negDays >= 7) { over = true; overReason = "資金繰りがつかず倒産しました。"; return; }
        } else negDays = 0;

        day++;
    }
    public int negDays;

    private void pushHist(Store s) {
        s.hist.add(s.profit);
        if (s.hist.size() > 21) s.hist.remove(0);
    }

    /** 客層 gi にとってのメニュー適合度 */
    private float menuFit(int gi, Store s, int beanStars) {
        Seg g = Core.SEGS[gi];
        float w = 0;
        for (MenuItem m : menu) {
            if (!m.on) continue;
            Prod p = Core.PRODUCTS[m.prodIdx];
            float a = 6 + m.stars * 2.2f;
            for (int L : g.likes) if (L == m.prodIdx) { a *= 1.6f; break; }
            if (p.type == 1) a *= Core.CONCEPTS[s.concept].food;
            if (p.bean) a *= 0.82f + beanStars * 0.06f;
            float ratio = menuPrice(m, s) / (float) p.fair;
            a *= (float) Math.max(0.06, Math.pow(1 / Math.max(ratio, 0.5f), 1.4 * g.sens));
            w += a;
        }
        return Core.clamp(w / 34f, 0.2f, 1.8f);
    }

    /** 客層 gi の平均客単価と原価 */
    private double[] priceCostFor(int gi, Store s, double beanCost) {
        Seg g = Core.SEGS[gi];
        double sw = 0, sp = 0, sc = 0;
        for (MenuItem m : menu) {
            if (!m.on) continue;
            Prod p = Core.PRODUCTS[m.prodIdx];
            float a = 6 + m.stars * 2.2f;
            for (int L : g.likes) if (L == m.prodIdx) { a *= 1.6f; break; }
            if (p.type == 1) a *= Core.CONCEPTS[s.concept].food;
            int pr = menuPrice(m, s);
            float ratio = pr / (float) p.fair;
            a *= (float) Math.max(0.06, Math.pow(1 / Math.max(ratio, 0.5f), 1.4 * g.sens));
            double cost = p.baseCost * (1 + inflationDrift) + (p.bean ? beanCost : 0);
            sw += a; sp += a * pr; sc += a * cost;
        }
        if (sw <= 0) return new double[]{0, 0};
        double waste = 1 + Math.max(0, countOn() - 7) * 0.02;
        if (hasTech(5)) waste = 1 + (waste - 1) * 0.5;
        return new double[]{ sp / sw * g.spend, sc / sw * waste };
    }
    public int countOn() { int n = 0; for (MenuItem m : menu) if (m.on) n++; return n; }

    /** 価格が客層の許容を超えている度合い(満足度に効く) */
    private float priceStress(Store s) {
        float st = 0; int n = 0;
        for (MenuItem m : menu) {
            if (!m.on) continue;
            float r = menuPrice(m, s) / (float) Core.PRODUCTS[m.prodIdx].fair;
            if (r > 1.2f) { st += Math.min(0.6f, r - 1.2f); n++; }
        }
        return n > 0 ? st / n : 0;
    }

    // ================= マクロ経済 =================
    private void macro() {
        if (day % 30 == 0) {
            policyRate = Math.max(0, Math.min(0.09, policyRate + (rnd.nextDouble() - 0.48) * 0.006));
            inflation = Math.max(-0.01, Math.min(0.09, inflation + (rnd.nextDouble() - 0.5) * 0.004));
            if (rnd.nextFloat() < 0.35f) {
                news(String.format("🏛 中央銀行が政策金利を %.2f%% に、インフレ率は %.1f%% です。",
                    policyRate * 100, inflation * 100));
            }
        }
        inflationDrift += inflation / 365.0;
    }

    // ================= 委任 =================
    private void delegation() {
        // 人事部: 欠員の自動補充と研修
        if (deleg[1] && exec(1) != null) {
            for (Store s : stores) {
                if (s.manager < 0 && cash > 4000000 && !market.isEmpty()) {
                    Person best = null; int bi = -1;
                    for (int i = 0; i < market.size(); i++)
                        if (best == null || market.get(i).product > best.product) { best = market.get(i); bi = i; }
                    if (best != null && cash > best.salary * 8) { hireFromMarket(bi, s.id); }
                }
            }
            Store weak = null;
            for (Store s : stores) if (weak == null || s.skill < weak.skill) weak = s;
            if (weak != null && weak.skill < 78 && cash > 3000000) { cash -= 90000; weak.skill = Math.min(100, weak.skill + 6); }
        }
        // 経理部: 余剰資金で繰上返済
        if (deleg[2] && exec(2) != null) {
            if (cash > 60000000 && !loans.isEmpty()) {
                Loan l = loans.get(0);
                double amt = Math.min(l.balance, cash * 0.3);
                cash -= amt; l.balance -= amt;
                if (l.balance < 1) loans.remove(l);
            }
        }
        // 商品開発部: 自動で開発
        if (deleg[3] && exec(3) != null && dev == null && menu.size() < Core.PRODUCTS.length && cash > 40000000) {
            startDev("AUTO-" + (menu.size() + 1), nextUndeveloped(), 8, 2000000);
        }
        // 宣伝部: 認知度維持
        if (deleg[4] && exec(4) != null) {
            for (City c : cities) {
                if (!c.unlocked) continue;
                if (c.awareness < 42 && cash > 20000000) {
                    double sp = 3000000;
                    pendingMkt += sp;
                    c.awareness = Math.min(100, c.awareness + 3.2f);
                }
            }
        }
        // 技術部: 自動で技術投資
        if (deleg[5] && exec(5) != null && techBuilding < 0) {
            for (int i = 0; i < Core.TECHS.length; i++)
                if (!techDone[i] && cash > Core.TECHS[i].cost * 2.5) { startTech(i); break; }
        }
        // 運営(取締役室): 人員の自動最適化。裁量に応じて店長が動かす
        for (Store s : stores) {
            if (s.autonomy >= 1 || (deleg[0] && exec(0) != null)) {
                int t = idealStaff(s);
                if (s.staff < t) s.staff++;
                else if (s.staff > t) s.staff--;
            }
        }
    }
    public int nextUndeveloped() {
        boolean[] have = new boolean[Core.PRODUCTS.length];
        for (MenuItem m : menu) have[m.prodIdx] = true;
        for (int i = 0; i < have.length; i++) if (!have[i]) return i;
        return -1;
    }

    // ================= 技術/開発 =================
    public void startTech(int i) {
        if (techBuilding >= 0 || techDone[i] || cash < Core.TECHS[i].cost) return;
        cash -= Core.TECHS[i].cost;
        ppe += Core.TECHS[i].cost * 0.6;
        techBuilding = i; techDays = Core.TECHS[i].days;
        news("💻 " + Core.TECHS[i].name + " の開発に着手しました(" + Core.TECHS[i].days + "日)");
    }
    private void techProgress() {
        if (techBuilding < 0) return;
        techDays--;
        if (techDays <= 0) {
            techDone[techBuilding] = true;
            if (techBuilding == 3) appVersion = 1;
            news("✅ " + Core.TECHS[techBuilding].name + " が完成しました。" + Core.TECHS[techBuilding].desc);
            techBuilding = -1;
        }
    }
    public void upgradeApp() {
        if (!techDone[3]) return;
        double c = 12000000 * (appVersion);
        if (cash < c) return;
        cash -= c; appVersion++;
        news("📲 自社アプリを v" + appVersion + ".0 に更新しました。利便性が上がっています。");
    }

    public void startDev(String code, int prodIdx, int weeks, int weeklyBudget) {
        if (dev != null || prodIdx < 0) return;
        dev = new DevProj();
        dev.code = code; dev.type = Core.PRODUCTS[prodIdx].type;
        dev.targetPrice = Core.PRODUCTS[prodIdx].fair;
        dev.weeks = weeks; dev.weeklyBudget = weeklyBudget;
        dev.daysLeft = weeks * 7; dev.totalDays = weeks * 7;
        devProd = prodIdx;
        news("🔬 商品開発「" + code + "」に着手(" + weeks + "週 / 週" + Core.big(weeklyBudget) + ")");
    }
    public int devProd = -1;
    private void devProgress() {
        if (dev == null) return;
        double daily = dev.weeklyBudget / 7.0;
        if (cash < daily) { news("💸 開発費が払えず、商品開発が中止されました。"); dev = null; return; }
        cash -= daily; dev.spent += daily;
        dev.daysLeft--;
        if (dev.daysLeft <= 0) {
            // 星の判定: 投入総額と期間から
            double q = dev.spent / 3_000_000.0 + dev.totalDays / 60.0;
            int stars = (int) Core.clamp((float) (1 + q * 1.15), 1, 5);
            MenuItem m = new MenuItem();
            m.prodIdx = devProd; m.stars = stars;
            m.price = (int) (Core.PRODUCTS[devProd].fair * (0.9 + stars * 0.05));
            menu.add(m);
            news("🎉 新商品「" + Core.PRODUCTS[devProd].name + "」が完成! 品質 " + Core.stars(stars));
            dev = null; devProd = -1;
        }
    }

    // ================= 融資 =================
    public double bankRate(Bank b) {
        double base = policyRate / 365.0 + b.spread / 365.0;
        double mult = 1.0 + (4 - ratingIdx()) * 0.16;
        return Math.max(0.00005, base * Math.max(0.5, mult));
    }
    public double bankCap(Bank b) {
        double avg = avgProfit(30);
        double assets = totalAssets();
        double base;
        switch (b.minRating) {
            case 0: base = Math.max(4000000, avg * 90); break;
            case 1: base = Math.max(2000000, avg * 150 + assets * 0.06); break;
            case 3: base = Math.max(0, avg * 260 + assets * 0.22); break;
            case 5: base = Math.max(0, avg * 480 + assets * 0.5); break;
            default: base = Math.max(0, valuation * 0.2); break;
        }
        double already = 0;
        for (Loan l : loans) if (l.name.equals(b.name)) already += l.balance;
        return Math.max(0, base - already);
    }
    public boolean canBorrow(Bank b) {
        if (ratingIdx() < b.minRating) return false;
        if (b.bullet && !listed) return false;
        return bankCap(b) >= 500000;
    }
    public void borrow(Bank b, double amt) {
        if (!canBorrow(b)) return;
        amt = Math.min(amt, bankCap(b));
        amt = Math.floor(amt / 100000) * 100000;
        if (amt < 500000) return;
        Loan l = new Loan();
        l.name = b.name; l.principal = amt; l.balance = amt; l.rate = (float) bankRate(b);
        l.term = b.term; l.daysLeft = b.term; l.bullet = b.bullet;
        loans.add(l);
        cash += amt;
        shares += (long) (amt / 1000);      // 融資実績が資本政策の下地になる
        news("🏦 " + b.name + " から " + Core.big(amt) + " を調達(金利 "
            + String.format("%.3f", l.rate * 100) + "%/日・" + b.term + "日)");
    }
    private void processLoans() {
        List<Loan> done = new ArrayList<>();
        for (Loan l : loans) {
            double in = l.balance * l.rate;
            dInt += in;
            l.daysLeft--;
            if (l.bullet) {
                if (l.daysLeft <= 0) {
                    if (cash >= l.balance) { dPrincipal += l.balance; l.balance = 0; done.add(l); news("📜 社債を満期償還しました"); }
                    else { missed++; l.missed++; l.daysLeft = 30; news("🚨 社債の償還ができません!"); }
                }
            } else {
                double due = l.principal / l.term;
                if (cash >= due + in) { dPrincipal += due; l.balance -= due; }
                else { missed++; l.missed++; if (l.missed == 1) news("🚨 " + l.name + " の返済が滞りました"); }
                if (l.balance <= 1) done.add(l);
            }
        }
        loans.removeAll(done);
        if (missed >= 25) { over = true; overReason = "返済不能に陥り、銀行団により会社は清算されました。"; }
    }
    private String calcRating() {
        double avg = avgProfit(30), debt = totalDebt(), assets = Math.max(1, totalAssets());
        double di = 0; for (Loan l : loans) di += l.balance * l.rate;
        double sc = 3.2;
        sc += Core.clamp((float) (avg / 150000), -2.5f, 2.5f);
        sc -= Core.clamp((float) (debt / assets * 2.6), 0, 3);
        sc += debt > 0 ? Core.clamp((float) (avg / Math.max(1, di) / 9), 0, 1.5f) : 0.8;
        sc += listed ? 0.4 : 0;
        sc += Core.clamp(stores.size() * 0.05f, 0, 0.7f);
        sc -= missed * 0.5;
        int i = (int) Math.round(sc);
        return Core.RATINGS[Math.max(0, Math.min(7, i))];
    }
    private double calcValuation() {
        double avg = avgProfit(30);
        double per = 12 + repAvg() * 1.6 + awAvg() / 22;
        double earn = Math.max(0, avg) * 365 * per;
        double a = Math.max(0, totalAssets() - totalDebt());
        double st = 0;
        for (Rival r : rivals) if (!r.acquired) st += r.cap * r.myShare / 100.0;
        return Math.max(5000000, earn + a * 0.9 + stores.size() * 9000000 + st);
    }
    public float repAvg() {
        if (stores.isEmpty()) return 3;
        float s = 0; for (Store x : stores) s += x.rep; return s / stores.size();
    }
    public float awAvg() {
        float s = 0; int n = 0;
        for (City c : cities) if (c.unlocked) { s += c.awareness; n++; }
        return n == 0 ? 0 : s / n;
    }

    // ================= 上場 =================
    public boolean canIpo() { return !listed && quartersPositive >= 3 && shares >= 300000; }
    public void ipo() {
        if (!canIpo()) return;
        listed = true;
        double raise = valuation * 0.28;
        cash += raise;
        ownerShare = 68;
        shares += (long) (shares * 0.32);
        news("🔔 株式を公開しました! 公募により " + Core.big(raise) + " を調達");
    }
    public void secondaryOffering() {
        if (!listed || ownerShare < 12) return;
        double raise = valuation * 0.06;
        cash += raise;
        ownerShare -= 5;
        shares += (long) (shares * 0.06);
        news("📈 公募増資で " + Core.big(raise) + " を調達(持株 " + Math.round(ownerShare) + "%)");
    }

    // ================= 店舗操作 =================
    public double buildCost(Prop p, int concept) {
        return Math.round((1200000 + p.rent * 110 + p.seats * 22000) * Core.CONCEPTS[concept].build);
    }
    public boolean openStore(int propIdx, int concept) {
        Prop p = props.get(propIdx);
        if (p.owner != -1) return false;
        Concept c = Core.CONCEPTS[concept];
        if (concept == 3 && !areas.get(p.area).suburb) return false;
        if (concept == 4 && !roastery) return false;
        double cost = buildCost(p, concept);
        if (cash < cost) return false;
        cash -= cost; ppe += cost * 0.8;
        Store s = new Store();
        s.id = seq++; s.prop = propIdx; s.concept = concept;
        s.name = areas.get(p.area).name + "・" + p.label;
        stores.add(s);
        p.owner = 0;
        news("🎊 " + s.name + " を " + c.emo + c.name + " として開店しました");
        return true;
    }
    public void closeStore(Store s) {
        props.get(s.prop).owner = -1;
        Person m = mgr(s);
        if (m != null) m.assign = -1;
        stores.remove(s);
        cash += 900000;
        news("🚪 " + s.name + " を閉店しました");
    }

    public void hireFromMarket(int marketIdx, int storeId) {
        if (marketIdx < 0 || marketIdx >= market.size()) return;
        Person p = market.get(marketIdx);
        double fee = p.salary * 8;
        if (cash < fee) return;
        cash -= fee;
        market.remove(marketIdx);
        people.add(p);
        int pi = people.size() - 1;
        if (storeId >= 0) {
            for (Store s : stores) if (s.id == storeId) { s.manager = pi; p.assign = s.id; }
        }
        news("🧑‍💼 " + p.name + " を採用しました(支度金 " + Core.big(fee) + ")");
    }
    public void assignToStore(int personIdx, Store s) {
        Person p = people.get(personIdx);
        for (Store o : stores) if (o.manager == personIdx) o.manager = -1;
        for (int i = 0; i < deptExec.length; i++) if (deptExec[i] == personIdx) deptExec[i] = -1;
        if (s.manager >= 0) people.get(s.manager).assign = -1;
        s.manager = personIdx; p.assign = s.id;
    }
    public void assignExec(int personIdx, int dept) {
        Person p = people.get(personIdx);
        for (Store o : stores) if (o.manager == personIdx) o.manager = -1;
        for (int i = 0; i < deptExec.length; i++) if (deptExec[i] == personIdx) deptExec[i] = -1;
        deptExec[dept] = personIdx; p.assign = -100 - dept;
    }

    public void openDept(int i) {
        if (deptOpen[i] || cash < Core.DEPTS[i].cost) return;
        if (i != 0 && !deptOpen[0]) return;
        cash -= Core.DEPTS[i].cost;
        ppe += Core.DEPTS[i].cost * 0.5;
        deptOpen[i] = true;
        news("🏛 " + Core.DEPTS[i].name + " を開設しました");
    }

    public void enterCity(int idx) {
        City c = cities.get(idx);
        if (c.unlocked || cash < c.entryCost) return;
        cash -= c.entryCost; c.unlocked = true;
        news("🌆 " + c.name + " への進出を決定しました");
    }

    public void buildRoastery() {
        if (roastery || cash < 120000000) return;
        cash -= 120000000; ppe += 120000000 * 0.9;
        roastery = true;
        news("🏭 自社焙煎所を建設しました。豆の原価が下がり、品質も1段階上がります。");
    }

    public void marketing(int cityIdx, int tier) {
        double[] cost = {2000000, 8000000, 30000000};
        double[] gain = {4, 12, 34};
        if (cash < cost[tier]) return;
        pendingMkt += cost[tier];
        City c = cities.get(cityIdx);
        c.awareness = Math.min(100, c.awareness + (float) gain[tier]);
        news("📣 " + c.name + " で広告を出稿しました(認知度 +" + (int) gain[tier] + ")");
    }

    // ================= 一括操作(地域単位) =================
    /** scope: 0=全社 1=都市 2=地区 */
    public List<Store> scopeStores(int scope, int idx) {
        List<Store> out = new ArrayList<>();
        for (Store s : stores) {
            if (scope == 0) out.add(s);
            else if (scope == 1 && cityOf(s) == idx) out.add(s);
            else if (scope == 2 && props.get(s.prop).area == idx) out.add(s);
        }
        return out;
    }
    public void bulkPriceMode(List<Store> ls, int mode) { for (Store s : ls) s.priceMode = mode; }
    public void bulkAutonomy(List<Store> ls, int a) { for (Store s : ls) s.autonomy = a; }
    public void bulkOptStaff(List<Store> ls) { for (Store s : ls) s.staff = idealStaff(s); }
    public double bulkInteriorCost(List<Store> ls, int lv) {
        double c = 0;
        for (Store s : ls) if (s.interior < lv) c += (lv - s.interior) * 420000;
        return c;
    }
    public void bulkInterior(List<Store> ls, int lv) {
        double c = bulkInteriorCost(ls, lv);
        if (cash < c) return;
        cash -= c; ppe += c * 0.8;
        for (Store s : ls) if (s.interior < lv) s.interior = lv;
        news("🛋 " + ls.size() + "店舗の内装をレベル" + lv + "に統一しました(" + Core.big(c) + ")");
    }
    public double bulkEquipCost(List<Store> ls, int cat, int starIdx) {
        double c = 0;
        for (Store s : ls) if (s.equip[cat] < starIdx) c += Core.EQUIPS[cat][starIdx].price;
        return c;
    }
    public void bulkEquip(List<Store> ls, int cat, int starIdx) {
        double c = bulkEquipCost(ls, cat, starIdx);
        if (cash < c) return;
        cash -= c; ppe += c * 0.85;
        for (Store s : ls) if (s.equip[cat] < starIdx) s.equip[cat] = starIdx;
        news("⚙️ " + ls.size() + "店舗の" + Core.EQUIP_CATS[cat] + "を"
            + Core.EQUIPS[cat][starIdx].name + "に統一しました(" + Core.big(c) + ")");
    }
    /** 全店の価格を一括で適正価格×倍率に設定 */
    public void bulkMenuPrice(float mul) {
        for (MenuItem m : menu) m.price = Math.round(Core.PRODUCTS[m.prodIdx].fair * mul);
        news("💴 全商品の価格を適正価格の" + Math.round(mul * 100) + "%に統一しました");
    }

    // ================= 競合AI =================
    private void rivalAi() {
        for (Rival r : rivals) {
            if (r.acquired) continue;
            r.cap *= 1 + (rnd.nextDouble() - 0.48) * 0.008;
            r.cd--;
            if (r.cd > 0) continue;
            r.cd = 45 + rnd.nextInt(45);
            if (r.person == 0) {
                // 資本力: プレイヤーの主力地区の空き物件を押さえる
                Prop best = null;
                for (Prop p : props) {
                    if (p.owner != -1) continue;
                    if (!cities.get(cityOf(p)).unlocked) continue;
                    boolean mineHere = false;
                    for (Store s : stores) if (props.get(s.prop).area == p.area) { mineHere = true; break; }
                    if (!mineHere) continue;
                    if (best == null || p.foot > best.foot) best = p;
                }
                if (best != null) {
                    best.owner = 1;
                    news("🌟 " + r.name + " が " + areas.get(best.area).name + " の好物件を押さえました");
                } else {
                    for (City c : cities) if (c.unlocked) c.awareness = Math.max(6, c.awareness - 2.0f);
                    news("🌟 " + r.name + " が大規模な広告攻勢。認知度が奪われました");
                }
            } else if (r.person == 1) {
                priceWar = 30;
                news("⚔️ " + r.name + " が全店値下げ。価格競争が始まりました(30日)");
            } else if (r.person == 2) {
                List<Integer> mgrs = new ArrayList<>();
                for (int i = 0; i < people.size(); i++) if (people.get(i).assign >= 0) mgrs.add(i);
                if (!mgrs.isEmpty()) {
                    int pick = mgrs.get(rnd.nextInt(mgrs.size()));
                    Person p = people.get(pick);
                    for (Store s : stores) if (s.manager == pick) s.manager = -1;
                    news("🫘 " + r.name + " に店長「" + p.name + "」が引き抜かれました");
                    people.remove(pick);
                    for (Store s : stores) if (s.manager > pick) s.manager--;
                    for (int i = 0; i < deptExec.length; i++) if (deptExec[i] > pick) deptExec[i]--;
                }
            } else {
                for (City c : cities) if (c.unlocked) c.awareness = Math.max(6, c.awareness - 2.5f);
                news("🐱 " + r.name + " のSNS企画がバズり、認知度が奪われました");
            }
        }
        if (priceWar > 0) priceWar--;
    }
    public int priceWar;
    public double pendingMkt;

    // ================= 財務三表 =================
    public double[] plMonthly() {
        // 直近30日
        int n = Math.min(30, profitHist.size());
        double net = 0;
        for (int i = profitHist.size() - n; i < profitHist.size(); i++) net += profitHist.get(i);
        return new double[]{ dRev * 30, dCogs * 30, dWage * 30, dRent * 30, dHq * 30, dDep * 30, dInt * 30, net };
    }
}
