package ci;

import java.util.ArrayList;
import java.util.List;

/**
 * 即時モードのUIツールキット。1フレームごとに画面が描画命令を出し、
 * タップ判定はその場で返る。スクロールは縦方向のみ。
 */
public class Ui {
    // ---- テーマ ----
    public static final int BG      = 0xFF120D08;
    public static final int BG2     = 0xFF1B140C;
    public static final int CARD    = 0xFF261C12;
    public static final int CARD2   = 0xFF322517;
    public static final int LINE    = 0xFF46362A;
    public static final int TEXT    = 0xFFF3E9DC;
    public static final int SUB     = 0xFFB29B80;
    public static final int ACCENT  = 0xFFD99A4E;
    public static final int ACCENT2 = 0xFF8C5A32;
    public static final int GOOD    = 0xFF7FC97F;
    public static final int BAD     = 0xFFE77C7C;
    public static final int GOLD    = 0xFFFFD27F;
    public static final int WARN    = 0xFFE8B04B;

    public final Gfx g;
    public float w, h;          // 論理サイズ(dp)
    public float pad = 12;

    // レイアウトカーソル
    public float cx, cy;        // 現在の描画位置
    public float colW;          // 現在の内容幅

    // スクロール
    public float scroll, contentH, maxScroll;
    private float scrollTarget;

    // 入力
    private boolean tapPending;
    private float tapX, tapY;
    public boolean dragging;
    private float lastY, downY, downX;
    private boolean moved;
    private long downTime;

    // モーダル
    public boolean modal;       // モーダル描画中はスクロール別管理
    public float modalScroll;

    public Ui(Gfx g) { this.g = g; }

    public void begin(float w, float h) {
        this.w = w; this.h = h;
        this.colW = w - pad * 2;
        this.cx = pad;
        this.cy = 0;
        this.contentH = 0;
    }

    // ================= 入力 =================
    public void onDown(float x, float y) {
        downX = x; downY = y; lastY = y; moved = false; dragging = true;
        downTime = System.currentTimeMillis();
    }
    public void onMove(float x, float y) {
        if (!dragging) return;
        float dy = y - lastY;
        lastY = y;
        if (Math.abs(y - downY) > 8 || Math.abs(x - downX) > 12) moved = true;
        if (moved) setScroll(getScroll() - dy);
    }
    public void onUp(float x, float y) {
        if (dragging && !moved && System.currentTimeMillis() - downTime < 700) {
            tapPending = true; tapX = x; tapY = y;
        }
        dragging = false;
    }
    public void cancelTap() { tapPending = false; }

    private float getScroll() { return modal ? modalScroll : scroll; }
    private void setScroll(float v) { if (modal) modalScroll = v; else scroll = v; }

    /** 領域がタップされたか。消費する。 */
    public boolean hit(float x, float y, float w, float h) {
        if (!tapPending) return false;
        float sy = y - getScroll();
        if (tapX >= x && tapX <= x + w && tapY >= sy && tapY <= sy + h) {
            tapPending = false;
            return true;
        }
        return false;
    }

    public void clampScroll(float viewH) {
        maxScroll = Math.max(0, contentH - viewH);
        float s = getScroll();
        if (s < 0) s = 0;
        if (s > maxScroll) s = maxScroll;
        setScroll(s);
    }

    // ================= 基本描画 =================
    public void bg(int color) { g.rect(0, 0, w, h, color); }

    /** スクロールを反映した実描画Y */
    public float sy(float y) { return y - getScroll(); }

    public void space(float dp) { cy += dp; contentH = Math.max(contentH, cy); }

    public void title(String s) {
        cy += 14;
        g.text(s, cx, sy(cy) + 12, 14.5f, GOLD, Gfx.LEFT, true);
        cy += 20;
        contentH = Math.max(contentH, cy);
    }

    /** 高さを指定してカードの背景を描き、内容領域の先頭Yを返す。 */
    public float card(float hgt) { return card(hgt, LINE, CARD); }
    public float card(float hgt, int border, int fill) {
        g.roundRect(cx, sy(cy), colW, hgt, 12, fill);
        g.roundRectStroke(cx, sy(cy), colW, hgt, 12, border, 1);
        float top = cy;
        cy += hgt + 9;
        contentH = Math.max(contentH, cy);
        return top;
    }

    public void label(String s, float x, float y, float size, int color) {
        g.text(s, x, sy(y), size, color, Gfx.LEFT, false);
    }
    public void labelR(String s, float x, float y, float size, int color) {
        g.text(s, x, sy(y), size, color, Gfx.RIGHT, false);
    }
    public void labelB(String s, float x, float y, float size, int color) {
        g.text(s, x, sy(y), size, color, Gfx.LEFT, true);
    }
    public void labelC(String s, float x, float y, float size, int color) {
        g.text(s, x, sy(y), size, color, Gfx.CENTER, false);
    }

    // ================= ウィジェット =================
    public boolean button(float x, float y, float bw, float bh, String label, boolean enabled) {
        return button(x, y, bw, bh, label, enabled, ACCENT, 0xFF2A1A08);
    }
    public boolean buttonSub(float x, float y, float bw, float bh, String label, boolean enabled) {
        return button(x, y, bw, bh, label, enabled, CARD2, TEXT);
    }
    public boolean button(float x, float y, float bw, float bh, String label,
                          boolean enabled, int fill, int fg) {
        int f = enabled ? fill : blend(fill, CARD, 0.65f);
        int t = enabled ? fg : blend(fg, CARD, 0.6f);
        g.roundRect(x, sy(y), bw, bh, 9, f);
        if (fill == CARD2) g.roundRectStroke(x, sy(y), bw, bh, 9, LINE, 1);
        g.text(label, x + bw / 2, sy(y) + bh / 2 + 4.4f, 12.5f, t, Gfx.CENTER, true);
        return enabled && hit(x, y, bw, bh);
    }

    public boolean chip(float x, float y, float bw, float bh, String label, boolean on) {
        g.roundRect(x, sy(y), bw, bh, 8, on ? ACCENT : CARD2);
        g.roundRectStroke(x, sy(y), bw, bh, 8, on ? ACCENT : LINE, 1);
        g.text(label, x + bw / 2, sy(y) + bh / 2 + 4f, 11.5f, on ? 0xFF2A1A08 : TEXT, Gfx.CENTER, on);
        return hit(x, y, bw, bh);
    }

    public void tag(float x, float y, String s, int color) {
        float tw = g.textW(s, 10.5f, false) + 10;
        g.roundRect(x, sy(y), tw, 15, 5, blend(color, CARD, 0.78f));
        g.roundRectStroke(x, sy(y), tw, 15, 5, blend(color, CARD, 0.4f), 1);
        g.text(s, x + tw / 2, sy(y) + 11, 10.5f, color, Gfx.CENTER, false);
    }
    public float tagW(String s) { return g.textW(s, 10.5f, false) + 10 + 5; }

    public void bar(float x, float y, float bw, float bh, float frac, int color) {
        g.roundRect(x, sy(y), bw, bh, bh / 2, 0xFF0F0A06);
        float fw = Math.max(0, Math.min(1, frac)) * bw;
        if (fw > 1) g.roundRect(x, sy(y), fw, bh, bh / 2, color);
    }

    /** 複数色の積み上げバー */
    public void stackBar(float x, float y, float bw, float bh, float[] fr, int[] cols) {
        g.roundRect(x, sy(y), bw, bh, bh / 2, 0xFF0F0A06);
        float ox = x;
        for (int i = 0; i < fr.length; i++) {
            float fw = Math.max(0, fr[i]) * bw;
            if (fw <= 0) continue;
            g.rect(ox, sy(y), fw, bh, cols[i]);
            ox += fw;
        }
    }

    public void ring(float cx0, float cy0, float r, float frac, int color, String center, String sub) {
        g.circleStroke(cx0, sy(cy0), r, 0xFF0F0A06, 6);
        g.arcStroke(cx0, sy(cy0), r, -90, 360 * Math.max(0, Math.min(1, frac)), color, 6);
        g.text(center, cx0, sy(cy0) + 4, 13, TEXT, Gfx.CENTER, true);
        if (sub != null) g.text(sub, cx0, sy(cy0) + r + 14, 10, SUB, Gfx.CENTER, false);
    }

    /** 折れ線グラフ */
    public void spark(float x, float y, float sw, float sh, float[] v, int color, boolean zeroLine) {
        if (v == null || v.length < 2) return;
        float mx = Float.NEGATIVE_INFINITY, mn = Float.POSITIVE_INFINITY;
        for (float f : v) { mx = Math.max(mx, f); mn = Math.min(mn, f); }
        if (zeroLine) { mx = Math.max(mx, 0); mn = Math.min(mn, 0); }
        float rg = (mx - mn) == 0 ? 1 : (mx - mn);
        if (zeroLine && mn < 0 && mx > 0) {
            float zy = sy(y) + sh - ((0 - mn) / rg) * sh;
            g.line(x, zy, x + sw, zy, LINE, 1);
        }
        float px = x, py = sy(y) + sh - ((v[0] - mn) / rg) * sh;
        for (int i = 1; i < v.length; i++) {
            float nx = x + i * sw / (v.length - 1);
            float ny = sy(y) + sh - ((v[i] - mn) / rg) * sh;
            g.line(px, py, nx, ny, color, 1.8f);
            px = nx; py = ny;
        }
    }

    /** 円グラフ(ドーナツ) */
    public void donut(float cx0, float cy0, float r, float[] fr, int[] cols) {
        float a = -90;
        for (int i = 0; i < fr.length; i++) {
            if (fr[i] <= 0.0005f) continue;
            float sweep = fr[i] * 360;
            g.arcStroke(cx0, sy(cy0), r * 0.72f, a, sweep, cols[i], r * 0.56f);
            a += sweep;
        }
    }

    public static int blend(int a, int b, float t) {
        int aa = (a >>> 24), ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = (b >>> 24), br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = (int) (ar + (br - ar) * t), gg = (int) (ag + (bg - ag) * t),
            bl = (int) (ab + (bb - ab) * t), al = (int) (aa + (ba - aa) * t);
        return (al << 24) | (r << 16) | (gg << 8) | bl;
    }

    // 文字列を幅に収める
    public String ellipsis(String s, float maxW, float size, boolean bold) {
        if (g.textW(s, size, bold) <= maxW) return s;
        for (int i = s.length() - 1; i > 0; i--) {
            String t = s.substring(0, i) + "…";
            if (g.textW(t, size, bold) <= maxW) return t;
        }
        return "…";
    }
}
