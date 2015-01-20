package se.poochoo.cardsui;

/**
 * Created by Theo on 2013-09-27.
 */
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import se.poochoo.cardsui.objects.Card;

import se.poochoo.R;

public class TaxiCard extends Card {

    @Override
    public View getCardContent(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_taxi, null);

        return view;
    }

}
