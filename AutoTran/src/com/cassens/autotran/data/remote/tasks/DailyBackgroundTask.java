package com.cassens.autotran.data.remote.tasks;

import android.content.Context;
import android.os.AsyncTask;
import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.Logs;
import com.cassens.autotran.backendpoc.PoCTabletStatus;
import com.cassens.autotran.backendpoc.PoCUtils;
import com.cassens.autotran.data.local.DataManager;
import com.sdgsystems.app_config.AppSetting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// AsyncTask to execute the SQLite query in the background
public class DailyBackgroundTask extends AsyncTask<Void, Void, Void> {
    private static final Logger log = LoggerFactory.getLogger(RemoteSyncTask.class.getSimpleName());

    Context context;

    public DailyBackgroundTask(Context context)
    {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        log.debug(Logs.BACKEND_POC, "Running daily background task...");
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (AppSetting.PRUNE_LOADS_DAILY.getBoolean()) {
            int loadsPruned = DataManager.pruneLoads(context);
            if (loadsPruned < 0) {
                log.debug(Logs.BACKEND_POC, "pruneLoads() encountered an error or was disabled");
            }
            else {
                log.debug(Logs.BACKEND_POC, String.format("Pruned %d loads", loadsPruned));
            }
        }
        if (AppSetting.REPORT_TABLET_STATUS_DAILY.getBoolean()) {
            PoCTabletStatus tabletStatus = PoCUtils.getTabletStatus(AutoTranApplication.getAppContext());
            PoCUtils.sendLambdaReportTabletStatusRequest(AutoTranApplication.getAppContext(), tabletStatus);
            log.debug(Logs.BACKEND_POC, "Reported tablet status: \n" + tabletStatus.toString());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void retval) {
        super.onPostExecute(null);
    }
}
