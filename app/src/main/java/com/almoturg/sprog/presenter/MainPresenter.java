package com.almoturg.sprog.presenter;

import com.almoturg.sprog.model.Poem;
import com.almoturg.sprog.model.PreferencesRepository;
import com.almoturg.sprog.ui.MainActivity;
import com.almoturg.sprog.util.ParsePoemsTask;
import com.almoturg.sprog.util.PoemsLoader;
import com.almoturg.sprog.util.SprogDbHelper;
import com.almoturg.sprog.util.Util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.TimeZone;

import static com.almoturg.sprog.SprogApplication.filtered_poems;
import static com.almoturg.sprog.SprogApplication.poems;

public class MainPresenter {
    private MainActivity activity;
    private PreferencesRepository preferences;
    private SprogDbHelper dbhelper;

    public ArrayList<String> new_read_poems = new ArrayList<>(); // Poems newly marked as read

    public boolean processing;
    public boolean updating;
    public boolean show_only_favorites;

    // for analytics tracking of search queries
    private String last_search_string = "";
    private boolean sent_search = false;

    public String sort_order = "Date";

    public MainPresenter(MainActivity activity, PreferencesRepository preferences,
                         SprogDbHelper dbhelper) {
        this.activity = activity;
        this.preferences = preferences;
        this.dbhelper = dbhelper;
    }

    public void onStart() {
        // A dialog asking whether to enable new poem notifications is shown
        // the second time MainActivity is started.
        if (Util.timeToShowNotifyDialog(preferences)) {
            activity.showNotifyDialog();
        }

        activity.cancelNotifications();
        boolean update = preferences.getUpdateNext();
        if (update) {
            preferences.setUpdateNext(false);
        }
        preparePoems(update);
    }

    public void onPause() {
        dbhelper.addReadPoems(new_read_poems);
        new_read_poems.clear();
        cancelLoadingPoems();
    }

    private void preparePoems(boolean update) { // not sure about the name...
        long last_update_tstamp = preferences.getLastUpdateTime();
        long last_fcm_tstamp = preferences.getLastFCMTimestamp();
        boolean internet_access = Util.isConnected(activity);

        if (last_update_tstamp == -1 || !Util.poemsFileExists(activity)) {
            if (internet_access) {
                updatePoems();
            } else {
                activity.setStatusNoInternet();
            }
        } else {
            if (!update) {
                update = Util.isUpdateTime(last_update_tstamp, last_fcm_tstamp);
            }

            if (update && internet_access) {
                updatePoems();
            } else if (poems.size() == 0) { // file exists by above (except race)
                processPoems();
            }
        }
    }

    public void updatePoems() {
        if (processing || PoemsLoader.receiver != null) {
            return;
        }
        updating = true;
        show_only_favorites = false;
        activity.clearStatus();
        activity.disableFavorites();

        poems.clear();
        filtered_poems.clear();
        activity.adapterDatasetChanged();
        activity.showUpdating();
        if (Util.poemsFileExists(activity)) {
            // If loading poems takes more than 5s and an old poems file is available
            // show a button to allow cancelling the download
            activity.showCancelButtonDelayed(5000);

        }
        PoemsLoader.loadPoems(activity, this);
    }

    public void changeSortOrder(String new_order) {
        if (!processing) {
            sort_order = new_order;
            sortPoems();
        }
    }

    public void sortPoems() {
        // sortPoems is automatically called when the spinner is created, poems might not be loaded yet
        if (poems.size() == 0) return;

        if (sort_order.equals("Date")) {
            Collections.sort(poems, new Comparator<Poem>() {
                @Override
                public int compare(Poem p1, Poem p2) {
                    return (int) (p2.timestamp - p1.timestamp);
                }
            });
        } else if (sort_order.equals("Score")) {
            Collections.sort(poems, new Comparator<Poem>() {
                @Override
                public int compare(Poem p1, Poem p2) {
                    return (p2.score - p1.score);
                }
            });
        } else if (sort_order.equals("Gold")) {
            Collections.sort(poems, new Comparator<Poem>() {
                @Override
                public int compare(Poem p1, Poem p2) {
                    return (p2.gold - p1.gold);
                }
            });
        }
        activity.searchPoems();
        activity.scrollToTop();
    }

    public void searchPoems(String search_string) {
        if (updating) {
            return;
        }

        if (!search_string.contains(last_search_string)) {
            if (!sent_search) {
                activity.trackSearch(last_search_string);
                sent_search = true;
            }
        } else {
            sent_search = false;
        }
        last_search_string = search_string;

        filtered_poems = new ArrayList<>();
        for (Poem p : poems) {
            String content = p.content.toLowerCase();
            if (content.contains(search_string) &&
                    (!show_only_favorites || p.favorite)) {
                filtered_poems.add(p);
            }
        }
        activity.setStatusNumPoems(filtered_poems.size());
        activity.adapterDatasetChanged();
    }

    public void processPoems() {
        processing = true;
        sort_order = "Date";
        activity.setProcessing();
        poems = new ArrayList<>();
        filtered_poems = new ArrayList<>();
        new ParsePoemsTask(activity).execute(activity);
    }

    public void cancelLoadingPoems() {
        PoemsLoader.cancelAllDownloads(activity);
        updating = false;
    }

    public void finishedProcessing(boolean status) {
        if (updating) {
            updating = false;
            if (poems.size() > 1000 && status) {

                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                preferences.setLastUpdateTime(now.getTimeInMillis());
                // Store timestamp of last poem for new poem notifications
                // Can't store double in sharedprefs so store timestamp in milliseconds as long
                preferences.setLastPoemTime((long) poems.get(0).timestamp * 1000);
            }
        }
        if (!status) {
            activity.showError();
        }
        processing = false;
    }

    public void optionMarkRead(boolean newValue) {
        preferences.setMarkRead(newValue);
        activity.adapterDatasetChanged();
    }

    public void optionResetRead() {
        new_read_poems.clear();
        dbhelper.clearReadPoems();
        for (Poem p : poems) {
            p.read = false;
        }
        activity.adapterDatasetChanged();
    }

    public void optionNotifyNew(boolean newValue) {
        preferences.setNotifyNew(newValue);
        // if the user set the notification manually we don't need to show the dialogue
        preferences.setDisplayedNotificationDialog(1);
    }

    public void toggleSearch(boolean old_state) {
        if (!old_state && !processing) {
            activity.enableSearch();
        } else {
            activity.disableSearch();
            if ((!sent_search) && last_search_string.length() > 0) {
                activity.trackSearch(last_search_string);
            }
            last_search_string = "";
            sent_search = false;
            searchPoems("");
        }
    }

    public void toggleFavorites() {
        if (updating) {
            return;
        }
        show_only_favorites = !show_only_favorites;
        activity.searchPoems();
        if (show_only_favorites) {
            // track number of favorites (if search is active add "s" in front)
            activity.trackEvent("showFavorites",
                    (last_search_string.length() > 0 ? "s" : "") +
                            Integer.toString(filtered_poems.size()),
                    null);
            activity.enableFavorites(last_search_string.length() == 0 && filtered_poems.size() == 0);

        } else {
            activity.disableFavorites();
        }
    }

    public void pressedCancelButton() {
        cancelLoadingPoems();
        processPoems();
    }

    public void downloadComplete() {
        processPoems();
        activity.setPoemsLoaded();
    }
}
