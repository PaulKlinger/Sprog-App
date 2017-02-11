package com.almoturg.sprog.presenter;

import com.almoturg.sprog.model.PoemStatistics;
import com.almoturg.sprog.view.StatsActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import static com.almoturg.sprog.model.Poems.poems;

public class StatsPresenter {
    private PoemStatistics stats;
    private StatsActivity activity;

    public StatsPresenter() {
        stats = new PoemStatistics(poems);
    }

    public void attachView(StatsActivity activity) {
        this.activity = activity;
        activity.displayStats(stats);
        initMonthsNPlot();
        initAvgScorePlot();
    }

    private void initMonthsNPlot() {
        LinkedHashMap<Integer, Integer> monthsNPoems = stats.getMonthNPoems();
        ArrayList<Number> xs = new ArrayList<>();
        ArrayList<Number> ys = new ArrayList<>();
        for (int key : monthsNPoems.keySet()) {
            xs.add(key);
            ys.add(monthsNPoems.get(key));
        }
        activity.addMonthsPlotData(xs, ys);
    }

    private void initAvgScorePlot() {
        LinkedHashMap<Integer, Double> monthsNPoems = stats.getMonthAvgScore();
        ArrayList<Number> xs = new ArrayList<>();
        ArrayList<Number> ys = new ArrayList<>();
        for (int key : monthsNPoems.keySet()) {
            xs.add(key);
            ys.add(monthsNPoems.get(key));
        }
        activity.addAvgScorePlotData(xs, ys);
    }
}
