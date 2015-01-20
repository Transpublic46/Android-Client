package se.poochoo.cardsui;

import se.poochoo.cardsui.objects.Card;
import se.poochoo.cardsui.views.CardUI;

import se.poochoo.proto.Messages.SmartListData;

/**
 * Created by Erik on 2013-10-01.
 */
public class CardStackBuilder {

    private final CardUI cardUI;
    private float swipeSensitivity = 1.0f;
    private boolean swipeable = true;
    private Card.OnCardSwiped swipeListener = null;
    private DepartureCard.OnCardClickListener clickListener = null;

    public CardStackBuilder(CardUI card) {
        this.cardUI = card;
    }

    public CardStackBuilder setSwipeable(boolean swipeable) {
        this.swipeable = swipeable;
        return this;
    }

    public CardStackBuilder setSwipeSensitivity(float swipeSensitivity) {
        this.swipeSensitivity = swipeSensitivity;
        return this;
    }

    public CardStackBuilder setSwipeListener(Card.OnCardSwiped swipeListener) {
        this.swipeListener = swipeListener;
        return this;
    }

    public CardStackBuilder setClickListener(DepartureCard.OnCardClickListener clickListener) {
        this.clickListener = clickListener;
        return this;
    }

    public DepartureCard buildCard(SmartListData listDataItem) {
        DepartureCard departureCard = DepartureCard.fromSmartListDataProto(listDataItem);
        departureCard.setOnCardSwipedListener(swipeListener);
        departureCard.setListener(clickListener);
        departureCard.setSwipeSensitivity(swipeSensitivity);
        departureCard.setSwipeable(swipeable);
        departureCard.setIndexInStack( cardUI.addCard(departureCard));
        return departureCard;
    }
}
