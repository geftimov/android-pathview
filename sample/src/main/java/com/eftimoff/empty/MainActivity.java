package com.eftimoff.empty;

import android.graphics.Color;
import android.graphics.Path;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;

import com.eftimoff.androipathview.PathView;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private PathView pathView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(this);
        pathView = (PathView) findViewById(R.id.pathView);
        pathView.setPathColor(Color.RED);

        // Create a straight line
        final Path path = new Path();
        final int x = 0;
        final int y = 350;
        path.moveTo(x, x);
        path.lineTo(x + y, x);
        path.lineTo(x + y, x + y);
        path.lineTo(x, x + y);
        path.lineTo(x, x);

        pathView.setPath(path);
    }

    @Override
    public void onClick(View v) {
        pathView.animatePath(5000);
    }
}
