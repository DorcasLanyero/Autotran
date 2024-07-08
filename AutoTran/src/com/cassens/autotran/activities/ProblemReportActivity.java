package com.cassens.autotran.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.gridlayout.widget.GridLayout;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Image;
import com.cassens.autotran.data.model.ProblemReport;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.dialogs.ImageViewDialog;
import com.cassens.autotran.handlers.ImageHandler;
import com.cassens.autotran.handlers.LocationHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ProblemReportActivity extends AutoTranActivity {
    private static final Logger log = LoggerFactory.getLogger(ProblemReportActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    public static final int REQ_CODE_CAPTURE_IMAGE = 1107;

    private LocationHandler locationHandler;
    private List<Image> activeImages = new ArrayList<>();

    private ImageHandler imageHandler;

    private EditText description;
    private GridLayout cameraImageLayout;
    private Spinner classificationSpinner;
    private ArrayAdapter classificationAdapter;

    private String mCurrentPhotoFileName;
    private String mImageFileNamePrefix = "Problem_Report_";

    private ProblemReport report;
    private int driver_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_problem_report);

        report = new ProblemReport();

        locationHandler = LocationHandler.getInstance(this);
        locationHandler.startLocationTracking();

        imageHandler = new ImageHandler(this);

        classificationSpinner = findViewById(R.id.classificationSpinner);
        description = findViewById(R.id.description);
        cameraImageLayout = findViewById(R.id.cameraImagesLL);



        classificationAdapter = new ArrayAdapter(this, R.layout.spinner_item,
                getResources().getStringArray(R.array.problem_report_categorgies)) {
            @Override
            public boolean isEnabled(int position) {
                if (position == 0) {
                    //first item is for hint only
                    return false;
                } else {
                    return true;
                }
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                if (position == 0) {
                    textView.setTextColor(Color.DKGRAY);
                } else {
                    textView.setTextColor(Color.BLACK);
                }
                return view;
            }
        };

        classificationSpinner.setAdapter(classificationAdapter);

        findViewById(R.id.done).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(ProblemReportActivity.this);
            if (description.getText().length() == 0) {
                builder.setTitle("No Problem Description Entered");
                builder.setMessage("Please select a category before submitting your problem report.");
                builder.setPositiveButton("Ok", (dialog, which) -> dialog.dismiss());
                builder.setCancelable(true);

                builder.create().show();
            } else if (classificationSpinner.getSelectedItemPosition() == 0) {
                builder.setTitle("No Category Selected");
                builder.setMessage("Please select a category before submitting your problem report.");
                builder.setPositiveButton("Ok", (dialog, which) -> dialog.dismiss());
                builder.setCancelable(true);

                builder.create().show();
            } else {
                CommonUtility.PromptForPayNumber(this,
                        new IPayNumberDialogCallback() {
                            @Override
                            public void complete() {
                                log.debug(Logs.INTERACTION, "Driver sent a problem report");
                                saveData();
                                finish();
                            }
                            @Override
                            public void cancelled() {
                                log.debug(Logs.INTERACTION, "Driver clicked 'Return to Report'");
                            }
                        },
                        "Thank you for taking the time to inform us of this problem!",
                        "Submit Problem Report",
                        "Enter your pay number to submit the report:",
                        true,
                        "Submit",
                        120 // Time out after 2 minutes
                );
            }
        });
    }

    @Override
    protected void onDestroy() {
        locationHandler.stopLocationTracking();
        super.onDestroy();
    }

    private void saveData() {
        report.timestamp = new Date().getTime();

        Location currentLocation = locationHandler.getLocation();
        report.latitude = currentLocation.getLatitude();
        report.longitude = currentLocation.getLongitude();

        report.category = classificationSpinner.getSelectedItem().toString();

        report.description = description.getText().toString();

        report.driver_id = CommonUtility.getDriverNumberAsInt(this);

        report.imageCount = activeImages.size();

        DataManager.saveProblemReport(getApplicationContext(), report);

        //save the images
        for(Image image: activeImages) {
            image.problem_report_guid = report.getGuid();
            image.s3_upload_status = Constants.SYNC_STATUS_NOT_UPLOADED;
            DataManager.insertImageToLocalDB(getApplicationContext(), image);
        }

        User driver = DataManager.getUserForDriverNumber(this, CommonUtility.getDriverNumber(this));
        CommonUtility.uploadLogMessage("Calling pushLocalDataToRemoteServer from ProblemReportActivity.saveData()");
        DataManager.pushLocalDataToRemoteServer(getApplicationContext(), driver.user_id, false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent incomingIntent) {
        super.onActivityResult(requestCode, resultCode, incomingIntent);

        switch (requestCode) {
            case REQ_CODE_CAPTURE_IMAGE:
                if (resultCode == RESULT_OK) {

                    CommonUtility.showText(" Picture was taken ");
                    //String hiresFilePath = CommonUtility.cachedImageFileFullPath(this, mCurrentPhotoFileName + "_hires");

                    Bitmap bitmap = ImageHandler.processThumbnailImage("problem report", CommonUtility.getDriverNumber(this), mCurrentPhotoFileName, true);

                    Image image = new Image();

                    File photoFile = new File(CommonUtility.cachedImageFileFullPath(this, mCurrentPhotoFileName + "_hires"));


                    try {
                        photoFile.createNewFile();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, new FileOutputStream(photoFile));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    activeImages.add(image);
                    addBitmapToList(bitmap, image, true);
                } else {
                    CommonUtility.showText(" Picture was not taken ");
                    CommonUtility.deleteCachedImageFile(this, mCurrentPhotoFileName);
                }

                System.out.println("Image capture Result Code :: " + resultCode);
                break;
            default:
                break;
        }
    }

    public void cameraButtonClick(View v) {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            try {
                //Add the current path of the full size image
                mCurrentPhotoFileName = mImageFileNamePrefix + report.guid + "_" + UUID.randomUUID().toString();
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
                e.printStackTrace();
            }
        }
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

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Prevent rapid clicks from opening multiple windows
                if (CommonUtility.doubleClickDetected()) {
                    return;
                }
                final View view = v;

                final ImageViewDialog imageViewDialog = new ImageViewDialog(ProblemReportActivity.this,
                        "Problem Report") {
                    @Override
                    public void DeleteImage() {
                        final ImageViewDialog imageViewDialog = this;
                        super.DeleteImage();
                        AlertDialog.Builder builder = new AlertDialog.Builder(ProblemReportActivity.this);
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
                        //If this is a honeywell device
                        if(CommonUtility.hasHoneywellScanner()) {
                            //We need to resolve the image rotation
                            int rotation = CommonUtility.getImageRotation(getApplicationContext(), CommonUtility.cachedImageFileFullPath(getApplicationContext(), image.filename));
                            if(rotation > 90) {
                                bigBitmap = CommonUtility.rotateImage(bigBitmap, rotation);
                            } else {
                                bigBitmap = CommonUtility.rotateImage(bigBitmap, 90);
                            }
                        }
                    }
                    imageViewDialog.SetBitmap(bigBitmap);
                }
                else {

                }
                imageViewDialog.deleteButtonEnabled(canDelete);
                imageViewDialog.show();
            }
        });
    }

    public void back(View v) {
        final Activity ctx = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(ProblemReportActivity.this);
        builder.setMessage("Do you wish leave this screen and discard the problem report?");
        builder.setPositiveButton("Discard", (dialog, which) -> ctx.finish());
        builder.setNegativeButton("Cancel", null);
        builder.setCancelable(true);
        builder.create().show();
    }
}