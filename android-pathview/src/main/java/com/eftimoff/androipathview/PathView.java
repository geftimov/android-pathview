package com.eftimoff.androipathview;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;

import com.eftimoff.mylibrary.R;

import java.util.ArrayList;
import java.util.List;

public class PathView extends View {

    public static final String LOG_TAG = "PathView";

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final SvgUtils mSvg = new SvgUtils(paint);

    private float progress = 0f;

    private int svgResourceId;
    private List<SvgUtils.SvgPath> mPaths = new ArrayList<SvgUtils.SvgPath>(0);
    private final Object mSvgLock = new Object();
    private Thread mLoader;


    public PathView(Context context) {
        this(context, null);
    }

    public PathView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PathView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        paint.setStyle(Paint.Style.STROKE);
        getFromAttributes(context, attrs);
    }

    private void getFromAttributes(Context context, AttributeSet attrs) {
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PathView);
        try {
            if (a != null) {
                paint.setColor(a.getColor(R.styleable.PathView_pathColor, 0xff00ff00));
                paint.setStrokeWidth(a.getFloat(R.styleable.PathView_pathWidth, 8.0f));
                svgResourceId = a.getResourceId(R.styleable.PathView_svg, 0);
            }
        } finally {
            if (a != null) {
                a.recycle();
            }
        }
    }

    public void setPaths(List<Path> paths) {
        for (Path path : paths) {
            mPaths.add(new SvgUtils.SvgPath(path, paint));
        }
        synchronized (mSvgLock) {
            updatePathsPhaseLocked();
        }
    }

    public void setPath(Path p) {
        mPaths.add(new SvgUtils.SvgPath(p, paint));
        synchronized (mSvgLock) {
            updatePathsPhaseLocked();
        }
    }


    public void setPercentage(float percentage) {
        if (percentage < 0.0f || percentage > 1.0f)
            throw new IllegalArgumentException("setPercentage not between 0.0f and 1.0f");
        progress = percentage;
        synchronized (mSvgLock) {
            updatePathsPhaseLocked();
        }
        invalidate();
    }

    private void updatePathsPhaseLocked() {
        final int count = mPaths.size();
        for (int i = 0; i < count; i++) {
            SvgUtils.SvgPath svgPath = mPaths.get(i);
            svgPath.path.reset();
            svgPath.measure.getSegment(0.0f, svgPath.length * progress, svgPath.path, true);
            // Required only for Android 4.4 and earlier
            svgPath.path.rLineTo(0.0f, 0.0f);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (mSvgLock) {
            canvas.save();
            canvas.translate(getPaddingLeft(), getPaddingTop());
            final int count = mPaths.size();
            for (int i = 0; i < count; i++) {
                final SvgUtils.SvgPath svgPath = mPaths.get(i);
                canvas.drawPath(svgPath.path, svgPath.paint);
            }
            canvas.restore();
        }
    }

    @Override
    protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (mLoader != null) {
            try {
                mLoader.join();
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Unexpected error", e);
            }
        }
        if (svgResourceId != 0) {
            mLoader = new Thread(new Runnable() {
                @Override
                public void run() {

                    mSvg.load(getContext(), svgResourceId);

                    synchronized (mSvgLock) {
                        mPaths = mSvg.getPathsForViewport(
                                w - getPaddingLeft() - getPaddingRight(),
                                h - getPaddingTop() - getPaddingBottom());
                        updatePathsPhaseLocked();
                    }

                }
            }, "SVG Loader");
            mLoader.start();
        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (svgResourceId != 0) {
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);
            setMeasuredDimension(widthSize, heightSize);
            return;
        }


        int desiredWidth = 0;
        int desiredHeight = 0;
        final float strokeWidth = paint.getStrokeWidth() / 2;
        for (SvgUtils.SvgPath path : mPaths) {
            desiredWidth += path.bounds.left + path.bounds.width() + strokeWidth;
            desiredHeight += path.bounds.top + path.bounds.height() + strokeWidth;

        }
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(widthMeasureSpec);

        int measuredWidth, measuredHeight;

        if (widthMode == MeasureSpec.AT_MOST) {
            measuredWidth = desiredWidth;
        } else
            measuredWidth = widthSize;

        if (heightMode == MeasureSpec.AT_MOST) {
            measuredHeight = desiredHeight;
        } else
            measuredHeight = heightSize;

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    private AnimatorBuilder animatorBuilder;

    public AnimatorBuilder getPathAnimator() {
        if (animatorBuilder == null) {
            animatorBuilder = new AnimatorBuilder(this);
        }
        return animatorBuilder;
    }

    public int getPathColor() {
        return paint.getColor();
    }

    public void setPathColor(final int color) {
        paint.setColor(color);
    }

    public float getPathWidth() {
        return paint.getStrokeWidth();
    }

    public void setPathWidth(final float width) {
        paint.setStrokeWidth(width);
    }

    public int getSvgResource() {
        return svgResourceId;
    }

    public void setSvgResource(int svgResource) {
        svgResourceId = svgResource;
    }

    public static class AnimatorBuilder {

        private int duration = 350;
        private Interpolator interpolator;
        private int delay = 0;
        private final ObjectAnimator anim;
        private ListenerStart listenerStart;
        private ListenerEnd animationEnd;
        private PathViewAnimatorListener pathViewAnimatorListener;

        public AnimatorBuilder(final PathView pathView) {
            anim = ObjectAnimator.ofFloat(pathView, "percentage", 0.0f, 1.0f);
        }

        public AnimatorBuilder duration(final int duration) {
            this.duration = duration;
            return this;
        }

        public AnimatorBuilder interpolator(final Interpolator interpolator) {
            this.interpolator = interpolator;
            return this;
        }

        public AnimatorBuilder delay(final int delay) {
            this.delay = delay;
            return this;
        }

        public AnimatorBuilder listenerStart(final ListenerStart listenerStart) {
            this.listenerStart = listenerStart;
            if (pathViewAnimatorListener == null) {
                pathViewAnimatorListener = new PathViewAnimatorListener();
                anim.addListener(pathViewAnimatorListener);
            }
            return this;
        }

        public AnimatorBuilder listenerEnd(final ListenerEnd animationEnd) {
            this.animationEnd = animationEnd;
            if (pathViewAnimatorListener == null) {
                pathViewAnimatorListener = new PathViewAnimatorListener();
                anim.addListener(pathViewAnimatorListener);
            }
            return this;
        }

        public void start() {
            anim.setDuration(duration);
            anim.setInterpolator(interpolator);
            anim.setStartDelay(delay);
            anim.start();
        }

        private class PathViewAnimatorListener implements Animator.AnimatorListener {

            @Override
            public void onAnimationStart(Animator animation) {
                if (listenerStart != null)
                    listenerStart.onAnimationStart();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (animationEnd != null)
                    animationEnd.onAnimationEnd();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        }

        public interface ListenerStart {
            void onAnimationStart();
        }

        public interface ListenerEnd {
            void onAnimationEnd();
        }
    }
}
