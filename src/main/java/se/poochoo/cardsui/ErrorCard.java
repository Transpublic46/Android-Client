package se.poochoo.cardsui;

/**
 * Created by Theo on 2013-09-27.
 */
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import se.poochoo.cardsui.objects.Card;

import se.poochoo.R;

public class ErrorCard extends Card {

    final int message;
    final int image;
    public ErrorCard(int image, int message){
        this.message = message;
        this.image = image;
    }

    @Override
    public View getCardContent(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_error, null);
        ((ImageView) view.findViewById(R.id.errorimage)).setImageResource(image);
        ((TextView) view.findViewById(R.id.errortext)).setText(message);
        return view;
    }

    @Override
    public boolean isSwipeable() {
        // Errors can't be swiped.
        return false;
    }
}
