package com.eftimoff.androipathview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;

import com.caverock.androidsvg.PreserveAspectRatio;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.util.ArrayList;
import java.util.List;

public class SVGUtils {


	public static final String LOG_TAG = "SVGUtils";

	public static List<Path> getPathsFromSvg(final Context context, final int rawSvgResource, float width, float height) {
		final List<Path> paths = new ArrayList<>();
		Canvas canvas = new Canvas() {
			private final Matrix mMatrix = new Matrix();

			@Override
			public void drawPath(Path path, Paint paint) {
				Path dst = new Path();

				// Get the current transform matrix
				getMatrix(mMatrix);
				// Apply the matrix to the path
				path.transform(mMatrix, dst);
				// Store the transformed path
//				mPaths.add(new SvgPath(dst, new Paint(mSourcePaint)));
				paths.add(dst);
			}
		};

		// Load an SVG document
		final SVG svg;
		try {
			svg = SVG.getFromResource(context, rawSvgResource);
			svg.setDocumentPreserveAspectRatio(PreserveAspectRatio.UNSCALED);
			RectF viewBox = svg.getDocumentViewBox();
			float scale = Math.min(width / viewBox.width(), height / viewBox.height());

			canvas.translate(
					(width - viewBox.width() * scale) / 2.0f,
					(height - viewBox.height() * scale) / 2.0f);
			canvas.scale(scale, scale);
			// Capture the paths
			svg.renderToCanvas(canvas);
		} catch (SVGParseException e) {
			Log.e(LOG_TAG, "Could not load specified SVG resource", e);
		}
		return paths;

	}


}
