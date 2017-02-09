package com.almoturg.sprog.presenter;

import com.almoturg.sprog.model.PoemStatistics;
import com.almoturg.sprog.view.StatsActivity;

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
    }
}
