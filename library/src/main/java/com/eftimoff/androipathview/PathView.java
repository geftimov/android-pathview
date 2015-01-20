package com.eftimoff.androipathview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.view.View;

import com.eftimoff.mylibrary.R;

public class PathView extends View {

	private Paint paint;
	private Path path;
	private int pathColor;
	private float pathWidth;

	private float progress = 0f;
	private float pathLength = 0f;


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
		final int asd = a.getInt(R.styleable.PathView_pathEffect, 3);

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
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(widthMeasureSpec);

		int measuredWidth, measuredHeight;

		if (widthMode == MeasureSpec.AT_MOST)
			throw new IllegalStateException("AnimatedPathView cannot have a WRAP_CONTENT property");
		else
			measuredWidth = widthSize;

		if (heightMode == MeasureSpec.AT_MOST)
			throw new IllegalStateException("AnimatedPathView cannot have a WRAP_CONTENT property");
		else
			measuredHeight = heightSize;

		setMeasuredDimension(measuredWidth, measuredHeight);
	}

	private enum PathEffectEnum {
		CORNER, DASH, DISCRETE, PATH
	}
}