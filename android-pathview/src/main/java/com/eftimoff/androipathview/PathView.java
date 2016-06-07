package com.eftimoff.androipathview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;

import com.eftimoff.mylibrary.R;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.AnimatorSet;

import java.util.ArrayList;
import java.util.List;

/**
 * PathView is a View that animates paths.
 */
@SuppressWarnings("unused")
public class PathView extends View implements SvgUtils.AnimationStepListener {
    /**
     * Logging tag.
     */
    public static final String LOG_TAG = "PathView";
    /**
     * The paint for the path.
     */
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /**
     * Utils to catch the paths from the svg.
     */
    private final SvgUtils svgUtils = new SvgUtils(paint);
    /**
     * All the paths provided to the view. Both from Path and Svg.
     */
    private List<SvgUtils.SvgPath> paths = new ArrayList<>();
    /**
     * This is a lock before the view is redrawn
     * or resided it must be synchronized with this object.
     */
    private final Object mSvgLock = new Object();
    /**
     * Thread for working with the object above.
     */
    private Thread mLoader;

    /**
     * The svg image from the raw directory.
     */
    private int svgResourceId;
    /**
     * Object that builds the animation for the path.
     */
    private AnimatorBuilder animatorBuilder;
    /**
     * Object that builds the animation set for the path.
     */
    private AnimatorSetBuilder animatorSetBuilder;
    /**
     * The progress of the drawing.
     */
    private float progress = 0f;

    /**
     * If the used colors are from the svg or from the set color.
     */
    private boolean naturalColors;
    /**
     * If the view is filled with its natural colors after path drawing.
     */
    private boolean fillAfter;
    /**
     * The view will be filled and showed as default without any animation.
     */
    private boolean fill;
    /**
     * The solid color used for filling svg when fill is true
     */
    private int fillColor;
    /**
     * The width of the view.
     */
    private int width;
    /**
     * The height of the view.
     */
    private int height;
    /**
     * Will be used as a temporary surface in each onDraw call for more control over content are
     * drawing.
     */
    private Bitmap mTempBitmap;
    /**
     * Will be used as a temporary Canvas for mTempBitmap for drawing content on it.
     */
    private Canvas mTempCanvas;


    /**
     * Default constructor.
     *
     * @param context The Context of the application.
     */
    public PathView(Context context) {
        this(context, null);
    }

    /**
     * Default constructor.
     *
     * @param context The Context of the application.
     * @param attrs   attributes provided from the resources.
     */
    public PathView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Default constructor.
     *
     * @param context  The Context of the application.
     * @param attrs    attributes provided from the resources.
     * @param defStyle Default style.
     */
    public PathView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        paint.setStyle(Paint.Style.STROKE);
        getFromAttributes(context, attrs);
    }

    /**
     * Get all the fields from the attributes .
     *
     * @param context The Context of the application.
     * @param attrs   attributes provided from the resources.
     */
    private void getFromAttributes(Context context, AttributeSet attrs) {
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PathView);
        try {
            if (a != null) {
                paint.setColor(a.getColor(R.styleable.PathView_pathColor, 0xff00ff00));
                paint.setStrokeWidth(a.getDimensionPixelSize(R.styleable.PathView_pathWidth, 8));
                svgResourceId = a.getResourceId(R.styleable.PathView_svg, 0);
                naturalColors = a.getBoolean(R.styleable.PathView_naturalColors, false);
                fill = a.getBoolean(R.styleable.PathView_fill,false);
                fillColor = a.getColor(R.styleable.PathView_fillColor,Color.argb(0,0,0,0));
            }
        } finally {
            if (a != null) {
                a.recycle();
            }
            //to draw the svg in first show , if we set fill to true
            invalidate();
        }
    }

    /**
     * Set paths to be drawn and animated.
     *
     * @param paths - Paths that can be drawn.
     */
    public void setPaths(final List<Path> paths) {
        for (Path path : paths) {
            this.paths.add(new SvgUtils.SvgPath(path, paint));
        }
        synchronized (mSvgLock) {
            updatePathsPhaseLocked();
        }
    }

    /**
     * Set path to be drawn and animated.
     *
     * @param path - Paths that can be drawn.
     */
    public void setPath(final Path path) {
        paths.add(new SvgUtils.SvgPath(path, paint));
        synchronized (mSvgLock) {
            updatePathsPhaseLocked();
        }
    }

    /**
     * Animate this property. It is the percentage of the path that is drawn.
     * It must be [0,1].
     *
     * @param percentage float the percentage of the path.
     */
    public void setPercentage(float percentage) {
        if (percentage < 0.0f || percentage > 1.0f) {
            throw new IllegalArgumentException("setPercentage not between 0.0f and 1.0f");
        }
        progress = percentage;
        synchronized (mSvgLock) {
            updatePathsPhaseLocked();
        }
        invalidate();
    }

    /**
     * This refreshes the paths before draw and resize.
     */
    private void updatePathsPhaseLocked() {
        final int count = paths.size();
        for (int i = 0; i < count; i++) {
            SvgUtils.SvgPath svgPath = paths.get(i);
            svgPath.path.reset();
            svgPath.measure.getSegment(0.0f, svgPath.length * progress, svgPath.path, true);
            // Required only for Android 4.4 and earlier
            svgPath.path.rLineTo(0.0f, 0.0f);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(mTempBitmap==null || (mTempBitmap.getWidth()!=canvas.getWidth()||mTempBitmap.getHeight()!=canvas.getHeight()) )
        {
            mTempBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
            mTempCanvas = new Canvas(mTempBitmap);
        }

        mTempBitmap.eraseColor(0);
        synchronized (mSvgLock) {
            mTempCanvas.save();
            mTempCanvas.translate(getPaddingLeft(), getPaddingTop());
            fill(mTempCanvas);
            final int count = paths.size();
            for (int i = 0; i < count; i++) {
                final SvgUtils.SvgPath svgPath = paths.get(i);
                final Path path = svgPath.path;
                final Paint paint1 = naturalColors ? svgPath.paint : paint;
                mTempCanvas.drawPath(path, paint1);
            }

            fillAfter(mTempCanvas);

            mTempCanvas.restore();

            applySolidColor(mTempBitmap);

            canvas.drawBitmap(mTempBitmap,0,0,null);
        }
    }
    /**
     * If there is svg , the user called setFillAfter(true) and the progress is finished.
     *
     * @param canvas Draw to this canvas.
     */
    private void fillAfter(final Canvas canvas) {
        if (svgResourceId != 0 && fillAfter && Math.abs(progress - 1f) < 0.00000001) {
            svgUtils.drawSvgAfter(canvas, width, height);
        }
    }

    /**
     * If there is svg , the user called setFill(true).
     *
     * @param canvas Draw to this canvas.
     */
    private void fill(final Canvas canvas) {
        if (svgResourceId != 0 && fill) {
            svgUtils.drawSvgAfter(canvas, width, height);
        }
    }

    /**
     * If fillColor had value before then we replace untransparent pixels of bitmap by solid color
     *
     * @param bitmap Draw to this canvas.
     */
    private void applySolidColor(final Bitmap bitmap) {
        if(fill && fillColor!=Color.argb(0,0,0,0) )
            if (bitmap != null) {
                for(int x=0;x<bitmap.getWidth();x++)
                {
                    for(int y=0;y<bitmap.getHeight();y++)
                    {
                        int argb = bitmap.getPixel(x,y);
                        int alpha = Color.alpha(argb);
                        if(alpha!=0)
                        {
                            int red = Color.red(fillColor);
                            int green = Color.green(fillColor);
                            int blue =  Color.blue(fillColor);
                            argb = Color.argb(alpha,red,green,blue);
                            bitmap.setPixel(x,y,argb);
                        }
                    }
                }
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

                    svgUtils.load(getContext(), svgResourceId);

                    synchronized (mSvgLock) {
                        width = w - getPaddingLeft() - getPaddingRight();
                        height = h - getPaddingTop() - getPaddingBottom();
                        paths = svgUtils.getPathsForViewport(width, height);
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
        for (SvgUtils.SvgPath path : paths) {
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
        } else {
            measuredWidth = widthSize;
        }

        if (heightMode == MeasureSpec.AT_MOST) {
            measuredHeight = desiredHeight;
        } else {
            measuredHeight = heightSize;
        }

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    /**
     * If the real svg need to be drawn after the path animation.
     *
     * @param fillAfter - boolean if the view needs to be filled after path animation.
     */
    public void setFillAfter(final boolean fillAfter) {
        this.fillAfter = fillAfter;
    }
    /**
     * If the real svg need to be drawn without the path animation.
     *
     * @param fill - boolean if the view needs to be filled after path animation.
     */
    public void setFill(final boolean fill) {
        this.fill = fill;
    }
    /**
     * The color for drawing svg in that color if the color be not transparent
     *
     * @param color - the color for filling in that
     */
    public void setFillColor(final int color){
        this.fillColor=color;
    }
    /**
     * If you want to use the colors from the svg.
     */
    public void useNaturalColors() {
        naturalColors = true;
    }

    /**
     * Animator for the paths of the view.
     *
     * @return The AnimatorBuilder to build the animation.
     */
    public AnimatorBuilder getPathAnimator() {
        if (animatorBuilder == null) {
            animatorBuilder = new AnimatorBuilder(this);
        }
        return animatorBuilder;
    }

    /**
     * AnimatorSet for the paths of the view to be animated one after the other.
     *
     * @return The AnimatorBuilder to build the animation.
     */
    public AnimatorSetBuilder getSequentialPathAnimator() {
        if (animatorSetBuilder == null) {
            animatorSetBuilder = new AnimatorSetBuilder(this);
        }
        return animatorSetBuilder;
    }

    /**
     * Get the path color.
     *
     * @return The color of the paint.
     */
    public int getPathColor() {
        return paint.getColor();
    }

    /**
     * Set the path color.
     *
     * @param color -The color to set to the paint.
     */
    public void setPathColor(final int color) {
        paint.setColor(color);
    }

    /**
     * Get the path width.
     *
     * @return The width of the paint.
     */
    public float getPathWidth() {
        return paint.getStrokeWidth();
    }

    /**
     * Set the path width.
     *
     * @param width - The width of the path.
     */
    public void setPathWidth(final float width) {
        paint.setStrokeWidth(width);
    }

    /**
     * Get the svg resource id.
     *
     * @return The svg raw resource id.
     */
    public int getSvgResource() {
        return svgResourceId;
    }

    /**
     * Set the svg resource id.
     *
     * @param svgResource - The resource id of the raw svg.
     */
    public void setSvgResource(int svgResource) {
        svgResourceId = svgResource;
    }

    /**
     * Object for building the animation of the path of this view.
     */
    public static class AnimatorBuilder {
        /**
         * Duration of the animation.
         */
        private int duration = 350;
        /**
         * Interpolator for the time of the animation.
         */
        private Interpolator interpolator;
        /**
         * The delay before the animation.
         */
        private int delay = 0;
        /**
         * ObjectAnimator that constructs the animation.
         */
        private final ObjectAnimator anim;
        /**
         * Listener called before the animation.
         */
        private ListenerStart listenerStart;
        /**
         * Listener after the animation.
         */
        private ListenerEnd animationEnd;
        /**
         * Animation listener.
         */
        private PathViewAnimatorListener pathViewAnimatorListener;

        /**
         * Default constructor.
         *
         * @param pathView The view that must be animated.
         */
        public AnimatorBuilder(final PathView pathView) {
            anim = ObjectAnimator.ofFloat(pathView, "percentage", 0.0f, 1.0f);
        }

        /**
         * Set the duration of the animation.
         *
         * @param duration - The duration of the animation.
         * @return AnimatorBuilder.
         */
        public AnimatorBuilder duration(final int duration) {
            this.duration = duration;
            return this;
        }

        /**
         * Set the Interpolator.
         *
         * @param interpolator - Interpolator.
         * @return AnimatorBuilder.
         */
        public AnimatorBuilder interpolator(final Interpolator interpolator) {
            this.interpolator = interpolator;
            return this;
        }

        /**
         * The delay before the animation.
         *
         * @param delay - int the delay
         * @return AnimatorBuilder.
         */
        public AnimatorBuilder delay(final int delay) {
            this.delay = delay;
            return this;
        }

        /**
         * Set a listener before the start of the animation.
         *
         * @param listenerStart an interface called before the animation
         * @return AnimatorBuilder.
         */
        public AnimatorBuilder listenerStart(final ListenerStart listenerStart) {
            this.listenerStart = listenerStart;
            if (pathViewAnimatorListener == null) {
                pathViewAnimatorListener = new PathViewAnimatorListener();
                anim.addListener(pathViewAnimatorListener);
            }
            return this;
        }

        /**
         * Set a listener after of the animation.
         *
         * @param animationEnd an interface called after the animation
         * @return AnimatorBuilder.
         */
        public AnimatorBuilder listenerEnd(final ListenerEnd animationEnd) {
            this.animationEnd = animationEnd;
            if (pathViewAnimatorListener == null) {
                pathViewAnimatorListener = new PathViewAnimatorListener();
                anim.addListener(pathViewAnimatorListener);
            }
            return this;
        }

        /**
         * Starts the animation.
         */
        public void start() {
            anim.setDuration(duration);
            anim.setInterpolator(interpolator);
            anim.setStartDelay(delay);
            anim.start();
        }

        /**
         * Animation listener to be able to provide callbacks for the caller.
         */
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

        /**
         * Called when the animation start.
         */
        public interface ListenerStart {
            /**
             * Called when the path animation start.
             */
            void onAnimationStart();
        }

        /**
         * Called when the animation end.
         */
        public interface ListenerEnd {
            /**
             * Called when the path animation end.
             */
            void onAnimationEnd();
        }
    }

    @Override
    public void onAnimationStep() {
        invalidate();
    }

    /**
     * Object for building the sequential animation of the paths of this view.
     */
    public static class AnimatorSetBuilder {
        /**
         * Duration of the animation.
         */
        private int duration = 1000;
        /**
         * Interpolator for the time of the animation.
         */
        private Interpolator interpolator;
        /**
         * The delay before the animation.
         */
        private int delay = 0;
        /**
         * List of ObjectAnimator that constructs the animations of each path.
         */
        private final List<Animator> animators = new ArrayList<>();
        /**
         * Listener called before the animation.
         */
        private AnimatorBuilder.ListenerStart listenerStart;
        /**
         * Listener after the animation.
         */
        private AnimatorBuilder.ListenerEnd animationEnd;
        /**
         * Animation listener.
         */
        private AnimatorSetBuilder.PathViewAnimatorListener pathViewAnimatorListener;
        /**
         * The animator that can animate paths sequentially
         */
        private AnimatorSet animatorSet = new AnimatorSet();
        /**
         * The list of paths to be animated.
         */
        private List<SvgUtils.SvgPath> paths;

        /**
         * Default constructor.
         *
         * @param pathView The view that must be animated.
         */
        public AnimatorSetBuilder(final PathView pathView) {
            paths = pathView.paths;
            for (SvgUtils.SvgPath path : paths) {
                path.setAnimationStepListener(pathView);
                ObjectAnimator animation = ObjectAnimator.ofFloat(path, "length", 0.0f, path.getLength());
                animators.add(animation);
            }
            animatorSet.playSequentially(animators);
        }

        /**
         * Sets the duration of the animation. Since the AnimatorSet sets the duration for each
         * Animator, we have to divide it by the number of paths.
         *
         * @param duration - The duration of the animation.
         * @return AnimatorSetBuilder.
         */
        public AnimatorSetBuilder duration(final int duration) {
            this.duration = duration / paths.size();
            return this;
        }

        /**
         * Set the Interpolator.
         *
         * @param interpolator - Interpolator.
         * @return AnimatorSetBuilder.
         */
        public AnimatorSetBuilder interpolator(final Interpolator interpolator) {
            this.interpolator = interpolator;
            return this;
        }

        /**
         * The delay before the animation.
         *
         * @param delay - int the delay
         * @return AnimatorSetBuilder.
         */
        public AnimatorSetBuilder delay(final int delay) {
            this.delay = delay;
            return this;
        }

        /**
         * Set a listener before the start of the animation.
         *
         * @param listenerStart an interface called before the animation
         * @return AnimatorSetBuilder.
         */
        public AnimatorSetBuilder listenerStart(final AnimatorBuilder.ListenerStart listenerStart) {
            this.listenerStart = listenerStart;
            if (pathViewAnimatorListener == null) {
                pathViewAnimatorListener = new PathViewAnimatorListener();
                animatorSet.addListener(pathViewAnimatorListener);
            }
            return this;
        }

        /**
         * Set a listener after of the animation.
         *
         * @param animationEnd an interface called after the animation
         * @return AnimatorSetBuilder.
         */
        public AnimatorSetBuilder listenerEnd(final AnimatorBuilder.ListenerEnd animationEnd) {
            this.animationEnd = animationEnd;
            if (pathViewAnimatorListener == null) {
                pathViewAnimatorListener = new PathViewAnimatorListener();
                animatorSet.addListener(pathViewAnimatorListener);
            }
            return this;
        }

        /**
         * Starts the animation.
         */
        public void start() {
            resetAllPaths();
            animatorSet.cancel();
            animatorSet.setDuration(duration);
            animatorSet.setInterpolator(interpolator);
            animatorSet.setStartDelay(delay);
            animatorSet.start();
        }

        /**
         * Sets the length of all the paths to 0.
         */
        private void resetAllPaths() {
            for (SvgUtils.SvgPath path : paths) {
                path.setLength(0);
            }
        }

        /**
         * Animation listener to be able to provide callbacks for the caller.
         */
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
    }
}
