package com.almoturg.sprog.view;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.widget.TextView;

import com.almoturg.sprog.R;
import com.almoturg.sprog.model.PoemStatistics;
import com.almoturg.sprog.presenter.StatsPresenter;
import com.almoturg.sprog.util.Util;


public class StatsActivity extends AppCompatActivity {
    private StatsPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (presenter == null) {
            presenter = new StatsPresenter();
        }

        setContentView(R.layout.activity_stats);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        presenter.attachView(this);
    }

    public void displayStats(PoemStatistics stats) {
        ((TextView) findViewById(R.id.statsText)).setText(Util.fromHtml(
                getResources().getString(R.string.statistics_list,
                        stats.avg_words, stats.total_words, stats.total_gold, stats.med_score,
                        stats.total_score, stats.total_timmy, stats.total_timmy_fucking_died)));
    }

}
