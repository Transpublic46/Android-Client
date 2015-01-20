package se.poochoo.cardsui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class QuickReturnListView extends ListView {

	private int mItemCount;
	private int mItemOffsetY[];
	private boolean scrollIsComputed = false;
	private int mHeight;
    private int deltaScrollY;

	public QuickReturnListView(Context context) {
		super(context);
        setOverScrollMode(OVER_SCROLL_ALWAYS);
	}

	public QuickReturnListView(Context context, AttributeSet attrs) {
		super(context, attrs);
        setOverScrollMode(OVER_SCROLL_ALWAYS);
	}

    public interface OnOverScrolledListener {
        void onOverScrolled(ListView listView,
                            int scrollX, int scrollY, boolean clampedX, boolean clampedY, int deltaScrollY);
    }

    private OnOverScrolledListener mOnOverScrolledListener;

    @Override
    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX,
                                   int scrollY, int scrollRangeX, int scrollRangeY,
                                   int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        deltaScrollY = deltaY;
        return super.overScrollBy(0, deltaY, 0, scrollY, 0, scrollRangeY, 0, maxOverScrollY, isTouchEvent);
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);

            mOnOverScrolledListener.onOverScrolled(this, scrollX, scrollY, clampedX, clampedY, deltaScrollY);
    }

    public OnOverScrolledListener getOnOverScrolledListener() {
        return mOnOverScrolledListener;
    }

    public void setOnOverScrolledListener(OnOverScrolledListener onOverScrolledListener) {
        this.mOnOverScrolledListener = onOverScrolledListener;
    }

	public int getListHeight() {
		return mHeight;
	}

	public void computeScrollY() {

		mHeight = 0;
		try {
			mItemCount = getAdapter().getCount();
			if (mItemOffsetY == null) {
				mItemOffsetY = new int[mItemCount];
			}
			for (int i = 0; i < mItemCount; ++i) {
				View view = getAdapter().getView(i, null, this);
				view.measure(
						MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
						MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
				mItemOffsetY[i] = mHeight;
				mHeight += view.getMeasuredHeight();
			}
			scrollIsComputed = true;
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean scrollYIsComputed() {
		return scrollIsComputed;
	}

	public int getComputedScrollY() {
		int pos, nScrollY, nItemY;
		View view = null;
		pos = getFirstVisiblePosition();
		view = getChildAt(0);
		nItemY = view.getTop();
		nScrollY = mItemOffsetY[pos] - nItemY;

		return nScrollY;
	}
}