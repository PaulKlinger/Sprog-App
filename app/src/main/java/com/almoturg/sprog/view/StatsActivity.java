package com.almoturg.sprog.view;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.almoturg.sprog.R;
import com.almoturg.sprog.model.PoemStatistics;
import com.almoturg.sprog.presenter.StatsPresenter;
import com.almoturg.sprog.util.Util;
import com.androidplot.xy.XYPlot;

import java.util.List;


public class StatsActivity extends AppCompatActivity {
    private StatsPresenter presenter;
    private XYPlot monthsNPlot;
    private XYPlot avgScorePlot;



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
        monthsNPlot = (XYPlot) findViewById(R.id.monthsNPlot);
        avgScorePlot = (XYPlot) findViewById(R.id.avgScorePlot);

        presenter.attachView(this);
    }

    public void displayStats(PoemStatistics stats) {
        ((TextView) findViewById(R.id.statsText)).setText(Util.fromHtml(
                getResources().getString(R.string.statistics_list,
                        stats.num, stats.num_read, stats.percent_read,
                        stats.avg_words, stats.total_words, stats.total_gold, stats.med_score,
                        stats.total_score, stats.total_timmy, stats.total_timmy_fucking_died)));
    }

    public void addMonthsPlotData(List<Number> xs, List<Number> ys) {
        Graphs.initGraph(this, monthsNPlot, xs, ys, Graphs.BAR, 10);
    }

    public void addAvgScorePlotData(List<Number> xs, List<Number> ys) {
        Graphs.initGraph(this, avgScorePlot, xs, ys, Graphs.LINE, 1000);
    }
}
