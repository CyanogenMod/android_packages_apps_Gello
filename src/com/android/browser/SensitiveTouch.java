package com.android.browser;

import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

import android.util.Log;

public class SensitiveTouch {

    private final Handler mHandler = new Handler();

    private final View mView;
    private final OnSensitiveTouchListener mOnSensitiveTouchListener;

    private SensitiveTouch(View view, OnSensitiveTouchListener mOnSensitiveTouchListener) {
        this.mView = view;
        this.mOnSensitiveTouchListener = mOnSensitiveTouchListener;
    }

    public static SensitiveTouch setup(View view,
            OnSensitiveTouchListener mOnSensitiveTouchListener) {
        return new SensitiveTouch(view, mOnSensitiveTouchListener);
    }

    public void start() {
        mView.setOnTouchListener(new SensitiveTouchListener(mOnSensitiveTouchListener));
    }

    public class SensitiveTouchListener implements View.OnTouchListener {

        public final OnSensitiveTouchListener onSensitiveTouchListener;

        SensitiveTouchListener(OnSensitiveTouchListener onSensitiveTouchListener) {
            this.onSensitiveTouchListener = onSensitiveTouchListener;
        }

        @Override
        public boolean onTouch(final View view, final MotionEvent motionEvent) {
            final float firstPressure = motionEvent.getPressure();

            if (motionEvent.getAction() != MotionEvent.ACTION_MOVE) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("SensitiveGello", "First: " + firstPressure
                                + " Now: " + motionEvent.getPressure());
                        if (firstPressure < motionEvent.getPressure()-0.12) {
                            ((Vibrator) mView.getContext()
                                    .getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);

                            onSensitiveTouchListener.onSensitiveTouch();

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    onSensitiveTouchListener.onLeave();
                                }
                            }, 800);

                            mHandler.removeCallbacksAndMessages(null);
                        }
                    }
                }, 200);
            }
            return true;
        }
    }

    public interface OnSensitiveTouchListener {
        void onSensitiveTouch();
        void onLeave();
    }
}
