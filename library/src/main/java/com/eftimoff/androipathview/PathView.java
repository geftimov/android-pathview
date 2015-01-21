package com.eftimoff.androipathview;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.eftimoff.mylibrary.R;

import java.util.Arrays;
import java.util.List;

public class PathView extends View {

	public static final String LOG_TAG = "PathView";

	private Paint paint;
	private int pathColor;
	private float pathWidth;

	private float progress = 0f;

	private int svgResourceId;
	private List<Path> paths;

	private final Object mSvgLock = new Object();
	private Thread mLoader;


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
		try {
			if (a != null) {
				pathColor = a.getColor(R.styleable.PathView_pathColor, 0xff00ff00);
				pathWidth = a.getFloat(R.styleable.PathView_pathWidth, 8.0f);
				svgResourceId = a.getResourceId(R.styleable.PathView_svg, 0);
			}
		} finally {
			if (a != null) {
				a.recycle();
			}
		}
	}


	private void init() {
		paint = new Paint();
		paint.setColor(pathColor);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(pathWidth);
		paint.setAntiAlias(true);

		setPath(new Path());
	}

	public void setPaths(List<Path> paths) {
		this.paths = paths;

	}

	public void setPath(Path p) {
		this.paths = Arrays.asList(p);
	}


	public void setPercentage(float percentage) {
		if (percentage < 0.0f || percentage > 1.0f)
			throw new IllegalArgumentException("setPercentage not between 0.0f and 1.0f");

		progress = percentage;
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);


		synchronized (mSvgLock) {
			canvas.save();
			canvas.translate(getPaddingLeft(), getPaddingTop());
			final int count = paths.size();
			for (int i = 0; i < count; i++) {
				final Path path = paths.get(i);
				updatePathAndPaint(path);
				canvas.drawPath(path, paint);
			}
			canvas.restore();
		}
	}

	private void updatePathAndPaint(Path path) {
		final PathMeasure measure = new PathMeasure(path, false);
		float pathLength = measure.getLength();
		final PathEffect pathEffect = new DashPathEffect(new float[]{pathLength, pathLength}, (pathLength - pathLength * progress));
		paint.setPathEffect(pathEffect);
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

		mLoader = new Thread(new Runnable() {
			@Override
			public void run() {
				synchronized (mSvgLock) {
					if (svgResourceId != 0) {
						paths = SVGUtils.getPathsFromSvg(getContext(), svgResourceId, w, h);
					}
				}
			}
		}, "SVG Loader");
		mLoader.start();

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

	public void animatePath(int duration) {
		animatePath(duration, new LinearInterpolator());
	}

	public void animatePath(int duration, LinearInterpolator interpolator) {
		final ObjectAnimator anim = ObjectAnimator.ofFloat(this, "percentage", 0.0f, 1.0f);
		anim.setDuration(duration);
		anim.setInterpolator(interpolator);
		anim.start();
	}

	public int getPathColor() {
		return pathColor;
	}


	public void setPathColor(final int color) {
		pathColor = color;
		paint.setColor(pathColor);
	}

	public float getPathWidth() {
		return pathWidth;
	}

	public void setPathWidth(final float width) {
		pathWidth = width;
	}

	public int getSvgResource() {
		return svgResourceId;
	}

	public void setSvgResource(int svgResource) {
		svgResourceId = svgResource;
		invalidate();
	}
}