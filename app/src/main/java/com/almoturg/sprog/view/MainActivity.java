package com.almoturg.sprog.view;

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.almoturg.sprog.data.MarkdownConverter;
import com.almoturg.sprog.model.PreferencesRepository;
import com.almoturg.sprog.presenter.MainPresenter;
import com.almoturg.sprog.R;
import com.almoturg.sprog.SprogApplication;
import com.almoturg.sprog.data.PoemsFileParser;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.messaging.FirebaseMessaging;


public class MainActivity extends AppCompatActivity {
    public static MainPresenter presenter;
    private PreferencesRepository preferences;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    public TextView statusView;
    public ViewFlipper viewFlipper;
    private View search_box;
    private EditText search_text;

    private Tracker mTracker;

    public static final int VIEWFLIPPER_RECYCLERVIEW = 0;
    public static final int VIEWFLIPPER_EMPTY_FAVORITES = 1;
    public static final int VIEWFLIPPER_UPDATING = 2;
    public static final int VIEWFLIPPER_ERROR = 3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = ((SprogApplication) getApplication()).getPreferences();
        if (presenter == null) {
            // Passing appcontext seems a little dubious but if I pass this activity instance then
            // the activity object leaks...
            // It should hopefully be ok here because none of these use anything specific to this
            // activity instance from Context.
            Context appcontext = getApplicationContext();
            presenter = new MainPresenter(preferences, SprogApplication.getDbHelper(appcontext),
                    new MarkdownConverter(appcontext), new PoemsFileParser(appcontext));
        }

        SprogApplication application = (SprogApplication) getApplication();
        mTracker = application.getDefaultTracker();

        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        myToolbar.setTitle(null);

        statusView = (TextView) findViewById(R.id.status);
        viewFlipper = (ViewFlipper) findViewById(R.id.poemsListEmptyFlipper);
        search_box = findViewById(R.id.search_box);
        search_text = (EditText) findViewById(R.id.search_text);

        final Spinner sortSpinner = (Spinner) findViewById(R.id.sort_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sort_orders, R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);

        sortSpinner.post(new Runnable() {
            public void run() {
                sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        presenter.changeSortOrder(parent.getItemAtPosition(position).toString());
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }
        });

        final EditText search_box = (EditText) findViewById(R.id.search_text);
        search_box.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchPoems();
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        FirebaseMessaging.getInstance().subscribeToTopic("PoemUpdates");

        presenter.attachView(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTracker.setScreenName("PoemsList");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        if (mAdapter == null) {
            mAdapter = new PoemsListAdapter(this, presenter, preferences);
            mRecyclerView.setAdapter(mAdapter);
        }
        presenter.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        presenter.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.detachView();
    }

    public void cancelButton(View view) {
        presenter.pressedCancelButton();
    }

    public void toggleFavorites(View view){
        presenter.toggleFavorites();
    }

    public void enableFavorites(boolean no_favorites){
        if (no_favorites){
            viewFlipper.setDisplayedChild(VIEWFLIPPER_EMPTY_FAVORITES);
        }
        findViewById(R.id.toggle_favorites).setBackgroundResource(
                R.drawable.favorites_button_background);
    }

    public void disableFavorites() {
        findViewById(R.id.toggle_favorites).setBackgroundColor(Color.TRANSPARENT);
        int toggle_favorites_padding = getResources().getDimensionPixelSize(R.dimen.toggle_favorites_padding);
        findViewById(R.id.toggle_favorites).setPadding(
                toggle_favorites_padding, toggle_favorites_padding,
                toggle_favorites_padding, toggle_favorites_padding);
        viewFlipper.setDisplayedChild(VIEWFLIPPER_RECYCLERVIEW);
    }

    public void toggleSearch(View view) {
        presenter.toggleSearch();
    }

    public void enableSearch(String search_string){
        search_box.setVisibility(View.VISIBLE);
        findViewById(R.id.toggle_search).setBackgroundResource(R.drawable.search_button_background);
        search_text.requestFocus();
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                .showSoftInput(search_text, InputMethodManager.SHOW_IMPLICIT);
        search_text.setText(search_string);
    }

    public void disableSearch(){
        search_box.setVisibility(View.GONE);
        search_text.setText("");
        findViewById(R.id.toggle_search).setBackgroundColor(Color.TRANSPARENT);
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(search_box.getWindowToken(), 0);
    }

    public void clearSearch(View view) {
        ((EditText) findViewById(R.id.search_text)).setText("");
        searchPoems();
    }

    public void searchPoems() {
        if (mAdapter == null) { // afterTextChanged gets called when EditText is created...
            return;
        }
        String search_string = ((EditText) findViewById(R.id.search_text))
                .getText().toString().toLowerCase();
        presenter.searchPoems(search_string);
    }

    public void setStatusNumPoems(int numPoems){
        statusView.setText(String.format("× %d", numPoems));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_toolbar, menu);
        menu.findItem(R.id.action_mark_read).setChecked(preferences.getMarkRead());
        menu.findItem(R.id.action_notify_new).setChecked(preferences.getNotifyNew());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

/*        if (id == R.id.action_force_refresh) {
            updatePoems(null);
            return true;
        } else */
        if (id == R.id.action_mark_read) {
            item.setChecked(!item.isChecked());
            presenter.optionMarkRead(item.isChecked());
        } else if (id == R.id.action_reset_read) {
            presenter.optionResetRead();
        } else if (id == R.id.action_notify_new) {
            item.setChecked(!item.isChecked());
            presenter.optionNotifyNew(item.isChecked());
            trackEvent("notificationOption", item.isChecked() ? "yes" : "no", null);
        }

        return super.onOptionsItemSelected(item);
    }

    public void scrollToTop(){mRecyclerView.scrollToPosition(0);}

    public void trackSearch(String search_string){
        mTracker.send(new HitBuilders.EventBuilder()
            .setCategory("search")
            .setAction(search_string)
            .build());
    }

    public void adapterDatasetChanged(){mAdapter.notifyDataSetChanged();}

    public void adapterItemRangeInserted(int from, int to) {
        mAdapter.notifyItemRangeInserted(from, to);
    }

    public void setProcessing(){
        ((Spinner) findViewById(R.id.sort_spinner)).setSelection(0); // 0 is Date (is there a better way to do this??)
        statusView.setText("processing");
        viewFlipper.setDisplayedChild(VIEWFLIPPER_RECYCLERVIEW);
        toggleSearch(null); // hide search box (checks processing)
    }

    public void showError(){
        statusView.setText("error");
        viewFlipper.setDisplayedChild(VIEWFLIPPER_ERROR);
    }

    public void showNotifyDialog(){
        NotifyDialog notifyDialog = new NotifyDialog();
        FragmentManager fm = this.getSupportFragmentManager();
        notifyDialog.show(fm, "DIALOG_NOTIFY");
    }

    public void cancelNotifications(){
        NotificationManager nm = (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);
        nm.cancelAll();
    }

    public void setStatusNoInternet(){
        statusView.setText("no internet");
    }

    public void showCancelButtonDelayed(int delay_ms){
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (viewFlipper.getDisplayedChild() == VIEWFLIPPER_UPDATING) {
                    findViewById(R.id.action_cancelUpdate).setVisibility(View.VISIBLE);}
            }
        }, delay_ms);
    }

    public void clearStatus(){statusView.setText("");}

    public void showUpdating(){
        findViewById(R.id.action_cancelUpdate).setVisibility(View.INVISIBLE);
        viewFlipper.setDisplayedChild(VIEWFLIPPER_UPDATING);
    }

    public void trackEvent(String category, String action, String label){
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .build());
    }

    public void setPoemsLoaded(){
        statusView.setText("poems\nloaded");
    }
}