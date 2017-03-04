package com.almoturg.sprog.presenter;

import com.almoturg.sprog.data.MarkdownConverter;
import com.almoturg.sprog.data.UpdateHelpers;
import com.almoturg.sprog.model.Poem;
import com.almoturg.sprog.model.Poems;
import com.almoturg.sprog.model.PreferencesRepository;
import com.almoturg.sprog.view.MainActivity;
import com.almoturg.sprog.data.PoemsFileParser;
import com.almoturg.sprog.data.PoemsLoader;
import com.almoturg.sprog.model.SprogDbHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static com.almoturg.sprog.model.Poems.filtered_poems;
import static com.almoturg.sprog.model.Poems.poems;

public class MainPresenter {
    private MainActivity activity;
    private PreferencesRepository preferences;
    private SprogDbHelper dbhelper;
    private MarkdownConverter markdownConverter;
    private PoemsFileParser poemsFileParser;

    private ArrayList<String> new_read_poems = new ArrayList<>(); // Poems newly marked as read

    private boolean processing = false;
    private boolean updating = false;
    public boolean show_only_favorites = false;

    private boolean show_search_bar = false;

    // for analytics tracking of search queries
    private String last_search_string = "";
    private boolean sent_search = false;

    public String sort_order = "Date";

    public MainPresenter(PreferencesRepository preferences,
                         SprogDbHelper dbhelper, MarkdownConverter markdownConverter,
                         PoemsFileParser poemsFileParser) {
        this.preferences = preferences;
        this.dbhelper = dbhelper;
        this.markdownConverter = markdownConverter;
        this.poemsFileParser = poemsFileParser;
    }

    public void attachView(MainActivity activity) {
        this.activity = activity;

        activity.setSortOrder(sort_order);

        if (updating) {
            activity.showUpdating();
        }
        if (processing) {
            activity.setProcessing();
        }
        if (show_only_favorites) {
            activity.enableFavorites(showEmptyFavoritesMessage());
        }
        if (show_search_bar) {
            activity.enableSearch(last_search_string);
        }
    }

    public void detachView(){
        this.activity = null;
    }

    public void onResume() {
        // A dialog asking whether to enable new poem notifications is shown
        // the second time MainActivity is started.
        if (timeToShowNotifyDialog()) {
            activity.showNotifyDialog();
        }

        boolean update = preferences.getUpdateNext();
        if (filtered_poems.size() > 0) {
            activity.setStatusNumPoems(filtered_poems.size());
            activity.adapterDatasetChanged(); // to update e.g. favorite status
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
        boolean internet_access = UpdateHelpers.isConnected(activity);

        if (last_update_tstamp == -1 || !UpdateHelpers.poemsFileExists(activity)) {
            if (internet_access) {
                updatePoems();
            } else {
                activity.setStatusNoInternet();
            }
        } else {
            if (!update) {
                update = UpdateHelpers.isUpdateTime(
                        Calendar.getInstance(TimeZone.getTimeZone("UTC")),
                        last_update_tstamp, last_fcm_tstamp);
            }

            if (update && internet_access) {
                updatePoems();
            } else if (poems.size() == 0) { // file exists by above (except race)
                processPoems();
            }
        }
    }

    private void updatePoems() {
        if (processing || PoemsLoader.receiver != null) {
            return;
        }
        updating = true;
        show_only_favorites = false;
        activity.clearStatus();
        activity.disableFavorites();

        Poems.clear();
        activity.adapterDatasetChanged();
        activity.showUpdating();
        if (UpdateHelpers.poemsFileExists(activity)) {
            // If loading poems takes more than 5s and an old poems file is available
            // show a button to allow cancelling the download
            activity.showCancelButtonDelayed(5000);

        }
        PoemsLoader.loadPoems(activity, this);
    }

    public void changeSortOrder(String new_order) {
        if (!processing && activity != null) {
            sort_order = new_order;
            sortPoems();
        }
    }

    private void sortPoems() {
        // sortPoems is automatically called when the spinner is created, poems might not be loaded yet
        if (poems.size() == 0) return;

        Poems.sort(sort_order);
        activity.searchPoems();
        activity.scrollToTop();
    }

    public void searchPoems(String search_string) {
        if (updating || activity == null) {
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

        Poems.filter(search_string, show_only_favorites);
        activity.setStatusNumPoems(filtered_poems.size());
        activity.adapterDatasetChanged();
    }

    private void processPoems() {
        processing = true;
        sort_order = "Date";
        activity.setProcessing();
        poems = new ArrayList<>();
        filtered_poems = new ArrayList<>();
        poemsFileParser.parsePoems(new PoemsFileParser.ParsePoemsCallbackInterface() {
            @Override
            public void addPoems(List<Poem> poems) {
                MainPresenter.this.addPoems(poems);
            }

            @Override
            public void finishedProcessing(boolean status) {
                MainPresenter.this.finishedProcessing(status);
            }
        }, dbhelper, markdownConverter);
    }

    private void addPoems(List<Poem> new_poems) {
        if (processing) { // don't add poems if processing has been cancelled
            Poems.add(new_poems);
            activity.setStatusNumPoems(poems.size());
            activity.adapterItemRangeInserted(filtered_poems.size(), new_poems.size());
        }
    }

    private void cancelLoadingPoems() {
        updating = false;
        PoemsLoader.cancelAllDownloads(activity);
        if (processing) {
            processing = false;
            poemsFileParser.cancelParsing();
            Poems.clear();
            activity.adapterDatasetChanged();
        }
    }

    private void finishedProcessing(boolean status) {
        if (updating) {
            updating = false;
            if (poems.size() > 1000 && status) {

                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                preferences.setLastUpdateTime(now.getTimeInMillis());
                preferences.setUpdateNext(false);
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

    public void clearReadPoems() {
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

    public void optionStats() {
        activity.trackEvent("openStats", "stats", null);
        activity.launchStats();
    }

    public void toggleSearch() {
        if (!show_search_bar && !processing) {
            show_search_bar = true;
            activity.enableSearch(last_search_string);
        } else {
            show_search_bar = false;
            activity.disableSearch();
            if ((!sent_search) && last_search_string.length() > 0) {
                activity.trackSearch(last_search_string);
            }
            last_search_string = "";
            sent_search = false;
            searchPoems("");
        }
    }

    private boolean showEmptyFavoritesMessage() {
        return show_only_favorites && last_search_string.length() == 0 &&
                filtered_poems.size() == 0;
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
            activity.enableFavorites(showEmptyFavoritesMessage());

        } else {
            activity.disableFavorites();
        }
    }

    public void pressedCancelButton() {
        cancelLoadingPoems();
        if (poems.size() == 0) {
            processPoems();
        }
    }

    public void downloadComplete() {
        activity.cancelNotifications();
        processPoems();
        activity.setPoemsLoaded();
    }

    public void notifyDialogYes() {
        preferences.setNotifyNew(true);
        activity.invalidateOptionsMenu();
        activity.trackEvent("notificationDialog", "yes", null);
    }

    public void notifyDialogNo() {
        preferences.setNotifyNew(false);
        activity.invalidateOptionsMenu();
        activity.trackEvent("notificationDialog", "no", null);
    }

    public void addNewReadPoem(Poem poem){
        new_read_poems.add(poem.link);
    }

    public boolean poemsReady(){
        return !updating && !processing;
    }

    public MarkdownConverter getMarkdownConverter(){
        return markdownConverter;
    }

    private boolean timeToShowNotifyDialog(){
        // This checks whether it's time to show the dialog asking whether to enable
        // notifications for new poems.
        // The state is stored in the "DISPLAY_NOTIFICATION_DIALOG" pref
        // not set: never launched, 0: launched once, 1: dialog shown
        if (preferences.getDisplayedNotificationDialog() == -1){
            preferences.setDisplayedNotificationDialog(0);
            return false;
        } else if (preferences.getDisplayedNotificationDialog() == 0) {
            preferences.setDisplayedNotificationDialog(1);
            return true;
        } else {
            return false;
        }
    }
}
