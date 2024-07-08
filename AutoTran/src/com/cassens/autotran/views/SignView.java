/**
 * Project : AutoTran
 * Author : xicom
 * Creation Date : 27-Nov-2013
 * Description : @TODO
 */
package com.cassens.autotran.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;

import com.amazonaws.auth.policy.Resource;
import com.cassens.autotran.R;

// A plain View was sufficient for signing: ImageView only for signature review.
public class SignView extends ImageView {
    public interface SignatureChangedListener {
        void onSignatureChanged();
    }

    private SignatureChangedListener mListener;
    private Bitmap mBitmap, cache;
    private Paint mBitmapPaint;
    private Path mPath;
    private Paint mPaint;
    private Context context;
    public boolean touched = false;

    private boolean mEmpty = true;

    // A ctor-overload with attributes is required for construction via layout.
    public SignView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context=context;
        init(context);
    }

    public SignView(Context context) {
        super(context);
        this.context=context;
        init(context);
    }

    public void setListener(SignatureChangedListener listener) {
        this.mListener = listener;
    }

    public void cache() {
        // Never cache an empty bitmap: onLayout will make a new one if we need one
        if(!mEmpty) cache = getBitmap();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        //setBitmap(mBitmap);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (cache != null && !mEmpty) {
            setBitmap(cache);
        }

        if (mBitmap == null) {

            // An empty signature causes a crash otherwise
            int width = this.getWidth();
            if(width == 0) {
                width = 1;
            }

            mBitmap = Bitmap.createBitmap(width, this.getHeight(), Bitmap.Config.ARGB_8888);
        } else if (!mBitmap.isMutable()) {
            mBitmap = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
        }

        mCanvas = new Canvas(mBitmap);
        mPath = new Path();

        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        setPaint();
    }

    private void init(Context context) {
        mEmpty = true;
        Log.d("SignView", "Empty is true");
    }

    private void setPaint() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(5);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); // draws e.g. the ImageView image (for sig review)
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.drawPath(mPath, mPaint);
    }

    public void resetView() {
        if(mBitmap  != null){
            mBitmap.eraseColor(getResources().getColor(R.color.white));
        }
        mEmpty = true;
        cache = null;
        invalidate();
    }

    public void setBitmap(Bitmap bitmap) {
        if(bitmap == null) {
            Log.d("SignView", "null bitmap, not setting to anything");

            return;
        } else {
            if (!bitmap.isMutable()) {
                mBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            } else {
                mBitmap = bitmap;
            }
            setImageBitmap(mBitmap);
            mEmpty = false;
        }
    }

    public Bitmap getBitmap() {
        buildDrawingCache(false);

        Bitmap cache = getDrawingCache();

        if(cache != null) {

            Bitmap b = cache.copy(Bitmap.Config.ARGB_8888, true);
            destroyDrawingCache();

            return b;
        } else {
            return null;
        }
    }

    public boolean isEmpty() {
        return mEmpty;
    }

    private float mX, mY;
    private Canvas mCanvas;
    private static final float TOUCH_TOLERANCE = 4;

    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE)
        {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
            mEmpty = false;
            //mListener.onSignatureChanged();
        }
    }

    private void touch_up() {
        mPath.lineTo(mX, mY);
        mCanvas.drawPath(mPath, mPaint);
        mPath.reset();
    }

    ISignatureTouchCallback mCallback;

    public void setOnTouchCallback(ISignatureTouchCallback callback) {
        mCallback = callback;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Ignore touch events when the view is disabled
        if (!isEnabled()) {
            if(mCallback != null) {
                mCallback.signatureTouchEvent(event);
            }
            return false;
        }

        touched = true;

        // Otherwise handle them
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            touch_start(x, y);
            invalidate();
            break;
        case MotionEvent.ACTION_MOVE:
            touch_move(x, y);
            invalidate();
            break;
        case MotionEvent.ACTION_UP:
            touch_up();
            invalidate();
            break;
        }
        return true;
    }
}
