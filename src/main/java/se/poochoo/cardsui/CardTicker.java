package se.poochoo.cardsui;

import android.os.CountDownTimer;
import android.util.Log;

import se.poochoo.cardsui.views.CardUI;

import java.util.List;

import se.poochoo.R;

/**
 * Created by Erik on 2013-10-13.
 * Updates the time shown on the cards when some time has passed.
 */
public class CardTicker {

    public final static int SECONDS_ABOUT_TO_DEPART = 45;
    public final static int SECONDS_DEPARTED = 0;
    public final static int SECONDS_REMOVE_CARD = 0;
    private final static String INVALIDATE = "invalidate_card";

    private final List<DepartureCard> listItems;
    private final long loadTime;
    private final CountDownTimer timer;
    private final CardUI cardUI;
    private final CardHelper helper;
    private boolean firstTick = true;

    //For saving the time of the last update
    private long oldTimeLeft = 0;
    //The duration until the timerview is refreshed
    private int redrawTimerTime;
    //For calculating the number of times the timerview is refreshed before the cards are refreshed
    private int timerRedrawsPerUiRedraws = 0;
    //For counting how many times the timerview has been refreshed since last cardrefresh.
    private int countingRedraws = 0;

    /**
     * Create a ticker to update the cards with new data as time goes by.
     * @param cardHelper
     * @param listItems
     * @param loadTime
     * @param cards
     * @param reloadTime how long time it takes for the ticker to finish and we reload all cards.
     * @param tickDelay how often to tick.
     * @param redrawTimerTime The duration until the timerview is refreshed.
     */
    public CardTicker(CardHelper cardHelper,
                      List<DepartureCard> listItems,
                      long loadTime,
                      CardUI cards,
                      int reloadTime,
                      int tickDelay,
                      int redrawTimerTime) {
        this.helper = cardHelper;
        this.listItems = listItems;
        this.loadTime = loadTime;
        this.cardUI = cards;
        this.redrawTimerTime = redrawTimerTime;

        //calculate the number of times the
        timerRedrawsPerUiRedraws = tickDelay/redrawTimerTime;
        //Make sure that the timer updates the cards when enough time has passed, begin with updating.
        countingRedraws = timerRedrawsPerUiRedraws-1;

        this.timer = new CountDownTimer(reloadTime, redrawTimerTime) {
            public void onTick(long millisUntilFinished) {

                //count the number of times the timerview is refreshed until the cards are refreshed.
                if(countingRedraws++ == timerRedrawsPerUiRedraws){
                    if (tick() || firstTick) {
                        cardUI.refresh();
                        firstTick = false;
                    }
                    //since fired, reset.
                    countingRedraws = 0;
                }

                //save the time until finished for later comparison
                oldTimeLeft = millisUntilFinished;
                //Update the timerview to match the new time.
                helper.animationControl(CardHelper.AnimationStateEnum.COUNTDOWN,(double)oldTimeLeft);
            }
            public void onFinish() {
                helper.clearCards();
                helper.loadDataFromServer();
            }
        };
    }

    public void start() {
        timer.start();
    }

    public long stop() {
        timer.cancel();
        //make sure that we return the latest timestamp available for creating a new timer from it if wanted.
        return oldTimeLeft;
    }

    public boolean tick() {
        boolean ticked = false;
        for (DepartureCard card : listItems) {
            ticked = ticked || tick(card);
        }
        return ticked;
    }

    private String getTickedDepartureTime(DepartureCard card, int currentSeconds, long currentTime) {
        String currentDepartureTime = card.getTimeText();
        int secDiff = (int)(currentTime - loadTime) / 1000;
        int currentMinutes = card.getMinutesLeft();
        int newSeconds = currentSeconds - secDiff;
        int newMinutes = newSeconds / 60;

        if (newSeconds < SECONDS_REMOVE_CARD) {
          card.setTimeText(helper.getI18nString(R.string.tickerDepartured));
          return INVALIDATE;
        } else if (newSeconds <= SECONDS_DEPARTED) {
          return helper.getI18nString(R.string.tickerDepartured);
        } else if (newSeconds <= SECONDS_ABOUT_TO_DEPART) {
          return helper.getI18nString(R.string.tickerAboutToDepart);
        } else if (newMinutes != currentMinutes && newMinutes > 0 && currentDepartureTime.contains(" ")) {
           // Tick down the minutes.
           card.setMinutesLeft(newMinutes);
           String newDepartureTime = currentDepartureTime.substring(currentDepartureTime.indexOf(" "));
           return newMinutes + newDepartureTime;
        }
        return null;
    }

    private boolean tick(DepartureCard card) {
        int secondsLeft = card.getListData().getDisplayItem().getSecondsLeft();
        long now = System.currentTimeMillis();
        String newDepartureTime = getTickedDepartureTime(card, secondsLeft, now);
        if (INVALIDATE.equals(newDepartureTime)) {
          markDepartureCardAsDeparted(card);
          return true;
        } else if (newDepartureTime != null) {
          card.setTimeText(newDepartureTime);
          return true;
        }
        return false;
    }

    private void markDepartureCardAsDeparted(DepartureCard card) {
        card.setAlpha(0.3f);
    }
}
