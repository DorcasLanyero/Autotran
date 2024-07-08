package com.cassens.autotran.dialogs
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Button
import com.cassens.autotran.AutoTranApplication
import com.cassens.autotran.Logs
import com.cassens.autotran.R
import com.cassens.autotran.hardware.PiccoloManager
import com.google.android.gms.flags.impl.SharedPreferencesFactory
import com.sdgsystems.app_config.AppSetting
import org.slf4j.LoggerFactory

class PiccoloAccessNeededDialog(context: Context) : Dialog(context, R.style.Theme_AppCompat_Dialog) {

    companion object {
        private val log = LoggerFactory.getLogger(PiccoloAccessNeededDialog::class.simpleName)

        private const val FIRST_PERMISSION_REMINDER_TIME = "FIRST_PERMISSION_REMINDER_TIME"

        private var showing = false;

        private var currentDialog: PiccoloAccessNeededDialog? = null;

        private fun setFirstPermissionReminderTime(permissionReminderTimeMillis: Long) {
            try {
                SharedPreferencesFactory.getSharedPreferences(AutoTranApplication.getAppContext())
                    .edit().putLong(FIRST_PERMISSION_REMINDER_TIME, permissionReminderTimeMillis)
                    .apply()
            } catch (ex: Exception) {
                log.debug(Logs.DEBUG, "setFirstPermissionReminderTime() got exception")
            }
        }

        private fun getFirstPermissionReminderTime(): Long {
            return try {
                SharedPreferencesFactory.getSharedPreferences(AutoTranApplication.getAppContext())
                    .getLong(FIRST_PERMISSION_REMINDER_TIME, 0L)
            } catch (ex: Exception) {
                log.debug(Logs.DEBUG, "getFirstPermissionReminderTime() got exception")
                0L
            }
        }

        fun resetReminderTime() {
            setFirstPermissionReminderTime(0L)
        }

        fun displayIfNeeded(context: Context): Boolean {
            PiccoloManager.detectUsbPermissionState(context)
            if (PiccoloManager.isUsbPermissionIssueDetected()) {
                val currentTime: Long = System.currentTimeMillis()
                if (getFirstPermissionReminderTime() == 0L) {
                    log.debug(Logs.DEBUG, "Setting getFirstPermissionReminderTime")
                    setFirstPermissionReminderTime(currentTime)
                }
                val lockoutMillis: Long = AppSetting.PICCOLO_PERMISSION_LOCKOUT_MINUTES.long * 60L * 1000L
                log.debug(Logs.DEBUG, "Seconds since last reminder "
                    + ((currentTime - getFirstPermissionReminderTime()) / 1000L))
                if ((currentTime - getFirstPermissionReminderTime()) < lockoutMillis) {
                    log.debug(Logs.PICCOLO_IO, "Displaying PiccoloAccessNeededDialog")
                    PiccoloAccessNeededDialog(context).show()
                    return true
                }
            }
            else {
                log.debug(Logs.DEBUG, "No permission issues detected")
                resetReminderTime();
            }
            return false
        }

        fun dismissIfShowing() {
            currentDialog?.dismiss()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentDialog = this;
        setContentView(R.layout.dialog_piccolo_access_needed)
        window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        val okButton = findViewById<Button>(R.id.ok_button)
        okButton.setOnClickListener {
            dismiss()
        }
        setCanceledOnTouchOutside(false)

        setOnDismissListener {
            currentDialog = null;
        }
    }
}
