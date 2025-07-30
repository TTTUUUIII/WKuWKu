package ink.snowland.wkuwku.view;

import static ink.snowland.wkuwku.interfaces.RetroDefine.*;
import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.ViewGroup;

public class GSurfaceView extends SurfaceView {
    private int mOriginW = 0;
    private int mOriginH = 0;
    private int mW = 0;
    private int mH = 0;
    public GSurfaceView(Context context) {
        this(context, null);
    }

    public GSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mOriginH != 0) {
            float ratio = (float) mOriginW / mOriginH;
            int orientation = getResources().getConfiguration().orientation;
            mW = MeasureSpec.getSize(widthMeasureSpec);
            mH = MeasureSpec.getSize(heightMeasureSpec);
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mW = (int) (mH * ratio);
                if (mW > displayMetrics.widthPixels) {
                    mW = displayMetrics.widthPixels;
                    mH = (int) (mW / ratio);
                }
            } else {
                mH = (int) (mW / ratio);
            }
        }
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(mW, MeasureSpec.EXACTLY);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(mH, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void adjustSurfaceSize(int w, int h) {
        if (mOriginW == w && mOriginH == h) {
            return;
        }
        mOriginW = w;
        mOriginH = h;
        post(this::requestLayout);
    }

    public void fullScreen() {
        mOriginW = 0;
        mOriginH = 0;
        getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        post(this::requestLayout);
    }

    public int getTouch(int id) {
        if (mW == 0 || mH == 0) return 0;
        float x = mTouchX / mW;
        float y = mTouchY / mH;
        switch (id) {
            case RETRO_DEVICE_ID_POINTER_PRESSED:
                return mTouching ? 1 : 0;
            case RETRO_DEVICE_ID_POINTER_X:
                return (int) (x * 65534 - 32767);
            case RETRO_DEVICE_ID_POINTER_Y:
                return (int) (y * 65534 - 32767);
            default:
        }
        return 0;
    }

    private boolean mTouching = false;
    private float mTouchX = 0;
    private float mTouchY = 0;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        performClick();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouching = true;
                mTouchX = event.getX();
                mTouchY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                mTouchX = event.getX();
                mTouchY = event.getY();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mTouching = false;
                break;
            default:
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
