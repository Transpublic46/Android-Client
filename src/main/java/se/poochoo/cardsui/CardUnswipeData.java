package se.poochoo.cardsui;

import se.poochoo.db.SelectionUserDataSource;

/**
 * Created by Erik on 2013-10-03.
 */
public class CardUnswipeData {
    private int multiplier;
    private SelectionUserDataSource.SelectionType selectionType;
    private DepartureCard card;

    public CardUnswipeData(int multiplier,
            SelectionUserDataSource.SelectionType selectionType, DepartureCard card) {
        this.multiplier = multiplier;
        this.selectionType = selectionType;
        this.card = card;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public SelectionUserDataSource.SelectionType getSelectionType() {
        return selectionType;
    }

    public DepartureCard getCard() {
        return card;
    }
}
