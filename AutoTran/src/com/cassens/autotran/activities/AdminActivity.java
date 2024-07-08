package com.cassens.autotran.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.DirSaveCallback;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.sdgsystems.android.amazon.s3transfer.models.S3Container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;

public class AdminActivity extends Activity implements DirSaveCallback {
    private static final Logger log = LoggerFactory.getLogger(AutoTranActivity.class.getSimpleName());
    Button createZip;
    Button uploadZip;
    TextView textView;
    ArrayList<S3Container> logs = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_admin);
        textView = (TextView)findViewById(R.id.textView);
        createZip = (Button)findViewById(R.id.createButton);
        uploadZip = (Button)findViewById(R.id.uploadButton);

        createZip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommonUtility.logButtonClick(log, view);
                new AlertDialog.Builder(AdminActivity.this)
                        .setTitle("Include DB")
                        .setMessage("Would you like to include the database?" + " (" + CommonUtility.getDbFileSize(AdminActivity.this) + ")")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                CommonUtility.logChoiceClick(log, "yes", "Include DB dialog");
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        CommonUtility utility = new CommonUtility();
                                        logs = CommonUtility.getLogS3Containers("admin", AdminActivity.this, "large", true, AdminActivity.this);
                                    }
                                });
                                thread.start();

                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                CommonUtility.logChoiceClick(log, "No", "Include DB dialog");
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        CommonUtility utility = new CommonUtility();
                                        logs = CommonUtility.getLogS3Containers("admin", AdminActivity.this, "small", true,AdminActivity.this);
                                    }
                                });
                                thread.start();
                            }
                        })
                        .show();
            }
        });

        uploadZip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommonUtility.logButtonClick(log, view);
                if (logs == null) {
                    new AlertDialog.Builder(AdminActivity.this)
                            .setTitle("No logs")
                            .setMessage("You must generate the zipped log file before uploading.")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    CommonUtility.logButtonClick(log,"Ok", "No logs dialog");

                                }
                            }).show();
                } else {
                    CommonUtility.uploadLogs("admin", AdminActivity.this, logs, true);
                }
            }
        });

    }


    @Override
    public void fileSaved(final String fileFullPath) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                String filename = fileFullPath.substring(fileFullPath.indexOf("Download/") + 9);
                textView.setText("Saved /Download/" + filename);
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + filename);
                MediaScannerConnection.scanFile(
                        AdminActivity.this,
                        new String[]{ file.getAbsolutePath() }, // "file" was created with "new File(...)"
                        null,
                        null);
            }
        });

    }
}
