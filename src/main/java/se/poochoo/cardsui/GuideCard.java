package se.poochoo.cardsui;

/**
 * Created by Theo on 2013-09-27.
 */
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import se.poochoo.cardsui.objects.Card;

import se.poochoo.DialogHelper;
import se.poochoo.R;

public class GuideCard extends Card implements Card.OnCardSwiped {

    private final Context context;

    public GuideCard(Context context){
        this.context = context;
        this.setOnCardSwipedListener(this);
    }
    @Override
    public View getCardContent(Context context) {
        return LayoutInflater.from(context).inflate(R.layout.card_guide, null);
    }

    @Override
    public boolean isSwipeable() {
        return true;
    }

    @Override
    public void onCardSwiped(Card card, View layout) {
        DialogHelper.handleEvent(context, R.string.card_guide_string_title);
    }
}
