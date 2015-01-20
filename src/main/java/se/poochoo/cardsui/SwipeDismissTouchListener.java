/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package se.poochoo.cardsui;

import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import se.poochoo.cardsui.objects.Card;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.AnimatorListenerAdapter;


public class SwipeDismissTouchListener implements View.OnTouchListener {
	// Cached ViewConfiguration and system-wide constant values
	private int mSlop;
	private int mMinFlingVelocity;
	private int mMaxFlingVelocity;
	private long mAnimationTime;

	// Fixed properties
	private View mView;
	private OnDismissCallback mCallback;
	private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero

	// Transient properties
    private Card mCard;
	private float mDownX;
	private boolean mSwiping;
	private VelocityTracker mVelocityTracker;
	private float mTranslationX;

	/**
	 * The callback interface used by {@link SwipeDismissTouchListener} to
	 * inform its client about a successful dismissal of the view for which it
	 * was created.
	 */
	public interface OnDismissCallback {
		public void onDismiss(View view, Card card);
	}

	public SwipeDismissTouchListener(View view, Card card,
			OnDismissCallback callback) {
		ViewConfiguration vc = ViewConfiguration.get(view.getContext());
		mSlop = vc.getScaledTouchSlop() * 2;
		mMinFlingVelocity = (int)(vc.getScaledMinimumFlingVelocity() / card.getSwipeSensitivity());
		mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
		mAnimationTime = view.getContext().getResources()
				.getInteger(android.R.integer.config_shortAnimTime);
		mView = view;
        mCard = card;
		mCallback = callback;
	}

	@Override
	public boolean onTouch(View view, MotionEvent motionEvent) {
		// offset because the view is translated during swipe
        boolean canDismiss = mCard.isSwipeable();
		motionEvent.offsetLocation(mTranslationX, 0);
		if (mViewWidth < 2) {
			mViewWidth = mView.getWidth();
		}

		switch (motionEvent.getActionMasked()) {
		case MotionEvent.ACTION_DOWN: {
			// TODO: ensure this is a finger, and set a flag
			mDownX = motionEvent.getRawX();
			mVelocityTracker = VelocityTracker.obtain();
			mVelocityTracker.addMovement(motionEvent);
			return false;
		}

		case MotionEvent.ACTION_UP: {
			if (mVelocityTracker == null) {
				break;
			}
			float deltaX = motionEvent.getRawX() - mDownX;
			mVelocityTracker.addMovement(motionEvent);
			mVelocityTracker.computeCurrentVelocity(1000);
			float velocityX = Math.abs(mVelocityTracker.getXVelocity());
			float velocityY = Math.abs(mVelocityTracker.getYVelocity());
			boolean dismiss = false;
			boolean dismissRight = false;
            if (canDismiss) {
                if (Math.abs(deltaX) > mViewWidth / 2) {
                    dismiss = true;
                    dismissRight = deltaX > 0;
                } else if (mMinFlingVelocity <= velocityX
                        && velocityX <= mMaxFlingVelocity && velocityY < velocityX) {
                    dismiss = true;
                    dismissRight = mVelocityTracker.getXVelocity() > 0;
                }
            }
			if (dismiss) {
                dismissCard(dismissRight);
			} else {
				// cancel
				mView.animate().translationX(0).alpha(1)
						.setDuration(mAnimationTime).setListener(null);

			}
			mVelocityTracker.recycle();
			mVelocityTracker = null;
			mTranslationX = 0;
			mDownX = 0;
			mSwiping = false;
			break;
		}

		case MotionEvent.ACTION_MOVE: {
			if (mVelocityTracker == null) {
				break;
			}

			mVelocityTracker.addMovement(motionEvent);
			float deltaX = motionEvent.getRawX() - mDownX;
			if (Math.abs(deltaX) > mSlop) {
				mSwiping = true;
				mView.getParent().requestDisallowInterceptTouchEvent(true);

				// Cancel listview's touch
				MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
				cancelEvent
						.setAction(MotionEvent.ACTION_CANCEL
								| (motionEvent.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
				mView.onTouchEvent(cancelEvent);
				cancelEvent.recycle();
			}

			if (mSwiping) {
				mTranslationX = deltaX;
                if (canDismiss) {
				    mView.setTranslationX(deltaX);
                } else {
                    mView.setTranslationX(deltaX * 0.6f);
                }

				// TODO: use an ease-out interpolator or such
                if (canDismiss) {
                    mView.setAlpha(
					    	Math.max(
						    		0f,
							    	Math.min(1f, 1f - 1.5f * Math.abs(deltaX)
								    		/ mViewWidth)));
                }
				return true;
			}
			break;
		}

		}
		return false;
	}

    private void dismissCard(boolean dismissRight) {
        mView.animate()
                .translationX(dismissRight ? mViewWidth : -mViewWidth)
                .alpha(0).setDuration(mAnimationTime)
                .setListener(new AnimatorListener() {

                    @Override
                    public void onAnimationStart(Animator arg0) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator arg0) {
                    }

                    @Override
                    public void onAnimationEnd(Animator arg0) {
                        performDismiss();
                    }

                    @Override
                    public void onAnimationCancel(Animator arg0) {

                    }
                });
    }

	private void performDismiss() {
		// Animate the dismissed view to zero-height and then fire the dismiss
		// callback.
		// This triggers layout on each animation frame; in the future we may
		// want to do something
		// smarter and more performant.

        final ViewGroup.LayoutParams lp = mView.getLayoutParams();
		final int originalHeight = mView.getHeight();

		ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1)
				.setDuration(mAnimationTime);

		animator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				mCallback.onDismiss(mView, mCard);
				// Reset view presentation
				mView.setAlpha(1f);
				mView.setTranslationX(0);
				// mView.setAlpha(1f);
				// mView.setTranslationX(0);
				lp.height = originalHeight;
				mView.setLayoutParams(lp);
			}
		});

		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				lp.height = (Integer) valueAnimator.getAnimatedValue();
				mView.setLayoutParams(lp);
			}
		});

		animator.start();
	}
}
