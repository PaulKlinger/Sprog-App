package com.almoturg.sprog.view;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.almoturg.sprog.R;
import com.almoturg.sprog.model.PoemStatistics;
import com.almoturg.sprog.presenter.StatsPresenter;
import com.almoturg.sprog.util.Util;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BarFormatter;
import com.androidplot.xy.BarRenderer;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.util.List;


public class StatsActivity extends AppCompatActivity {
    private StatsPresenter presenter;
    private XYPlot graph;

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
        graph = (XYPlot) findViewById(R.id.graph);

        presenter.attachView(this);
    }

    public void displayStats(PoemStatistics stats) {
        ((TextView) findViewById(R.id.statsText)).setText(Util.fromHtml(
                getResources().getString(R.string.statistics_list,
                        stats.avg_words, stats.total_words, stats.total_gold, stats.med_score,
                        stats.total_score, stats.total_timmy, stats.total_timmy_fucking_died)));
    }

    public void addGraphData(List<Number> xs, List<Number> ys) {
        XYSeries data = new SimpleXYSeries(xs, ys, "months n");
        BarFormatter bf = new BarFormatter(Color.DKGRAY, Color.TRANSPARENT);
        graph.addSeries(data, bf);
        BarRenderer br = graph.getRenderer(BarRenderer.class);
        br.setBarGroupWidth(BarRenderer.BarGroupWidthMode.FIXED_GAP, 0);

    }

}
