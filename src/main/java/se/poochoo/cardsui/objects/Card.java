package se.poochoo.cardsui.objects;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import se.poochoo.R;
import se.poochoo.cardsui.Utils;

public abstract class Card extends AbstractCard {

	protected View mCardLayout;
	private OnCardSwiped onCardSwipedListener;
	private OnClickListener mListener;
	private OnLongClickListener mLongListener;
	private int mBackgroundResourceId = R.drawable.card_background_shadow;

	public Card() {

	}

	public Card(String title) {
		this.title = title;
	}

	public Card(String title, String desc) {
		this.title = title;
		this.desc = desc;
	}

	public Card(String title, int image) {
		this.title = title;
		this.image = image;
	}

	public Card(String title, String desc, int image) {
		this.title = title;
		this.desc = desc;
		this.image = image;
	}

	public Card(String titlePlay, String description, String color,
			String titleColor, Boolean hasOverflow, Boolean isClickable) {

		this.titlePlay = titlePlay;
		this.description = description;
		this.color = color;
		this.titleColor = titleColor;
		this.hasOverflow = hasOverflow;
		this.isClickable = isClickable;
	}

	public Card(String titlePlay, String description, int imageRes,
			String titleColor, Boolean hasOverflow, Boolean isClickable) {

		this.titlePlay = titlePlay;
		this.description = description;
		this.titleColor = titleColor;
		this.hasOverflow = hasOverflow;
		this.isClickable = isClickable;
		this.imageRes = imageRes;
	}

	@Override
	public View getView(Context context, boolean swipable) {
		return getView(context, false);
	}

	@Override
	public View getView(Context context) {

		View view = LayoutInflater.from(context).inflate(getCardLayout(), null);
		view.setBackgroundResource(mBackgroundResourceId);

		mCardLayout = view;

		try {
			((FrameLayout) view.findViewById(R.id.cardContent))
					.addView(getCardContent(context));
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

		// ((TextView) view.findViewById(R.id.title)).setText(this.title);

		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		int bottom = Utils.convertDpToPixelInt(context, 12);
		lp.setMargins(0, 0, 0, bottom);

		view.setLayoutParams(lp);

		return view;
	}

	public View getViewLast(Context context) {

		View view = LayoutInflater.from(context).inflate(getLastCardLayout(),
				null);
		view.setBackgroundResource(mBackgroundResourceId);

		mCardLayout = view;

		try {
			((FrameLayout) view.findViewById(R.id.cardContent))
					.addView(getCardContent(context));
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

		// ((TextView) view.findViewById(R.id.title)).setText(this.title);

		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		int bottom = Utils.convertDpToPixelInt(context, 12);
		lp.setMargins(0, 0, 0, bottom);

		view.setLayoutParams(lp);

		return view;
	}

	public View getViewFirst(Context context) {

		View view = LayoutInflater.from(context).inflate(getFirstCardLayout(),
				null);
		view.setBackgroundResource(mBackgroundResourceId);

		mCardLayout = view;

		try {
			((FrameLayout) view.findViewById(R.id.cardContent))
					.addView(getCardContent(context));
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

		// ((TextView) view.findViewById(R.id.title)).setText(this.title);

		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		int bottom = Utils.convertDpToPixelInt(context, 12);
		lp.setMargins(0, 0, 0, bottom);

		view.setLayoutParams(lp);

		return view;
	}

	public abstract View getCardContent(Context context);

	public OnClickListener getClickListener() {
		return mListener;
	}
	
	
	public void setOnClickListener(OnClickListener listener) {
		mListener = listener;
	}
	
	
	public OnLongClickListener getLongClickListener() {
		return mLongListener;
	}

	public void setOnLongClickListener(OnLongClickListener listener) {
		mLongListener = listener;
		
	}

	public void OnSwipeCard() {
		if (onCardSwipedListener != null)
			onCardSwipedListener.onCardSwiped(this, mCardLayout);
		// TODO: find better implementation to get card-object's used content
		// layout (=> implementing getCardContent());
	}

	public OnCardSwiped getOnCardSwipedListener() {
		return onCardSwipedListener;
	}

	public void setOnCardSwipedListener(OnCardSwiped onEpisodeSwipedListener) {
		this.onCardSwipedListener = onEpisodeSwipedListener;
	}

	public void setBackgroundResource(int resid) {
		mBackgroundResourceId = resid;
	}

	protected int getCardLayout() {
		return R.layout.item_card;
	}

	protected int getLastCardLayout() {
		return R.layout.item_card_empty_last;
	}

	protected int getFirstCardLayout() {
		return R.layout.item_play_card_empty_first;
	}

	public interface OnCardSwiped {
		public void onCardSwiped(Card card, View layout);
	}

    public float getSwipeSensitivity() {
        // Lower = less sensitive to swipe.
        return 1.0f;
    }
}
