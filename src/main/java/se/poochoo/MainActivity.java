package se.poochoo;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import se.poochoo.cardsui.views.CardUI;

import se.poochoo.cardsui.CardHelper;
import se.poochoo.cardsui.views.QuickReturnListView;
import se.poochoo.db.SelectionUserDataSource;
import se.poochoo.net.NetworkInterface;
import se.poochoo.proto.Messages;
import se.poochoo.proto.Messages.DataSelector;
import se.poochoo.proto.Messages.SmartRequest;
import se.poochoo.search.CustomSuggestionProvider;

public class MainActivity extends Activity implements CardHelper.RequestProvider, LocationHelper.LocationCallBack {
    private final int SEARCH_QUERY_TYPE_DELAY = 700; // Milliseconds to wait before searching.
    private final int SEARCH_MIN_QUERY_LENGTH = 2; // Minium characters required for search.
    private LocationHelper locationHelper;
    private String currentSearchQuery;
    private String lastPerformedSearch;
    private long nextAllowedSearch = 0;
    String youtubeVideo = "vnd.youtube:2uVkHwE_g8w";

    //part of pull down to update
    QuickReturnListView mListView;
    int lastUpdateHappendAt = 0;
    int calculatedScroll = 0;

    @Deprecated
    private SelectionUserDataSource dataStore;
    private boolean debugSetting;
    private CardHelper cardHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cards_layout);
        dataStore = new SelectionUserDataSource(this);
        dataStore.open();
        locationHelper = LocationHelper.create(this, this);
        CardUI cardUI= (CardUI) findViewById(R.id.cardsview);
        cardUI.setSwipeable(true);
        cardHelper = new CardHelper(cardUI, this, dataStore, this);

        //pull-down-to-refresh listener and action
        mListView = (QuickReturnListView) findViewById(R.id.listView);
        pullDownToRefreshFunction();

        //Search
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
        super.onNewIntent(intent);
    }

    private void handleIntent(Intent intent){
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            currentSearchQuery = query;
            cardHelper.loadDataFromServer();
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    CustomSuggestionProvider.AUTHORITY, CustomSuggestionProvider.MODE);
            suggestions.saveRecentQuery(query, null);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        locationHelper.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        cardHelper.clearCards();
        locationHelper.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NetworkInterface.close();
        dataStore.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        debugSetting = sharedPrefs.getBoolean("preference_debug", false);
    }

    private SmartRequest.Builder buildRequest() {
        SmartRequest.Builder builder = SmartRequest.newBuilder()
            .setDebug(debugSetting)
            .setUserSelection(dataStore.getAllStoredUserActions());
        locationHelper.addLocationsToRequest(builder.getDeviceDataBuilder());
        if (currentSearchQuery != null && !currentSearchQuery.equals("")) {
            builder.setSearchQuery(Messages.SearchQuery.newBuilder().setQuery(currentSearchQuery));
        }
        return builder;
    }

    @Deprecated
    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(
                        MainActivity.this, message, Toast.LENGTH_LONG)
                        .show();
            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setQueryRefinementEnabled(true);
        searchView.setQueryHint(getString(R.string.LABEL_SEARCH_STATION));
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                currentSearchQuery = "";
                cardHelper.loadDataFromServer();
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_beta:
                DialogHelper.showFeedbackDialog(this);
                break;
            case R.id.action_refresh:
                cardHelper.clearCards();
                cardHelper.loadDataFromServer();
                break;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.action_about:
                Intent aboutintent = new Intent(this, AboutActivity.class);
                startActivity(aboutintent);
                break;
            case R.id.action_help:
                try{
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(youtubeVideo));
                    startActivity(intent);
                } catch (ActivityNotFoundException ex){
                    Intent intent=new Intent(Intent.ACTION_VIEW,
                            Uri.parse(youtubeVideo));
                    startActivity(intent);
                }
                break;

            case R.id.action_community:
                String url = "http://bit.ly/sl-appen";
                Intent communityintent = new Intent(Intent.ACTION_VIEW);
                communityintent.setData(Uri.parse(url));
                startActivity(communityintent);
                break;

            case R.id.action_share:
                String shareString = getString(R.string.shareApp);
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareString);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CardHelper.DEPARTURE_DIALOG:
                try {
                    DataSelector selector = DataSelector.parseFrom(data.getByteArrayExtra(DialogActivity.LIST_SELECTOR));
                    boolean isPromoted = dataStore.isPromoted(selector);
                    cardHelper.setPromoteStatus(selector, isPromoted);
                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("Failed to parse dialog result");
                }
                break;

        }
    }

    public void undoAction(View view) {
        cardHelper.undoAction(view);
    }

    @Override
    public SmartRequest.Builder get() {
        return buildRequest();
    }

    private void preformSearchLater() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Only allow fresh queries.
                if (System.currentTimeMillis() < nextAllowedSearch) {
                    return;
                }
                // Check that there is a search query.
                if (currentSearchQuery != null && currentSearchQuery.length() >= SEARCH_MIN_QUERY_LENGTH) {
                  // Make sure not to perform search twice for the same query.
                  if (!currentSearchQuery.equals(lastPerformedSearch)) {
                    cardHelper.loadDataFromServer();
                    lastPerformedSearch = currentSearchQuery;
                  }
                }
            }
        }, SEARCH_QUERY_TYPE_DELAY);
    }

    @Override
    public void locationInitialized() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cardHelper.loadDataFromServer();
            }
        });
    }

    private void pullDownToRefreshFunction() {
        //Set listener
        mListView.setOnOverScrolledListener(new QuickReturnListView.OnOverScrolledListener() {
            @Override
            public void onOverScrolled(ListView listView, int scrollX, int scrollY, boolean clampedX, boolean clampedY, int deltaScrollY) {

                //Make overscroll on top or bottom irrelevant
                if (deltaScrollY < 0) {
                    calculatedScroll += deltaScrollY;
                } else {
                    calculatedScroll -= deltaScrollY;
                }

                //if the overscrollamount currently overscrolled is smaller than -100 update send the amount to cardhelper.changeTimer function
                if ((calculatedScroll - lastUpdateHappendAt) < -100) {
                    cardHelper.changeTimer(((calculatedScroll - lastUpdateHappendAt) * -1) * 20);

                    lastUpdateHappendAt = calculatedScroll;
                }
            }
        });
    }
}
