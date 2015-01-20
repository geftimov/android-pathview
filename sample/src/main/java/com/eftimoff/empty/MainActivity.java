package com.eftimoff.empty;

import android.animation.ObjectAnimator;
import android.graphics.Path;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.animation.LinearInterpolator;

import com.eftimoff.androipathview.PathView;


public class MainActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Create a straight line
		Path path = new Path();
		path.moveTo(32, 32);
		path.lineTo(232, 32);

		final PathView view = (PathView) findViewById(R.id.pathView);
		view.setPath(path);

		ObjectAnimator anim = ObjectAnimator.ofFloat(view, "percentage", 0.0f, 1.0f);
		anim.setDuration(2000);
		anim.setInterpolator(new LinearInterpolator());
		anim.start();
	}
}
