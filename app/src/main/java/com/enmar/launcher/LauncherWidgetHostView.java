package com.enmar.launcher;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class LauncherWidgetHostView extends AppWidgetHostView {
    private boolean hasPerformedLongPress;
    private CheckForLongPress pendingCheckForLongPress;
    private OnLongClickListener longClickListener;

    public LauncherWidgetHostView(Context context) {
        super(context);
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        this.longClickListener = l;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (hasPerformedLongPress) {
            hasPerformedLongPress = false;
            return true;
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                postCheckForLongClick();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                hasPerformedLongPress = false;
                if (pendingCheckForLongPress != null) {
                    removeCallbacks(pendingCheckForLongPress);
                }
                break;
        }
        return false;
    }

    private void postCheckForLongClick() {
        hasPerformedLongPress = false;
        if (pendingCheckForLongPress == null) {
            pendingCheckForLongPress = new CheckForLongPress();
        }
        pendingCheckForLongPress.rememberWindowAttachCount();
        postDelayed(pendingCheckForLongPress, ViewConfiguration.getLongPressTimeout());
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        hasPerformedLongPress = false;
        if (pendingCheckForLongPress != null) {
            removeCallbacks(pendingCheckForLongPress);
        }
    }

    private class CheckForLongPress implements Runnable {
        private int originalWindowAttachCount;

        @Override
        public void run() {
            if (getParent() != null && hasWindowFocus()
                    && originalWindowAttachCount == getWindowAttachCount()
                    && !hasPerformedLongPress) {
                if (longClickListener != null && longClickListener.onLongClick(LauncherWidgetHostView.this)) {
                    hasPerformedLongPress = true;
                }
            }
        }

        public void rememberWindowAttachCount() {
            originalWindowAttachCount = getWindowAttachCount();
        }
    }
}
