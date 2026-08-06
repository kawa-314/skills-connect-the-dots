package ci.desk;

import ci.App;
import ci.Core;
import ci.Core.*;
import ci.Game;

/** デスクトップで実機同等の画面をPNGに出す。Pixel 8 Pro 相当(412x915dp)。 */
public class Shot {
    static final float W = 412, H = 915;
    static final float SCALE = 2f;

    static App make() {
        AwtGfx gx = new AwtGfx((int) (W * SCALE), (int) (H * SCALE), SCALE);
        App app = new App(gx);
        return app;
    }

    static void render(App app, String path) throws Exception {
        AwtGfx gx = new AwtGfx((int) (W * SCALE), (int) (H * SCALE), SCALE);
        // 同じAppを別Gfxで描くため差し替え
        app.ui = new ci.Ui(gx) {{ }};
        app.frame(W, H);
        gx.writePng(path);
        System.out.println("wrote " + path);
    }

    public static void main(String[] args) throws Exception {
        String dir = args.length > 0 ? args[0] : ".";
        App app = make();
        Game g = app.g;

        // ある程度進んだ状態を作る
        for (int i = 0; i < g.props.size(); i++) {
            Prop p = g.props.get(i);
            if (p.owner != -1 || g.cityOf(p) != 0) continue;
            if (g.stores.size() >= 6) break;
            g.cash += 20000000;
            g.openStore(i, g.stores.size() % 3);
        }
        g.cash = 240000000;
        for (int i = 0; i < 3; i++) {
            g.hireFromMarket(0, g.stores.get(i).id);
        }
        g.openDept(0); g.openDept(1); g.openDept(4);
        g.hireFromMarket(0, -1); g.assignExec(g.people.size() - 1, 0); g.deleg[0] = true;
        g.hireFromMarket(0, -1); g.assignExec(g.people.size() - 1, 1); g.deleg[1] = true;
        g.borrow(Core.BANKS[0], g.bankCap(Core.BANKS[0]));
        g.startTech(0);
        for (int d = 0; d < 150; d++) {
            for (Store s : g.stores) s.staff = g.idealStaff(s);
            g.step();
        }
        g.cash = 180000000;

        String[] names = {"home", "props", "stores", "org", "menu", "fin", "market"};
        for (int t = 0; t < names.length; t++) {
            app.tab = t; app.ui.scroll = 0; app.modal = 0;
            render(app, dir + "/n-" + names[t] + ".png");
        }
        // モーダル
        app.tab = 2; app.modal = 2; app.ui.modalScroll = 0;
        render(app, dir + "/n-bulk.png");
        app.modal = 3; app.mStore = 0; app.ui.modalScroll = 0;
        render(app, dir + "/n-store.png");
        app.tab = 1; app.modal = 1; app.mProp = firstFree(g); app.ui.modalScroll = 0;
        render(app, dir + "/n-open.png");
    }
    static int firstFree(Game g) {
        for (int i = 0; i < g.props.size(); i++) {
            Prop p = g.props.get(i);
            if (p.owner == -1 && g.cityOf(p) == 0) return i;
        }
        return 0;
    }
}
