package ci;

/**
 * 描画の抽象化。Android では Canvas、デスクトップ検証では Java2D が実装する。
 * 座標は論理dp。呼び出し側はプラットフォームを意識しない。
 */
public interface Gfx {
    int LEFT = 0, CENTER = 1, RIGHT = 2;

    void save();
    void restore();
    void translate(float x, float y);
    void clip(float x, float y, float w, float h);

    void rect(float x, float y, float w, float h, int color);
    void roundRect(float x, float y, float w, float h, float r, int color);
    void roundRectStroke(float x, float y, float w, float h, float r, int color, float sw);
    void circle(float cx, float cy, float r, int color);
    void circleStroke(float cx, float cy, float r, int color, float sw);
    void arcStroke(float cx, float cy, float r, float startDeg, float sweepDeg, int color, float sw);
    void line(float x1, float y1, float x2, float y2, int color, float sw);
    void poly(float[] pts, int color);

    /** bold=太字。size は dp。 */
    void text(String s, float x, float y, float size, int color, int align, boolean bold);
    float textW(String s, float size, boolean bold);

    void linearGradient(float x, float y, float w, float h, int c0, int c1, boolean vertical);
}
