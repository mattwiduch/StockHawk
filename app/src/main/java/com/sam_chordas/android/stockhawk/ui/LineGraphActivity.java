package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.sam_chordas.android.stockhawk.R;

/**
 * Created by frano on 21/03/2016.
 */
public class LineGraphActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            arguments.putString(getString(R.string.line_graph_extra),
                    getIntent().getStringExtra(getString(R.string.line_graph_extra)));

            LineGraphFragment fragment = new LineGraphFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.stock_detail_container, fragment)
                    .commit();
        }
    }
}
