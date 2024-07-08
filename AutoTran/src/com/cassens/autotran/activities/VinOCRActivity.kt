package com.cassens.autotran.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.cassens.autotran.CommonUtility
import com.cassens.autotran.R
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException


class VinOCRActivity : AutoTranActivity() {
    // For some reason companion object won't work here, so we use named object
    private object statics {
        val log = LoggerFactory.getLogger(VinOCRActivity::class.java)
    }
    private var mCameraView: SurfaceView? = null
    private var mTextView: TextView? = null
    private var mCameraSource: CameraSource? = null

    private var lastVIN = ""
    private var cyclesSinceLastVin = 0

    override fun getLogger(): Logger? {
        //log = LoggerFactory.getLogger(this.javaClass)
        return statics.log
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vin_ocr)
        mCameraView = findViewById(R.id.surfaceView)
        mTextView = findViewById(R.id.text_view)

        findViewById<Button>(R.id.done_button).setOnClickListener {
            CommonUtility.logButtonClick(statics.log, findViewById<Button>(R.id.done_button), "VIN OCR dialog")
            val data = Intent()
            data.putExtra(DATA_SCANNED_VIN, lastVIN)
            setResult(Activity.RESULT_OK, data)
            finish()
        }

        findViewById<ImageView>(R.id.img_back).setOnClickListener { onBackPressed() }

        startCameraSource()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val data = Intent()
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != requestPermissionID) {
            Log.d(TAG, "Got unexpected permission result: $requestCode")
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
                mCameraSource!!.start(mCameraView!!.holder)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
        }
    }

    private fun startCameraSource() {
        //Create the TextRecognizer
        val textRecognizer = TextRecognizer.Builder(applicationContext).build()
        if (!textRecognizer.isOperational) {
            Log.w(TAG, "Detector dependencies not loaded yet")
        } else {
            //Initialize camerasource to use high resolution and set Autofocus on.
            mCameraSource = CameraSource.Builder(applicationContext, textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(2560, 1920)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(1.0f)
                    .build()

            /**
             * Add call back to SurfaceView and check if camera permission is granted.
             * If permission is granted we can start our cameraSource and pass it to surfaceView
             */
            mCameraView!!.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    try {
                        if (ActivityCompat.checkSelfPermission(applicationContext,
                                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this@VinOCRActivity, arrayOf(Manifest.permission.CAMERA),
                                    requestPermissionID)
                            return
                        }
                        mCameraSource!!.start(mCameraView!!.holder)

                        val size = mCameraSource!!.previewSize
                        val aspectRatio = size.width.toDouble() / size.height.toDouble()
                        val displayMetrics = DisplayMetrics()
                        windowManager.defaultDisplay.getMetrics(displayMetrics)
                        val width = displayMetrics.widthPixels
                        mCameraView!!.layoutParams = RelativeLayout.LayoutParams(width, (width * aspectRatio).toInt())
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } catch (e: RuntimeException) {
                        e.printStackTrace()
                    }
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    mCameraSource!!.stop()
                }
            })

            //Set the TextRecognizer's Processor.
            textRecognizer.setProcessor(object : Detector.Processor<TextBlock> {
                override fun release() {}

                /**
                 * Detect all the text from camera using TextBlock and the values into a stringBuilder
                 * which will then be set to the textView.
                 */
                override fun receiveDetections(detections: Detector.Detections<TextBlock>) {
                    val items = detections.detectedItems
                    if (items.size() != 0) {
                        val allItems = StringBuilder()
                        for (i in 0 until items.size()) {
                            val item = items.valueAt(i)
                            allItems.append(item.value.replace(" ", ""))
                        }
                        val vin = allItems.toString()
                        cyclesSinceLastVin++
                        if (vin.length > 17) {
                            for (i in 0 until vin.length - 17) {
                                val subvin = vin.substring(i, i + 17)
                                if (CommonUtility.checkVinNoPopupNoLogging(subvin)) {
                                    lastVIN = subvin
                                    cyclesSinceLastVin = 0
                                    break
                                }
                            }
                        } else if (vin.length == 17) {
                            if (CommonUtility.checkVinNoPopupNoLogging(vin)) {
                                lastVIN = vin
                                cyclesSinceLastVin = 0
                            }
                        }
                        val display: String = if (lastVIN.isNotEmpty()) {
                            lastVIN
                        } else {
                            cyclesSinceLastVin++
                            "No VIN detected"
                        }
                        mTextView!!.post { mTextView!!.text = display }
                    }
                }
            })
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val requestPermissionID = 101

        const val DATA_SCANNED_VIN = "scanned_vin"
    }
}