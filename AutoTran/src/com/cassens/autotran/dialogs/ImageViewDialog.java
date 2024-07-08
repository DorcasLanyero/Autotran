package com.cassens.autotran.dialogs;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.cassens.autotran.R.layout.dialog_image_view;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.R;
import com.cassens.autotran.data.model.Image;
import com.sdgsystems.util.NoNullsArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import com.cassens.autotran.util.Typefaces;

/**
 * Created by bfriedberg on 2/3/15.
 */
public class ImageViewDialog extends Dialog implements View.OnClickListener {

    private static final Logger log = LoggerFactory.getLogger(ImageViewDialog.class.getSimpleName());

    private static final String TAG = "ImageViewDialog";

    private String mTitle;
    private Activity mCurrentActivity;
    private NoNullsArrayList<Image> mActiveImages;
    private ImageView mImageView;

    public enum ScreenPosition {
        FULL_SCREEN,
        SHIELD_VIEW
    }


    public ImageViewDialog(final Activity activity, String title) {
        super(activity);

        mCurrentActivity = activity;
        mTitle = title;

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        final Window window = getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        WindowManager.LayoutParams wlp = window.getAttributes();

        wlp.gravity = Gravity.LEFT|Gravity.TOP|Gravity.RIGHT;

        Drawable d = new ColorDrawable(Color.BLACK);
        d.setAlpha(200);

        window.setBackgroundDrawable(d);

        this.setContentView(dialog_image_view);

        ((Button) findViewById(R.id.dialog_image_dismiss)).setOnClickListener(this);
        ((Button) findViewById(R.id.dialog_image_delete)).setOnClickListener(this);
        ((Button) findViewById(R.id.dialog_image_delete)).setVisibility(VISIBLE);

        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ImageViewDialog.this.dismiss();
            }
        };

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);

        activity.registerReceiver(receiver, filter);

        this.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                activity.unregisterReceiver(receiver);
            }
        });



        displayMessage();
    }

    private void displayMessage() {

        //Typeface mTypefaceBold = Typefaces.get(mCurrentActivity, "fonts/Montserrat-Bold.ttf");
        //Typeface mTypefaceRegular = Typefaces.get(mCurrentActivity, "fonts/Montserrat-Regular.ttf");

        TextView title = ((TextView) findViewById(R.id.dialog_vin_num));
        title.setText(mTitle);
        //title.setTypeface(mTypefaceBold);

        mImageView = ((ImageView) findViewById(R.id.dialog_image_image));

    }

    public void DeleteImage()
    {
    }

    public void SetBitmap(Bitmap bitmap)
    {
        mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mImageView.setImageBitmap(bitmap);
    }

    @Override
    public void onClick(View view) {
        CommonUtility.logButtonClick(log, view);
        if(view.getId() == R.id.dialog_image_dismiss) {
            dismiss();
        }
        else if (view.getId() == R.id.dialog_image_delete) {
            DeleteImage();
        }
    }

    public void deleteButtonEnabled(boolean enabled) {
        ((Button) findViewById(R.id.dialog_image_delete)).setVisibility(enabled ? VISIBLE : GONE);
    }
}
