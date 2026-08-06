package ci.desk;

import ci.Gfx;

import java.awt.*;
import java.awt.geom.*;
import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** デスクトップ検証用の Gfx 実装(Java2D)。実機と同じ画面をPNGに出せる。 */
public class AwtGfx implements Gfx {
    public final BufferedImage img;
    private final Graphics2D g;
    private final float scale;
    private static Font BASE, BOLD;

    static {
        try {
            String[] cands = {
                "/usr/share/fonts/truetype/fonts-japanese-gothic.ttf",
                "/usr/share/fonts/opentype/ipafont-gothic/ipagp.ttf",
                "/usr/share/fonts/opentype/ipafont-gothic/ipag.ttf",
            };
            for (String c : cands) {
                File f = new File(c);
                if (f.exists()) {
                    BASE = Font.createFont(Font.TRUETYPE_FONT, f);
                    BOLD = BASE.deriveFont(Font.BOLD);
                    break;
                }
            }
        } catch (Exception e) { /* fallback below */ }
        if (BASE == null) { BASE = new Font("SansSerif", Font.PLAIN, 12); BOLD = BASE.deriveFont(Font.BOLD); }
    }

    public AwtGfx(int wpx, int hpx, float scale) {
        this.scale = scale;
        img = new BufferedImage(wpx, hpx, BufferedImage.TYPE_INT_RGB);
        g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(scale, scale);
    }

    public void writePng(String path) throws Exception { ImageIO.write(img, "png", new File(path)); }

    private void col(int c) {
        g.setColor(new Color((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24)));
    }
    private Font font(float size, boolean bold) {
        return (bold ? BOLD : BASE).deriveFont(size);
    }

    @Override public void save() { g.setClip(null); }
    @Override public void restore() { g.setClip(null); }
    @Override public void translate(float x, float y) { g.translate(x, y); }
    @Override public void clip(float x, float y, float w, float h) {
        g.setClip(new Rectangle2D.Float(x, y, w, h));
    }
    @Override public void rect(float x, float y, float w, float h, int c) {
        col(c); g.fill(new Rectangle2D.Float(x, y, w, h));
    }
    @Override public void roundRect(float x, float y, float w, float h, float r, int c) {
        col(c); g.fill(new RoundRectangle2D.Float(x, y, w, h, r * 2, r * 2));
    }
    @Override public void roundRectStroke(float x, float y, float w, float h, float r, int c, float sw) {
        col(c); g.setStroke(new BasicStroke(sw));
        g.draw(new RoundRectangle2D.Float(x, y, w, h, r * 2, r * 2));
    }
    @Override public void circle(float cx, float cy, float r, int c) {
        col(c); g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
    }
    @Override public void circleStroke(float cx, float cy, float r, int c, float sw) {
        col(c); g.setStroke(new BasicStroke(sw));
        g.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
    }
    @Override public void arcStroke(float cx, float cy, float r, float start, float sweep, int c, float sw) {
        col(c); g.setStroke(new BasicStroke(sw, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        // Java2Dは反時計回り正・Y下向きなので符号を反転
        g.draw(new Arc2D.Float(cx - r, cy - r, r * 2, r * 2, -start, -sweep, Arc2D.OPEN));
    }
    @Override public void line(float x1, float y1, float x2, float y2, int c, float sw) {
        col(c); g.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(x1, y1, x2, y2));
    }
    @Override public void poly(float[] pts, int c) {
        col(c);
        Path2D.Float p = new Path2D.Float();
        p.moveTo(pts[0], pts[1]);
        for (int i = 2; i < pts.length; i += 2) p.lineTo(pts[i], pts[i + 1]);
        p.closePath();
        g.fill(p);
    }
    @Override public void text(String s, float x, float y, float size, int c, int align, boolean bold) {
        if (s == null) return;
        col(c); g.setFont(font(size, bold));
        float w = g.getFontMetrics().stringWidth(s);
        float dx = align == CENTER ? -w / 2 : align == RIGHT ? -w : 0;
        g.drawString(s, x + dx, y);
    }
    @Override public float textW(String s, float size, boolean bold) {
        if (s == null) return 0;
        g.setFont(font(size, bold));
        return g.getFontMetrics().stringWidth(s);
    }
    @Override public void linearGradient(float x, float y, float w, float h, int c0, int c1, boolean vertical) {
        Color a = new Color((c0 >> 16) & 255, (c0 >> 8) & 255, c0 & 255, (c0 >>> 24));
        Color b = new Color((c1 >> 16) & 255, (c1 >> 8) & 255, c1 & 255, (c1 >>> 24));
        g.setPaint(new GradientPaint(x, y, a, vertical ? x : x + w, vertical ? y + h : y, b));
        g.fill(new Rectangle2D.Float(x, y, w, h));
        g.setPaint(null);
    }
}
