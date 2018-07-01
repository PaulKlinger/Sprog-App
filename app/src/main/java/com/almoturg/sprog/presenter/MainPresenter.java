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
    public PreferencesRepository preferences;
    private SprogDbHelper dbhelper;
    private MarkdownConverter markdownConverter;
    private PoemsFileParser poemsFileParser;

    private ArrayList<String> new_read_poems = new ArrayList<>(); // Poems newly marked as read

    private boolean processing = false;
    private PoemsLoader.UpdateType updating = null;
    public boolean show_only_favorites = false;
    private boolean filter_unread = false;
    private boolean filter_short = false;
    private boolean filter_long = false;

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

        if (updating != null) {
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
            activity.setFilterButtonState(filter_unread, filter_short, filter_long);
        }
    }

    public void detachView() {
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
        long last_full_update_tstamp = preferences.getLastFullUpdateTime();
        long last_fcm_tstamp = preferences.getLastFCMTimestamp();
        boolean internet_access = UpdateHelpers.isConnected(activity);

        if (last_full_update_tstamp == -1 || !UpdateHelpers.poemsFullFileExists(activity)) {
            if (internet_access) {
                updatePoems(PoemsLoader.UpdateType.FULL);
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
                updatePoems(UpdateHelpers.getUpdateType(
                        Calendar.getInstance(TimeZone.getTimeZone("UTC")),
                        last_full_update_tstamp));
            } else if (poems.size() == 0) { // file exists by above (except race)
                processPoems();
            }
        }
    }

    private void updatePoems(PoemsLoader.UpdateType update_type) {
        if (processing || PoemsLoader.receiver != null) {
            return;
        }
        updating = update_type;
        show_only_favorites = false;
        activity.clearStatus();
        activity.disableFavorites();

        Poems.clear();
        activity.adapterDatasetChanged();
        activity.showUpdating();
        if (UpdateHelpers.poemsFullFileExists(activity)) {
            // If loading poems takes more than 5s and an old poems file is available
            // show a button to allow cancelling the download
            activity.showCancelButtonDelayed(5000);

        }
        PoemsLoader.loadPoems(activity, this, update_type);
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
        if (updating != null || activity == null) {
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

        Poems.filter(search_string, show_only_favorites, filter_unread, filter_long, filter_short);
        activity.setStatusNumPoems(filtered_poems.size());
        activity.adapterDatasetChanged();
    }

    private void processPoems() {
        processing = true;
        sort_order = "Date";
        activity.setProcessing();
        poems = new ArrayList<>();
        filtered_poems = new ArrayList<>();

        // Only process the partial file if we just got a partial update
        // or the last update (including the current one) was a partial one
        boolean process_partial_file = (
                updating == PoemsLoader.UpdateType.PARTIAL ||
                        (preferences.getLastUpdateTime() > preferences.getLastFullUpdateTime()
                                && updating != PoemsLoader.UpdateType.FULL)
        );

        poemsFileParser.parsePoems(new PoemsFileParser.ParsePoemsCallbackInterface() {
            @Override
            public void addPoems(List<Poem> poems) {
                MainPresenter.this.addPoems(poems);
            }

            @Override
            public void finishedProcessing(boolean status) {
                MainPresenter.this.finishedProcessing(status);
            }
        }, dbhelper, markdownConverter, process_partial_file);
    }

    private void addPoems(List<Poem> new_poems) {
        if (processing) { // don't add poems if processing has been cancelled
            Poems.add(new_poems, last_search_string, show_only_favorites,
                    filter_unread, filter_long, filter_short);
            activity.setStatusNumPoems(poems.size());
            activity.adapterItemRangeInserted(filtered_poems.size(), new_poems.size());
        }
    }

    private void cancelLoadingPoems() {
        if (activity == null) {
            // not sure how this could ever be reached but this might fix the crash by
            // NullPointerException in PoemsLoader.cancelAllDownloads.
            return;
        }
        updating = null;
        PoemsLoader.cancelAllDownloads(activity);
        if (processing) {
            processing = false;
            poemsFileParser.cancelParsing();
            Poems.clear();
            activity.adapterDatasetChanged();
        }
    }

    private void finishedProcessing(boolean status) {
        if (updating != null) {
            if (poems.size() > 1000 && status) {
                long now_ms = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
                if (updating == PoemsLoader.UpdateType.FULL) {
                    preferences.setLastUpdateTime(now_ms);
                    preferences.setLastFullUpdateTime(now_ms);
                } else if (updating == PoemsLoader.UpdateType.PARTIAL) {
                    preferences.setLastUpdateTime(now_ms);
                }
                preferences.setUpdateNext(false);
                // Store timestamp of last poem for new poem notifications
                // Can't store double in sharedprefs so store timestamp in milliseconds as long
                preferences.setLastPoemTime((long) poems.get(0).timestamp * 1000);
            }
            updating = null;
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
        activity.trackEvent("notificationOption", newValue ? "yes" : "no", null);
    }

    public void optionLongPress(boolean newValue) {
        preferences.setLongPressLink(newValue);
    }

    public void optionStats() {
        activity.trackEvent("openStats", "stats", null);
        activity.launchStats();
    }

    public void optionDarkTheme(boolean newValue) {
        activity.trackEvent("darkTheme", newValue ? "enabled" : "disabled", null);
        preferences.setDarkTheme(newValue);
        activity.recreate();
    }

    public void toggleSearch() {
        if (!show_search_bar && !processing) {
            show_search_bar = true;
            activity.enableSearch(last_search_string);
            activity.setFilterButtonState(filter_unread, filter_short, filter_long);
        } else {
            show_search_bar = false;
            activity.disableSearch();
            if ((!sent_search) && last_search_string.length() > 0) {
                activity.trackSearch(last_search_string);
            }
            last_search_string = "";
            sent_search = false;
            filter_unread = false;
            filter_short = false;
            filter_long = false;
            searchPoems("");
        }
    }

    private boolean showEmptyFavoritesMessage() {
        return show_only_favorites && last_search_string.length() == 0
                && !filter_short && !filter_long && !filter_unread &&
                filtered_poems.size() == 0;
    }

    public void toggleFavorites() {
        if (updating != null) {
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

    public void toggleFilterUnread() {
        filter_unread = !filter_unread;
        activity.trackEvent("filter", "unread", filter_unread ? "on" : "off");
        activity.searchPoems();
        activity.setFilterButtonState(filter_unread, filter_short, filter_long);
    }

    public void toggleFilterLong() {
        filter_long = !filter_long;
        activity.trackEvent("filter", "long", filter_long ? "on" : "off");
        if (filter_long) {
            filter_short = false;
        }
        activity.searchPoems();
        activity.setFilterButtonState(filter_unread, filter_short, filter_long);
    }

    public void toggleFilterShort() {
        filter_short = !filter_short;
        activity.trackEvent("filter", "short", filter_short ? "on" : "off");
        if (filter_short) {
            filter_long = false;
        }
        activity.searchPoems();
        activity.setFilterButtonState(filter_unread, filter_short, filter_long);
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

    public void addNewReadPoem(Poem poem) {
        new_read_poems.add(poem.link);
    }

    public boolean poemsReady() {
        return (updating == null) && !processing;
    }

    public MarkdownConverter getMarkdownConverter() {
        return markdownConverter;
    }

    private boolean timeToShowNotifyDialog() {
        // This checks whether it's time to show the dialog asking whether to enable
        // notifications for new poems.
        // The state is stored in the "DISPLAY_NOTIFICATION_DIALOG" pref
        // not set: never launched, 0: launched once, 1: dialog shown
        if (preferences.getDisplayedNotificationDialog() == -1) {
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
