package com.eftimoff.patternview;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;


public class MainActivity extends ActionBarActivity {

    private PatternView patternView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        patternView = (PatternView) findViewById(R.id.patternView);

        patternView.setOnPatternListener(new PatternView.OnPatternListener() {

            private String patternString;

            @Override
            public void onPatternStart() {
                Log.i("!!!", "onPatternStart");

            }

            @Override
            public void onPatternCleared() {
                Log.i("!!!", "onPatternCleared");

            }

            @Override
            public void onPatternCellAdded() {
                Log.i("!!!", "onPatternCellAdded");
//
            }

            @Override
            public void onPatternDetected() {
                Log.i("!!!", "onPatternDetected");
                if (patternString == null) {
                    patternString = patternView.getPatternString();
                    patternView.clearPattern();
                    return;
                }
                if (patternString.equals(patternView.getPatternString())) {
                    Log.i("!!!", "CORRECT");
                }
//                patternView.setDisplayMode(PatternView.DisplayMode.Wrong);
//                patternView.setDisplayMode(PatternView.DisplayMode.Correct);
            }
        });

    }
}
