package com.almoturg.sprog.view;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.almoturg.sprog.R;
import com.almoturg.sprog.model.PoemStatistics;

import static com.almoturg.sprog.model.Poems.poems;

public class StatsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        PoemStatistics stats = new PoemStatistics(poems);
        ((TextView) findViewById(R.id.statsText)).setText(
                String.format(
                        "# poems: %d\n total score: %d\n total words: %d\n median score: %f", stats.num, stats.total_score, stats.total_words, stats.med_score));
    }

}
