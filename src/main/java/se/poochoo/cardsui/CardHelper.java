package se.poochoo.cardsui;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.Toast;

import se.poochoo.cardsui.objects.Card;
import se.poochoo.cardsui.views.CardUI;

import java.util.ArrayList;
import java.util.List;

import se.poochoo.DialogActivity;
import se.poochoo.DialogHelper;
import se.poochoo.LocationHelper;
import se.poochoo.R;
import se.poochoo.db.SelectionUserDataSource;
import se.poochoo.net.NetworkInterface;
import se.poochoo.proto.Messages;

/**
 * A class to help add and remove cards based on actions and events in the activity.
 */
public class CardHelper implements NetworkInterface.ResponseCallBack,
        DepartureCard.OnCardClickListener,
        Card.OnCardSwiped {

    public static interface RequestProvider {
        public Messages.SmartRequest.Builder get();
    }
    private static final int NETWORK_TIMEOUT = 20*1000;
    // After this amount of time the view will reload.
    //private final static int TICKER_TIMEOUT = 2*60*1000;
    private final static int TICKER_TIMEOUT = 20*1000;
    // How often we should update the list and change the minutes left and stuff.
    private final static int TICK_DELAY = 5*1000;
    // How often we should update the timer (in ms).
    private final static int REDRAW_TIMER_TIME = 50;

    public static final int DEPARTURE_DIALOG = 0xdead + 1;

    private final CardUI cardUI;
    private final Activity activity;
    private final SelectionUserDataSource dataStore;
    private RequestProvider requestProvider;
    private CardTicker ticker;
    private List<DepartureCard> listItems = new ArrayList<DepartureCard>();
    private CardUnswipeData lastDismissedCard;

    public enum AnimationStateEnum {
        COUNTDOWN,
        LOADING,
        IDLE
    }

    //Animationdeclaration start
    AnimatorSet loadingDataAnimationHolder = new AnimatorSet();
    boolean reapeatAnimation = true;

    //animation variables initiated in factorymethod
    View actionBarDivider; //the timerview
    int screenWidthInt; //pixels wide the users screen is as int
    double screenWidthDouble; //pixels wide the users screen is as double
    int oldCalculatedTimeTickerInt = 0;  //Variable for holding the last position of the timerview.
    //Animationdeclaration end

    public static int[] errorImageAndMessageIds(NetworkInterface.Status problem) {
        int[] ids = new int[2];
        switch (problem){
            case NOT_CONNECTED:
                //network trouble :/
                ids[0] = R.drawable.card_error_signal;
                ids[1] = R.string.errorTextNotConnected;
                break;
            case EMPTY_RESPONSE:
                if (!LocationHelper.hasFreshLocation()) {
                    //not even close to a position on the user :/
                    ids[0] = R.drawable.card_error_location;
                    ids[1] = R.string.errorTextEmptyResponseNoPosition;
                    break;
                } else {
                    ids[0] = R.drawable.card_error_departures;
                    ids[1] = R.string.errorTextEmptyResponseNoDepartures;
                    break;
                }
            case TIMEOUT:
                //server not reachable outage :/
                ids[0] = R.drawable.card_error_server;
                ids[1] = R.string.errorTextTimeout;
                break;
            default:
                //trouble but no idea why
                ids[0] = R.drawable.card_error_bug;
                ids[1] = R.string.errorTextDefault;
                break;
        }
        return ids;
    }

    public CardHelper(CardUI cardUI, Activity activity, SelectionUserDataSource dataStore, RequestProvider requestProvider) {
        this.cardUI = cardUI;
        this.activity = activity;
        this.dataStore = dataStore;
        this.requestProvider = requestProvider;

        actionBarDivider = activity.findViewById(R.id.actionBarDivider);
        screenWidthInt = activity.getResources().getDisplayMetrics().widthPixels;
        screenWidthDouble = activity.getResources().getDisplayMetrics().widthPixels;
    }

    @Override
    public void handleResponse(final Messages.SmartResponse response, final NetworkInterface.Status status) {
        if (ticker != null) {
            ticker.stop();
        }
        loading = false;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (status == NetworkInterface.Status.SUCCESS) {
                    processSuccessfulServerResponse(response);
                } else {
                    createErrorCard(status);
                }
            }
        });
    }

    /**
     * Load data from the server and populate the cards with the result.
     */
    private boolean loading = false;
    public synchronized void loadDataFromServer() {
        if (!loading) {
            loading = true;

            //start the loadinganimation
            animationControl(AnimationStateEnum.LOADING,0);

            Messages.SmartRequest.Builder requestBuilder = requestProvider.get();
            NetworkInterface.provider.get(activity, true)
                    .sendRequest(requestBuilder, NETWORK_TIMEOUT, this);
            if (ticker != null) {
                ticker.stop();
            }
            oldCalculatedTimeTickerInt = 0;
        }
    }

    /**
     * Add a card that brings up the call taxi flow when touched.
     */
    public void addTaxiCard() {
        Messages.SmartRequest.Builder requestBuilder = requestProvider.get();
        Messages.SelectionDeviceData.Position position = null;
        if (requestBuilder.hasDeviceData() && requestBuilder.getDeviceData().hasPosition()) {
            position = requestBuilder.getDeviceData().getPosition();
        }
        final Messages.SelectionDeviceData.Position setPosition = position;
        TaxiCard taxiCard = new TaxiCard();
        taxiCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogHelper.showTaxiDialog(activity, setPosition);
            }
        });
        taxiCard.setSwipeable(false);
        cardUI.addCard(taxiCard);
    }

    //Add first time help-guide-demo-card stuff
    public void addGuideCard() {
        GuideCard guideCard = new GuideCard(activity);
        cardUI.addCard(guideCard);
        }

    /**
     * Refresh the promotion status for all cards matching the departure.
     * @param selector
     * @param promoted
     */
    public void setPromoteStatus(Messages.DataSelector selector, boolean promoted) {
        for (DepartureCard listCard : listItems) {
            if (listCard.getSelector().equals(selector)) {
                listCard.setSwipeable(!promoted);
            }
        }
        cardUI.refresh();
    }

    /**
     * Try to handle a problem with a user friendly message.
     * @param problem
     */
    public void createErrorCard(final NetworkInterface.Status problem){

        if (problem == NetworkInterface.Status.SUCCESS) {
            throw new RuntimeException("This isn't a problem...?");
        }
        cardUI.clearCards();
        int[] errorImageAndMessage = errorImageAndMessageIds(problem);
        ErrorCard errorCard = new ErrorCard(errorImageAndMessage[0], errorImageAndMessage[1]);
        cardUI.addCard(errorCard);
        errorCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (problem){
                    case EMPTY_RESPONSE:
                        if (LocationHelper.hasFreshLocation()) {
                            activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            break;
                        } else {
                            break;
                        }
                    case TIMEOUT:
                        break;
                    case NOT_CONNECTED:
                        activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        break;
                    default:
                        break;
                }
            }
        });

        //Add taxi card as a alternative for the user.
        addTaxiCard();

        cardUI.refresh();

        //Fade out the actionbar devider since error.
        animationControl(AnimationStateEnum.IDLE, 0);
    }

    private void processSuccessfulServerResponse(Messages.SmartResponse response) {
        if (response.getListDataCount() == 0) {
            createErrorCard(NetworkInterface.Status.EMPTY_RESPONSE);
            return;
        }
        // So we can fine tune it.
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        Float swipeSensitivity = Float.valueOf(sharedPrefs.getString("swipe_factor", "0.01f"));

        listItems = new ArrayList<DepartureCard>();
        cardUI.clearCards();
        if(DialogHelper.shouldHandleOneTimeEvent(activity, R.string.card_guide_string_title, 1)){
            addGuideCard();
        }
        CardStackBuilder cardStackBuilder = new CardStackBuilder(cardUI)
                .setClickListener(this)
                .setSwipeListener(this)
                .setSwipeSensitivity(swipeSensitivity);

        for (Messages.SmartListData listDataItem :  response.getListDataList()) {
            boolean isPromoted = dataStore.isPromoted(listDataItem.getSelector());
            listItems.add(
                    cardStackBuilder.setSwipeable(!isPromoted)
                            .buildCard(listDataItem));
        }

        addTaxiCard();
        ticker = new CardTicker(this, listItems, System.currentTimeMillis(), cardUI,
                TICKER_TIMEOUT, TICK_DELAY, REDRAW_TIMER_TIME);
        ticker.start();
    }

    @Override
    public void onClick(DepartureCard card) {
        Intent dialogIntent = new Intent(activity, DialogActivity.class);
        dialogIntent.putExtra(DialogActivity.SMART_LIST_DATA_EXTRA, card.getListData().toByteArray());
        activity.startActivityForResult(dialogIntent, DEPARTURE_DIALOG);
    }

    @Override
    public void onCardSwiped(Card card, View layout) {
        final View undoButton = activity.findViewById(R.id.undoButton);
        DepartureCard departureCard = (DepartureCard) card;
        storeDismissData(departureCard);
        dataStore.storeAction(departureCard.getSelector(), SelectionUserDataSource.SelectionType.DEMOTE);
        undoButton.setVisibility(View.VISIBLE);
        new CountDownTimer(7000, 1000) {
            public void onTick(long millisUntilFinished) {
                //gradual transparency? HERE!
            }
            public void onFinish() {
                //what to do when the time runs out?
                undoButton.setVisibility(View.GONE);
            }
        }.start();
    }

    private void storeDismissData(DepartureCard dismissedCard) {
        // It's not possible to demote favorites, so assume it's a demotion.
        int currentMultiplier = dataStore.getCurrentMultiplier(dismissedCard.getSelector(),
                SelectionUserDataSource.SelectionType.DEMOTE);
        lastDismissedCard = new CardUnswipeData(
                currentMultiplier,
                SelectionUserDataSource.SelectionType.DEMOTE,
                dismissedCard);
    }

    public void undoAction(View view) {
        final View undoButton = activity.findViewById(R.id.undoButton);
        CardUnswipeData dismissedCard = lastDismissedCard;
        lastDismissedCard = null;
        if (dismissedCard != null) {
            DepartureCard restoreCard = dismissedCard.getCard();
            cardUI.addCardAt(restoreCard, restoreCard.getIndexInStack());
            dataStore.storeAction(
                    restoreCard.getSelector(),
                    dismissedCard.getSelectionType(),
                    dismissedCard.getMultiplier());
            undoButton.setVisibility(View.GONE);
            cardUI.refresh();
        }
    }

    /**
     * Remove everything from the card stack.
     */
    public void clearCards() {
        if (ticker != null) {
            ticker.stop();
            animationControl(AnimationStateEnum.IDLE,0);
        }
        // TODO: Should we animate this?
        cardUI.clearCards();
    }

    public void pause() {
        if (ticker != null) {
            ticker.stop();
            animationControl(AnimationStateEnum.IDLE,0);
        }
    }

    public String getI18nString(int id) {
        return activity.getString(id);
    }

    public void changeTimer (int overScrolledDistance){

        if (ticker != null) {
            //The user has overscrolled and the timer should be updated
            //Stop the timer and save the returned time
            int oldTickerTime = (int)ticker.stop();
            //Create and start a new timer with the updated time.
            ticker = new CardTicker(CardHelper.this, listItems, System.currentTimeMillis(), cardUI, (oldTickerTime-overScrolledDistance), TICK_DELAY, REDRAW_TIMER_TIME);
            ticker.start();
        }
    }

    //Animation control method
    //TODO: make into enum if possible
    public void animationControl(AnimationStateEnum AnimationState, double loadingLevel){

        if (AnimationState == AnimationStateEnum.COUNTDOWN){ //COUNTDOWN

            if ( actionBarDivider.getBackground() instanceof AnimationDrawable ) {
                AnimationDrawable loadinganimationholder = (AnimationDrawable) actionBarDivider.getBackground();
                if (loadinganimationholder.isRunning()) {loadinganimationholder.stop();}
                actionBarDivider.setBackgroundResource(R.color.silver);
            }

            //get the pixels/per second
            double timeamount = TICKER_TIMEOUT;
            double  currentTimerviewPosition = (screenWidthDouble/timeamount)*loadingLevel;
            int currentTimerviewPositionWidth = (int)screenWidthDouble-(int)currentTimerviewPosition;

            //Create the animationset
            AnimationSet as = new AnimationSet(true);
            //Tell the view to keep the state after the animation
            as.setFillAfter(true);
            as.setFillEnabled(true);
            //Set the animation to go from the previous position to the new calculated position
            TranslateAnimation ta = new TranslateAnimation(oldCalculatedTimeTickerInt, currentTimerviewPositionWidth, 0, 0);
            //Set the duration to be the same as the refreshrate of the timerview (closest guess for a smooth animation)
            ta.setDuration(REDRAW_TIMER_TIME);
            //Tell the view to keep the state after the animation
            ta.setFillAfter(true);
            ta.setFillEnabled(true);
            //Assign and start animation
            as.addAnimation(ta);
            actionBarDivider.setAnimation(as);

            //Make sure to cancel all other animations on the same view.
            //loadingDataAnimationHolder.cancel();
            reapeatAnimation = false;

            //Save the position of the view for next iteration
            oldCalculatedTimeTickerInt = currentTimerviewPositionWidth;

        } else if (AnimationState == AnimationStateEnum.LOADING){
        //LOADING

            //Make sure to reset everything.
            AnimationSet resetanimationset = new AnimationSet(true);
            resetanimationset.setFillAfter(true);
            resetanimationset.setFillEnabled(true);
            TranslateAnimation resetanimation = new TranslateAnimation(screenWidthInt, 0, 0, 0);
            resetanimation.setDuration(500);
            resetanimation.setFillAfter(true);
            resetanimation.setFillEnabled(true);
            resetanimationset.addAnimation(resetanimation);
            actionBarDivider.setAnimation(resetanimationset);

            actionBarDivider.setBackgroundResource(R.anim.loadinganimation);

            AnimationDrawable loadinganimationholder = (AnimationDrawable) actionBarDivider.getBackground();
            loadinganimationholder.start();

        } else if (AnimationState == AnimationStateEnum.IDLE){
            //Idle state (error and timeout)

            if ( actionBarDivider.getBackground() instanceof AnimationDrawable ) {
                AnimationDrawable loadinganimationholder = (AnimationDrawable) actionBarDivider.getBackground();
                if (loadinganimationholder.isRunning()) {loadinganimationholder.stop();}
                actionBarDivider.setBackgroundResource(R.color.silver);
            }

            //Make sure to remove the view.
            AnimationSet removeanimationset = new AnimationSet(true);
            removeanimationset.setFillAfter(true);
            removeanimationset.setFillEnabled(true);
            TranslateAnimation removeanimation = new TranslateAnimation(screenWidthInt, screenWidthInt, 0, 0);
            removeanimation.setDuration(0);
            removeanimation.setFillAfter(true);
            removeanimation.setFillEnabled(true);
            removeanimationset.addAnimation(removeanimation);
            actionBarDivider.setAnimation(removeanimationset);

            reapeatAnimation = false;

        }
    }

}