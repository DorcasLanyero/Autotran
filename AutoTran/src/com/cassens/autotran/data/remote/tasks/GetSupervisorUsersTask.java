package com.cassens.autotran.data.remote.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.constants.URLS;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.remote.CallWebServices;
import com.sdgsystems.util.HelperFuncs;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GetSupervisorUsersTask extends AsyncTask {
    private static final Logger log = LoggerFactory.getLogger(GetSupervisorUsersTask.class.getSimpleName());

    private final Context context;
    private boolean taskSucceeded = true;

    public GetSupervisorUsersTask(Context context) {
        this.context = context;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        try {
            log.debug(Logs.DEBUG, "getting supervisor information");
            CommonUtility.dispatchLogThreadStartStop( "Started GetSupervisorUsersTask", true);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

            nameValuePairs.add(new BasicNameValuePair("serial", CommonUtility.getDeviceSerial()));

            String response = CallWebServices.sendJson(URLS.poll_supervisors, nameValuePairs, context);

            if (HelperFuncs.isNullOrEmpty(response)) {
                log.debug(Logs.DEBUG, "No response from server");
            } else {
                JSONObject jsonObject = new JSONObject(response);
                String success = "";
                if(jsonObject.has("status")) {
                    success = jsonObject.getString("status");
                }
                if(success.equalsIgnoreCase("success")) {
                    log.debug(Logs.DEBUG, "response from server");
                    JSONArray data = jsonObject.getJSONArray("data");

                    for (int index = 0; index < data.length(); index++) {
                        JSONObject userObject = data.getJSONObject(index).getJSONObject("User");
                        User newUser = new User();

                        newUser.user_remote_id = userObject.getString("id");

                        newUser.firstName = userObject.getString("first_name");
                        newUser.lastName = userObject.getString("last_name");
                        newUser.email = userObject.getString("email");
                        newUser.driverNumber = userObject.getString("user_id");
                        newUser.deviceToken = userObject.getString("device_token");
                        newUser.deviceID = userObject.getString("device_id");
                        newUser.password = userObject.getString("password");
                        newUser.role = userObject.getString("role");
                        newUser.userType = userObject.getString("user_type");
                        newUser.activationLink = userObject.getString("activation_link");
                        newUser.status = userObject.getString("status");
                        newUser.created = userObject.getString("created");
                        newUser.modified = userObject.getString("modified");
                        newUser.fullName = userObject.getString("full_name");
                        newUser.highClaims = userObject.getInt("highClaims");
                        newUser.requiresAudit = userObject.getInt("requiresAudit");
                        newUser.inspectionAccess = userObject.getInt("inspectionAccess");
                        newUser.supervisorCardCode = userObject.getString("supervisorCardCode");
                        newUser.driverLicenseExpiration =  HelperFuncs.simpleDateStringToDate(userObject.getString("driver_license_expiration"));
                        newUser.medicalCertificateExpiration = HelperFuncs.simpleDateStringToDate(userObject.getString("medical_certificate_expiration"));
                        newUser.autoInspectLastDelivery = userObject.getBoolean("autoInspectLastDelivery");
                        DataManager.insertUserToLocalDB(context, newUser);
                    }
                }
            }
        } catch (Exception ex) {
            taskSucceeded = false;
            log.debug(Logs.DEBUG, "Supervisor update got exception: " + ex);
        }
        return "";
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        CommonUtility.dispatchLogThreadStartStop( "Completed GetSupervisorUsersTask", false);
        log.debug(Logs.DEBUG, "Supervisor update " + (taskSucceeded ? "succeeded" : "failed"));
    }
}
