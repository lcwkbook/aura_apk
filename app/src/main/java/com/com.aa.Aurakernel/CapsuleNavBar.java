package com.aa.ABC;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class CapsuleNavBar extends View {
    public interface Listener { void onSelect(int tab); }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private Listener listener;
    private int active = 0;
    private float indicator = 0f;
    private ValueAnimator animator;
    private boolean pressed = false;
    private boolean dark = false;

    public CapsuleNavBar(android.content.Context context) {
        super(context);
        setClickable(true);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setDarkMode(boolean dark) {
        this.dark = dark;
        invalidate();
    }

    public void setActive(int tab) {
        tab = tab <= 0 ? 0 : 1;
        active = tab;
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(indicator, tab);
        animator.setDuration(340);
        animator.setInterpolator(new DecelerateInterpolator(1.9f));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                indicator = ((Float) animation.getAnimatedValue()).floatValue();
                invalidate();
            }
        });
        animator.start();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            pressed = true;
            invalidate();
            return true;
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            pressed = false;
            invalidate();
            return true;
        }
        if (action == MotionEvent.ACTION_UP) {
            pressed = false;
            int tab = event.getX() < getWidth() / 2f ? 0 : 1;
            invalidate();
            if (listener != null) listener.onSelect(tab);
            return true;
        }
        return true;
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float outerPad = dp(3);
        float radius = h / 2f;
        rect.set(outerPad, outerPad, w - outerPad, h - outerPad);

        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        p.setColor(dark ? Color.rgb(22, 26, 36) : Color.argb(238, 255, 255, 255));
        c.drawRoundRect(rect, radius, radius, p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(1));
        p.setColor(dark ? Color.rgb(42, 48, 62) : Color.rgb(232, 237, 246));
        c.drawRoundRect(rect, radius, radius, p);

        float innerPad = dp(7);
        float gap = dp(4);
        float tabW = (w - innerPad * 2f) / 2f;
        float left = innerPad + tabW * indicator + gap;
        float top = innerPad + gap * 0.25f;
        float right = left + tabW - gap * 2f;
        float bottom = h - innerPad - gap * 0.25f;

        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(22, 119, 255));
        c.drawRoundRect(new RectF(left, top, right, bottom), (bottom - top) / 2f, (bottom - top) / 2f, p);

        if (pressed) {
            p.setColor(Color.argb(20, 0, 0, 0));
            c.drawRoundRect(rect, radius, radius, p);
        }

        float leftCenter = innerPad + tabW * 0.5f;
        float rightCenter = innerPad + tabW * 1.5f;
        int activeColor = Color.WHITE;
        int normalColor = dark ? Color.rgb(148, 157, 174) : Color.rgb(103, 114, 132);
        drawHome(c, leftCenter, h * 0.50f, active == 0 ? activeColor : normalColor);
        drawUser(c, rightCenter, h * 0.50f, active == 1 ? activeColor : normalColor);
    }

    private void drawHome(Canvas c, float x, float y, int color) {
        p.setShader(null);
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(2.4f));
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        path.reset();
        path.moveTo(x - dp(10), y - dp(1));
        path.lineTo(x, y - dp(12));
        path.lineTo(x + dp(10), y - dp(1));
        c.drawPath(path, p);
        c.drawRoundRect(new RectF(x - dp(7), y, x + dp(7), y + dp(11)), dp(3), dp(3), p);
    }

    private void drawUser(Canvas c, float x, float y, int color) {
        p.setShader(null);
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(2.3f));
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        c.drawCircle(x, y - dp(6), dp(4.6f), p);
        c.drawArc(new RectF(x - dp(10), y + dp(1), x + dp(10), y + dp(15)), 205, 130, false, p);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
