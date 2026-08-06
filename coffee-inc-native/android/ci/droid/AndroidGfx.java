package ci.droid;

import android.graphics.*;
import ci.Gfx;

/** Android Canvas 上の Gfx 実装。 */
public class AndroidGfx implements Gfx {
    public Canvas c;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float dp;

    public AndroidGfx(float density) { dp = density; }

    private void col(int color) { p.setShader(null); p.setColor(color); p.setStyle(Paint.Style.FILL); }
    private void stroke(int color, float sw) {
        p.setShader(null); p.setColor(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(sw * dp);
    }
    private float d(float v) { return v * dp; }

    @Override public void save() { c.save(); }
    @Override public void restore() { c.restore(); }
    @Override public void translate(float x, float y) { c.translate(d(x), d(y)); }
    @Override public void clip(float x, float y, float w, float h) {
        c.clipRect(d(x), d(y), d(x + w), d(y + h));
    }
    @Override public void rect(float x, float y, float w, float h, int color) {
        col(color); c.drawRect(d(x), d(y), d(x + w), d(y + h), p);
    }
    @Override public void roundRect(float x, float y, float w, float h, float rad, int color) {
        col(color); r.set(d(x), d(y), d(x + w), d(y + h)); c.drawRoundRect(r, d(rad), d(rad), p);
    }
    @Override public void roundRectStroke(float x, float y, float w, float h, float rad, int color, float sw) {
        stroke(color, sw); r.set(d(x), d(y), d(x + w), d(y + h)); c.drawRoundRect(r, d(rad), d(rad), p);
    }
    @Override public void circle(float cx, float cy, float rad, int color) {
        col(color); c.drawCircle(d(cx), d(cy), d(rad), p);
    }
    @Override public void circleStroke(float cx, float cy, float rad, int color, float sw) {
        stroke(color, sw); c.drawCircle(d(cx), d(cy), d(rad), p);
    }
    @Override public void arcStroke(float cx, float cy, float rad, float start, float sweep, int color, float sw) {
        stroke(color, sw); p.setStrokeCap(Paint.Cap.BUTT);
        r.set(d(cx - rad), d(cy - rad), d(cx + rad), d(cy + rad));
        c.drawArc(r, start, sweep, false, p);
    }
    @Override public void line(float x1, float y1, float x2, float y2, int color, float sw) {
        stroke(color, sw); p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(d(x1), d(y1), d(x2), d(y2), p);
    }
    @Override public void poly(float[] pts, int color) {
        col(color);
        Path path = new Path();
        path.moveTo(d(pts[0]), d(pts[1]));
        for (int i = 2; i < pts.length; i += 2) path.lineTo(d(pts[i]), d(pts[i + 1]));
        path.close();
        c.drawPath(path, p);
    }
    @Override public void text(String s, float x, float y, float size, int color, int align, boolean bold) {
        if (s == null) return;
        col(color);
        p.setTextSize(d(size));
        p.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        p.setTextAlign(align == CENTER ? Paint.Align.CENTER : align == RIGHT ? Paint.Align.RIGHT : Paint.Align.LEFT);
        c.drawText(s, d(x), d(y), p);
    }
    @Override public float textW(String s, float size, boolean bold) {
        if (s == null) return 0;
        p.setTextSize(d(size));
        p.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        return p.measureText(s) / dp;
    }
    @Override public void linearGradient(float x, float y, float w, float h, int c0, int c1, boolean vertical) {
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(d(x), d(y), vertical ? d(x) : d(x + w), vertical ? d(y + h) : d(y),
            c0, c1, Shader.TileMode.CLAMP));
        c.drawRect(d(x), d(y), d(x + w), d(y + h), p);
        p.setShader(null);
    }
}
