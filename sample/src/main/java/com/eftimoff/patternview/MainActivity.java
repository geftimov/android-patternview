package com.eftimoff.patternview;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {

    private PatternView patternView;

    private String patternString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        patternView = (PatternView) findViewById(R.id.patternView);
        patternView.setTactileFeedbackEnabled(false);
        Toast.makeText(getApplicationContext(), "ENTER PATTERN", Toast.LENGTH_LONG).show();
        patternView.setPathColor(Color.RED);
        patternView.setDotColor(Color.RED);
        patternView.setCircleColor(Color.RED);
        patternView.setOnPatternDetectedListener(new PatternView.OnPatternDetectedListener() {

            @Override
            public void onPatternDetected() {
                if (patternString == null) {
                    patternString = patternView.getPatternString();
//                    patternView.clearPattern();
                    return;
                }
                if (patternString.equals(patternView.getPatternString())) {
                    Toast.makeText(getApplicationContext(), "PATTERN CORRECT", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(getApplicationContext(), "PATTERN NOT CORRECT", Toast.LENGTH_SHORT).show();
//                patternView.clearPattern();
            }
        });

    }
}
