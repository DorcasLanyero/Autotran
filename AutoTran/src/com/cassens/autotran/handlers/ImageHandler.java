package com.cassens.autotran.handlers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Image;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by adam on 6/2/16.
 */
public class ImageHandler {
    private static final Logger log = LoggerFactory.getLogger(ImageHandler.class.getSimpleName());

    private static String TAG = "SupplementalNotesActivity";
    private final Context ctx;

    public ImageHandler(Context ctx) {
        this.ctx = ctx;
    }

    public Bitmap buildBitmap(String currentPhotoFileName) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        String newImageFilePath = CommonUtility.cachedImageFileFullPath(this.ctx, currentPhotoFileName);
        BitmapFactory.decodeFile(newImageFilePath, bmOptions);
        // Determine how much to scale down the image
        int scaleFactor = CommonUtility.getImageScaleFactor(bmOptions.outWidth, bmOptions.outHeight);


        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(newImageFilePath, bmOptions);

        log.debug(Logs.DEBUG, "current photopath: " + newImageFilePath);

        //If this is a honeywell device
        if (CommonUtility.hasHoneywellScanner()) {
            //We need to resolve the image rotation
            int rotation = CommonUtility.getImageRotation(ctx, newImageFilePath);
            if (rotation > 0) {
                bitmap = CommonUtility.rotateImage(bitmap, rotation);
            }
        }

        if (bitmap == null) {
            log.debug(Logs.DEBUG, "bitmap was null");
        }

        return bitmap;
    }

    public Bitmap buildHiresBitmap(String currentPhotoFileName) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = false;
        String newImageFilePath = CommonUtility.cachedImageFileFullPath(this.ctx, currentPhotoFileName);

        Bitmap bitmap = BitmapFactory.decodeFile(newImageFilePath, bmOptions);

        log.debug(Logs.DEBUG, "current photopath: " + newImageFilePath);

        //If this is a honeywell device
        if (CommonUtility.hasHoneywellScanner()) {
            //We need to resolve the image rotation
            int rotation = CommonUtility.getImageRotation(ctx, newImageFilePath);
            if (rotation > 0) {
                bitmap = CommonUtility.rotateImage(bitmap, rotation);
            }
        }

        if (bitmap == null) {
            log.debug(Logs.DEBUG, "bitmap was null");
        }

        return bitmap;
    }

    public void cameraButtonClick(String mCurrentPhotoFileName, int REQ_CODE_CAPTURE_IMAGE) {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(this.ctx.getPackageManager()) != null) {

            try {
                //Add the current path of the full size image
                File photoFile = new File(CommonUtility.cachedImageFileFullPath(this.ctx, mCurrentPhotoFileName + "_hires"));
                photoFile.createNewFile();

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                } else {
                    Uri photoUri = FileProvider.getUriForFile(this.ctx, this.ctx.getPackageName() + ".provider", photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                }
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (takePictureIntent.resolveActivity(this.ctx.getPackageManager()) != null) {
                    ((Activity) this.ctx).startActivityForResult(takePictureIntent, REQ_CODE_CAPTURE_IMAGE);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @SuppressLint("RestrictedApi")
    public static Bitmap processImage(String opString, String vinOrLoadNumber, String currentPhotoFileName) {
        double[] latLong = null;
        long savedDateTime = 0;

        String hiresFilePath = CommonUtility.cachedImageFileFullPath(CommonUtility.getCurrentActivity(), currentPhotoFileName + "_hires");

        try {
            ExifInterface hiresExif = new ExifInterface(hiresFilePath);
            latLong = hiresExif.getLatLong();
            savedDateTime = hiresExif.getDateTime();

        } catch (Exception e) {
            log.debug(Logs.EXCEPTIONS, e.toString());
        }

        if (latLong == null) {
            latLong = new double[]{0.0, 0.0};
        }

        final double savedLatLong[] = latLong;
        final long savedDateAndTime = savedDateTime;

        Bitmap hiresBitmap = BitmapFactory.decodeFile(hiresFilePath);

        log.debug(Logs.DEBUG, "current photopath: " + hiresFilePath);

        if (hiresBitmap == null) {
            log.debug(Logs.DEBUG, "bigBitmap was null");
        } else {
            //If this is a honeywell device
            if (CommonUtility.hasHoneywellScanner()) {
                //We need to resolve the image rotation
                int rotation = CommonUtility.getImageRotation(CommonUtility.getCurrentActivity(), hiresFilePath);
                hiresBitmap = CommonUtility.rotateImage(hiresBitmap, rotation);
            }
        }

        log.debug(Logs.DEBUG, "JUNK: Processing hi-res image in background");
        String finalOpString = opString;

        AsyncTask<Bitmap, Void, Void> saveBigBitmap = new AsyncTask<Bitmap, Void, Void>() {
            @Override
            protected Void doInBackground(Bitmap... bitmaps) {
                Bitmap newBigBitmap = HelperFuncs.addWatermarkOnBottom(bitmaps[0], vinOrLoadNumber, finalOpString, 8);
                CommonUtility.saveBitmap(newBigBitmap, hiresFilePath);

                try {
                    ExifInterface exif = new ExifInterface(hiresFilePath);
                    exif.setLatLong(savedLatLong[0], savedLatLong[1]);
                    exif.setDateTime(savedDateAndTime);
                    exif.saveAttributes();
                    log.debug(Logs.DEBUG, "JUNK: Finished processing hi-res image");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                Image largeImage = DataManager.getImageForFilename(CommonUtility.getCurrentActivity(), currentPhotoFileName);

                if (opString.contains("Supplemental notes")) {

                    if (largeImage != null) {
                        largeImage.s3_upload_status = Constants.SYNC_STATUS_NOT_UPLOADED;
                        DataManager.insertImageToLocalDB(CommonUtility.getCurrentActivity(), largeImage);
                        User driver = DataManager.getUserForDriverNumber(CommonUtility.getCurrentActivity(), CommonUtility.getDriverNumber(CommonUtility.getCurrentActivity()));
                        DataManager.pushLocalDataToRemoteServer(CommonUtility.getCurrentActivity(), driver.user_id, false);
                        SyncManager.sendNextPhoto(CommonUtility.getCurrentActivity());
                    }
                }

                newBigBitmap.recycle();

                return null;

            }
        };

        if (hiresBitmap != null) {
            saveBigBitmap.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, hiresBitmap);
        }

        return hiresBitmap;

    }

    @SuppressLint("RestrictedApi")
    public static Bitmap processThumbnailImage(String opString, String vinOrLoadNumber, String currentPhotoFileName, boolean watermark) {

        String hiresFilePath = CommonUtility.cachedImageFileFullPath(CommonUtility.getCurrentActivity(), currentPhotoFileName + "_hires");
        String thumbnailFilePath = CommonUtility.cachedImageFileFullPath(CommonUtility.getCurrentActivity(), currentPhotoFileName);

        double[] latLong = null;
        long savedDateTime = 0;

        try {
            ExifInterface hiresExif = new ExifInterface(hiresFilePath);
            latLong = hiresExif.getLatLong();
            savedDateTime = hiresExif.getDateTime();
        } catch (Exception e) {
            log.debug(Logs.EXCEPTIONS, e.toString());
        }

        if (latLong == null) {
            latLong = new double[]{0.0, 0.0};
        }

        final double savedLatLong[] = latLong;
        final long savedDateAndTime = savedDateTime;

        Bitmap thumbnailBitmap = CommonUtility.getBitmapThumbnail(CommonUtility.getCurrentActivity(), hiresFilePath);

        if (thumbnailBitmap == null) {
            log.debug(Logs.DEBUG, "thumbnail was null");
        }

        if (watermark) {
            thumbnailBitmap = HelperFuncs.addWatermarkOnBottom(thumbnailBitmap, vinOrLoadNumber, opString, 1);
        }

        if (thumbnailBitmap == null) {
            log.debug(Logs.DEBUG, "thumbnail was null");
        }

        CommonUtility.saveBitmap(thumbnailBitmap, thumbnailFilePath);

        try {
            ExifInterface lowresExif = new ExifInterface(thumbnailFilePath);
            lowresExif.setLatLong(savedLatLong[0], savedLatLong[1]);
            lowresExif.setDateTime(savedDateTime);
            lowresExif.saveAttributes();
        } catch (Exception e) {
            log.debug(Logs.EXCEPTIONS, e.toString());
        }

        return thumbnailBitmap;
    }

    public Image addImageLocation(Image image) {
          
    }
}
