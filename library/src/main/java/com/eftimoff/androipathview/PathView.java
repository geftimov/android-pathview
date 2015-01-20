package com.eftimoff.androipathview;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.eftimoff.mylibrary.R;

public class PathView extends View {

    private Paint paint;
    private Path path;
    private int pathColor;
    private float pathWidth;

    private float progress = 0f;
    private float pathLength = 0f;

    /* Huge rectangle to bound all possible paths */
    private Region clip = new Region(0, 0, 10000, 10000);
    /* Define the region */
    private final Region region = new Region();


    public PathView(Context context) {
        this(context, null);
        init();
    }

    public PathView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        init();
    }

    public PathView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getFromAttributes(context, attrs);
        init();
    }

    private void getFromAttributes(Context context, AttributeSet attrs) {
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PathView);
        pathColor = a.getColor(R.styleable.PathView_pathColor, 0xff00ff00);
        pathWidth = a.getFloat(R.styleable.PathView_pathWidth, 8.0f);
        a.recycle();
    }


    private void init() {
        paint = new Paint();
        paint.setColor(pathColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(pathWidth);
        paint.setAntiAlias(true);

        setPath(new Path());
    }

    public void setPath(Path p) {
        path = p;
        PathMeasure measure = new PathMeasure(path, false);
        pathLength = measure.getLength();
    }

    /**
     * Set the drawn path using an array of array of floats. First is x parameter, second is y.
     *
     * @param points The points to set on
     */
    public void setPath(float[]... points) {
        if (points.length == 0)
            throw new IllegalArgumentException("Cannot have zero points in the line");

        Path p = new Path();
        p.moveTo(points[0][0], points[0][1]);

        for (int i = 1; i < points.length; i++) {
            p.lineTo(points[i][0], points[i][1]);
        }

        setPath(p);
    }

    public void setPercentage(float percentage) {
        if (percentage < 0.0f || percentage > 1.0f)
            throw new IllegalArgumentException("setPercentage not between 0.0f and 1.0f");

        progress = percentage;
        invalidate();
    }

    public void scalePathBy(float x, float y) {
        final Matrix m = new Matrix();
        m.postScale(x, y);
        path.transform(m);
        PathMeasure measure = new PathMeasure(path, false);
        pathLength = measure.getLength();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final PathEffect pathEffect = new DashPathEffect(new float[]{pathLength, pathLength}, (pathLength - pathLength * progress));
        paint.setPathEffect(pathEffect);

        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        region.setPath(path, clip);

        final int widthOfPaths = (int) (pathWidth * 2);

        int desiredWidth = region.getBounds().left + region.getBounds().width() + widthOfPaths;
        int desiredHeight = region.getBounds().top + region.getBounds().height() + widthOfPaths;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;


        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            width = Math.min(desiredWidth, widthSize);
        } else {
            //Be whatever you want
            width = desiredWidth;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            height = Math.min(desiredHeight, heightSize);
        } else {
            //Be whatever you want
            height = desiredHeight;
        }

        setMeasuredDimension(width + 1, height + 1);
    }

    public void animatePath(int duration) {
        animatePath(duration, new LinearInterpolator());
    }

    public void animatePath(int duration, LinearInterpolator interpolator) {
        final ObjectAnimator anim = ObjectAnimator.ofFloat(this, "percentage", 0.0f, 1.0f);
        anim.setDuration(duration);
        anim.setInterpolator(interpolator);
        anim.start();
    }


    public void setPathColor(final int color) {
        pathColor = color;
        paint.setColor(pathColor);
    }

    public void setPathWidth(final int width) {
        pathWidth = width;
    }
}