package com.almoturg.sprog.view;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.almoturg.sprog.R;
import com.almoturg.sprog.model.PoemStatistics;
import com.almoturg.sprog.presenter.StatsPresenter;
import com.almoturg.sprog.util.Util;
import com.androidplot.xy.BarFormatter;
import com.androidplot.xy.BarRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PanZoom;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.List;

import com.annimon.stream.Stream;


public class StatsActivity extends AppCompatActivity {
    private StatsPresenter presenter;
    private XYPlot monthsNPlot;
    private XYPlot avgScorePlot;

    private static final int BAR = 1;
    private static final int LINE = 2;

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
                        stats.avg_words, stats.total_words, stats.total_gold, stats.med_score,
                        stats.total_score, stats.total_timmy, stats.total_timmy_fucking_died)));
    }

    public void addMonthsPlotData(List<Number> xs, List<Number> ys) {
        addGraphData(monthsNPlot, xs, ys, BAR, 10);
    }

    public void addAvgScorePlotData(List<Number> xs, List<Number> ys) {
        addGraphData(avgScorePlot, xs, ys, LINE, 1000);
    }

    private void addGraphData(XYPlot plot, List<Number> xs, List<Number> ys, int type, int yStep) {
        XYSeries data = new SimpleXYSeries(xs, ys, "");
        if (type == BAR) {
            BarFormatter formatter = new BarFormatter(
                    ContextCompat.getColor(this, R.color.colorGraph), Color.TRANSPARENT);
            formatter.setLegendIconEnabled(false);
            plot.addSeries(data, formatter);
            BarRenderer br = plot.getRenderer(BarRenderer.class);
            br.setBarGroupWidth(BarRenderer.BarGroupWidthMode.FIXED_GAP, 2);
            plot.getGraph().getDomainGridLinePaint().setColor(Color.TRANSPARENT);
        } else if (type == LINE) {
            LineAndPointFormatter formatter = new LineAndPointFormatter(
                    ContextCompat.getColor(this, R.color.colorGraph),
                    Color.TRANSPARENT, Color.TRANSPARENT, null);
            formatter.setLegendIconEnabled(false);
            plot.addSeries(data, formatter);
        } else {
            throw new UnsupportedOperationException();
        }


        plot.setDomainBoundaries((int) xs.get(xs.size() - 1) - Util.getDisplayWidthDp(this) / 20,
                xs.get(xs.size() - 1).intValue() + 0.5, BoundaryMode.FIXED);
        int maxy = Stream.of(ys).mapToInt(n -> n.intValue()).max().orElse(0);
        plot.setRangeBoundaries(0, maxy, BoundaryMode.FIXED);
        PanZoom panZoom = PanZoom.attach(plot);
        panZoom.setPan(PanZoom.Pan.HORIZONTAL);
        panZoom.setZoom(PanZoom.Zoom.NONE);
        plot.getOuterLimits().set((int) xs.get(0) - 1, xs.get(xs.size() - 1).intValue() + 0.5,
                -10, maxy);

        plot.getGraph().setLineLabelEdges(XYGraphWidget.Edge.BOTTOM, XYGraphWidget.Edge.LEFT);
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new NumberFormat() {
            @Override
            public StringBuffer format(long val, StringBuffer buffer, FieldPosition pos) {
                return null;
            }

            @Override
            public Number parse(String source, ParsePosition parsePosition) {
                return null;
            }

            @Override
            public StringBuffer format(double val, StringBuffer buffer, FieldPosition pos) {
                int year = (int) val / 12;
                int month = (int) val % 12 + 1;
                if (month == 1) {
                    return new StringBuffer(String.format("%d-%d", year, month));
                } else {
                    return new StringBuffer(String.format("%d", month));
                }
            }
        });
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new NumberFormat() {
            @Override
            public StringBuffer format(long obj, StringBuffer buffer, FieldPosition pos) {
                return null;
            }

            @Override
            public Number parse(String source, ParsePosition parsePosition) {
                return null;
            }

            @Override
            public StringBuffer format(double val, StringBuffer buffer, FieldPosition pos) {
                if (val < 1000) {
                    return new StringBuffer(String.format("%.0f", val));
                } else {
                    return new StringBuffer(String.format("%.0fK", val / 1000));
                }
            }

        });
        plot.setUserDomainOrigin(0); // This makes vertical lines move with panning
        plot.setDomainStep(StepMode.INCREMENT_BY_VAL, 2);
        plot.getGraph().setLinesPerDomainLabel(1);

        plot.setRangeStep(StepMode.INCREMENT_BY_VAL, yStep);
        plot.setLinesPerRangeLabel(1);

        plot.setBorderPaint(null);
        plot.getBackgroundPaint().setColor(Color.TRANSPARENT);
        plot.setPlotMargins(0, 0, 0, 0);
    }

}
