package ci;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ci.Core.*;

/** 画面。Gfx にのみ依存するのでAndroidでもデスクトップでも同じ絵が出る。 */
public class App {
    public Game g = new Game();
    public Ui ui;
    public int tab = 0;
    public static final String[] TABS = {"ホーム", "物件", "店舗", "組織", "商品", "財務", "市場"};
    public static final String[] TICONS = {"■", "▲", "●", "◆", "☕", "¥", "↗"};

    // モーダル
    public int modal = 0;          // 0=なし
    public int mProp = -1, mStore = -1, mDept = -1;
    public int bulkScope = 1, bulkTarget = 0;
    public int devWeeks = 8, devBudget = 2000000, devProd = -1;

    public float headerH = 62, tabH = 54;

    public App(Gfx gfx) { ui = new Ui(gfx); }

    // ================= フレーム =================
    public void frame(float w, float h) {
        Gfx gx = ui.g;
        ui.begin(w, h);
        ui.bg(Ui.BG);

        // 本体(スクロール領域)
        gx.save();
        gx.clip(0, headerH, w, h - headerH - tabH);
        gx.translate(0, headerH);
        ui.cy = 4;
        switch (tab) {
            case 0: home(); break;
            case 1: propsTab(); break;
            case 2: storesTab(); break;
            case 3: orgTab(); break;
            case 4: menuTab(); break;
            case 5: finTab(); break;
            case 6: marketTab(); break;
        }
        ui.space(30);
        gx.restore();
        gx.translate(0, -headerH);
        ui.clampScroll(h - headerH - tabH);

        header(w);
        tabbar(w, h);

        if (modal != 0) drawModal(w, h);
        ui.cancelTap();
    }

    // ================= ヘッダ =================
    private void header(float w) {
        Gfx gx = ui.g;
        gx.linearGradient(0, 0, w, headerH, 0xFF2E2110, 0xFF1B140C, true);
        gx.line(0, headerH, w, headerH, Ui.LINE, 1);
        float save = ui.scroll; ui.scroll = 0;
        int y = (g.day - 1) / 365 + 1, d = (g.day - 1) % 365 + 1;
        ui.label(y + "年目 " + d + "日", 12, 18, 12, Ui.GOLD);
        ui.labelR(Core.yen(g.cash), w - 12, 20, 17, g.cash < 0 ? Ui.BAD : Ui.TEXT);

        String kpi = "評判★" + String.format("%.1f", g.repAvg()) + "  認知" + Math.round(g.awAvg())
            + "  店舗" + g.stores.size() + "  " + g.rating;
        ui.label(kpi, 12, 36, 10.5f, Ui.SUB);
        ui.labelR("日次 " + Core.yen(g.dNet), w - 12, 36, 11, g.dNet >= 0 ? Ui.GOOD : Ui.BAD);

        // 速度
        float bx = w - 118, by = 42;
        String[] sp = {"| |", "▶", "▶▶"};
        int[] spv = {0, 1, 3};
        for (int i = 0; i < 3; i++) {
            if (ui.chip(bx + i * 38, by, 34, 17, sp[i], g.speed == spv[i])) g.speed = spv[i];
        }
        ui.label(g.over ? "経営終了" : (g.speed == 0 ? "一時停止中" : "進行中"), 12, 54, 10, g.over ? Ui.BAD : Ui.SUB);
        ui.scroll = save;
    }

    private void tabbar(float w, float h) {
        Gfx gx = ui.g;
        float y = h - tabH;
        gx.rect(0, y, w, tabH, 0xFF1B140C);
        gx.line(0, y, w, y, Ui.LINE, 1);
        float tw = w / TABS.length;
        float save = ui.scroll; ui.scroll = 0;
        for (int i = 0; i < TABS.length; i++) {
            boolean on = tab == i;
            if (on) gx.linearGradient(i * tw, y + 1, tw, tabH - 1, 0xFF33240F, 0xFF1B140C, true);
            gx.text(TICONS[i], i * tw + tw / 2, y + 22, 15, on ? Ui.GOLD : Ui.SUB, Gfx.CENTER, true);
            gx.text(TABS[i], i * tw + tw / 2, y + 38, 9.5f, on ? Ui.GOLD : Ui.SUB, Gfx.CENTER, false);
            if (ui.hit(i * tw, y, tw, tabH)) { tab = i; ui.scroll = 0; }
        }
        ui.scroll = save;
    }

    // ================= ホーム =================
    private void home() {
        float W = ui.colW, x = ui.cx;
        // KPI 4枚
        float cw = (W - 8) / 2;
        float top = ui.cy;
        kpiCard(x, top, cw, 56, "本日の来客", g.dVisitors + "人", Ui.TEXT);
        kpiCard(x + cw + 8, top, cw, 56, "本日の売上", Core.big(g.dRev), Ui.TEXT);
        kpiCard(x, top + 64, cw, 56, "本日の利益", Core.yen(g.dNet), g.dNet >= 0 ? Ui.GOOD : Ui.BAD);
        kpiCard(x + cw + 8, top + 64, cw, 56, "機会損失", g.dLost + "人", g.dLost > 30 ? Ui.WARN : Ui.SUB);
        ui.cy = top + 128;

        // ゲージ
        float gy = ui.cy;
        ui.card(74);
        ui.ring(x + W * 0.18f, gy + 30, 20, g.repAvg() / 5, Ui.GOLD, String.format("%.1f", g.repAvg()), "評判");
        ui.ring(x + W * 0.5f, gy + 30, 20, g.awAvg() / 100, 0xFF7FB0C9, "" + Math.round(g.awAvg()), "認知度");
        float du = g.stores.isEmpty() ? 0f : (float) (g.dCogs / Math.max(1.0, g.dRev));
        ui.ring(x + W * 0.82f, gy + 30, 20, du, Ui.ACCENT, Math.round(du * 100) + "%", "原価率");

        ui.title("利益の推移");
        float cy2 = ui.cy; ui.card(88);
        float[] hist = tail(g.profitHist, 40);
        if (hist.length > 1) ui.spark(x + 10, cy2 + 12, W - 20, 64, hist, Ui.ACCENT, true);
        else ui.label("営業データを蓄積中…", x + 12, cy2 + 44, 12, Ui.SUB);

        ui.title("店舗成績");
        List<Store> ss = new ArrayList<>(g.stores);
        ss.sort((a, b) -> Double.compare(b.profit, a.profit));
        if (ss.isEmpty()) { ui.card(40); ui.label("店舗がありません。物件タブから出店しましょう。", x + 12, ui.cy - 32, 12, Ui.SUB); }
        for (int i = 0; i < Math.min(6, ss.size()); i++) {
            Store s = ss.get(i);
            float ry = ui.cy; ui.card(34);
            ui.label((i + 1) + ". " + ui.ellipsis(s.name, W - 150, 12, false), x + 10, ry + 15, 12, Ui.TEXT);
            ui.label(s.visitors + "人 / 客単価" + Core.yen(s.avgPrice), x + 10, ry + 28, 10, Ui.SUB);
            ui.labelR(Core.yen(s.profit), x + W - 10, ry + 21, 12.5f, s.profit >= 0 ? Ui.GOOD : Ui.BAD);
            if (ui.hit(x, ry, W, 34)) { mStore = g.stores.indexOf(s); modal = 3; ui.modalScroll = 0; }
        }

        ui.title("ニュース");
        float ny = ui.cy;
        int n = Math.min(8, g.log.size());
        ui.card(Math.max(30, n * 17 + 12));
        for (int i = 0; i < n; i++)
            ui.label(ui.ellipsis(g.log.get(i), W - 20, 11, false), x + 10, ny + 18 + i * 17, 11, Ui.SUB);
    }

    private void kpiCard(float x, float y, float w, float h, String label, String val, int col) {
        Gfx gx = ui.g;
        gx.roundRect(x, ui.sy(y), w, h, 12, Ui.CARD);
        gx.roundRectStroke(x, ui.sy(y), w, h, 12, Ui.LINE, 1);
        ui.label(label, x + 10, y + 20, 10.5f, Ui.SUB);
        ui.labelB(val, x + 10, y + 42, 16, col);
    }

    // ================= 物件 =================
    private int propCity = 0;
    private void propsTab() {
        float W = ui.colW, x = ui.cx;
        // 都市タブ
        float cy = ui.cy;
        float bw = (W - 12) / 3;
        for (int i = 0; i < g.cities.size(); i++) {
            City c = g.cities.get(i);
            if (ui.chip(x + i * (bw + 6), cy, bw, 28, c.name + (c.unlocked ? "" : " 🔒"), propCity == i)) propCity = i;
        }
        ui.cy = cy + 36;
        City c = g.cities.get(propCity);
        if (!c.unlocked) {
            float ay = ui.cy; ui.card(96);
            ui.labelB(c.name + " へ進出する", x + 12, ay + 22, 13.5f, Ui.GOLD);
            ui.label("進出費用 " + Core.big(c.entryCost) + " ・ 物価指数 家賃×"
                + String.format("%.2f", c.rentIdx) + " 賃金×" + String.format("%.2f", c.wageIdx),
                x + 12, ay + 42, 11, Ui.SUB);
            ui.label("新しい市場には競合がまだ少なく、良い物件が残っています。", x + 12, ay + 58, 11, Ui.SUB);
            if (ui.button(x + 12, ay + 66, W - 24, 24, "進出する " + Core.big(c.entryCost), g.cash >= c.entryCost))
                g.enterCity(propCity);
            return;
        }

        // 地区ごとに物件を並べる
        for (int ai = 0; ai < g.areas.size(); ai++) {
            Area a = g.areas.get(ai);
            if (a.city != propCity) continue;
            List<Prop> ps = new ArrayList<>();
            for (Prop p : g.props) if (p.area == ai) ps.add(p);
            ps.sort((p1, p2) -> p2.foot - p1.foot);
            ui.title(a.name + "  " + segLine(a.seg));
            for (Prop p : ps) {
                int pi = g.props.indexOf(p);
                float py = ui.cy;
                boolean mine = p.owner == 0;
                ui.card(58, mine ? Ui.ACCENT : Ui.LINE, Ui.CARD);
                ui.labelB(p.label, x + 10, py + 19, 12.5f, mine ? Ui.GOLD : Ui.TEXT);
                ui.label("人通り " + String.format("%,d", p.foot) + "/日   席 " + p.seats
                    + "   家賃 " + Core.yen(p.rent) + "/日", x + 10, py + 35, 10.5f, Ui.SUB);
                float bx = x + W - 96;
                if (p.owner == -1) {
                    if (ui.button(bx, py + 16, 86, 26, "出店する", true)) { mProp = pi; modal = 1; ui.modalScroll = 0; }
                } else if (mine) {
                    ui.tag(bx + 30, py + 22, "自社店舗", Ui.GOOD);
                } else {
                    ui.tag(bx, py + 22, g.rivals.get(p.owner - 1).name, Ui.BAD);
                }
                // 人通りバー
                ui.bar(x + 10, py + 46, W - 20, 5, p.foot / 22000f, mine ? Ui.ACCENT : Ui.ACCENT2);
            }
        }
    }
    private String segLine(float[] seg) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < seg.length; i++) if (seg[i] >= 0.2f)
            b.append(Core.SEGS[i].name).append(Math.round(seg[i] * 100)).append("% ");
        return b.toString();
    }

    // ================= 店舗 =================
    private void storesTab() {
        float W = ui.colW, x = ui.cx;
        float cy = ui.cy;
        ui.card(66);
        ui.labelB("店舗 " + g.stores.size() + "店", x + 12, cy + 22, 13.5f, Ui.TEXT);
        ui.label("地域を選んでまとめて操作できます", x + 12, cy + 38, 11, Ui.SUB);
        if (ui.button(x + W - 118, cy + 18, 106, 30, "一括管理を開く", !g.stores.isEmpty())) {
            modal = 2; ui.modalScroll = 0;
        }
        for (Store s : g.stores) {
            float sy = ui.cy;
            Prop p = g.props.get(s.prop);
            ui.card(112);
            drawShop(x + 8, sy + 8, W - 16, 46, s);
            ui.labelB(ui.ellipsis(s.name, W - 120, 12.5f, true), x + 10, sy + 70, 12.5f, Ui.TEXT);
            ui.labelR("★" + String.format("%.1f", s.rep), x + W - 10, sy + 70, 12, Ui.GOLD);
            ui.label(Core.CONCEPTS[s.concept].emo + Core.CONCEPTS[s.concept].name
                + " / " + (s.priceMode > 0 ? "高価格" : s.priceMode < 0 ? "低価格" : "標準価格")
                + " / " + (s.manager >= 0 ? "店長あり" : "店長なし"), x + 10, sy + 85, 10.5f,
                s.manager >= 0 ? Ui.SUB : Ui.WARN);
            ui.label("来客" + s.visitors + " 需要" + s.potential + " 人員" + s.staff
                + " 席" + p.seats, x + 10, sy + 100, 10.5f, Ui.SUB);
            ui.labelR(Core.yen(s.profit), x + W - 10, sy + 100, 12, s.profit >= 0 ? Ui.GOOD : Ui.BAD);
            if (ui.hit(x, sy, W, 112)) { mStore = g.stores.indexOf(s); modal = 3; ui.modalScroll = 0; }
        }
        if (g.stores.isEmpty()) {
            float e = ui.cy; ui.card(44);
            ui.label("まだ店舗がありません。物件タブで人通りの多い物件を探しましょう。", x + 12, e + 26, 11.5f, Ui.SUB);
        }
    }

    /** 店の外観 */
    private void drawShop(float x, float y, float w, float h, Store s) {
        Gfx gx = ui.g;
        float sy = ui.sy(y);
        boolean closed = s.closedDays > 0;
        gx.roundRect(x, sy, w, h, 8, 0xFF1A120B);
        // 背景の街
        for (int i = 0; i < 8; i++) {
            float bh = 10 + ((i * 37) % 16);
            gx.rect(x + 6 + i * (w - 12) / 8f, sy + h - 12 - bh, (w - 12) / 9f, bh, 0xFF241A10);
        }
        float sw = Math.min(120, w * 0.42f), sx = x + w / 2 - sw / 2;
        int awn = new int[]{0xFF8C5A32, 0xFF3F7A6B, 0xFF7A4A6B, 0xFF4A5F8C, 0xFF8C3A2A}[s.concept];
        gx.rect(sx, sy + h - 30, sw, 30, closed ? 0xFF3A2B1C : 0xFF4A3524);
        gx.rect(sx + 6, sy + h - 24, sw - 12, 16, closed ? 0xFF241A10 : 0xFFF0D9A8);
        for (int i = 0; i < 6; i++)
            gx.rect(sx + i * (sw / 6f), sy + h - 36, sw / 6f, 6, i % 2 == 0 ? awn : 0xFFE8DCC8);
        // スタッフ
        int st = Math.min(5, s.staff);
        for (int i = 0; i < st; i++) person(sx + 12 + i * ((sw - 24) / Math.max(1, st - 1 == 0 ? 1 : st - 1)),
            sy + h - 12, 0xFF6B4A2A, 0.8f);
        // 行列
        int q = closed ? 0 : Math.min(6, s.lost / 26);
        for (int i = 0; i < q; i++) person(sx - 10 - i * 11, sy + h - 5, i < 3 ? 0xFFE0C9A8 : 0xFFA8917A, 0.85f);
        // 入店客
        int inn = closed ? 0 : Math.min(4, s.visitors / 60);
        for (int i = 0; i < inn; i++) person(sx + sw + 10 + i * 11, sy + h - 5, 0xFFC9B092, 0.85f);
        if (closed) gx.text("CLOSED", x + w / 2, sy + h / 2, 11, Ui.BAD, Gfx.CENTER, true);
    }
    private void person(float x, float y, int col, float sc) {
        Gfx gx = ui.g;
        gx.circle(x, y - 9 * sc, 3.2f * sc, col);
        gx.poly(new float[]{x - 3.4f * sc, y - 5 * sc, x + 3.4f * sc, y - 5 * sc, x + 2.8f * sc, y, x - 2.8f * sc, y}, col);
    }

    // ================= 組織 =================
    private void orgTab() {
        float W = ui.colW, x = ui.cx;
        ui.title("本社の部署");
        for (int i = 0; i < Core.DEPTS.length; i++) {
            Dept d = Core.DEPTS[i];
            boolean open = g.deptOpen[i];
            Person ex = g.exec(i);
            float dy = ui.cy;
            ui.card(open ? 104 : 84, open ? Ui.ACCENT2 : Ui.LINE, Ui.CARD);
            ui.labelB(d.emo + " " + d.name, x + 10, dy + 20, 13, open ? Ui.GOLD : Ui.TEXT);
            ui.labelR(open ? "維持費 " + Core.yen(d.upkeep) + "/日" : Core.big(d.cost),
                x + W - 10, dy + 20, 11, Ui.SUB);
            wrap(d.desc, x + 10, dy + 36, W - 20, 10.5f, Ui.SUB, 2);
            if (!open) {
                boolean can = g.cash >= d.cost && (i == 0 || g.deptOpen[0]);
                if (ui.button(x + W - 110, dy + 54, 100, 24,
                    i != 0 && !g.deptOpen[0] ? "要:取締役室" : "開設する", can)) g.openDept(i);
            } else {
                ui.label(ex != null ? "👤 " + ex.name + " (" + d.execTitle + ")" : "空席 — " + d.execTitle + "を招聘",
                    x + 10, dy + 72, 11, ex != null ? Ui.GOOD : Ui.WARN);
                if (ui.button(x + W - 210, dy + 76, 96, 22, ex != null ? "交代" : "招聘する", true)) {
                    mDept = i; modal = 4; ui.modalScroll = 0;
                }
                boolean on = g.deleg[i];
                if (ui.button(x + W - 108, dy + 76, 98, 22, on ? "委任 ON" : "委任 OFF", ex != null,
                    on ? Ui.ACCENT : Ui.CARD2, on ? 0xFF2A1A08 : Ui.TEXT)) g.deleg[i] = !g.deleg[i];
            }
        }

        ui.title("人材市場");
        for (int i = 0; i < g.market.size(); i++) {
            Person p = g.market.get(i);
            float py = ui.cy;
            ui.card(70);
            ui.labelB(p.name, x + 10, py + 20, 12.5f, Ui.TEXT);
            ui.labelR("日給 " + Core.yen(p.salary), x + W - 10, py + 20, 11, Ui.SUB);
            skillBar(x + 10, py + 30, (W - 30) / 3, "人事", p.hr);
            skillBar(x + 10 + (W - 30) / 3 + 5, py + 30, (W - 30) / 3, "論理", p.logic);
            skillBar(x + 10 + ((W - 30) / 3 + 5) * 2, py + 30, (W - 30) / 3, "商品", p.product);
            if (ui.button(x + W - 96, py + 44, 86, 20, "採用 " + Core.big(p.salary * 8), g.cash >= p.salary * 8L)) {
                g.hireFromMarket(i, -1);
                break;
            }
        }

        ui.title("従業員 " + g.people.size() + "人");
        for (int i = 0; i < g.people.size(); i++) {
            Person p = g.people.get(i);
            float py = ui.cy;
            ui.card(46);
            ui.labelB(p.name, x + 10, py + 19, 12, Ui.TEXT);
            String pos = p.assign <= -100 ? Core.DEPTS[-100 - p.assign].execTitle
                : p.assign >= 0 ? storeName(p.assign) : "待機中";
            ui.label(pos + " / 平均" + Math.round(p.avg()) + " / 勤続" + p.days + "日",
                x + 10, py + 34, 10.5f, p.assign == -1 ? Ui.WARN : Ui.SUB);
            ui.labelR(Core.yen(p.salary) + "/日", x + W - 10, py + 26, 10.5f, Ui.SUB);
        }
    }
    private String storeName(int id) {
        for (Store s : g.stores) if (s.id == id) return s.name;
        return "店舗";
    }
    private void skillBar(float x, float y, float w, String label, float v) {
        ui.label(label, x, y + 9, 9.5f, Ui.SUB);
        ui.bar(x + 24, y + 3, w - 44, 6, v / 100f, v > 70 ? Ui.GOOD : v > 45 ? Ui.ACCENT : Ui.SUB);
        ui.labelR("" + Math.round(v), x + w - 2, y + 9, 9.5f, Ui.TEXT);
    }

    // ================= 商品 =================
    private void menuTab() {
        float W = ui.colW, x = ui.cx;
        ui.title("メニューと価格");
        float sy0 = ui.cy;
        ui.card(58);
        ui.label("全商品の価格を一括で調整", x + 10, sy0 + 20, 11.5f, Ui.SUB);
        float bw = (W - 40) / 4;
        String[] lb = {"安売り 85%", "標準 100%", "強気 115%", "高級 130%"};
        float[] mul = {0.85f, 1.0f, 1.15f, 1.3f};
        for (int i = 0; i < 4; i++)
            if (ui.button(x + 10 + i * (bw + 6), sy0 + 28, bw, 22, lb[i], true, Ui.CARD2, Ui.TEXT))
                g.bulkMenuPrice(mul[i]);

        for (MenuItem m : g.menu) {
            Prod p = Core.PRODUCTS[m.prodIdx];
            float my = ui.cy;
            ui.card(56);
            ui.labelB(p.emo + " " + p.name, x + 10, my + 20, 12.5f, m.on ? Ui.TEXT : Ui.SUB);
            ui.label(Core.stars(m.stars) + "  原価 " + Core.yen(p.baseCost + (p.bean ? g.beanCostPerCup(0) : 0))
                + "  適正 " + Core.yen(p.fair), x + 10, my + 36, 10.5f, Ui.SUB);
            if (ui.button(x + W - 40, my + 12, 30, 24, "−", true, Ui.CARD2, Ui.TEXT)) m.price = Math.max(50, m.price - 10);
            ui.labelC(Core.yen(m.price), x + W - 78, my + 29, 12, Ui.TEXT);
            if (ui.button(x + W - 118, my + 12, 30, 24, "＋", true, Ui.CARD2, Ui.TEXT)) m.price += 10;
            if (ui.button(x + W - 178, my + 12, 54, 24, m.on ? "提供中" : "休止", true,
                m.on ? Ui.ACCENT2 : Ui.CARD2, m.on ? Ui.TEXT : Ui.SUB)) m.on = !m.on;
        }

        ui.title("商品開発");
        float dy = ui.cy;
        if (g.dev != null) {
            ui.card(70);
            ui.labelB("開発中: " + g.dev.code, x + 10, dy + 20, 12.5f, Ui.GOLD);
            ui.label("残り " + g.dev.daysLeft + "日 / 投入額 " + Core.big(g.dev.spent)
                + " (週 " + Core.big(g.dev.weeklyBudget) + ")", x + 10, dy + 36, 11, Ui.SUB);
            ui.bar(x + 10, dy + 46, W - 20, 8, 1 - g.dev.daysLeft / (float) g.dev.totalDays, Ui.ACCENT);
        } else {
            int next = g.nextUndeveloped();
            ui.card(84);
            if (next < 0) {
                ui.label("全商品を開発済みです。", x + 10, dy + 30, 12, Ui.SUB);
            } else {
                ui.labelB("次の商品: " + Core.PRODUCTS[next].emo + Core.PRODUCTS[next].name, x + 10, dy + 20, 12.5f, Ui.TEXT);
                ui.label("期間 " + devWeeks + "週 / 週次予算 " + Core.big(devBudget)
                    + " → 想定品質 " + Core.stars(estStars()), x + 10, dy + 36, 11, Ui.SUB);
                float q = (W - 30) / 4;
                if (ui.button(x + 10, dy + 46, q, 22, "期間−", devWeeks > 2, Ui.CARD2, Ui.TEXT)) devWeeks -= 2;
                if (ui.button(x + 12 + q, dy + 46, q, 22, "期間＋", devWeeks < 16, Ui.CARD2, Ui.TEXT)) devWeeks += 2;
                if (ui.button(x + 14 + q * 2, dy + 46, q, 22, "予算−", devBudget > 500000, Ui.CARD2, Ui.TEXT)) devBudget -= 500000;
                if (ui.button(x + 16 + q * 3, dy + 46, q, 22, "予算＋", devBudget < 5000000, Ui.CARD2, Ui.TEXT)) devBudget += 500000;
                if (ui.button(x + 10, dy + 72, W - 20, 0, "", false)) { }
                if (ui.button(x + 10, dy + 70, W - 20, 0, "", false)) { }
            }
        }
        if (g.dev == null && g.nextUndeveloped() >= 0) {
            float by = ui.cy;
            if (ui.button(x, by, W, 30, "開発を開始する(総額 " + Core.big((double) devBudget * devWeeks) + ")",
                g.cash > (double) devBudget * devWeeks * 0.3)) {
                g.startDev("PJ-" + (g.menu.size() + 1), g.nextUndeveloped(), devWeeks, devBudget);
            }
            ui.cy = by + 38;
        }

        ui.title("豆の仕入れ(都市ごとの焙煎会社)");
        for (int ci = 0; ci < g.cities.size(); ci++) {
            if (!g.cities.get(ci).unlocked) continue;
            ui.label(g.cities.get(ci).name, x, ui.cy + 12, 11.5f, Ui.GOLD);
            ui.cy += 18;
            for (int ri = 0; ri < g.roasters.size(); ri++) {
                Roaster r = g.roasters.get(ri);
                if (r.city != ci) continue;
                float ry = ui.cy;
                boolean cur = g.roasterOf[ci] == ri;
                ui.card(44, cur ? Ui.ACCENT : Ui.LINE, Ui.CARD);
                ui.labelB(r.name, x + 10, ry + 19, 12, cur ? Ui.GOLD : Ui.TEXT);
                ui.label(Core.stars(r.stars) + "  1杯あたり " + Core.yen(r.pricePerCup), x + 10, ry + 34, 10.5f, Ui.SUB);
                if (cur) ui.tag(x + W - 60, ry + 15, "契約中", Ui.GOOD);
                else if (ui.button(x + W - 76, ry + 11, 66, 22, "契約", true, Ui.CARD2, Ui.TEXT)) g.roasterOf[ci] = ri;
            }
        }
        float ry = ui.cy;
        ui.card(58);
        ui.labelB("🏭 自社焙煎所", x + 10, ry + 20, 12.5f, g.roastery ? Ui.GOLD : Ui.TEXT);
        ui.label(g.roastery ? "稼働中。豆の原価-28%、品質+1段階。" : "建設費 1.2億。豆の原価-28%、品質+1段階。",
            x + 10, ry + 38, 11, Ui.SUB);
        if (!g.roastery && ui.button(x + W - 96, ry + 16, 86, 26, "建設", g.cash >= 120000000)) g.buildRoastery();
    }
    private int estStars() {
        double q = (double) devBudget * devWeeks * 7 / 7 / 3_000_000.0 + devWeeks * 7 / 60.0;
        return (int) Core.clamp((float) (1 + q * 1.15), 1, 5);
    }

    // ================= 財務 =================
    private int finPage = 0;
    private void finTab() {
        float W = ui.colW, x = ui.cx;
        float cy = ui.cy;
        String[] pg = {"損益", "貸借", "資金", "融資"};
        float bw = (W - 18) / 4;
        for (int i = 0; i < 4; i++) if (ui.chip(x + i * (bw + 6), cy, bw, 26, pg[i], finPage == i)) finPage = i;
        ui.cy = cy + 34;

        if (finPage == 0) {
            ui.title("損益計算書(日次)");
            float py = ui.cy; ui.card(206);
            float yy = py + 22;
            yy = plRow(x, yy, W, "売上高", g.dRev, Ui.GOOD);
            yy = plRow(x, yy, W, "売上原価", -g.dCogs, Ui.TEXT);
            yy = plRow(x, yy, W, "　売上総利益", g.dRev - g.dCogs, Ui.GOLD);
            yy = plRow(x, yy, W, "店舗人件費", -g.dWage, Ui.TEXT);
            yy = plRow(x, yy, W, "地代家賃", -g.dRent, Ui.TEXT);
            yy = plRow(x, yy, W, "本社費・役員報酬", -g.dHq, Ui.TEXT);
            yy = plRow(x, yy, W, "広告宣伝費", -g.dMkt, Ui.TEXT);
            yy = plRow(x, yy, W, "減価償却費", -g.dDep, Ui.TEXT);
            yy = plRow(x, yy, W, "支払利息", -g.dInt, Ui.TEXT);
            ui.g.line(x + 10, ui.sy(yy - 6), x + W - 10, ui.sy(yy - 6), Ui.LINE, 1);
            plRow(x, yy + 4, W, "当期純利益", g.dNet, g.dNet >= 0 ? Ui.GOOD : Ui.BAD);
        } else if (finPage == 1) {
            ui.title("貸借対照表");
            float py = ui.cy; ui.card(150);
            float yy = py + 22;
            yy = plRow(x, yy, W, "現金及び預金", g.cash, Ui.TEXT);
            yy = plRow(x, yy, W, "有形固定資産", g.ppe - g.accumDep, Ui.TEXT);
            yy = plRow(x, yy, W, "　資産合計", g.totalAssets(), Ui.GOLD);
            yy = plRow(x, yy, W, "借入金", g.totalDebt(), Ui.TEXT);
            yy = plRow(x, yy, W, "　純資産", g.equity(), g.equity() >= 0 ? Ui.GOOD : Ui.BAD);
            plRow(x, yy, W, "自己資本比率", 0, Ui.TEXT);
            ui.labelR(String.format("%.0f%%", g.totalAssets() > 0 ? g.equity() / g.totalAssets() * 100 : 0),
                x + W - 12, yy + 13, 12, Ui.TEXT);
        } else if (finPage == 2) {
            ui.title("キャッシュフロー(日次)");
            float py = ui.cy; ui.card(140);
            float yy = py + 22;
            yy = plRow(x, yy, W, "営業CF(純利益+減価償却)", g.dNet + g.dDep, Ui.GOOD);
            yy = plRow(x, yy, W, "投資CF(出店・設備)", 0, Ui.SUB);
            yy = plRow(x, yy, W, "財務CF(元本返済)", -g.dPrincipal, Ui.TEXT);
            ui.g.line(x + 10, ui.sy(yy - 6), x + W - 10, ui.sy(yy - 6), Ui.LINE, 1);
            plRow(x, yy + 4, W, "現金増減", g.dNet + g.dDep - g.dPrincipal, Ui.TEXT);
            ui.title("マクロ経済");
            float my = ui.cy; ui.card(66);
            ui.label("政策金利", x + 12, my + 22, 11.5f, Ui.SUB);
            ui.labelR(String.format("%.2f%%", g.policyRate * 100), x + W - 12, my + 22, 12.5f, Ui.TEXT);
            ui.label("インフレ率", x + 12, my + 42, 11.5f, Ui.SUB);
            ui.labelR(String.format("%.2f%%", g.inflation * 100), x + W - 12, my + 42, 12.5f, Ui.TEXT);
            ui.label("累積物価上昇", x + 12, my + 58, 10.5f, Ui.SUB);
            ui.labelR(String.format("+%.1f%%", g.inflationDrift * 100), x + W - 12, my + 58, 10.5f, Ui.SUB);
        } else {
            ui.title("信用格付け");
            float ry = ui.cy; ui.card(72);
            ui.labelB(g.rating, x + 12, ry + 34, 26, ratingCol());
            ui.label("収益力・負債比率・返済実績で決まります", x + 74, ry + 26, 11, Ui.SUB);
            ui.label("借入残高 " + Core.big(g.totalDebt()) + " / 延滞 " + g.missed + "回", x + 74, ry + 44, 11, Ui.SUB);
            ui.bar(x + 12, ry + 56, W - 24, 6, (g.ratingIdx() + 1) / 8f, ratingCol());

            ui.title("融資を申し込む");
            for (Bank b : Core.BANKS) {
                float by = ui.cy;
                boolean ok = g.canBorrow(b);
                ui.card(78, ok ? Ui.LINE : Ui.LINE, Ui.CARD);
                ui.labelB(b.name, x + 10, by + 20, 12.5f, ok ? Ui.TEXT : Ui.SUB);
                ui.labelR("要 " + Core.RATINGS[b.minRating] + "以上", x + W - 10, by + 20, 10.5f, Ui.SUB);
                wrap(b.desc, x + 10, by + 34, W - 20, 10.5f, Ui.SUB, 1);
                ui.label("金利 " + String.format("%.4f", g.bankRate(b) * 100) + "%/日  期間 " + b.term
                    + "日  枠 " + Core.big(g.bankCap(b)), x + 10, by + 50, 10.5f, Ui.SUB);
                if (ui.button(x + W - 150, by + 52, 70, 20, "半額", ok, Ui.CARD2, Ui.TEXT))
                    g.borrow(b, g.bankCap(b) / 2);
                if (ui.button(x + W - 76, by + 52, 66, 20, "満額", ok)) g.borrow(b, g.bankCap(b));
            }
            if (!g.loans.isEmpty()) {
                ui.title("返済中");
                for (Loan l : g.loans) {
                    float ly = ui.cy; ui.card(48);
                    ui.labelB(l.name, x + 10, ly + 19, 12, Ui.TEXT);
                    ui.label("残 " + Core.big(l.balance) + " / 残り" + l.daysLeft + "日"
                        + (l.bullet ? " (満期一括)" : " (日割 " + Core.yen(l.principal / l.term) + ")"),
                        x + 10, ly + 34, 10.5f, Ui.SUB);
                    ui.bar(x + W - 90, ly + 22, 80, 6, (float) (1 - l.balance / l.principal), Ui.ACCENT);
                }
            }
            ui.title("株式");
            float sy = ui.cy; ui.card(96);
            if (!g.listed) {
                ui.labelB("未上場", x + 12, sy + 22, 13, Ui.TEXT);
                ui.label((g.quartersPositive >= 3 ? "✅" : "❌") + " 3四半期連続の黒字 (" + g.quartersPositive + "/3)",
                    x + 12, sy + 42, 11, g.quartersPositive >= 3 ? Ui.GOOD : Ui.SUB);
                ui.label((g.shares >= 300000 ? "✅" : "❌") + " 発行済株式 30万株 (" + String.format("%,d", g.shares) + ")",
                    x + 12, sy + 58, 11, g.shares >= 300000 ? Ui.GOOD : Ui.SUB);
                ui.label("※ 融資の実行と返済を重ねると株式数が増えます", x + 12, sy + 74, 10, Ui.SUB);
                if (ui.button(x + W - 106, sy + 60, 96, 26, "上場する", g.canIpo())) g.ipo();
            } else {
                ui.labelB("上場企業", x + 12, sy + 22, 13, Ui.GOLD);
                ui.labelR("時価総額 " + Core.big(g.valuation), x + W - 12, sy + 22, 12, Ui.TEXT);
                ui.label("株価 " + Core.yen(g.sharePrice) + " / 発行済 " + String.format("%,d", g.shares) + "株",
                    x + 12, sy + 42, 11, Ui.SUB);
                ui.label("創業者持株 " + Math.round(g.ownerShare) + "%", x + 12, sy + 58, 11, Ui.SUB);
                if (ui.button(x + W - 126, sy + 62, 116, 26, "公募増資", g.ownerShare > 12)) g.secondaryOffering();
            }
        }
    }
    private int ratingCol() {
        int i = g.ratingIdx();
        return i >= 5 ? Ui.GOOD : i >= 3 ? Ui.GOLD : Ui.BAD;
    }
    private float plRow(float x, float y, float W, String label, double v, int col) {
        ui.label(label, x + 12, y + 13, 11.5f, Ui.SUB);
        if (v != 0 || label.contains("利益") || label.contains("合計"))
            ui.labelR(Core.yen(v), x + W - 12, y + 13, 12, col);
        return y + 19;
    }

    // ================= 市場 =================
    private void marketTab() {
        float W = ui.colW, x = ui.cx;
        ui.title("業界ランキング(時価総額)");
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"あなたの会社", g.valuation, true});
        for (Rival r : g.rivals) if (!r.acquired) rows.add(new Object[]{r.name, r.cap, false});
        rows.sort((a, b) -> Double.compare((Double) b[1], (Double) a[1]));
        double mx = (Double) rows.get(0)[1];
        float ry = ui.cy;
        ui.card(rows.size() * 26 + 14);
        for (int i = 0; i < rows.size(); i++) {
            boolean me = (Boolean) rows.get(i)[2];
            float yy = ry + 20 + i * 26;
            ui.label((i + 1) + ". " + rows.get(i)[0], x + 10, yy, 11.5f, me ? Ui.GOLD : Ui.TEXT);
            ui.bar(x + 118, yy - 9, W - 210, 10, (float) ((Double) rows.get(i)[1] / mx), me ? Ui.GOLD : Ui.ACCENT2);
            ui.labelR(Core.big((Double) rows.get(i)[1]), x + W - 10, yy, 10.5f, Ui.SUB);
        }

        ui.title("ブランド認知と広告");
        for (int ci = 0; ci < g.cities.size(); ci++) {
            City c = g.cities.get(ci);
            if (!c.unlocked) continue;
            float cy = ui.cy;
            ui.card(78);
            ui.labelB(c.name, x + 10, cy + 20, 12.5f, Ui.TEXT);
            ui.labelR("認知度 " + Math.round(c.awareness), x + W - 10, cy + 20, 11.5f, Ui.GOLD);
            ui.bar(x + 10, cy + 28, W - 20, 8, c.awareness / 100f, Ui.ACCENT);
            String[] lb = {"チラシ 200万", "TV 800万", "全国CM 3000万"};
            float bw = (W - 32) / 3;
            for (int t = 0; t < 3; t++) {
                double[] cost = {2000000, 8000000, 30000000};
                if (ui.button(x + 10 + t * (bw + 6), cy + 44, bw, 24, lb[t], g.cash >= cost[t], Ui.CARD2, Ui.TEXT))
                    g.marketing(ci, t);
            }
        }

        ui.title("競合他社");
        for (Rival r : g.rivals) {
            float ry2 = ui.cy;
            ui.card(78);
            ui.labelB(r.emo + " " + r.name, x + 10, ry2 + 20, 12.5f, r.acquired ? Ui.SUB : Ui.TEXT);
            ui.labelR(Core.big(r.cap), x + W - 10, ry2 + 20, 11.5f, Ui.SUB);
            wrap(r.desc, x + 10, ry2 + 36, W - 20, 10.5f, Ui.SUB, 2);
            int cnt = 0;
            for (Prop p : g.props) if (p.owner == g.rivals.indexOf(r) + 1) cnt++;
            ui.label("保有物件 " + cnt + "件", x + 10, ry2 + 68, 10.5f, Ui.SUB);
        }

        ui.title("技術部のプロダクト");
        for (int i = 0; i < Core.TECHS.length; i++) {
            Tech t = Core.TECHS[i];
            float ty = ui.cy;
            ui.card(62);
            boolean done = g.techDone[i], building = g.techBuilding == i;
            ui.labelB(t.emo + " " + t.name, x + 10, ty + 20, 12.5f, done ? Ui.GOOD : Ui.TEXT);
            ui.label(t.desc, x + 10, ty + 36, 10.5f, Ui.SUB);
            if (done) ui.tag(x + W - 62, ty + 14, "導入済", Ui.GOOD);
            else if (building) {
                ui.label("開発中 残り" + g.techDays + "日", x + 10, ty + 52, 10.5f, Ui.GOLD);
                ui.bar(x + W - 110, ty + 20, 100, 6, 1 - g.techDays / (float) t.days, Ui.ACCENT);
            } else if (ui.button(x + W - 106, ty + 16, 96, 26, Core.big(t.cost),
                g.deptOpen[5] && g.techBuilding < 0 && g.cash >= t.cost)) g.startTech(i);
            if (!done && !building && !g.deptOpen[5])
                ui.label("※ 技術部の開設が必要", x + 10, ty + 52, 10, Ui.WARN);
        }
    }

    // ================= モーダル =================
    private void drawModal(float w, float h) {
        Gfx gx = ui.g;
        gx.rect(0, 0, w, h, 0xC0000000);
        float mh = h * 0.82f, my = h - mh;
        gx.roundRect(0, my, w, mh, 16, Ui.BG2);
        gx.roundRectStroke(0, my, w, mh, 16, Ui.LINE, 1);
        // 閉じる
        float save = ui.scroll;
        ui.scroll = 0;
        if (ui.button(w - 74, my + 10, 62, 26, "閉じる", true, Ui.CARD2, Ui.TEXT)) { modal = 0; ui.modalScroll = 0; }
        ui.scroll = save;

        ui.modal = true;
        gx.save();
        gx.clip(0, my + 44, w, mh - 44);
        gx.translate(0, my + 44);
        float sc = ui.cy; ui.cy = 0; ui.contentH = 0;
        float cw = ui.colW; ui.colW = w - 24;
        switch (modal) {
            case 1: modalOpenStore(w); break;
            case 2: modalBulk(w); break;
            case 3: modalStore(w); break;
            case 4: modalExec(w); break;
        }
        ui.space(20);
        gx.restore();
        gx.translate(0, -(my + 44));
        ui.clampScroll(mh - 44);
        ui.colW = cw; ui.cy = sc;
        ui.modal = false;
        // タイトル
        String t = modal == 1 ? "出店する" : modal == 2 ? "地域を選んで一括管理" : modal == 3 ? "店舗の詳細" : "幹部を招聘";
        gx.text(t, 14, my + 28, 14.5f, Ui.GOLD, Gfx.LEFT, true);
    }

    private void modalOpenStore(float w) {
        float W = ui.colW, x = ui.cx;
        Prop p = g.props.get(mProp);
        Area a = g.areas.get(p.area);
        float py = ui.cy; ui.card(60);
        ui.labelB(a.name + " " + p.label, x + 10, py + 20, 13, Ui.TEXT);
        ui.label("人通り " + String.format("%,d", p.foot) + "/日  席 " + p.seats + "  家賃 "
            + Core.yen(p.rent) + "/日", x + 10, py + 38, 11, Ui.SUB);
        ui.label("客層 " + segLine(a.seg), x + 10, py + 52, 10.5f, Ui.SUB);
        for (int i = 0; i < Core.CONCEPTS.length; i++) {
            Concept c = Core.CONCEPTS[i];
            boolean ok = !(i == 3 && !a.suburb) && !(i == 4 && !g.roastery);
            double cost = g.buildCost(p, i);
            float cy = ui.cy;
            ui.card(84);
            ui.labelB(c.emo + " " + c.name, x + 10, cy + 20, 12.5f, ok ? Ui.TEXT : Ui.SUB);
            wrap(c.desc, x + 10, cy + 36, W - 120, 10.5f, Ui.SUB, 2);
            ui.label("能力×" + String.format("%.2f", c.cap) + " 家賃×" + String.format("%.2f", c.rent)
                + " 集客×" + String.format("%.2f", c.appeal), x + 10, cy + 68, 10, Ui.SUB);
            if (!ok) ui.label(i == 3 ? "郊外の物件のみ" : "自社焙煎所が必要", x + W - 100, cy + 68, 10, Ui.WARN);
            if (ui.button(x + W - 106, cy + 16, 96, 28, Core.big(cost), ok && g.cash >= cost)) {
                if (g.openStore(mProp, i)) { modal = 0; }
            }
        }
    }

    private void modalBulk(float w) {
        float W = ui.colW, x = ui.cx;
        // スコープ
        float cy = ui.cy; ui.card(96);
        ui.label("対象", x + 10, cy + 18, 11, Ui.SUB);
        float bw = (W - 26) / 3;
        String[] sc = {"全社", "都市", "地区"};
        for (int i = 0; i < 3; i++)
            if (ui.chip(x + 10 + i * (bw + 3), cy + 24, bw, 24, sc[i], bulkScope == i)) { bulkScope = i; bulkTarget = 0; }
        // 対象の選択
        if (bulkScope == 1) {
            int n = 0;
            for (int i = 0; i < g.cities.size(); i++) {
                if (!g.cities.get(i).unlocked) continue;
                if (ui.chip(x + 10 + n * (bw + 3), cy + 56, bw, 24, g.cities.get(i).name, bulkTarget == i)) bulkTarget = i;
                n++;
            }
        } else if (bulkScope == 2) {
            int n = 0;
            for (int i = 0; i < g.areas.size(); i++) {
                Area a = g.areas.get(i);
                if (!g.cities.get(a.city).unlocked) continue;
                boolean has = false;
                for (Store s : g.stores) if (g.props.get(s.prop).area == i) { has = true; break; }
                if (!has) continue;
                float bx = x + 10 + (n % 3) * (bw + 3);
                float by = cy + 56 + (n / 3) * 27;
                if (by < cy + 84 && ui.chip(bx, by, bw, 24,
                    g.cities.get(a.city).name.charAt(0) + "・" + a.name, bulkTarget == i)) bulkTarget = i;
                n++;
            }
        }
        List<Store> ls = g.scopeStores(bulkScope, bulkTarget);
        float ly = ui.cy; ui.card(34);
        ui.labelB("対象店舗 " + ls.size() + "店", x + 10, ly + 22, 12.5f, Ui.GOLD);

        // 価格戦略
        ui.title("価格戦略");
        float py = ui.cy; ui.card(46);
        String[] pm = {"低価格", "標準", "高価格"};
        for (int i = 0; i < 3; i++)
            if (ui.button(x + 10 + i * (bw + 3), py + 12, bw, 24, pm[i], !ls.isEmpty(), Ui.CARD2, Ui.TEXT))
                g.bulkPriceMode(ls, i - 1);

        // 内装
        ui.title("内装をまとめて引き上げる");
        for (int lv = 2; lv <= 5; lv++) {
            double c = g.bulkInteriorCost(ls, lv);
            if (c <= 0) continue;
            float iy = ui.cy; ui.card(40);
            ui.label("内装レベル " + lv + " に統一", x + 10, iy + 24, 12, Ui.TEXT);
            if (ui.button(x + W - 116, iy + 8, 106, 24, Core.big(c), g.cash >= c)) { g.bulkInterior(ls, lv); }
        }

        // 設備
        ui.title("設備をまとめて更新");
        for (int cat = 0; cat < Core.EQUIP_CATS.length; cat++) {
            float ey = ui.cy; ui.card(58);
            ui.labelB(Core.EQUIP_CATS[cat], x + 10, ey + 20, 12, Ui.TEXT);
            float q = (W - 30) / 3;
            for (int st = 2; st <= 4; st++) {
                double c = g.bulkEquipCost(ls, cat, st);
                boolean can = c > 0 && g.cash >= c;
                if (ui.button(x + 10 + (st - 2) * (q + 5), ey + 28, q, 22,
                    "★" + (st + 1) + " " + Core.big(c), can, Ui.CARD2, Ui.TEXT)) g.bulkEquip(ls, cat, st);
            }
        }

        // 人員と裁量
        ui.title("人員と裁量");
        float ay = ui.cy; ui.card(88);
        if (ui.button(x + 10, ay + 12, W - 20, 24, "人員を需要に合わせて最適化する", !ls.isEmpty(), Ui.CARD2, Ui.TEXT))
            g.bulkOptStaff(ls);
        ui.label("店長への裁量", x + 10, ay + 52, 11, Ui.SUB);
        String[] au = {"本部が管理", "一部委任", "全面委任"};
        for (int i = 0; i < 3; i++)
            if (ui.button(x + 10 + i * (bw + 3), ay + 58, bw, 22, au[i], !ls.isEmpty(), Ui.CARD2, Ui.TEXT))
                g.bulkAutonomy(ls, i);
    }

    private void modalStore(float w) {
        if (mStore < 0 || mStore >= g.stores.size()) { modal = 0; return; }
        Store s = g.stores.get(mStore);
        Prop p = g.props.get(s.prop);
        float W = ui.colW, x = ui.cx;
        float sy = ui.cy;
        ui.card(70);
        drawShop(x + 8, sy + 8, W - 16, 54, s);
        float iy = ui.cy; ui.card(112);
        ui.labelB(s.name, x + 10, iy + 20, 13, Ui.TEXT);
        ui.labelR("★" + String.format("%.1f", s.rep), x + W - 10, iy + 20, 12.5f, Ui.GOLD);
        ui.label("人通り " + String.format("%,d", p.foot) + " / 需要 " + s.potential
            + " / 来客 " + s.visitors + " / 席 " + p.seats, x + 10, iy + 38, 11, Ui.SUB);
        ui.label("人員 " + s.staff + "(推奨" + g.idealStaff(s) + ") / スキル " + Math.round(s.skill)
            + " / 士気 " + Math.round(s.morale), x + 10, iy + 54, 11, Ui.SUB);
        ui.label("客単価 " + Core.yen(s.avgPrice) + " / 売上 " + Core.big(s.rev)
            + " / 利益 " + Core.yen(s.profit), x + 10, iy + 70, 11, s.profit >= 0 ? Ui.GOOD : Ui.BAD);
        float[] mixv = new float[Core.SEGS.length];
        int[] mixc = new int[Core.SEGS.length];
        for (int i = 0; i < mixv.length; i++) { mixv[i] = s.segMix[i]; mixc[i] = Core.SEGS[i].col; }
        ui.stackBar(x + 10, iy + 82, W - 20, 10, mixv, mixc);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mixv.length; i++) if (mixv[i] > 0.06f)
            sb.append(Core.SEGS[i].name).append(Math.round(mixv[i] * 100)).append("% ");
        ui.label(sb.toString(), x + 10, iy + 104, 10, Ui.SUB);

        ui.title("人員");
        float ay = ui.cy; ui.card(40);
        if (ui.button(x + 10, ay + 8, 60, 24, "− 1人", s.staff > 1, Ui.CARD2, Ui.TEXT)) s.staff--;
        if (ui.button(x + 76, ay + 8, 60, 24, "＋ 1人", s.staff < 14, Ui.CARD2, Ui.TEXT)) s.staff++;
        if (ui.button(x + W - 116, ay + 8, 106, 24, "最適化(" + g.idealStaff(s) + "人)", true)) s.staff = g.idealStaff(s);

        ui.title("価格戦略");
        float py = ui.cy; ui.card(40);
        String[] pm = {"低価格", "標準", "高価格"};
        float bw = (W - 26) / 3;
        for (int i = 0; i < 3; i++)
            if (ui.button(x + 10 + i * (bw + 3), py + 8, bw, 24, pm[i], true,
                s.priceMode == i - 1 ? Ui.ACCENT : Ui.CARD2, s.priceMode == i - 1 ? 0xFF2A1A08 : Ui.TEXT))
                s.priceMode = i - 1;

        ui.title("店長");
        float my = ui.cy;
        Person m = g.mgr(s);
        ui.card(58);
        ui.label(m != null ? m.name + " / 商品管理 " + Math.round(m.product) + " / 人事 " + Math.round(m.hr)
            : "店長が不在です。組織タブで採用して配属しましょう。", x + 10, my + 24, 11.5f, m != null ? Ui.TEXT : Ui.WARN);
        int freeIdx = -1;
        for (int i = 0; i < g.people.size(); i++) if (g.people.get(i).assign == -1) { freeIdx = i; break; }
        if (freeIdx >= 0 && ui.button(x + 10, my + 32, W - 20, 22,
            "待機中の " + g.people.get(freeIdx).name + " を配属する", true, Ui.CARD2, Ui.TEXT)) {
            g.assignToStore(freeIdx, s);
        }

        ui.title("設備");
        for (int cat = 0; cat < Core.EQUIP_CATS.length; cat++) {
            float ey = ui.cy; ui.card(48);
            Equip cur = Core.EQUIPS[cat][s.equip[cat]];
            ui.label(Core.EQUIP_CATS[cat], x + 10, ey + 20, 11.5f, Ui.SUB);
            ui.label(Core.stars(cur.stars) + " " + cur.name, x + 10, ey + 36, 11, Ui.TEXT);
            if (s.equip[cat] < 4) {
                Equip nx = Core.EQUIPS[cat][s.equip[cat] + 1];
                if (ui.button(x + W - 116, ey + 12, 106, 24, "★" + nx.stars + " " + Core.big(nx.price),
                    g.cash >= nx.price)) { g.cash -= nx.price; g.ppe += nx.price * 0.85; s.equip[cat]++; }
            }
        }

        float cy2 = ui.cy;
        if (ui.button(x, cy2, W, 30, "この店舗を閉店する", true, 0xFF6E3030, 0xFFFFD9D9)) {
            g.closeStore(s); modal = 0;
        }
        ui.cy = cy2 + 38;
    }

    private void modalExec(float w) {
        float W = ui.colW, x = ui.cx;
        Dept d = Core.DEPTS[mDept];
        float dy = ui.cy; ui.card(64);
        ui.labelB(d.emo + " " + d.name + " / " + d.execTitle, x + 10, dy + 22, 13, Ui.GOLD);
        wrap(d.desc, x + 10, dy + 38, W - 20, 10.5f, Ui.SUB, 2);
        ui.title("待機中の人材");
        boolean any = false;
        for (int i = 0; i < g.people.size(); i++) {
            Person p = g.people.get(i);
            if (p.assign != -1) continue;
            any = true;
            float py = ui.cy; ui.card(64);
            ui.labelB(p.name, x + 10, py + 20, 12.5f, Ui.TEXT);
            ui.label("人事" + Math.round(p.hr) + " 論理" + Math.round(p.logic) + " 商品" + Math.round(p.product)
                + " / 役員報酬 " + Core.yen(p.salary * 1.8) + "/日", x + 10, py + 38, 10.5f, Ui.SUB);
            if (ui.button(x + W - 96, py + 18, 86, 26, "就任", true)) { g.assignExec(i, mDept); modal = 0; }
        }
        if (!any) {
            float ey = ui.cy; ui.card(44);
            ui.label("待機中の人材がいません。組織タブの人材市場で採用してください。", x + 10, ey + 26, 11, Ui.SUB);
        }
    }

    // ================= 補助 =================
    private void wrap(String s, float x, float y, float w, float size, int col, int maxLines) {
        int start = 0; int line = 0;
        while (start < s.length() && line < maxLines) {
            int end = start;
            while (end < s.length() && ui.g.textW(s.substring(start, end + 1), size, false) < w) end++;
            if (end == start) end = Math.min(s.length(), start + 1);
            ui.label(s.substring(start, end), x, y + line * (size + 4), size, col);
            start = end; line++;
        }
    }
    private float[] tail(List<Double> l, int n) {
        int from = Math.max(0, l.size() - n);
        float[] out = new float[l.size() - from];
        for (int i = from; i < l.size(); i++) out[i - from] = l.get(i).floatValue();
        return out;
    }

    // ================= 入力 =================
    public void down(float x, float y) { ui.onDown(x, y); }
    public void move(float x, float y) { ui.onMove(x, y); }
    public void up(float x, float y) { ui.onUp(x, y); }
    public void tickIfRunning(long nowMs) {
        if (g.over || g.speed == 0) return;
        long iv = g.speed == 1 ? 1400 : 450;
        if (nowMs - lastTick >= iv) { lastTick = nowMs; g.step(); }
    }
    private long lastTick;
}
