package ci.desk;

import ci.Core;
import ci.Core.*;
import ci.Game;

import java.util.ArrayList;
import java.util.List;

/** エンジンのヘッドレス検証。UIなしでシミュレーションを回して数字を見る。 */
public class Headless {

    static String money(double v) { return Core.big(v); }

    public static void main(String[] args) {
        System.out.println("=== 世界生成 ===");
        Game g = new Game();
        System.out.printf("都市 %d / 地区 %d / 物件 %d / 競合 %d%n",
            g.cities.size(), g.areas.size(), g.props.size(), g.rivals.size());
        int free = 0, taken = 0;
        for (Prop p : g.props) if (g.cities.get(g.cityOf(p)).unlocked) { if (p.owner == -1) free++; else taken++; }
        System.out.printf("東京の空き物件 %d / 競合が保有 %d%n", free, taken);

        // 人通り上位
        List<Prop> tk = new ArrayList<>();
        for (Prop p : g.props) if (g.cityOf(p) == 0) tk.add(p);
        tk.sort((a, b) -> b.foot - a.foot);
        System.out.println("--- 東京の人通り上位5件 ---");
        for (int i = 0; i < 5; i++) {
            Prop p = tk.get(i);
            System.out.printf("  %s %s  人通り%,d/日  家賃%s/日  席%d  %s%n",
                g.areas.get(p.area).name, p.label, p.foot, money(p.rent), p.seats,
                p.owner == -1 ? "空き" : "競合:" + g.rivals.get(p.owner - 1).name);
        }

        // 1号店を出す
        int best = -1;
        for (int i = 0; i < g.props.size(); i++) {
            Prop p = g.props.get(i);
            if (p.owner != -1 || g.cityOf(p) != 0) continue;
            if (best < 0 || p.foot > g.props.get(best).foot) best = i;
        }
        System.out.println("\n=== 1号店 ===");
        Prop bp = g.props.get(best);
        System.out.printf("出店: %s %s (人通り%,d) 建設費 %s%n",
            g.areas.get(bp.area).name, bp.label, bp.foot, money(g.buildCost(bp, 0)));
        g.openStore(best, 0);

        for (int i = 0; i < 60; i++) { for (Store s : g.stores) s.staff = g.idealStaff(s); g.step(); }
        Store s0 = g.stores.get(0);
        System.out.printf("60日後: 来客%d人/日 需要%d 席%d 人員%d 客単価%s 店舗利益%s 評判★%.1f%n",
            s0.visitors, s0.potential, bp.seats, s0.staff, money(s0.avgPrice), money(s0.profit), s0.rep);
        System.out.printf("会社: 現金%s 日次純利益%s 格付%s 認知度%.0f%n",
            money(g.cash), money(g.dNet), g.rating, g.cities.get(0).awareness);

        // 客層内訳
        System.out.print("客層: ");
        for (int i = 0; i < Core.SEGS.length; i++) System.out.printf("%s%.0f%% ", Core.SEGS[i].name, s0.segMix[i] * 100);
        System.out.println();

        // 融資
        System.out.println("\n=== 融資枠 ===");
        for (Bank b : Core.BANKS) {
            System.out.printf("  %-16s 金利%.4f%%/日 期間%4d日 要%s 枠%s %s%n",
                b.name, g.bankRate(b) * 100, b.term, Core.RATINGS[b.minRating],
                money(g.bankCap(b)), g.canBorrow(b) ? "" : "(不可)");
        }
        g.borrow(Core.BANKS[0], g.bankCap(Core.BANKS[0]));

        // 拡大: 空き物件に順次出店しつつ店長を雇う
        System.out.println("\n=== 拡大 (3年) ===");
        int yr = 0;
        for (int d = 0; d < 1095; d++) {
            for (Store s : g.stores) s.staff = g.idealStaff(s);
            // 店長の補充
            for (Store s : g.stores) {
                if (s.manager < 0 && !g.market.isEmpty() && g.cash > 8000000) {
                    int bi = 0;
                    for (int i = 1; i < g.market.size(); i++)
                        if (g.market.get(i).product > g.market.get(bi).product) bi = i;
                    g.hireFromMarket(bi, s.id);
                    break;
                }
            }
            // 部署の開設
            for (int i = 0; i < Core.DEPTS.length; i++)
                if (!g.deptOpen[i] && g.cash > Core.DEPTS[i].cost * 3) { g.openDept(i); break; }
            // 幹部の就任と委任
            for (int i = 0; i < Core.DEPTS.length; i++) {
                if (g.deptOpen[i] && g.exec(i) == null && !g.market.isEmpty() && g.cash > 25000000) {
                    int bi = 0;
                    for (int k = 1; k < g.market.size(); k++)
                        if (g.market.get(k).avg() > g.market.get(bi).avg()) bi = k;
                    g.hireFromMarket(bi, -1);
                    g.assignExec(g.people.size() - 1, i);
                    g.deleg[i] = true;
                    break;
                }
            }
            // 出店
            {
                int pick = -1;
                for (int i = 0; i < g.props.size(); i++) {
                    Prop p = g.props.get(i);
                    if (p.owner != -1 || !g.cities.get(g.cityOf(p)).unlocked) continue;
                    if (pick < 0 || p.foot > g.props.get(pick).foot) pick = i;
                }
                if (pick >= 0 && g.cash > g.buildCost(g.props.get(pick), 0) * 1.8) g.openStore(pick, 0);
            }
            // 都市進出
            for (int i = 1; i < g.cities.size(); i++)
                if (!g.cities.get(i).unlocked && g.cash > g.cities.get(i).entryCost * 2.2) g.enterCity(i);
            // 商品開発
            if (g.dev == null && g.cash > 60000000 && g.nextUndeveloped() >= 0)
                g.startDev("P" + g.menu.size(), g.nextUndeveloped(), 10, 3000000);
            // 焙煎所と良い豆
            if (!g.roastery && g.cash > 350000000) g.buildRoastery();
            if (g.cash > 80000000) for (int c = 0; c < g.cities.size(); c++)
                if (g.cities.get(c).unlocked && g.roasterOf[c] % 3 == 0) g.roasterOf[c] = c * 3 + 1;
            // 広告(認知度が下がったら打つ)
            for (int c = 0; c < g.cities.size(); c++) {
                if (!g.cities.get(c).unlocked) continue;
                if (g.cities.get(c).awareness < 30 && g.cash > 40000000) { g.marketing(c, 1); break; }
            }
            // 上場
            if (g.canIpo()) g.ipo();
            g.step();
            if (g.over) { System.out.println("GAME OVER: " + g.overReason + " (day " + g.day + ")"); break; }
            if (d % 365 == 364) {
                yr++;
                System.out.printf("%d年目: 店舗%d 現金%s 日次利益%s 時価%s 格付%s 評判★%.1f 部署%d 幹部%d 上場%s%n",
                    yr, g.stores.size(), money(g.cash), money(g.dNet), money(g.valuation), g.rating,
                    g.repAvg(), countTrue(g.deptOpen), countExec(g), g.listed ? "済" : "-");
            }
        }

        System.out.println("\n=== 最終 ===");
        System.out.printf("日数%d 店舗%d 従業員(店長/幹部)%d 現金%s 借入%s 時価総額%s%n",
            g.day, g.stores.size(), g.people.size(), money(g.cash), money(g.totalDebt()), money(g.valuation));
        System.out.printf("政策金利%.2f%% インフレ%.2f%% 累積物価上昇%.1f%%%n",
            g.policyRate * 100, g.inflation * 100, g.inflationDrift * 100);
        System.out.println("ライバル:");
        for (Rival r : g.rivals) System.out.printf("  %-14s %s %s%n", r.name, money(r.cap), r.acquired ? "(買収済)" : "");
        System.out.println("\n直近ニュース:");
        for (int i = 0; i < Math.min(8, g.log.size()); i++) System.out.println("  " + g.log.get(i));

        // 一括操作の検証
        System.out.println("\n--- 店舗別の内訳 ---");
        System.out.printf("  %-22s %6s %6s %8s %8s %8s %s%n","店舗","人通り","来客","売上","利益","家賃","人員");
        for (Store s : g.stores) {
            Prop p = g.props.get(s.prop);
            System.out.printf("  %-22s %6d %6d %8s %8s %8s %d人 席%d%n",
                s.name, p.foot, s.visitors, money(s.rev), money(s.profit), money(g.storeRent(s)), s.staff, p.seats);
        }
        System.out.printf("  合計: 売上%s 原価%s 人件費%s 家賃%s 本社%s 広告%s 減価償却%s 利息%s => 純利益%s%n",
            money(g.dRev), money(g.dCogs), money(g.dWage), money(g.dRent), money(g.dHq),
            money(g.dMkt), money(g.dDep), money(g.dInt), money(g.dNet));

        System.out.println("\n=== 地域一括操作 ===");
        if (!g.stores.isEmpty()) {
            List<Store> tokyo = g.scopeStores(1, 0);
            System.out.println("東京の店舗数: " + tokyo.size());
            System.out.println("内装Lv4に統一する費用: " + money(g.bulkInteriorCost(tokyo, 4)));
            System.out.println("エスプレッソマシンを★4に統一する費用: " + money(g.bulkEquipCost(tokyo, 0, 3)));
            g.bulkPriceMode(tokyo, 1);
            int hi = 0; for (Store s : tokyo) if (s.priceMode == 1) hi++;
            System.out.println("高価格に設定できた店舗: " + hi + "/" + tokyo.size());
        }
    }
    static int countTrue(boolean[] b) { int n = 0; for (boolean x : b) if (x) n++; return n; }
    static int countExec(Game g) { int n = 0; for (int i : g.deptExec) if (i >= 0) n++; return n; }
}
