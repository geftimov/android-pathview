package com.eftimoff.empty;

import android.graphics.Path;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.eftimoff.androipathview.PathView;


public class SecondActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        final PathView pathView = (PathView) findViewById(R.id.pathView);
//		final Path path = makeConvexArrow(50, 100);
//		pathView.setPath(path);
        pathView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pathView.getPathAnimator().
                        delay(500).
                        duration(4500).
                        interpolator(new AccelerateDecelerateInterpolator()).
                        listenerStart(new PathView.AnimatorBuilder.ListenerStart() {
                            @Override
                            public void onAnimationStart() {
                                Log.i("PATHVIEW", "ANIMATION START");
                            }
                        }).
                        listenerEnd(new PathView.AnimatorBuilder.ListenerEnd() {
                            @Override
                            public void onAnimationEnd() {
                                Log.i("PATHVIEW", "ANIMATION END");
                            }
                        }).
                        start();
            }
        });
    }

    private Path makeConvexArrow(float length, float height) {
        final Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        path.lineTo(length / 4f, 0.0f);
        path.lineTo(length, height / 2.0f);
        path.lineTo(length / 4f, height);
        path.lineTo(0.0f, height);
        path.lineTo(length * 3f / 4f, height / 2f);
        path.lineTo(0.0f, 0.0f);
        path.close();
        return path;
    }


}
