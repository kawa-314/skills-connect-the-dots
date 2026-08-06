package ci.droid;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import ci.App;

public class MainActivity extends Activity {
    private GameView view;
    private long lastBack;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        view = new GameView(this);
        setContentView(view);
    }

    @Override public void onBackPressed() {
        if (view != null && view.app != null && view.app.modal != 0) { view.app.modal = 0; view.invalidate(); return; }
        long now = System.currentTimeMillis();
        if (now - lastBack < 2000) super.onBackPressed();
        else { lastBack = now; Toast.makeText(this, "もう一度押すと終了します", Toast.LENGTH_SHORT).show(); }
    }

    /** 描画とタッチ。1秒に約30回だけ再描画する。 */
    public static class GameView extends View {
        public App app;
        private AndroidGfx gfx;
        private final Handler handler = new Handler();
        private final float density;

        public GameView(Context ctx) {
            super(ctx);
            DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            density = dm.density <= 0 ? 2f : dm.density;
            gfx = new AndroidGfx(density);
            app = new App(gfx);
            setBackgroundColor(0xFF120D08);
            handler.post(loop);
        }

        private final Runnable loop = new Runnable() {
            @Override public void run() {
                if (app != null) app.tickIfRunning(System.currentTimeMillis());
                invalidate();
                handler.postDelayed(this, 33);
            }
        };

        @Override protected void onDraw(Canvas c) {
            gfx.c = c;
            float w = getWidth() / density, h = getHeight() / density;
            try {
                app.frame(w, h);
            } catch (Throwable t) {
                // 画面の一部で例外が出てもアプリが落ちないようにする
                android.graphics.Paint p = new android.graphics.Paint();
                p.setColor(0xFFE77C7C); p.setTextSize(28);
                c.drawText("描画エラー: " + t.getClass().getSimpleName(), 24, 80, p);
            }
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            float x = e.getX() / density, y = e.getY() / density;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: app.down(x, y); break;
                case MotionEvent.ACTION_MOVE: app.move(x, y); break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: app.up(x, y); break;
            }
            invalidate();
            return true;
        }
    }
}
