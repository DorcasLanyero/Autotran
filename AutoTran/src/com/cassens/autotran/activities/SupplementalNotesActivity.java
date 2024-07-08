package com.cassens.autotran.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Spanned;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.FileProvider;
import androidx.gridlayout.widget.GridLayout;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.Image;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.model.interfaces.VehicleBatchInterface;
import com.cassens.autotran.data.remote.tasks.RemoteSyncTask;
import com.cassens.autotran.dialogs.ImageViewDialog;
import com.cassens.autotran.handlers.ImageHandler;
import com.cassens.autotran.handlers.LocationHandler;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;


public class SupplementalNotesActivity extends AutoTranActivity {
    private static final Logger log = LoggerFactory.getLogger(SupplementalNotesActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    private static String TAG = "SupplementalNotesActivity";

    private LocationHandler locationHandler;

    public static final int REQ_CODE_NOTES = 1006;
    public static final int REQ_CODE_CAPTURE_IMAGE = 1007;

    private VehicleBatchInterface batch;
    private int operation;
    private String newNotes = "";
    private List<Image> activeImages = new ArrayList<>();

    private TextView notesList;
    private GridLayout cameraImageLayout;

    private String mCurrentPhotoFileName;
    private String mImageFileNamePrefix;

    private String mfg;
    private Load loadForThisDelivery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationHandler = LocationHandler.getInstance(this);
        locationHandler.startLocationTracking();

        // view changes
        setContentView(R.layout.activity_supplemental_notes);

        String title = "Notes - ";
        Bundle bundle = getIntent().getExtras();

        operation = bundle.getInt(Constants.CURRENT_OPERATION);
        if (operation == Constants.DELIVERY_OPERATION) {
            Delivery delivery = DataManager.getDelivery(this, bundle.getInt(Constants.CURRENT_LOOKUP_ID));
            loadForThisDelivery = DataManager.getLoad(this, delivery.load_id);
            title += loadForThisDelivery.loadNumber;

            if(!loadForThisDelivery.shuttleLoad) {
                title += " - " + delivery.dealer.customer_number;
                mfg = delivery.dealer.mfg;
            }
            batch = delivery;
        } else {
            Load load = DataManager.getLoad(this, bundle.getInt(Constants.CURRENT_LOOKUP_ID));
            title += load.loadNumber;
            batch = load;
            if (!load.shuttleLoad) {
                for (Delivery delivery : load.deliveries) {
                    if (mfg == null) {
                        mfg = delivery.dealer.mfg;
                    } else {
                        mfg = mfg + "," + delivery.dealer.mfg;
                    }
                }
            }
        }

        ((TextView) findViewById(R.id.ACTIVITY_TITLE)).setText(title);
        notesList = findViewById(R.id.notesList);
        notesList.setVisibility(View.VISIBLE);

        cameraImageLayout = findViewById(R.id.cameraImagesLL);
        // end view changes

        notesList.setText(NotesActivity.formatPredefNotesForTextView(batch.getNotes()));

        if (operation == Constants.DELIVERY_OPERATION) {

            if(((Delivery)batch).shuttleLoad) {
                mImageFileNamePrefix = Constants.DELIVERY_IMAGE_FILE_PREFIX + (DataManager.getLoad(this, ((Delivery)batch).load_id)).loadNumber
                        + Constants.IMAGE_FILE_DELIM + Constants.IMAGE_FILE_DELIM;
            } else {
                mImageFileNamePrefix = Constants.DELIVERY_IMAGE_FILE_PREFIX + (DataManager.getLoad(this, ((Delivery) batch).load_id)).loadNumber
                        + Constants.IMAGE_FILE_DELIM + ((Delivery) batch).dealer.customer_number + Constants.IMAGE_FILE_DELIM;
            }
        }
        else {
            mImageFileNamePrefix = Constants.PRELOAD_IMAGE_FILE_PREFIX + ((Load)batch).loadNumber + Constants.IMAGE_FILE_DELIM;
        }

        for (Image img : batch.getImages()) {
            if(!img.filename.contains("hires")) {
                String newFilepath = CommonUtility.cachedImageFileFullPath(this, img.filename);
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                Bitmap bitmap = BitmapFactory.decodeFile(newFilepath, bmOptions);
                addBitmapToList(bitmap, img, false);
            }
        }
    }

    @Override
    protected void onDestroy() {
        locationHandler.stopLocationTracking();
        super.onDestroy();
    }

    private Spanned getNotes() {
        StringBuilder allNotes = new StringBuilder(batch.getNotes());
        if (allNotes.length() != 0 && !HelperFuncs.isNullOrEmpty(newNotes)) {
            allNotes.append("\n\n");
        }
        allNotes.append(newNotes);

        return NotesActivity.formatPredefNotesForTextView(allNotes.toString());
    }

    public void notesClick(View v) {
        CommonUtility.logButtonClick(log, v);
        Intent intent = new Intent(SupplementalNotesActivity.this, NotesActivity.class);

        intent.putExtra(NotesActivity.EXTRA_STATE, NotesActivity.DELIVERY_DRIVER_SIGNOFF);
        String oldNotes = batch.getNotes();
        int oldNotesLen = (oldNotes == null) ? 0 : oldNotes.length();
        intent.putExtra(NotesActivity.EXTRA_OLD_NOTES, oldNotes);
        intent.putExtra(NotesActivity.EXTRA_NOTES, newNotes);
        // Reduce maximum note length to allow for old notes and header
        int maxNewNoteLen;
        if (operation == Constants.DELIVERY_OPERATION) {
            maxNewNoteLen = getResources().getInteger(R.integer.max_delivery_note_length);
        }
        else {
            maxNewNoteLen = getResources().getInteger(R.integer.max_note_length);
        }
        maxNewNoteLen -= (oldNotesLen + generateNotesHeader(oldNotesLen == 0).length());
        if (maxNewNoteLen <= 0) {
            maxNewNoteLen = 0;
        }
        intent.putExtra(NotesActivity.EXTRA_MAX_LENGTH, maxNewNoteLen);
        intent.putExtra(Constants.CURRENT_OPERATION, operation);
        if (mfg != null) {
            intent.putExtra(NotesActivity.EXTRA_MFG, mfg);
        }
        intent.putExtra(NotesActivity.EXTRA_TITLE, "Notes");
        intent.putExtra(NotesActivity.EXTRA_PROMPT, "Type a note, or choose predefined notes");
        intent.putExtra(NotesActivity.EXTRA_IS_EDITABLE, true);
        startActivityForResult(intent, REQ_CODE_NOTES);
    }




    @SuppressLint("RestrictedApi")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent incomingIntent) {
        super.onActivityResult(requestCode, resultCode, incomingIntent);

        switch (requestCode) {
            case REQ_CODE_CAPTURE_IMAGE:
                if (resultCode == RESULT_OK) {

                   String opString = "";
                    String loadNumber = "";
                    int load_id = -1;

                    Image smallImage = makeImages(loadNumber, load_id);

                    String message = "Picture was taken ";

                    if (operation == Constants.PRELOAD_OPERATION || operation == Constants.SHUTTLE_LOAD_OPERATION) {
                        opString = "- Supplemental notes - Preload -";
                        loadNumber = ((Load) batch).loadNumber;
                        message += " for load " + loadNumber;
                    } else {
                        opString = "- Supplemental notes - Delivery -";
                        Load tmpLoad = DataManager.getLoad(this, ((Delivery) batch).load_id);
                        loadNumber = tmpLoad.loadNumber;
                        load_id = tmpLoad.load_id;
                        message += " for load " + loadNumber;
                        if (batch != null && ((Delivery) batch).dealer != null) {
                            message += " delivery to " + ((Delivery) batch).dealer.customer_number;
                        }
                    }

                    Bitmap thumbnail = ImageHandler.processThumbnailImage(opString, loadNumber, mCurrentPhotoFileName, true);
                    Bitmap bitmap = ImageHandler.processImage(opString, loadNumber, mCurrentPhotoFileName);

                   addBitmapToList(thumbnail, smallImage, true);

                } else{
                        CommonUtility.showText(" Picture was not taken ");
                        CommonUtility.deleteCachedImageFile(this, mCurrentPhotoFileName);
                    }

                    System.out.println("Image capture Result Code :: " + resultCode);
                    break;

                    case REQ_CODE_NOTES:
                        if (resultCode == RESULT_OK && incomingIntent != null) {
                            newNotes = incomingIntent.getStringExtra("notes");

                            notesList.setText(this.getNotes());
                        }
                        break;
                    default:
                        break;
                }
        }

    private Image makeImages(String loadNumber, int load_id) {
        Image smallImage = new Image();

        smallImage.preloadImage = (operation != Constants.DELIVERY_OPERATION);
        if (operation == Constants.DELIVERY_OPERATION && !HelperFuncs.isNullOrEmpty(batch.getRemoteId())) {
            smallImage.delivery_id = Integer.parseInt(batch.getRemoteId());

            log.debug(Logs.DEBUG, "Saving load id of " + load_id + " ldnbr " + loadNumber + " for supplemental image");

            smallImage.load_id = load_id;
        } else if (!HelperFuncs.isNullOrEmpty(batch.getRemoteId())){
            smallImage.load_id = Integer.parseInt(batch.getRemoteId());
        }

        Location currentLocation = locationHandler.getLocation();
        smallImage.imageLat = String.valueOf(currentLocation.getLatitude());
        smallImage.imageLon = String.valueOf(currentLocation.getLongitude());

        smallImage.filename = mCurrentPhotoFileName;
        DataManager.insertImageToLocalDB(this, smallImage);

        activeImages.add(smallImage);

        if (AppSetting.SEND_HIRES_ON_SUPPLEMENTALS.getBoolean()) {
            Image largeImage = HelperFuncs.getHiresCopy(smallImage);
            DataManager.insertImageToLocalDB(this, largeImage);
        }

        return smallImage;
    }

    private static String generateNotesHeader(boolean first) {
        StringBuilder notesHeader = new StringBuilder(first ? "" : "\n\n");

        SimpleDateFormat notesDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz");
        notesDF.setTimeZone(TimeZone.getDefault());
        notesHeader.append(notesDF.format(new Date()));
        notesHeader.append("\n");
        return notesHeader.toString();
    }

    private void addBitmapToList(Bitmap bitmap, Image image, final boolean canDelete) {
        if (image == null) {
            log.debug(Logs.DEBUG, "creating new image since image object was null...");
            image = new Image();
        }

        ImageView imageView = new ImageView(this);
        //setting image resource
        imageView.setImageBitmap(bitmap);
        //setting image position
        imageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        imageView.setPadding(10, 10, 10, 10);

        imageView.setTag(image);

        cameraImageLayout.addView(imageView);

        final String loadNumber;
        if (operation == Constants.PRELOAD_OPERATION || operation == Constants.SHUTTLE_LOAD_OPERATION) {
            loadNumber = ((Load) batch).loadNumber;
        } else {
            Load tmpLoad = DataManager.getLoad(this, ((Delivery) batch).load_id);
            loadNumber = tmpLoad.loadNumber;
        }

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Prevent rapid clicks from opening multiple windows
                if (CommonUtility.doubleClickDetected()) {
                    return;
                }

                final View view = v;

                log.debug(Logs.DEBUG, "Clicked the image: " + activeImages.size());

                final ImageViewDialog imageViewDialog = new ImageViewDialog(SupplementalNotesActivity.this,
                        "Load #" + loadNumber) {
                    @Override
                    public void DeleteImage() {
                        final ImageViewDialog imageViewDialog = this;
                        super.DeleteImage();
                        AlertDialog.Builder builder = new AlertDialog.Builder(SupplementalNotesActivity.this);
                        builder.setTitle("Delete ?");
                        builder.setMessage("Would you like to delete this image?");
                        DialogInterface.OnClickListener dialogListener = (dialog, which) -> {
                            switch (which) {
                                case -1:
                                    Image image1 = (Image) view.getTag();

                                    if (image1 == null) {
                                        log.debug(Logs.DEBUG, "The image tag of the view being deleted was null, resulting in the image not being removed from active images");
                                        return;
                                    }

                                    if (image1.filename != null) {
                                        CommonUtility.deleteCachedImageFile(getApplicationContext(), image1.filename + "_hires");
                                        CommonUtility.deleteCachedImageFile(getApplicationContext(), image1.filename);
                                    }

                                    log.debug(Logs.DEBUG, "activeimages size: " + activeImages.size());
                                    activeImages.remove(image1);
                                    log.debug(Logs.DEBUG, "activeimages post-remove size: " + activeImages.size());

                                    cameraImageLayout.removeView(view);
                                    imageViewDialog.dismiss();
                                    break;
                                case -2:
                                    dialog.dismiss();
                                    break;
                                default:
                                    System.out.println("which value : " + which);
                                    break;
                            }
                        };
                        builder.setPositiveButton("Yes", dialogListener);
                        builder.setNegativeButton("No", dialogListener);
                        builder.create().show();
                    }
                };

                Image image = (Image) view.getTag();
                //
                // The jpg with the full-resolution image created by the camera is cached on the device
                // until the load associated with the image is deleted.  If the file for the image still
                // exists, we use that in order to display the highest resolution; otherwise, we use
                // the lower-resolution data stored in the database.
                //
                // Note: Prior to version 1.129, the filenames stored in the image didn't match up to
                //       any temporary files on disk.

                String filename = CommonUtility.cachedImageFileFullPath(getApplicationContext(),image.filename);

                File bigFile = new File(CommonUtility.cachedImageFileFullPath(getApplicationContext(),image.filename + "_hires"));

                if(bigFile.exists()) {
                    filename = bigFile.getAbsolutePath();
                }

                if (new File(filename).exists()) {
                    Bitmap bigBitmap = BitmapFactory.decodeFile(filename);
                    if (bigBitmap == null) {
                        log.debug(Logs.DEBUG, "bigBitmap was null");
                    } else {
                        imageViewDialog.SetBitmap(bigBitmap);
                    }
                }

                imageViewDialog.deleteButtonEnabled(canDelete);
                imageViewDialog.show();
            }
        });
    }

    public void saveClick(View v) {
        CommonUtility.logButtonClick(log, v);
        saveChanges();
    }

    private void saveChanges() {
        addNoteToVehicleBatch(this, batch, newNotes);

        for (Image image : activeImages) {
            if (!image.filename.contains("hires")) {
                image.s3_upload_status = Constants.SYNC_STATUS_NOT_UPLOADED;
                DataManager.insertImageToLocalDB(this, image);
            }
        }

        if (activeImages.size() > 0 || !HelperFuncs.isNullOrEmpty(newNotes)) {
            RemoteSyncTask syncTask = new RemoteSyncTask(this);

            int driverId = -1;
            if (operation == Constants.DELIVERY_OPERATION) {
                driverId = DataManager.getLoad(this, ((Delivery) batch).load_id).driver_id;
            } else {
                driverId = ((Load) batch).driver_id;
            }
            syncTask.execute(driverId);
        }

        User driver = DataManager.getUserForDriverNumber(this, CommonUtility.getDriverNumber(this));
        CommonUtility.uploadLogMessage("Calling pushLocalDataToRemoteServer from SupplementalNotesActivity.saveClick()");
        DataManager.pushLocalDataToRemoteServer(getApplicationContext(), driver.user_id, false);

        if (batch instanceof Load) {
            if (!HelperFuncs.isNullOrWhitespace(newNotes)) {
                CommonUtility.highLevelLog("Driver $driverNumber added supplementary preload note to load $loadNumber: " + newNotes, (Load)batch);
            }
            if (activeImages.size() > 0) {
                CommonUtility.highLevelLog(String.format("Driver $driverNumber added %d supplementary preload image(s) to load $loadNumber", activeImages.size()), (Load)batch);
            }
        }
        else if (batch instanceof Delivery) {
            if (!HelperFuncs.isNullOrWhitespace(newNotes)) {
                CommonUtility.highLevelLog("Driver $driverNumber added supplementary note to delivery ID $deliveryId: " + newNotes, loadForThisDelivery, (Delivery)batch);
            }
            if (activeImages.size() > 0) {
                CommonUtility.highLevelLog(String.format("Driver $driverNumber added %d supplementary image(s) for delivery ID $deliveryId", activeImages.size()), loadForThisDelivery, (Delivery)batch);
            }
        }

        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    public static void addNoteToVehicleBatch(Context ctx, VehicleBatchInterface currentBatch, String addedNotes) {
        if (!HelperFuncs.isNullOrEmpty(addedNotes)) {
            StringBuilder allNotes = new StringBuilder(currentBatch.getNotes());
            allNotes.append(generateNotesHeader(allNotes.length() == 0));
            allNotes.append(addedNotes);
            currentBatch.setNotes(allNotes.toString());
            currentBatch.setUploaded(false);
            currentBatch.save(ctx);
        }
    }

    public void back(View v) {
        final Activity ctx = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(SupplementalNotesActivity.this);
        builder.setMessage("Do you wish to save any current changes, or discard and lose unsaved progress?");
        builder.setPositiveButton("Save",(dailog, which) -> {
            CommonUtility.logButtonClick(log, "Save", "at confirmation dialog");
            saveChanges();
        });
        builder.setNegativeButton("Discard", (dialog, which) -> {
            CommonUtility.logButtonClick(log, "Discard", "at confirmation dialog");
            ctx.finish();
        });
        builder.setNeutralButton("Cancel", null);
        builder.setCancelable(true);
        builder.create().show();
    }

    @Override
    public void onBackPressed() {
        log.debug(Logs.INTERACTION, "Back pressed");
        back(null);
    }

    public void cameraButtonClick(View v) {
        CommonUtility.logButtonClick(log, v);
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            try {
                //Add the current path of the full size image
                mCurrentPhotoFileName = mImageFileNamePrefix + UUID.randomUUID().toString();
                File photoFile = new File(CommonUtility.cachedImageFileFullPath(this, mCurrentPhotoFileName + "_hires"));
                photoFile.createNewFile();

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                } else {
                    Uri photoUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                }
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (takePictureIntent.resolveActivity(getApplicationContext().getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQ_CODE_CAPTURE_IMAGE);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
