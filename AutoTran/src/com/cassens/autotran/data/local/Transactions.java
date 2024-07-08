package com.cassens.autotran.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.Logs;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.model.Damage;
import com.cassens.autotran.data.model.DamageNote;
import com.cassens.autotran.data.model.DamageNoteTemplate;
import com.cassens.autotran.data.model.Dealer;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.DriverAction;
import com.cassens.autotran.data.model.Image;
import com.cassens.autotran.data.model.Inspection;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.LoadEvent;
import com.cassens.autotran.data.model.LotCodeMessage;
import com.cassens.autotran.data.model.PlantReturn;
import com.cassens.autotran.data.model.PredefinedNote;
import com.cassens.autotran.data.model.ProblemReport;
import com.cassens.autotran.data.model.Questionnaire;
import com.cassens.autotran.data.model.ReceivedVehicle;
import com.cassens.autotran.data.model.TrainingRequirement;
import com.cassens.autotran.data.model.TrendingAlert;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.model.VIN;
import com.cassens.autotran.data.model.WMRequest;
import com.cassens.autotran.data.model.YardExit;
import com.cassens.autotran.data.model.YardInventory;
import com.cassens.autotran.data.model.lookup.AreaCode;
import com.cassens.autotran.data.model.lookup.LotCode;
import com.cassens.autotran.data.model.lookup.ScacCode;
import com.cassens.autotran.data.model.lookup.SeverityCode;
import com.cassens.autotran.data.model.lookup.ShuttleMove;
import com.cassens.autotran.data.model.lookup.SpecialCode;
import com.cassens.autotran.data.model.lookup.Terminal;
import com.cassens.autotran.data.model.lookup.TrainingType;
import com.cassens.autotran.data.model.lookup.TypeCode;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


class Transactions {
    private static final Logger log = LoggerFactory.getLogger(Transactions.class.getSimpleName());
    private static final String TAG = "Transactions";

    private static final boolean DEBUG = true;
    private AutotranDB appDb;
    private SQLiteDatabase sqliteDb;

    public Transactions(Context context) {
        // ignore context argument and use application context instead
        // TODO: Remove context argument from the Transactions constructor on next major release
        appDb = new AutotranDB(AutoTranApplication.getAppContext());
    }

    public void startTransaction() {
        appDb.getReadableDatabase().beginTransaction();
    }

    public void endTransaction() {
        appDb.getReadableDatabase().endTransaction();
    }

    public static String getContentValuesString(ContentValues values) {
        Set<Map.Entry<String, Object>> s = values.valueSet();
        Iterator itr = s.iterator();

        StringBuilder builder = new StringBuilder();

        while(itr.hasNext()) {
            Map.Entry me = (Map.Entry) itr.next();
            String key = me.getKey().toString();

            String value = "";
            if(me.getValue() != null) {
                value = me.getValue().toString();
            }

            builder.append("{" + key + "|" + value + "}");
        }

        return builder.toString();
    }

    public long insertUserToLocalDB(User user) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_remote_id", user.user_remote_id);
        values.put("firstName", user.firstName);
        values.put("lastName", user.lastName);
        values.put("email", user.email);
        values.put("driverNumber", user.driverNumber);
        values.put("deviceToken", user.deviceToken);
        values.put("deviceID", user.deviceID);
        values.put("password", user.password);
        values.put("role", user.role);
        values.put("userType", user.userType);
        values.put("activationLink", user.activationLink);
        values.put("status", user.status);
        values.put("created", user.created);
        values.put("modified", user.modified);
        values.put("fullName", user.fullName);
        values.put("highClaims", user.highClaims);
        values.put("requiresAudit", user.requiresAudit);
        values.put("inspectionAccess", user.inspectionAccess);
        values.put("supervisorPreloadChk", user.supervisorPreloadChk);
        values.put("supervisorCardCode", user.supervisorCardCode);
        values.put("driverLicenseExpiration", HelperFuncs.dateToSimpleDateString(user.driverLicenseExpiration));
        values.put("medicalCertificateExpiration", HelperFuncs.dateToSimpleDateString(user.medicalCertificateExpiration));
        values.put("helpTerm", user.helpTerm);
        values.put("autoInspectLastDelivery", boolToInt(user.autoInspectLastDelivery));


        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_USER,
                        new String[] {"user_id", "user_remote_id"},
                        "user_remote_id = " + user.user_remote_id ,
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){
                    duplicateCursor.moveToFirst();
                    long duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("user_id"));

                    //update record
                    sqliteDb.update(AutotranDB.TABLE_USER, values,
                            "user_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    x = duplicate_id;
                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insert(AutotranDB.TABLE_USER, null, values);
                }
                //sqliteDb.close();
            }
        } catch (Exception ex) {
            log.debug(Logs.TRANSACTIONS, "insertUserToLocalDB() caught exception: " + ex.getClass().getName());
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
        return x;
    }

    public long insertDeliveryToLocalDB(Delivery delivery) {
        return insertDeliveryToLocalDB(delivery, false);
    }

    public long insertDeliveryToLocalDB(Delivery delivery, Boolean fromRemoteServer) {

        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("delivery_remote_id", delivery.delivery_remote_id  );
        values.put("timestamp", delivery.timestamp );
        values.put("token", delivery.token );
        values.put("load_id ", delivery.load_id );

        if(!fromRemoteServer) {
            values.put("dealerContact", delivery.dealerContact );
            values.put("dealerEmail", delivery.dealerEmail );
            values.put("dealerSignatureLat", delivery.dealerSignatureLat );
            values.put("dealerSignatureLon", delivery.dealerSignatureLon );
            values.put("driverSignatureLat", delivery.driverSignatureLat );
            values.put("driverSignatureLon", delivery.driverSignatureLon );
            values.put("driverComment", delivery.driverComment );
            values.put("driverContact", delivery.driverContact );
            values.put("dealerComment", delivery.dealerComment );
            values.put("preloadUploadStatus",delivery.preloadUploadStatus);
        }

        if (fromRemoteServer || !HelperFuncs.isNullOrEmpty(delivery.driverSignature)) {
            values.put("driverSignature", delivery.driverSignature);
        }
        if (fromRemoteServer || !HelperFuncs.isNullOrEmpty(delivery.dealerSignature)) {
            values.put("dealerSignature", delivery.dealerSignature);
        }
        if (fromRemoteServer || !HelperFuncs.isNullOrEmpty(delivery.driverSignatureSignedAt)) {
            values.put("driverSignatureSignedAt", delivery.driverSignatureSignedAt);
        }
        if (fromRemoteServer || !HelperFuncs.isNullOrEmpty(delivery.dealerSignatureSignedAt)) {
            values.put("dealerSignatureSignedAt", delivery.dealerSignatureSignedAt );
        }

        values.put("deliveryUploadStatus",delivery.deliveryUploadStatus);
        values.put("uploaded", delivery.uploaded ? 1:0);

        values.put("sti", delivery.sti );
        values.put("afrhrs", delivery.afrhrs );
        values.put("userType", delivery.userType );
        values.put("delivery", delivery.delivery );
        values.put("callback", delivery.callback );
        values.put("ship_date", delivery.ship_date );
        values.put("estdeliverdate", delivery.estdeliverdate );
        values.put("status", delivery.status );
        values.put("notes", delivery.notes);
        values.put("dockTerm", delivery.dockTerm);
        values.put("safeDelivery", delivery.safeDelivery);

        if(delivery.dealer != null) {
            values.put("dealer_id", delivery.dealer.dealer_id );
        }

        values.put("shuttleLoad", delivery.shuttleLoad ? 1 : 0);

        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                String filter;

                log.debug(Logs.TRANSACTIONS, "checking for delivery duplicates with local id " + delivery.delivery_id);
                filter = "delivery_id = " + delivery.delivery_id;

                if (delivery.delivery_remote_id != null && !delivery.delivery_remote_id.isEmpty()) {
                    log.debug(Logs.TRANSACTIONS, "checking for delivery duplicates with remote_id " + delivery.delivery_remote_id);
                    filter += " or delivery_remote_id = " + delivery.delivery_remote_id;
                }

                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_DELIVERY,
                        new String[]{"delivery_id", "delivery_remote_id", "status"},
                        filter,
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){

                    duplicateCursor.moveToFirst();
                    long duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("delivery_id"));
                    log.debug(Logs.TRANSACTIONS, "duplicate delivery found, updating delivery " + duplicate_id);

                    //update record
                    int numrows = sqliteDb.update(AutotranDB.TABLE_DELIVERY, values,
                            "delivery_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    x = duplicate_id;
                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insertOrThrow(AutotranDB.TABLE_DELIVERY, null, values);
                }

                //sqliteDb.close();
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
        return x;
    }

    public long insertLoadInfoToLocalDB(Load load) {
        return insertLoadInfoToLocalDB(false, load);
    }

    //fromRemoteServer is a flag to indicate that fields originating from the local device shouldn't be overwritten
    public long insertLoadInfoToLocalDB(Boolean fromRemoteServer, Load load) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("load_remote_id", load.load_remote_id);
        values.put("loadNumber", load.loadNumber);

        if(!fromRemoteServer) {
            values.put("driverPreLoadComment", load.driverPreLoadComment);
            values.put("driverPreLoadContact", load.driverPreLoadContact);
            values.put("driverPreLoadSignatureLat", load.driverPreLoadSignatureLat);
            values.put("driverPreLoadSignatureLon", load.driverPreLoadSignatureLon);
            values.put("deliveryUploadStatus",load.deliveryUploadStatus);
        }

        if(fromRemoteServer || !HelperFuncs.isNullOrEmpty(load.driverPreLoadSignature)) {
            values.put("driverPreLoadSignature", load.driverPreLoadSignature);
        }
        if(fromRemoteServer || !HelperFuncs.isNullOrEmpty(load.driverPreLoadSignatureSignedAt)) {
            values.put("driverPreLoadSignatureSignedAt", load.driverPreLoadSignatureSignedAt);
        }
        values.put("uploaded", load.uploaded ? 1 : 0);
        values.put("preloadUploadStatus",load.preloadUploadStatus);

        values.put("status",load.status);
        values.put("shuttleLoad",load.shuttleLoad ? 1 : 0);
        values.put("driver_number", load.driverNumber);
        values.put("truck_number", load.truckNumber);
        values.put("trailerNumber", load.trailerNumber);

        if (load.shuttleLoad && load.shuttleMove != null) {
            values.put("shuttleMoveId", load.shuttleMove.shuttleMoveId);
        }

        values.put("driver_id",load.driver_id);
        values.put("lastUpdated", Constants.dateFormatter().format(new Date()));

        values.put("relayLoad", load.relayLoad ? 1 : 0);
        values.put("originLoad", load.originLoad ? 1 : 0);
        values.put("originLoadNumber", load.originLoadNumber);
        values.put("relayLoadNumber", load.relayLoadNumber);
        values.put("relayLoadDealerName", load.relayLoadDealerName);
        values.put("ldtyp", load.loadType);
        values.put("notes", load.notes);
        values.put("originTerminal", load.originTerminal);
        values.put("helpTerminal", load.helpTerminal);
        values.put("supervisorSignature", load.supervisorSignature);
        values.put("supervisorSignedAt", load.supervisorSignedAt);
        values.put("supervisorSignatureLat", load.supervisorSignatureLat);
        values.put("supervisorSignatureLon", load.supervisorSignatureLon);
        values.put("driverHighClaimsAudit", load.driverHighClaimsAudit);
        values.put("lotCodeMsgSeen", load.lotCodeMsgSeen ? 1 : 0);
        values.put("nextDispatch", load.nextDispatch);
        if (load.firstDrop != null) {
            values.put("firstDrop", load.firstDrop);
        }
        if (load.lastDrop != null) {
            values.put("lastDrop", load.lastDrop);
        }
        values.put("parentLoad", load.parentLoad ? 1 : 0);
        values.put("parent_load_id", load.parent_load_id);

		values.put("preloadSupervisorSignature", load.preloadSupervisorSignature);
		values.put("preloadSupervisorSignedAt", load.preloadSupervisorSignedAt);

		values.put("pickSheetImageRequired", boolToInt(load.pickSheetImageRequired));
        values.put("extraDocImageRequired", load.extraDocImageRequired);

		log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen())
            {
                String filter;

                log.debug(Logs.TRANSACTIONS, "checking for load duplicates with local id " + load.load_id);
                filter = "load_id = " + load.load_id;

                if (load.load_remote_id != null && !load.load_remote_id.isEmpty()) {
                    log.debug(Logs.TRANSACTIONS, "checking for load duplicates with remote_id " + load.load_remote_id);
                    filter += " or load_remote_id = " + load.load_remote_id;
                }



                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_LOAD,
                        new String[]{"load_id", "load_remote_id", "status"},
                        filter,
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){

                    duplicateCursor.moveToFirst();
                    long duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("load_id"));
                    log.debug(Logs.TRANSACTIONS, "duplicate load found, updating load " + duplicate_id);

                    //update record
                    int numRows = sqliteDb.update(AutotranDB.TABLE_LOAD, values,
                            "load_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});
                    if(DEBUG) log.debug(Logs.TRANSACTIONS, "Rows updated: " + String.valueOf(numRows));


                    x = duplicate_id;
                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insertOrThrow(AutotranDB.TABLE_LOAD, null, values);
                }
                //sqliteDb.close();
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
        return x;
    }

    public int getLoadIdForLoadNumber(String loadNumber) {
        int x = -2;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen())
            {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_LOAD,
                        new String[]{"load_id"},
                        "loadNumber = '" + loadNumber + "'",
                        null, null, null, null);

                if (duplicateCursor.getCount() == 0) {
                    x = 0;
                }
                else if (duplicateCursor.getCount() != 1) {
                    x = -1;
                }
                duplicateCursor.moveToFirst();
                int duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("load_id"));
                x = duplicate_id;
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
        return x;
    }

    public int updateLoadStatus(int load_id, String status) {
        sqliteDb = appDb.getWritableDatabase();
        int numRows = 0;
        ContentValues values = new ContentValues();
        values.put("status", status);
        log.debug(Logs.TRANSACTIONS, "Updated load " + load_id + ": set status='" + status + "'");

        try {
            if (sqliteDb.isOpen())
            {
                //update record
                numRows = sqliteDb.update(AutotranDB.TABLE_LOAD, values,
                        "load_id = ?",
                        new String[] {String.valueOf(load_id)});

                if(DEBUG) log.debug(Logs.TRANSACTIONS, "Rows updated: " + String.valueOf(numRows));
            }
        } catch (Exception ex) {
            System.err.print(ex);
        }
        return numRows;
    }

    public int updateLoadDriverId(int load_id, int driver_id) {
        sqliteDb = appDb.getWritableDatabase();
        int numRows = 0;
        ContentValues values = new ContentValues();
        values.put("driver_id", driver_id);
        log.debug(Logs.TRANSACTIONS, "Updated load " + load_id + ": set driver_id=" + driver_id);

        try {
            if (sqliteDb.isOpen())
            {
                //update record
                numRows = sqliteDb.update(AutotranDB.TABLE_LOAD, values,
                        "load_id = ?",
                        new String[] {String.valueOf(load_id)});

                if(DEBUG) log.debug(Logs.TRANSACTIONS, "Rows updated: " + String.valueOf(numRows));
            }
        } catch (Exception ex) {
            System.err.print(ex);
        }
        return numRows;
    }

    public int markLoadDeletable(int load_id) {
        // Hide the load by setting the driver_id to 0, and mark to be deleted during
        // load pruning by setting its status to "delete"
        sqliteDb = appDb.getWritableDatabase();
        int numRows = 0;
        ContentValues values = new ContentValues();
        values.put("driver_id", 0);
        values.put("status", "deletable");
        values.put("parent_load_id", -1);
        log.debug(Logs.TRANSACTIONS, "Updated load " + load_id + ": set driver_id=0 and status='deletable'");

        try {
            if (sqliteDb.isOpen())
            {
                //update record
                numRows = sqliteDb.update(AutotranDB.TABLE_LOAD, values,
                        "load_id = ?",
                        new String[] {String.valueOf(load_id)});

                if(DEBUG) log.debug(Logs.TRANSACTIONS, "Rows updated: " + String.valueOf(numRows));
            }
        } catch (Exception ex) {
            System.err.print(ex);
        }
        return numRows;
    }

    private int boolToInt(boolean val) {
        return val ? 1 : 0;
    }

    public long insertTypeCodeToLocalDB( TypeCode typeCode) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("type_code_remote_id", typeCode.type_code_remote_id);
        values.put("code", typeCode.getCode());
        values.put("active", typeCode.active ? 1 : 0);
        values.put("description", typeCode.getDescription());
        if (typeCode.modified != null) {
            values.put("modified", typeCode.modified.getTime());
        }


        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen())
            {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_TYPE_CODE,
                        new String[] {"type_code_id", "type_code_remote_id"},
                        "type_code_remote_id = " + typeCode.type_code_remote_id ,
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){

                    duplicateCursor.moveToFirst();
                    long duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("type_code_id"));

                    //update record
                    int numRows = sqliteDb.update(AutotranDB.TABLE_TYPE_CODE, values,
                            "type_code_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    if(DEBUG) log.debug(Logs.TRANSACTIONS, "Rows updated: " + String.valueOf(numRows));

                    x = duplicate_id;
                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insert(AutotranDB.TABLE_TYPE_CODE, null, values);
                }

                //sqliteDb.close();
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
        return x;
    }

    public long insertSvrtyCodeToLocalDB( SeverityCode severityCode) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("severity_code_remote_id", severityCode.severity_code_remote_id);
        values.put("code", severityCode.getCode());
        values.put("active", severityCode.active ? 1 : 0);
        values.put("description", severityCode.getDescription());
        if (severityCode.modified != null) {
            values.put("modified", severityCode.modified.getTime());
        }

        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {

                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_SVRTY_CODE,
                        new String[] {"severity_code_id", "severity_code_remote_id"},
                        "severity_code_remote_id = " + severityCode.severity_code_remote_id ,
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){

                    duplicateCursor.moveToFirst();
                    long duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("severity_code_id"));

                    //update area code info
                    sqliteDb.update(AutotranDB.TABLE_SVRTY_CODE, values,
                            "severity_code_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    x = duplicate_id;
                } else {
                    //if not duplicate
                    //insert new area code
                    x = sqliteDb.insert(AutotranDB.TABLE_SVRTY_CODE, null, values);
                }

                //sqliteDb.close();
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
        return x;
    }

    public long insertSpecialCodeToLocalDB( SpecialCode specialCode) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("special_code_remote_id", specialCode.special_code_remote_id);
        values.put("area_code", specialCode.getAreaCode());
        values.put("type_code", specialCode.getTypeCode());
        values.put("severity_code", specialCode.getSeverityCode());
        values.put("active", specialCode.active ? 1 : 0);
        values.put("description", specialCode.getDescription());
        if (specialCode.modified != null) {
            values.put("modified", specialCode.modified.getTime());
        }


        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_SPECIAL_CODE,
                        new String[] {"special_code_id"},
                        "area_code = ? AND type_code = ? AND severity_code = ?" ,
                        new String[] {specialCode.getAreaCode(), specialCode.getTypeCode(), specialCode.getSeverityCode()}, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){

                    duplicateCursor.moveToFirst();
                    long duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("special_code_id"));

                    //update record
                    sqliteDb.update(AutotranDB.TABLE_SPECIAL_CODE, values,
                            "special_code_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    x = duplicate_id;

                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insert(AutotranDB.TABLE_SPECIAL_CODE, null, values);
                }

                //sqliteDb.close();
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
        return x;
    }

    public long insertVinToLocalDB(VIN vin) {
        sqliteDb = appDb.getReadableDatabase();

        long id = -1;

        //duplicate if vin_remote_id exists
        long duplicate_id = -1;
        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {

                ContentValues values = new ContentValues();
                values.put("vin_remote_id",vin.vin_remote_id);
                values.put("body",vin.body);
                values.put("weight",vin.weight);
                values.put("colordes",vin.colordes);
                values.put("ldnbr",vin.ldnbr);
                values.put("type",vin.type);
                values.put("created",vin.created);
                values.put("vin_number",vin.vin_number);
                values.put("driver_comment",vin.driver_comment);
                values.put("load_id",vin.load_id);
                values.put("is_damage",vin.is_damage);
                values.put("fillers",vin.fillers);
                values.put("ats",vin.ats);
                values.put("notes_image",vin.notes_image);
                values.put("modified",vin.modified);

                //Why wasn't this in the db model?
                //values.put("ldpos",ldpos);

                values.put("color",vin.color);
                values.put("status",vin.status);
                values.put("dealer_id",vin.dealer_id);
                values.put("callback",vin.callback);
                values.put("notes",vin.notes);
                values.put("dealer_comment",vin.dealer_comment);
                values.put("customer_name", vin.customer_name);

                log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_VIN,
                        new String[] {"vin_id", "vin_remote_id"},
                        "vin_number = '" + vin.vin_number + "'",
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){

                    duplicateCursor.moveToFirst();
                    duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("vin_id"));

                    //update record
                    sqliteDb.update(AutotranDB.TABLE_VIN, values,
                            "vin_number = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    id = duplicate_id;

                    if(DEBUG) log.debug(Logs.TRANSACTIONS, "updated vin: " + vin.vin_number + " " + id);

                } else {
                    //if not duplicate
                    //insert new record
                    id = sqliteDb.insert(AutotranDB.TABLE_VIN, null, values);
                    if(DEBUG) log.debug(Logs.TRANSACTIONS, "inserted vin: " + vin.vin_number);
                }
                //sqliteDb.close();
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }

        return id;
    }

    public Cursor getVinFromLocalDB(int vin_id) {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = null;

            if(vin_id != -1) {
                filter = "vin_id = " + vin_id;
            }

            cursor = sqliteDb.query(AutotranDB.TABLE_VIN, new String[] {
                    "vin_id",
                    "vin_remote_id",
                    "body",
                    "weight",
                    "colordes",
                    "type",
                    "created",
                    "vin_number",
                    "driver_comment",
                    "load_id",
                    "is_damage",
                    "fillers",
                    "ats",
                    "notes_image",
                    "modified",
                    "color",
                    "dealer_id",
                    "callback",
                    "notes",
                    "dealer_comment",
                    "position",
                    "customer_name",
                    "status"
                    }, filter, null, null,	null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }


    public Cursor getTypeCodeListFromLocalDB() {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_TYPE_CODE, new String[] {
                    "type_code_id",
                    "type_code_remote_id",
                    "active",
                    "code",
                    "description"
            }, "active = 1", null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getSpecialCodeListFromLocalDB() {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_SPECIAL_CODE, new String[] {
                    "specialCode" ,
                    "special_code_id",
                    "area_code",
                    "type_code",
                    "severity_code",
                    "special_code_remote_id",
                    "description"
            }, "active = 1", null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }
    /*
	public Cursor getAreaCodeFromLocalDB() {
		Cursor cursor = null;
		try {
			sqliteDb = appDb.getReadableDatabase();
			cursor = sqliteDb.query(AutotranDB.TABLE_AREA_CODE, new String[] {"area_code_id", "area_code_remote_id", "parent_area_code_id", "code", "description"}, null, null, null,
					null, null);
		} catch (Exception exce) {
			exce.printStackTrace();
		}
		return cursor;
	}
	*/
    public Cursor getSvrtyCodeListFromLocalDB() {
        log.debug(Logs.TRANSACTIONS, "retrieving severity codes...");
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_SVRTY_CODE, new String[] {
                    "severity_code_id",
                    "severity_code_remote_id",
                    "active",
                    "code",
                    "description"
            }, "active = 1", null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getVinForDamageByVinNumber(String vinNumber) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_VIN, new String[] {
                    "vin_id",
                    "vin_remote_id",
                    "body",
                    "weight",
                    "colordes",
                    "type",
                    "created",
                    "vin_number",
                    "driver_comment",
                    "load_id",
                    "is_damage",
                    "fillers",
                    "ats",
                    "notes_image",
                    "modified",
                    "color",
                    "dealer_id",
                    "callback",
                    "notes",
                    "dealer_comment",
                    "position",
                    "customer_name",
                    "status"
                    }, "vin_number=?", new String[]{vinNumber}, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getLoadFromDB() {
        return getLoadFromDB(-1, -1, null);
    }

    public Cursor getLoadFromDB(int load_id, int driver_id, Boolean shuttleLoad) {
        return getLoadFromDB(load_id, driver_id, shuttleLoad, -1);
    }

    public Cursor getLoadFromDB(int load_id, int driver_id, Boolean shuttleLoad, int limit) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = "not status = 'cleared'";

            if(load_id != -1) {
                filter += " and load_id = " + String.valueOf(load_id);
            }

            if(driver_id != -1) {
                filter += " and driver_id = " + String.valueOf(driver_id);
            }

            if (shuttleLoad != null) {
                filter += " and shuttleLoad = " + (shuttleLoad ? 1 : 0) + " and not ldtyp = '" + Constants.LOAD_PICKUP + "'";
            }

            log.debug(Logs.TRANSACTIONS, "fetching loads: " + filter);

            cursor = sqliteDb.query(AutotranDB.TABLE_LOAD, new String[] {
                    "loadNumber",
                    "load_id",
                    "load_remote_id",
                    "driverPreLoadSignature",
                    "driverPreLoadSignatureLat",
                    "driverPreLoadSignatureLon",
                    "driverPreLoadContact",
                    "driverPreLoadComment",
                    "status",
                    "driver_id",
                    "uploaded",
                    "preloadUploadStatus",
                    "deliveryUploadStatus",
                    "driverPreLoadSignatureSignedAt",
                    "helpTerminal",
                    "shuttleLoad",
                    "driver_number",
                    "truck_number",
                    "shuttleMoveId",
                    "lastUpdated",
                    "relayLoad",
                    "originLoad",
                    "originLoadNumber",
                    "relayLoadNumber",
                    "relayLoadDealerName",
                    "ldtyp",
                    "notes",
                    "originTerminal",
                    "supervisorSignature",
                    "supervisorSignedAt",
                    "supervisorSignatureLat",
                    "supervisorSignatureLon",
                    "driverHighClaimsAudit",
                    "nextDispatch",
                    "firstDrop",
                    "lastDrop",
                    "lotCodeMsgSeen",
                    "parentLoad",
                    "parent_load_id",
                    "preloadSupervisorSignature",
                    "preloadSupervisorSignedAt",
                    "trailerNumber",
                    "pickSheetImageRequired",
                    "extraDocImageRequired"
            }, filter, null, null, null, "load_id desc", (limit > -1 ? String.valueOf(limit) : null));
		} catch (Exception exce) {
			exce.printStackTrace();
		}
		return cursor;

    }

    public Cursor getLoadsMarkedDeletableFromDB() {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = "driver_id = 0 and status = 'deletable'";

            log.debug(Logs.TRANSACTIONS, "fetching deletable loads: " + filter);

            cursor = sqliteDb.query(AutotranDB.TABLE_LOAD, new String[] {
                    "loadNumber",
                    "load_id",
                    "load_remote_id",
                    "driverPreLoadSignature",
                    "driverPreLoadSignatureLat",
                    "driverPreLoadSignatureLon",
                    "driverPreLoadContact",
                    "driverPreLoadComment",
                    "status",
                    "driver_id",
                    "uploaded",
                    "preloadUploadStatus",
                    "deliveryUploadStatus",
                    "driverPreLoadSignatureSignedAt",
                    "helpTerminal",
                    "shuttleLoad",
                    "driver_number",
                    "truck_number",
                    "shuttleMoveId",
                    "lastUpdated",
                    "relayLoad",
                    "originLoad",
                    "originLoadNumber",
                    "relayLoadNumber",
                    "relayLoadDealerName",
                    "ldtyp",
                    "notes",
                    "originTerminal",
                    "supervisorSignature",
                    "supervisorSignedAt",
                    "supervisorSignatureLat",
                    "supervisorSignatureLon",
                    "driverHighClaimsAudit",
                    "nextDispatch",
                    "firstDrop",
                    "lastDrop",
                    "lotCodeMsgSeen",
                    "parentLoad",
                    "parent_load_id",
                    "preloadSupervisorSignature",
                    "preloadSupervisorSignedAt",
                    "trailerNumber",
                    "pickSheetImageRequired",
                    "extraDocImageRequired"
            }, filter, null, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;

    }

    public Cursor getLoadIdsFromDb(Context context, int driver_id) {
        String filter = "";
        if(driver_id != -1)
            filter += "driver_id = " + String.valueOf(driver_id);

        return sqliteDb.query(AutotranDB.TABLE_LOAD, new String[] {"load_remote_id"}, filter, null, null, null, null);

    }

    public Cursor getEmptyChildLoads(int driver_id) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

			String query = "SELECT load.load_id, count(delivery_vin_id)"
					+ " FROM " + AutotranDB.TABLE_LOAD + ", " + AutotranDB.TABLE_DELIVERY
					+ " left join " + AutotranDB.TABLE_DELIVERY_VIN + " on delivery.delivery_id = delivery_vin.delivery_id "
					+ " WHERE load.load_id = delivery.load_id AND NOT (load.parent_load_id = -1 or load.parent_load_id is null) AND load.driver_id = " + String.valueOf(driver_id)
					+ " GROUP BY load.load_id"
					+ " HAVING count(delivery_vin_id) = 0";
			cursor = sqliteDb.rawQuery(query, null);

        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }


    public Cursor getLoadFromDBForLoadNumber(String loadNumber, boolean skipCleared) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = "loadNumber = " + "'" + loadNumber + "'";
            if (skipCleared) {
                filter += " and not status = 'cleared'";
            }

            log.debug(Logs.TRANSACTIONS, "fetching loads: " + filter);

            cursor = sqliteDb.query(AutotranDB.TABLE_LOAD, new String[] {
                    "loadNumber",
                    "load_id",
                    "load_remote_id",
                    "driverPreLoadSignature",
                    "driverPreLoadSignatureLat",
                    "driverPreLoadSignatureLon",
                    "driverPreLoadContact",
                    "driverPreLoadComment",
                    "status",
                    "driver_id",
                    "uploaded",
                    "preloadUploadStatus",
                    "deliveryUploadStatus",
                    "driverPreLoadSignatureSignedAt",
                    "helpTerminal",
                    "shuttleLoad",
                    "driver_number",
                    "truck_number",
                    "shuttleMoveId",
                    "lastUpdated",
                    "relayLoad",
                    "originLoad",
                    "originLoadNumber",
                    "relayLoadNumber",
                    "relayLoadDealerName",
                    "ldtyp",
                    "notes",
                    "originTerminal",
                    "supervisorSignature",
                    "supervisorSignedAt",
                    "supervisorSignatureLat",
                    "supervisorSignatureLon",
                    "driverHighClaimsAudit",
                    "nextDispatch",
                    "firstDrop",
                    "lastDrop",
                    "lotCodeMsgSeen",
                    "parentLoad",
                    "parent_load_id",
                    "preloadSupervisorSignature",
                    "preloadSupervisorSignedAt",
                    "trailerNumber",
                    "pickSheetImageRequired",
                    "extraDocImageRequired"

            }, filter, null, null, null, "load_id desc");
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getLoadFromDBForLoadNumber(String loadNumber) {
        return getLoadFromDBForLoadNumber(loadNumber, false);
    }

    public Cursor getLoadFromDBForRemote(String load_remote_id) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = "not status = 'cleared'";

            filter += " and load_remote_id = " + String.valueOf(load_remote_id);

            cursor = sqliteDb.query(AutotranDB.TABLE_LOAD, new String[] {
                    "loadNumber",
                    "load_id",
                    "load_remote_id",
                    "driverPreLoadSignature",
                    "driverPreLoadSignatureLat",
                    "driverPreLoadSignatureLon",
                    "driverPreLoadContact",
                    "driverPreLoadComment",
                    "status",
                    "driver_id",
                    "uploaded",
                    "preloadUploadStatus",
                    "deliveryUploadStatus",
                    "driverPreLoadSignatureSignedAt",
                    "helpTerminal",
                    "shuttleLoad",
                    "driver_number",
                    "truck_number",
                    "shuttleMoveId",
                    "lastUpdated",
                    "relayLoad",
                    "originLoad",
                    "originLoadNumber",
                    "relayLoadNumber",
                    "relayLoadDealerName",
                    "notes",
                    "ldtyp",
                    "originTerminal",
                    "supervisorSignature",
                    "supervisorSignedAt",
                    "supervisorSignatureLat",
                    "supervisorSignatureLon",
                    "driverHighClaimsAudit",
                    "nextDispatch",
                    "firstDrop",
                    "lastDrop",
                    "lotCodeMsgSeen",
                    "parentLoad",
                    "parent_load_id",
                    "preloadSupervisorSignature",
                    "preloadSupervisorSignedAt",
                    "trailerNumber",
                    "pickSheetImageRequired",
                    "extraDocImageRequired"
            }, filter, null, null, null, null);
		} catch (Exception exce) {
			exce.printStackTrace();
		}
		return cursor;
	}

    public Cursor getUserFromDBForDriverNumber(String driverNumber) {
        Cursor cursor = null;
        try {

            String filter = null;

            if(driverNumber != null) {
                filter = "driverNumber = " + driverNumber;
            }

			sqliteDb = appDb.getReadableDatabase();

			cursor = sqliteDb.query(AutotranDB.TABLE_USER, new String[] {
			        "user_id",
                    "user_remote_id",
                    "firstName",
                    "lastName",
                    "email",
                    "driverNumber",
                    "deviceToken",
                    "deviceID",
                    "password",
                    "role",
                    "userType",
                    "activationLink",
                    "status",
                    "created",
                    "modified",
                    "fullName",
                    "highClaims",
                    "requiresAudit",
                    "inspectionAccess",
                    "supervisorPreloadChk",
                    "supervisorCardCode",
                    "helpTerm",
                    "driverLicenseExpiration",
                    "medicalCertificateExpiration",
                    "autoInspectLastDelivery"
            }, filter, null, null,null, null);
		} catch (Exception exce) {
			exce.printStackTrace();
		}
		return cursor;
	}

	public Cursor getUserFromDBForSupervisorCardCode(String supervisorCardCode) {
		Cursor cursor = null;
		try {
			String filter = null;

			if(supervisorCardCode.contains("\n")) {
                supervisorCardCode = supervisorCardCode.replace("\n", "\\n");
            }


			if(supervisorCardCode != null) {
				filter = "supervisorCardCode = '" + supervisorCardCode + "'";
			}

			sqliteDb = appDb.getReadableDatabase();
			cursor = sqliteDb.query(AutotranDB.TABLE_USER, new String[] {
			        "user_id",
                    "user_remote_id",
                    "firstName",
                    "lastName",
                    "email",
                    "driverNumber",
                    "deviceToken",
					"deviceID",
                    "password",
                    "role",
                    "userType",
                    "activationLink",
                    "status",
                    "created",
                    "modified",
                    "fullName",
                    "highClaims",
                    "requiresAudit",
                    "inspectionAccess",
                    "supervisorPreloadChk",
                    "supervisorCardCode",
                    "helpTerm",
                    "driverLicenseExpiration",
                    "medicalCertificateExpiration",
                    "autoInspectLastDelivery"
            }, filter, null, null,null, null);

		} catch (Exception exce) {
			exce.printStackTrace();
		}
		return cursor;
	}

    public Cursor getUserFromDB(String driverId) {
        Cursor cursor = null;
        try {

            String filter = null;

            if (driverId != null) {
                filter = "user_id = " + driverId;
            }

			sqliteDb = appDb.getReadableDatabase();
			cursor = sqliteDb.query(AutotranDB.TABLE_USER, new String[]{
			        "user_id",
                    "user_remote_id",
                    "firstName",
                    "lastName",
                    "email",
                    "driverNumber",
                    "deviceToken",
                    "deviceID",
                    "password",
                    "role",
                    "userType",
                    "activationLink",
                    "status",
                    "created",
                    "modified",
                    "fullName",
                    "highClaims",
                    "requiresAudit",
                    "inspectionAccess",
                    "supervisorCardCode",
                    "supervisorPreloadChk",
                    "helpTerm",
                    "driverLicenseExpiration",
                    "medicalCertificateExpiration",
                    "autoInspectLastDelivery"
            }, filter, null, null,null, null);
		} catch (Exception exce) {
			exce.printStackTrace();
		}
		return cursor;
	}

    public void deleteDeliveryVinTableData() {
        log.debug(Logs.TRANSACTIONS, "");

        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        try {
            if (sqliteDb.isOpen()) {
                x = sqliteDb.delete(AutotranDB.TABLE_DELIVERY_VIN, null, null);
                if (x >= 0) {
                    log.info(Logs.TRANSACTIONS, "Delivery Vin table data deleted");
                }
                //sqliteDb.close();
            }
        } catch (Exception exce) {

        }
    }

    public void deleteLoadTableData() {
        log.debug(Logs.TRANSACTIONS, "");

        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        try {
            if (sqliteDb.isOpen()) {
                x = sqliteDb.delete(AutotranDB.TABLE_LOAD, null, null);
                if (x >= 0) {
                    log.info(Logs.TRANSACTIONS, "Load table data deleted");
                }
                //sqliteDb.close();
            }
        } catch (Exception exce) {

        }
    }

    public void deleteLoad(int load_id) {
        log.debug(Logs.TRANSACTIONS, "");

        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        try {
            if (sqliteDb.isOpen()) {
                x = sqliteDb.delete(AutotranDB.TABLE_LOAD, "load_id = " + load_id, null);
                if (x >= 0) {
                    log.info(Logs.TRANSACTIONS, "Load " + load_id + " deleted");
                }
                //sqliteDb.close();
            }
        } catch (Exception exce) {

        }
    }

    private void setTableRowsInactive(String table) {
        log.debug(Logs.TRANSACTIONS, "");

        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        try {
            if (sqliteDb.isOpen()) {
                ContentValues values = new ContentValues();
                values.put("active", 0);
                x = sqliteDb.update(table, values, null, null);
                if (x >= 0) {
                    log.info(Logs.TRANSACTIONS, table + " table data deleted");
                }
                //sqliteDb.close();
            }
        } catch (Exception exce) {

        }
    }

    public void setAreaCodeTableRowsInactive() {
        setTableRowsInactive(AutotranDB.TABLE_AREA_CODE);
    }

    public void setTypeCodeTableRowsInactive() {
        setTableRowsInactive(AutotranDB.TABLE_TYPE_CODE);
    }

    public void setSvrtyCodeTableRowsInactive() {
        setTableRowsInactive(AutotranDB.TABLE_SVRTY_CODE);
    }

    public void setSpecialCodeTableRowsInactive() {
        setTableRowsInactive(AutotranDB.TABLE_SPECIAL_CODE);
    }

    public void setScacCodeTableRowsInactive() {
        setTableRowsInactive(AutotranDB.TABLE_SCAC_CODE);
    }

    public void setTerminalTableRowsInactive() {
        setTableRowsInactive(AutotranDB.TABLE_TERMINAL);
    }

    public void setLotCodeTableRowsInactive() {
        setTableRowsInactive(AutotranDB.TABLE_LOT_CODE);
    }

    public void setLotCodeMsgsTableRowsInactive() {
        setTableRowsInactive(AutotranDB.TABLE_LOT_CODE_MSGS);
    }

    public void setShuttleMoveTableRowsInactive() {
        setTableRowsInactive(AutotranDB.TABLE_SHUTTLE_MOVE);
    }

    public void setDamageNoteTemplatesTableRowsInactive() {
        setTableRowsInactive(AutotranDB.TABLE_DAMAGE_NOTE_TEMPLATES);
    }

    public void setPredefinedNotesTableRowsInactive() {
        setTableRowsInactive(AutotranDB.TABLE_PREDEFINED_NOTES);
    }


    public void deleteVinTableData() {
        log.debug(Logs.TRANSACTIONS, "");

        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        try {
            if (sqliteDb.isOpen()) {
                x = sqliteDb.delete(AutotranDB.TABLE_VIN, null, null);
                if (x >= 0) {
                    log.info(Logs.TRANSACTIONS, "vin table data deleted");
                }
                //sqliteDb.close();
            }
        } catch (Exception exce) {

        }
    }


    public void deleteDeliveryTableData() {
        log.debug(Logs.TRANSACTIONS, "");

        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        try {
            if (sqliteDb.isOpen())

            {
                x = sqliteDb.delete(AutotranDB.TABLE_DELIVERY, null, null);
                if (x >= 0) {
                    log.info(Logs.TRANSACTIONS, "Delivery table data deleted");
                }
                //sqliteDb.close();
            }
        } catch (Exception exce) {

        }
    }

    public boolean deleteVinDataFromDB(int vin_id) {
        log.debug(Logs.TRANSACTIONS, "");

        boolean b = false;
        //String KEY_NAME = "_id";
        String whereClause = "vin_id"+"=?";
        String[]whereArgs = new String[] {String.valueOf(vin_id)};
        try{
            sqliteDb = appDb.getWritableDatabase();
            if(sqliteDb.isOpen())
            {
                int rowsAffected = sqliteDb.delete(AutotranDB.TABLE_VIN, whereClause, whereArgs);
                System.out.println("ROWS AFFECTED : "+rowsAffected);
                b = true;
            }
        }catch(Exception ex)
        {
            b = false;
            ex.printStackTrace();
        }
        return b;
    }

    public boolean deleteDeliveryVinDataFromDB(int delivery_vin_id) {
        log.debug(Logs.TRANSACTIONS, "");

        boolean b = false;
        //String KEY_NAME = "_id";
        String whereClause = "delivery_vin_id"+"=?";
        String[]whereArgs = new String[] {String.valueOf(delivery_vin_id)};
        try{
            sqliteDb = appDb.getWritableDatabase();
            if(sqliteDb.isOpen()) {
                int rowsAffected = sqliteDb.delete(AutotranDB.TABLE_DELIVERY_VIN, whereClause, whereArgs);
                System.out.println("ROWS AFFECTED : "+rowsAffected);
                b = true;
            }
        }catch(Exception ex) {
            b = false;
            ex.printStackTrace();
        }
        return b;
    }

    public boolean deleteDeliveryDataFromDB(int delivery_id) {
        log.debug(Logs.TRANSACTIONS, "");

        boolean b = false;
        //String KEY_NAME = "_id";
        String whereClause = "delivery_id"+"=?";
        String[] whereArgs = new String[] {String.valueOf(delivery_id)};
        try{
            sqliteDb = appDb.getWritableDatabase();
            if(sqliteDb.isOpen()) {
                int rowsAffected = sqliteDb.delete(AutotranDB.TABLE_DELIVERY, whereClause, whereArgs);
                System.out.println("ROWS AFFECTED : "+rowsAffected);
                b = true;
            }
        }catch(Exception ex) {
            b = false;
            ex.printStackTrace();
        }
        return b;
    }

    public Cursor getDeliveryVinFromLocalDB(String remote_id) {
        String filter = "";
        if(!HelperFuncs.isNullOrEmpty(remote_id)) {
            filter += " and delivery_vin_remote_id = " + remote_id;
        }
        return getDeliveryVinCursor(filter);
    }

    public Cursor getDeliveryVinFromLocalDB(int delivery_vin_id) {
        String filter = "";
        if(delivery_vin_id != -1) {
            filter += " and delivery_vin_id = " + delivery_vin_id;
        }
        return getDeliveryVinCursor(filter);
    }

    public Cursor getChildLoadDeliveryVin(int vin_id) {
        String filter = "";
        if(vin_id != -1) {
            filter += " and vin_id = " + vin_id + " and inspectedPreload = '1'";
        }
        return getDeliveryVinCursor(filter);
    }

    public Cursor getDeliveryVinCursor(String filterSupplement) {
        Cursor cursorLoad = null;
        String filter = "(not status = 'cleared' or status is null) " + filterSupplement;

		try {
            //CommonUtility.dispatchUploadLog("Using Transaction.cursorLoad cursor for getDeliveryVinCursor()");
			sqliteDb = appDb.getReadableDatabase();
			cursorLoad = sqliteDb.query(AutotranDB.TABLE_DELIVERY_VIN, new String[]{
					"delivery_vin_id",
                    "delivery_vin_remote_id",
                    "token",
                    "timestamp",
                    "vin_id",
                    "facing",
                    "ats",
                    "preloadNotes",
                    "deliveryNotes",
                    "position",
                    "delivery_id",
                    "user_type",
                    "status",
                    "key",
                    "byteArray",
					"pro",
					"bckhlnbr",
					"rowbay",
					"backdrv",
					"rejected_by",
					"rldspickup",
					"do_lotlocate",
					"lot",
					"von",
					"rte1",
					"rte2	",
					"shuttleLoadProdStatus",
                    "shuttleLoadRoute",
					"status",
                    "ldseq",
                    "inspectedPreload",
                    "inspectedDelivery",
					"supervisorSignature",
                    "supervisorSignatureSignedAt",
                    "supervisorSignatureLat",
                    "supervisorSignatureLon",
                    "supervisorComment",
                    "supervisorContact",
                    "uploaded",
                    "preloadUploadStatus",
                    "deliveryUploadStatus",
                    "finalMfg",
                    "finalDealer"
			}, filter, null, null, null, null);


        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursorLoad;
    }

    public Cursor getDeliveryVinListFromLocalDB(int delivery_id) {
        Cursor cursorLoad = null;
        String filter = "not status = 'cleared' ";

        if(delivery_id != -1) {
            filter += "and delivery_id = " + delivery_id;
        }

        try {
            //CommonUtility.dispatchUploadLog("Using Transaction.cursorLoad cursor for getDeliveryVinListFromLocalDB()");
            sqliteDb = appDb.getReadableDatabase();
            cursorLoad = sqliteDb.query(AutotranDB.TABLE_DELIVERY_VIN, new String[] {
                    "delivery_vin_id",
                    "delivery_vin_remote_id",
                    "token",
                    "timestamp",
                    "vin_id",
                    "facing",
                    "ats",
                    "preloadNotes",
                    "deliveryNotes",
                    "position",
                    "delivery_id",
                    "user_type",
                    "status",
                    "key",
                    "byteArray",
                    "pro",
                    "bckhlnbr",
                    "rowbay",
                    "backdrv",
                    "rejected_by",
                    "rldspickup",
                    "do_lotlocate",
                    "lot",
                    "preloadUploadStatus",
                    "deliveryUploadStatus",
                    "von",
                    "rte1",
                    "rte2	",
                    "shuttleLoadProdStatus",
                    "shuttleLoadRoute",
                    "status",
                    "ldseq",
                    "inspectedPreload",
                    "inspectedDelivery",
                    "supervisorSignature",
                    "supervisorSignatureSignedAt",
                    "supervisorSignatureLat",
                    "supervisorSignatureLon",
                    "supervisorComment",
                    "supervisorContact",
                    "uploaded",
                    "finalMfg",
                    "finalDealer"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursorLoad;
    }

	public Cursor getDeliveryVinListFromLocalDB()
	{
        Cursor cursorLoad = null;
		String filter = "not status = 'cleared'";

		try {
            //CommonUtility.dispatchUploadLog("Using Transaction.cursorLoad cursor for getDeliveryVinListFromLocalDB()");
			sqliteDb = appDb.getReadableDatabase();
			cursorLoad = sqliteDb.query(AutotranDB.TABLE_DELIVERY_VIN, new String[] {
					"delivery_vin_id",
                    "delivery_vin_remote_id",
                    "token",
                    "timestamp",
                    "vin_id",
                    "facing",
                    "ats",
                    "preloadNotes",
                    "deliveryNotes",
                    "position",
                    "delivery_id",
                    "user_type",
                    "status",
                    "key",
                    "byteArray",
					"pro",
					"bckhlnbr",
					"rowbay",
					"backdrv",
					"rejected_by",
					"rldspickup",
					"do_lotlocate",
					"lot",
                    "preloadUploadStatus",
                    "deliveryUploadStatus",
					"von",
					"rte1",
					"rte2	",
					"status",
                    "ldseq",
                    "inspectedPreload",
                    "inspectedDelivery",
					"supervisorSignature",
                    "supervisorSignatureSignedAt",
                    "supervisorSignatureLat",
                    "supervisorSignatureLon",
                    "supervisorContact",
                    "supervisorComment",
                    "uploaded",
                    "finalMfg",
                    "finalDealer"
			}, filter, null, null, null, null);
		} catch (Exception exce) {
			exce.printStackTrace();
		}
		return cursorLoad;
	}

    public Cursor getDeliveryFromLocalDB(int delivery_id) {
        Cursor cursorDelivery = null;
        String filter = "not status = 'cleared' ";

        if(delivery_id != -1) {
            filter += " and delivery_id = " + delivery_id;
        }

        try {
            //CommonUtility.dispatchUploadLog("Using Transaction.cursorDelivery cursor for getDeliveryFromLocalDB()");
            sqliteDb = appDb.getReadableDatabase();
            cursorDelivery = sqliteDb.query(AutotranDB.TABLE_DELIVERY, new String[] {
                    "delivery_id",
                    "delivery_remote_id",
                    "load_id",
                    "dealer_id",
                    "timestamp",
                    "token",
                    "ship_date",
                    "estdeliverdate	",
                    "dealerSignature",
                    "dealerSignatureSignedAt",
                    "dealerContact",
                    "dealerEmail",
                    "dealerSignatureLat",
                    "dealerSignatureLon",
                    "dealerComment",
                    "driverSignature",
                    "driverSignatureSignedAt",
                    "driverSignatureLat",
                    "driverSignatureLon",
                    "driverComment",
                    "driverContact",
                    "sti",
                    "afrhrs",
                    "delivery",
                    "callback",
                    "status",
                    "preloadUploadStatus",
                    "deliveryUploadStatus",
                    "userType",
                    "shuttleLoad",
                    "uploaded",
                    "notes",
                    "dockTerm",
                    "safeDelivery"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursorDelivery;
    }


    public Cursor getDeliveriesFromLocalDB(int load_id) {
        Cursor cursorDelivery = null;
        String filter = "not status = 'cleared' ";

        if(load_id != -1) {
            filter += " and load_id = " + load_id;
        }

        try {
            //CommonUtility.dispatchUploadLog("Using Transaction.cursorDelivery cursor for getDeliveriesFromLocalDB()");
            sqliteDb = appDb.getReadableDatabase();
            cursorDelivery = sqliteDb.query(AutotranDB.TABLE_DELIVERY, new String[] {
                    "delivery_id",
                    "delivery_remote_id",
                    "load_id",
                    "dealer_id",
                    "timestamp",
                    "token",
                    "ship_date",
                    "estdeliverdate	",
                    "dealerSignature",
                    "dealerSignatureSignedAt",
                    "dealerContact",
                    "dealerEmail",
                    "dealerSignatureLat",
                    "dealerSignatureLon",
                    "dealerComment",
                    "driverSignature",
                    "driverSignatureSignedAt",
                    "driverSignatureLat",
                    "driverSignatureLon",
                    "driverComment",
                    "driverContact",
                    "sti",
                    "afrhrs",
                    "delivery",
                    "callback",
                    "status",
                    "preloadUploadStatus",
                    "deliveryUploadStatus",
                    "userType",
                    "shuttleLoad",
                    "notes",
                    "uploaded",
                    "dockTerm",
                    "safeDelivery"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursorDelivery;
    }

    public void closeDatabase() {
        //sqliteDb.close();
    }


    public long insertDamageToLocalDB(Damage damage) {
        sqliteDb = appDb.getReadableDatabase();

        long id = -1;

        //duplicate if damage_id exists
        long duplicate_id = -1;
        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {

                String duplicateCheck = " and (delivery_vin_id = " + damage.delivery_vin_id + ")";

                String guidIdCheck = String.format("(damage_id = %s or guid = '%s')" + duplicateCheck, damage.damage_id, damage.guid);

                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_DAMAGE,
                        new String[] {"damage_id", "preloadUploadStatus", "deliveryUploadStatus"},
                        guidIdCheck /* + duplicateCheck*/, //don't filter out duplicated values
                        null, null, null, null);

                ContentValues values = new ContentValues();
                values.put("area_code_id",damage.area_code_id);
                values.put("svrty_code_id",damage.svrty_code_id);
                values.put("type_code_id",damage.type_code_id);
                values.put("special_code_id",damage.special_code_id);
                values.put("delivery_vin_id",damage.delivery_vin_id);
                values.put("inspection_guid",damage.inspection_guid);
                values.put("preLoadDamage", damage.preLoadDamage ? 1 : 0);
                values.put("uploaded", damage.uploaded ? 1 : 0);
                values.put("readonly", damage.readonly ? 1 : 0);
                values.put("guid", damage.guid);
                values.put("source", damage.source);

                //, "preloadUploadStatus", "deliveryUploadStatus"
                //, int preloadUploadStatus, int deliveryUploadStatus
                values.put("preloadUploadStatus",damage.preloadUploadStatus);
                values.put("deliveryUploadStatus",damage.deliveryUploadStatus);
                values.put("uploadStatus",damage.uploadStatus);

                log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

                //if duplicate
                if(duplicateCursor.getCount() > 0){
                    log.debug(Logs.TRANSACTIONS, "duplicate damage, updating intstead...");

                    duplicateCursor.moveToFirst();
                    duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("damage_id"));

                    //only replicate the preload and delivery upload status fields if the
                    //record has not already been uploaded for preload or delivery
                    int duplicatePreloadUploadStatus = duplicateCursor.getInt(duplicateCursor.getColumnIndex("preloadUploadStatus"));
                    int duplicateDeliveryUploadStatus = duplicateCursor.getInt(duplicateCursor.getColumnIndex("deliveryUploadStatus"));
                    if(duplicatePreloadUploadStatus == Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD ||
                            duplicateDeliveryUploadStatus == Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY) {
                        values.remove("preloadUploadStatus");
                        values.remove("deliveryUploadStatus");
                    }

                    //update record
                    sqliteDb.update(AutotranDB.TABLE_DAMAGE, values,
                            "damage_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    id = duplicate_id;
                } else {
                    //if not duplicate
                    //insert new record
                    id = sqliteDb.insert(AutotranDB.TABLE_DAMAGE, null, values);
                }
                //sqliteDb.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }

        return id;
    }

    public Cursor getDealerFromLocalDB(int dealer_id) {
        String filter = null;
        Cursor cursor = null;
        if(dealer_id != -1) {
            filter = "dealer_id = " + dealer_id;
        }

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_DEALER, new String[] {
                    "dealer_id",
                    "dealer_remote_id",
                    "mfg",
                    "customer_number",
                    "customer_name",
                    "city",
                    "state",
                    "address",
                    "zip",
                    "contact_name",
                    "email",
                    "phone",
                    "monam",
                    "tueam",
                    "wedam",
                    "thuam",
                    "friam",
                    "satam",
                    "sunam",
                    "monpm",
                    "tuepm",
                    "wedpm",
                    "thupm",
                    "fripm",
                    "satpm",
                    "sunpm",
                    "afthr",
                    "comments",
                    "high_claims",
                    "alwaysUnattended",
                    "photosOnUnattended",
                    "lotLocateRequired",
                    "lot_code_id",
                    "countryCode",
                    "last_updated",
                   "updated_fields"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getDealerFromLocalDB(String customer_number, String mfg) {
        String filter = String.format("customer_number = '%s' and mfg = '%s'", customer_number, mfg);
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_DEALER, new String[] {
                    "dealer_id",
                    "dealer_remote_id",
                    "mfg",
                    "customer_number",
                    "customer_name",
                    "city",
                    "state",
                    "address",
                    "zip",
                    "contact_name",
                    "email",
                    "phone",
                    "monam",
                    "tueam",
                    "wedam",
                    "thuam",
                    "friam",
                    "satam",
                    "sunam",
                    "monpm",
                    "tuepm",
                    "wedpm",
                    "thupm",
                    "fripm",
                    "satpm",
                    "sunpm",
                    "afthr",
                    "comments",
                    "high_claims",
                    "alwaysUnattended",
                    "photosOnUnattended",
                    "lotLocateRequired",
                    "lot_code_id",
                    "countryCode",
                    "last_updated",
                    "updated_fields"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getDamagesForDeliveryVinFromLocalDB(int delivery_vin_id) {
        String filter = null;
        Cursor cursor = null;
        if(delivery_vin_id != -1) {
            filter = "delivery_vin_id = " + delivery_vin_id;
        }

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_DAMAGE, new String[] {
                    "damage_id" ,
                    "guid",
                    "delivery_vin_id" ,
                    "type_code_id" ,
                    "svrty_code_id" ,
                    "area_code_id" ,
                    "special_code_id",
                    "preLoadDamage",
                    "uploaded",
                    "preloadUploadStatus",
                    "deliveryUploadStatus",
                    "readonly",
                    "source"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getDamagesForInspectionFromLocalDB(String inspection_guid) {
        Cursor cursor = null;
        String filter = "inspection_guid = '" + inspection_guid + "'";
        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_DAMAGE, new String[] {
                    "damage_id" ,
                    "guid",
                    "inspection_guid" ,
                    "type_code_id" ,
                    "svrty_code_id" ,
                    "area_code_id" ,
                    "special_code_id",
                    "preLoadDamage",
                    "uploaded",
                    "preloadUploadStatus",
                    "deliveryUploadStatus",
                    "readonly",
                    "source"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getTypeCodeByRemoteIdFromLocalDB(int type_code_id) {
        String filter = null;
        if (type_code_id != -1) {
            filter = "type_code_remote_id = '" + type_code_id + "'";
        }
        return getTypeCodeCursorFromLocalDB(filter);
    }

    public Cursor getTypeCodeFromLocalDB(int type_code_id) {
        String filter = null;
        if(type_code_id != -1) {
            filter = "type_code_id = " + type_code_id;
        }
        return getTypeCodeCursorFromLocalDB(filter);
    }

    private Cursor getTypeCodeCursorFromLocalDB(String filter) {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_TYPE_CODE, new String[] {
                    "type_code_id",
                    "type_code_remote_id",
                    "active",
                    "code",
                    "description"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getQuestionnaireFromLocalDB() {
//        String filter = null;
        Cursor cursor = null;
//        if(id != -1) {
//            filter = "id = " + id;
//        }

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_QUESTIONNAIRE, new String[] {
                    "id",
                    "questions"
            }, /*filter*/null, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getAreaCodeFromLocalDB(int area_code_id) {
        String filter = null;
        Cursor cursor = null;
        if(area_code_id != -1) {
            filter = "area_code_id = " + area_code_id;
        }

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_AREA_CODE, new String[] {
                    "area_code_id",
                    "active",
                    "parent_area_code_id",
                    "area_code_remote_id",
                    "code",
                    "description"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getAreaCodeListFromLocalDB() {
        Cursor cursorAreaCode = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursorAreaCode = sqliteDb.query(AutotranDB.TABLE_AREA_CODE, new String[] {
                    "area_code_id",
                    "parent_area_code_id",
                    "area_code_remote_id",
                    "code",
                    "active",
                    "description"
            }, "active = 1", null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursorAreaCode;
    }

    public Cursor getSeverityCodeByRemoteIdFromLocalDB(int svrty_code_id) {
        String filter = null;
        if (svrty_code_id != -1) {
            filter = "severity_code_remote_id = '" + svrty_code_id + "'";
        }
        return getSeverityCodeCursorFromLocalDB(filter);
    }

    public Cursor getSeverityCodeFromLocalDB(int svrty_code_id) {
        String filter = null;
        if(svrty_code_id != -1) {
            filter = "severity_code_id = " + svrty_code_id;
        }
        return getSeverityCodeCursorFromLocalDB(filter);
    }

    private Cursor getSeverityCodeCursorFromLocalDB(String filter) {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_SVRTY_CODE, new String[] {
                    "severity_code_id",
                    "severity_code_remote_id",
                    "code",
                    "active",
                    "description"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public long insertDealerToLocalDB(Dealer dealer) {
        sqliteDb = appDb.getReadableDatabase();

        long id = -1;

        //duplicate if remote_dealer_id exists
        long duplicate_id = -1;
        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen())
            {

                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_DEALER,
                        new String[] {"dealer_id", "dealer_remote_id"},
                        "dealer_remote_id = " + dealer.dealer_remote_id ,
                        null, null, null, null);

                ContentValues values = new ContentValues();
                values.put("dealer_remote_id", dealer.dealer_remote_id);
                values.put("mfg", dealer.mfg);
                values.put("customer_number", dealer.customer_number);
                values.put("customer_name", dealer.customer_name);
                values.put("city", dealer.city);
                values.put("state", dealer.state);
                values.put("address", dealer.address);
                values.put("zip", dealer.zip);
                values.put("contact_name", dealer.contact_name);
                values.put("email", dealer.email);
                values.put("phone", dealer.phone);
                values.put("monam", dealer.monam);
                values.put("tueam", dealer.tueam);
                values.put("wedam", dealer.wedam);
                values.put("thuam", dealer.thuam);
                values.put("friam", dealer.friam);
                values.put("satam", dealer.satam);
                values.put("sunam", dealer.sunam);
                values.put("monpm", dealer.monpm);
                values.put("tuepm", dealer.tuepm);
                values.put("wedpm", dealer.wedpm);
                values.put("thupm", dealer.thupm);
                values.put("fripm", dealer.fripm);
                values.put("satpm", dealer.satpm);
                values.put("sunpm", dealer.sunpm);
                values.put("afthr", dealer.afthr);
                values.put("comments", dealer.comments);
                values.put("status", dealer.status);
                values.put("high_claims", boolToInt(dealer.high_claims));
                values.put("alwaysUnattended", boolToInt(dealer.alwaysUnattended));
                values.put("photosOnUnattended", boolToInt(dealer.photosOnUnattended));
                values.put("lotLocateRequired", boolToInt(dealer.lotLocateRequired));
                values.put("lot_code_id", dealer.lot_code_id);
                values.put("countryCode", dealer.countryCode);
                values.put("last_updated", dealer.lastUpdated != null ?
                        Constants.dateFormatter().format(dealer.lastUpdated) :	"");

                List <String> updatedFields = dealer.getUpdatedFieldsStringList();
                if (updatedFields.size() > 0) {
                    values.put("updated_fields", TextUtils.join(",", updatedFields));
                }

                log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

                //if duplicate
                if(duplicateCursor.getCount() > 0){

                    duplicateCursor.moveToFirst();
                    duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("dealer_id"));

                    //update area code info
                    sqliteDb.update(AutotranDB.TABLE_DEALER, values,
                            "dealer_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    id = duplicate_id;
                } else {
                    //if not duplicate
                    //insert new area code
                    id = sqliteDb.insert(AutotranDB.TABLE_DEALER, null, values);
                }
                //sqliteDb.close();
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }

        return id;
    }

//    public long insertHighClaimsAuditToLocalDB(Dealer dealer) {
//        sqliteDb = appDb.getReadableDatabase();
//
//        long id = -1;
//
//        //duplicate if remote_dealer_id exists
//        long duplicate_id = -1;
//        Cursor duplicateCursor
//        try {
//            if (sqliteDb.isOpen()) {
//
//                duplicateCursor = sqliteDb.query(
//                        AutotranDB.TABLE_AUDIT_QUESTIONNAIRE,
//                        new String[]{"user_id", "load_id"},
//                        "dealer_remote_id = " + dealer.dealer_remote_id,
//                        null, null, null, null);
//
//                ContentValues values = new ContentValues();
//                values.put("dealer_remote_id", dealer.dealer_remote_id);
//                values.put("mfg", dealer.mfg);
//                values.put("customer_number", dealer.customer_number);
//                values.put("customer_name", dealer.customer_name);
//                values.put("city", dealer.city);
//
//                log.debug(Logs.TRANSACTIONS, getContentValuesString(values));
//
//                //if duplicate
//                if(duplicateCursor.getCount() > 0){
//
//                    duplicateCursor.moveToFirst();
//                    duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("dealer_id"));
//
//                    //update area code info
//                    sqliteDb.update(AutotranDB.TABLE_DEALER, values,
//                            "dealer_id = ?" ,
//                            new String[] {String.valueOf(duplicate_id)});
//
//                    id = duplicate_id;
//                } else {
//                    //if not duplicate
//                    //insert new area code
//                    id = sqliteDb.insert(AutotranDB.TABLE_DEALER, null, values);
//                }
//                //sqliteDb.close();
//            }
//        } catch (Exception ex) {
//            System.err.print(ex);
//        } finally {
//            if (duplicateCursor != null) {
//                duplicateCursor.close();
//            }
//        }
//
//        return id;
//    }

    public long insertAreaCodeToLocalDB(AreaCode areaCode) {
        sqliteDb = appDb.getReadableDatabase();

        long id = -1;

        //duplicate if remote_area_code_id exists
        long duplicate_id = -1;
        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_AREA_CODE,
                        new String[] {"area_code_id", "area_code_remote_id"},
                        "area_code_remote_id = " + areaCode.area_code_remote_id ,
                        null, null, null, null);

                ContentValues values = new ContentValues();
                values.put("parent_area_code_id", areaCode.parent_area_code_id);
                values.put("area_code_remote_id", areaCode.area_code_remote_id);
                values.put("code", areaCode.getCode());
                values.put("active", areaCode.active ? 1 : 0);
                values.put("description", areaCode.getDescription());
                if (areaCode.modified != null) {
                    values.put("modified", areaCode.modified.getTime());
                }

                log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

                //if duplicate
                if(duplicateCursor.getCount() > 0){

                    duplicateCursor.moveToFirst();
                    duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("area_code_id"));

                    //update area code info
                    sqliteDb.update(AutotranDB.TABLE_AREA_CODE, values,
                            "area_code_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    id = duplicate_id;
                } else {
                    //if not duplicate
                    //insert new area code
                    id = sqliteDb.insert(AutotranDB.TABLE_AREA_CODE, null, values);
                }
                //sqliteDb.close();
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }

        return id;
    }

    public long insertDeliveryVinToLocalDB(DeliveryVin deliveryVin) {
        return insertDeliveryVinToLocalDB(false, deliveryVin);
    }

    public long insertDeliveryVinToLocalDB(Boolean fromRemoteServer, DeliveryVin deliveryVin) {

        sqliteDb = appDb.getReadableDatabase();

        long id = -1;

        //duplicate if remote_id exists
        long duplicate_id = -1;
        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                String filter = "";

                log.debug(Logs.TRANSACTIONS, "checking for deliveryVin duplicates with local id " + deliveryVin.delivery_vin_id);
                filter = "delivery_vin_id = " + deliveryVin.delivery_vin_id;

                if (deliveryVin.delivery_vin_remote_id != null && !deliveryVin.delivery_vin_remote_id.isEmpty()) {
                    log.debug(Logs.TRANSACTIONS, "checking for deliveryVin duplicates with remote_id " + deliveryVin.delivery_vin_remote_id);
                    filter += " or delivery_vin_remote_id = " + deliveryVin.delivery_vin_remote_id;
                }

                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_DELIVERY_VIN,
                        new String[]{"delivery_vin_id", "status", String.valueOf(deliveryVin.delivery_vin_remote_id)},
                        filter,
                        null, null, null, null);

                ContentValues values = new ContentValues();

                if(!fromRemoteServer) {
                    values.put("preloadUploadStatus",deliveryVin.preloadUploadStatus);
                    values.put("deliveryUploadStatus",deliveryVin.deliveryUploadStatus);
                    values.put("uploaded", deliveryVin.uploaded ? 1 : 0);
                    values.put("supervisorSignature", deliveryVin.supervisorSignature);
                    values.put("supervisorSignatureSignedAt", deliveryVin.supervisorSignatureSignedAt);
                    values.put("supervisorSignatureLat", deliveryVin.supervisorSignatureLat);
                    values.put("supervisorSignatureLon", deliveryVin.supervisorSignatureLon);
                    values.put("supervisorComment", deliveryVin.supervisorComment);
                    values.put("supervisorContact", deliveryVin.supervisorContact);
                }
                values.put("inspectedPreload", deliveryVin.inspectedPreload ? 1 : 0);
                values.put("inspectedDelivery", deliveryVin.inspectedDelivery ? 1 : 0);

                //only update these values here if this is an insert and not an update
                if(duplicateCursor.getCount() == 0) {
                    values.put("position", deliveryVin.position);
                    values.put("backdrv", deliveryVin.backdrv);
                    values.put("facing", deliveryVin.facing);
                }

                values.put("delivery_vin_remote_id", deliveryVin.delivery_vin_remote_id);
                values.put("token ", deliveryVin.token );
                values.put("timestamp", deliveryVin.timestamp);
                values.put("vin_id", deliveryVin.vin_id);
                values.put("ats", deliveryVin.ats);
                values.put("preloadNotes", deliveryVin.preloadNotes);
                values.put("deliveryNotes", deliveryVin.deliveryNotes);
                values.put("delivery_id", deliveryVin.delivery_id);
                values.put("user_type", deliveryVin.user_type);
                values.put("key", deliveryVin.key);
                values.put("byteArray", deliveryVin.byteArray);
                values.put("shuttleLoadProdStatus", deliveryVin.shuttleLoadProductionStatus);
                values.put("shuttleLoadRoute", deliveryVin.shuttleLoadRoute);

                values.put("pro", deliveryVin.pro);
                values.put("bckhlnbr", deliveryVin.bckhlnbr);
                values.put("rowbay", deliveryVin.rowbay);
                values.put("rejected_by", deliveryVin.rejected_by);
                values.put("rldspickup", deliveryVin.rldspickup);
                values.put("do_lotlocate", deliveryVin.do_lotlocate);
                values.put("lot", deliveryVin.lot);
                values.put("von", deliveryVin.von);
                values.put("rte1", deliveryVin.rte1);
                values.put("rte2", deliveryVin.rte2	);
                values.put("status", deliveryVin.status);
                values.put("ldseq", deliveryVin.ldseq);
                values.put("finalMfg", deliveryVin.finalMfg);
                values.put("finalDealer", deliveryVin.finalDealer);

                log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

                //if duplicate
                if(duplicateCursor.getCount() > 0){
                    duplicateCursor.moveToFirst();
                    duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("delivery_vin_id"));
                    log.debug(Logs.TRANSACTIONS, "duplicate deliveryVin found, updating deliveryVin " + duplicate_id);

                    //update these values if the old delivery vin was NOT a 'cleared' delivery vin
                    if(!duplicateCursor.getString(duplicateCursor.getColumnIndex("status")).equals("cleared")) {
                        values.put("position", deliveryVin.position);
                        values.put("backdrv", deliveryVin.backdrv);
                        values.put("facing", deliveryVin.facing);
                    }

                    int rowsAffected = sqliteDb.update(AutotranDB.TABLE_DELIVERY_VIN, values,
                            "delivery_vin_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    log.debug(Logs.TRANSACTIONS, "rows affected: " + rowsAffected + " " + deliveryVin.vin.vin_number);
                    id = duplicate_id;
                } else {
                    //if not duplicate
                    //insert new delivery vin
                    id = sqliteDb.insert(AutotranDB.TABLE_DELIVERY_VIN, null, values);
                    log.debug(Logs.TRANSACTIONS, "new id: " + id + " vin number:" + deliveryVin.vin.vin_number);
                }
                //sqliteDb.close();
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }

        return id;
    }

    public void deleteImage(int image_id) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();

        log.debug(Logs.TRANSACTIONS, "id:" + image_id);

		try {
			if (sqliteDb.isOpen()) {
                x = sqliteDb.delete(AutotranDB.TABLE_IMAGE, "image_id = " + String.valueOf(image_id), null);
                if (x >= 0) {
                    log.info(Logs.TRANSACTIONS, "Image deleted");
                }
                //sqliteDb.close();
            }
        } catch (Exception exce) {

        }
    }

    public Cursor getImageFromLocalDB(String filename) {
        String filter = null;
        Cursor cursor = null;
        if(filename != null) {
            filter = "filename = '" + filename + "'";
        }

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_IMAGE, new String[] {
                    "image_id",
                    "image_bytes",
                    "delivery_vin_id",
                    "inspection_guid",
                    "load_id",
                    "delivery_id",
                    "problem_report_guid",
                    "uploaded",
                    "preloadUploadStatus",
                    "deliveryUploadStatus",
                    "uploadStatus",
                    "uploadIndex",
                    "s3_upload_status",
                    "imageLat",
                    "imageLon",
                    "uploaded",
                    "preloadImage",
                    "foreignKey",
                    "foreignKeyLabel",
                    "filename",
                    "preauth_url",
                    "retries"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getImageFromLocalDB(int image_id) {
        String filter = null;
        Cursor cursor = null;
        if(image_id != -1) {
            filter = "image_id = " + image_id;
        }

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_IMAGE, new String[] {
                    "image_id",
                    "image_bytes",
                    "delivery_vin_id",
                    "inspection_guid",
                    "load_id",
                    "delivery_id",
                    "problem_report_guid",
                    "uploaded",
                    "preloadUploadStatus",
                    "deliveryUploadStatus",
                    "uploadStatus",
                    "uploadIndex",
                    "s3_upload_status",
                    "imageLat",
                    "imageLon",
                    "uploaded",
                    "preloadImage",
                    "foreignKey",
                    "foreignKeyLabel",
                    "filename",
                    "preauth_url",
                    "retries"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getLoadImageListFromLocalDB(String load_id, boolean includeImageData) {
        String filter = null;

        if (load_id != null) {
            filter = "delivery_vin_id =-1 and delivery_id = -1 and load_id = " + load_id;
            return getImageListFromLocalDB(filter, includeImageData);
        } else {
            //return nothing if load_id is null. If used in the query above, it will return EVERYTHING which is bad.
            return null;
        }
    }

    public Cursor getDeliveryImageListFromLocalDB(int delivery_id, boolean includeImageData) {
        String filter = null;

        if (delivery_id != -1) {
            filter = "delivery_id = " + delivery_id;
            return getImageListFromLocalDB(filter, includeImageData);
        } else {
            return null;
        }
    }
    public Cursor getDeliveryVinImageListFromLocalDB(int delivery_vin_id, boolean includeImageData) {
        String filter = null;

        if(delivery_vin_id != -1) {
            filter = "delivery_vin_id = " + delivery_vin_id;
            return getImageListFromLocalDB(filter, includeImageData);
        } else {
            return null;
        }
    }

    public Cursor getImageListFromLocalDB(String filter, boolean includeImageData) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            ArrayList<String> columnList = new ArrayList<String>();
            columnList.add("image_id");
            columnList.add("load_id");
            columnList.add("delivery_id");
            columnList.add("preloadImage");
            columnList.add("delivery_vin_id");
            columnList.add("problem_report_guid");
            columnList.add("uploaded");
            columnList.add("preloadUploadStatus");
            columnList.add("deliveryUploadStatus");
            columnList.add("uploadStatus");
            columnList.add("s3_upload_status");
            columnList.add("uploadIndex");
            columnList.add("imageLat");
            columnList.add("imageLon");
            columnList.add("foreignKey");
            columnList.add("foreignKeyLabel");
            columnList.add("filename");
            columnList.add("preauth_url");
            columnList.add("retries");

            if(includeImageData)
                columnList.add("image_bytes");

            String[] columns = new String[columnList.size()];
            columns = columnList.toArray(columns);

            cursor = sqliteDb.query(AutotranDB.TABLE_IMAGE, columns, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getInspectionImageListFromLocalDB(String inspection_guid) {
        Cursor cursor = null;
        String filter = "inspection_guid = '" + inspection_guid + "'";

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_IMAGE, new String[] {
                    "image_id",
                    "image_bytes",
                    "inspection_guid",
                    "uploaded",
                    "preloadUploadStatus",
                    "deliveryUploadStatus",
                    "uploadStatus",
                    "uploadIndex",
                    "s3_upload_status",
                    "imageLat",
                    "imageLon",
                    "preloadImage",
                    "foreignKey",
                    "foreignKeyLabel",
                    "preauth_url",
                    "retries",
                    "filename"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public long updateImageStatusColumn(int image_id, String column, int value) {
        log.debug(Logs.TRANSACTIONS, "Inserting image " + image_id);
        sqliteDb = appDb.getReadableDatabase();
        long id = -1;
        long duplicate_id = -1;
        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_IMAGE,
                        new String[] {"image_id", "preloadUploadStatus", "deliveryUploadStatus"},
                        "image_id = " + image_id,
                        null, null, null, null);

                ContentValues values = new ContentValues();
                values.put(column,value);
                log.debug(Logs.TRANSACTIONS, getContentValuesString(values));
                //if duplicate
                if(duplicateCursor.getCount() > 0){
                    duplicateCursor.moveToFirst();
                    duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("image_id"));

                    sqliteDb.update(AutotranDB.TABLE_IMAGE, values,
                            "image_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    id = duplicate_id;
                }
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
        return id;
    }

    public long updatePreauthUrlColumn(int image_id, String value) {
        log.debug(Logs.TRANSACTIONS, "inserting URL for image " + image_id);
        sqliteDb = appDb.getReadableDatabase();
        long id = -1;
        long duplicate_id = -1;
        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_IMAGE,
                        new String[] {"image_id", "preauth_url"},
                        "image_id = " + image_id,
                        null, null, null, null);

                ContentValues values = new ContentValues();
                values.put("preauth_url", value);
                log.debug(Logs.TRANSACTIONS, getContentValuesString(values));
                //if duplicate
                if(duplicateCursor.getCount() > 0){

                    duplicateCursor.moveToFirst();
                    duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("image_id"));

                    sqliteDb.update(AutotranDB.TABLE_IMAGE, values,
                            "image_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    id = duplicate_id;
                }
            }

        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
        return id;
    }

    public long updateImageParentIdColumn(int image_id, String column, int value) {
        log.debug(Logs.TRANSACTIONS, "Inserting image " + image_id);
        sqliteDb = appDb.getReadableDatabase();
        long id = -1;
        long duplicate_id = -1;
        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_IMAGE,
                        new String[] {"image_id"},
                        "image_id = " + image_id,
                        null, null, null, null);

                ContentValues values = new ContentValues();
                values.put(column,value);
                log.debug(Logs.TRANSACTIONS, getContentValuesString(values));
                //if duplicate
                if(duplicateCursor.getCount() > 0) {
                    duplicateCursor.moveToFirst();
                    duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("image_id"));

                    sqliteDb.update(AutotranDB.TABLE_IMAGE, values,
                            "image_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    id = duplicate_id;
                }
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
        return id;
    }

    public long insertImageToLocalDb(Image image) {
        log.debug(Logs.TRANSACTIONS, "Inserting image " + image.filename);

        sqliteDb = appDb.getReadableDatabase();

        long id = -1;

        //duplicate if image_id exists
        long duplicate_id = -1;
        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_IMAGE,
                        new String[] {"image_id", "preloadUploadStatus", "deliveryUploadStatus", "preauth_url"},
                        "image_id = " + image.image_id + " or filename = '" + image.filename + "'",
                        null, null, null, null);

                ContentValues values = new ContentValues();
                values.put("delivery_vin_id", image.delivery_vin_id);
                values.put("load_id", image.load_id);
                values.put("delivery_id", image.delivery_id);
                values.put("inspection_guid", image.inspection_guid);
                values.put("problem_report_guid", image.problem_report_guid);
                values.put("uploaded", image.uploaded ? 1 : 0);
                values.put("preloadImage", image.preloadImage ? 1 : 0);
                values.put("imageLat", image.imageLat);
                values.put("imageLon", image.imageLon);
                values.put("preloadUploadStatus",image.preloadUploadStatus);
                values.put("deliveryUploadStatus",image.deliveryUploadStatus);
                values.put("uploadStatus",image.uploadStatus);
                values.put("s3_upload_status",image.s3_upload_status);

                values.put("foreignKey", image.foreignKey);
                values.put("filename", image.filename);
                //Log.d(TAG, "ben: url: "+ image.preauth_url);
                values.put("preauth_url", image.preauth_url);
                values.put("retries", image.retries);

                if(image.foreignKeyLabel != null && image.foreignKeyLabel.length() > 0)
                    values.put("foreignKeyLabel", image.foreignKeyLabel);

                log.debug(Logs.TRANSACTIONS, getContentValuesString(values));
                //Log.d("NARF2", "ben: " + getContentValuesString(values));

                //if duplicate
                if(duplicateCursor.getCount() > 0){

                    duplicateCursor.moveToFirst();
                    duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("image_id"));

                    //only replicate the preload and delivery upload status fields if the
                    //image has not already been uploaded for preload or delivery
                    int duplicatePreloadUploadStatus = duplicateCursor.getInt(duplicateCursor.getColumnIndex("preloadUploadStatus"));
                    int duplicateDeliveryUploadStatus = duplicateCursor.getInt(duplicateCursor.getColumnIndex("deliveryUploadStatus"));
                    if(duplicatePreloadUploadStatus == Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD ||
                            duplicateDeliveryUploadStatus == Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY) {
                        values.remove("preloadUploadStatus");
                        values.remove("deliveryUploadStatus");
                    }

                    String duplicatePreauthUrl = duplicateCursor.getString(duplicateCursor.getColumnIndex("preauth_url"));
                    if (duplicatePreauthUrl != null) {
                        values.put("preauth_url", duplicatePreauthUrl);
                    }

                    //update record info
                    sqliteDb.update(AutotranDB.TABLE_IMAGE, values,
                            "image_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    id = duplicate_id;
                } else {
                    //if not duplicate
                    //insert new record
                    id = sqliteDb.insert(AutotranDB.TABLE_IMAGE, null, values);
                }
                //sqliteDb.close();
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }

        return id;
    }

    public Cursor getUserFromDBForRemoteId(int driver_remote_id) {
        Cursor cursor = null;
        try {
            String filter = null;

            if(driver_remote_id != -1) {
                filter = "user_remote_id = " + driver_remote_id;
            }

			sqliteDb = appDb.getReadableDatabase();
			cursor = sqliteDb.query(AutotranDB.TABLE_USER, new String[] {
                    "user_id",
                    "user_remote_id",
                    "firstName",
                    "lastName",
                    "email",
                    "driverNumber",
                    "deviceToken",
                    "deviceID",
                    "password",
                    "role",
                    "userType",
                    "activationLink",
                    "status",
                    "created",
                    "modified",
                    "fullName",
                    "highClaims",
                    "requiresAudit",
                    "inspectionAccess",
                    "supervisorCardCode",
                    "supervisorPreloadChk",
                    "helpTerm",
                    "driverLicenseExpiration",
                    "medicalCertificateExpiration",
                    "autoInspectLastDelivery"
            }, filter, null, null, null, null);

		} catch (Exception exce) {
			exce.printStackTrace();
		}
		return cursor;
	}

    public Cursor getSpecialCodeFromLocalDB(int special_code_id) {
        Cursor cursor = null;
        try {
            String filter = "special_code_id = " + special_code_id;

            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_SPECIAL_CODE, new String[] {
                    "specialCode" ,
                    "special_code_id",
                    "area_code",
                    "type_code",
                    "severity_code",
                    "special_code_remote_id",
                    "description"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;

    }

    public Cursor getSpecialCodeForCodeFromLocalDB(String special_code_remote_id) {
        Cursor cursor = null;
        try {
            String filter = "special_code_remote_id = " + special_code_remote_id;

            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_SPECIAL_CODE, new String[] {
                    "specialCode" ,
                    "special_code_id",
                    "area_code",
                    "type_code",
                    "severity_code",
                    "special_code_remote_id",
                    "description"
            }, filter, null, null,null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;

    }

    public Cursor getSpecialCodeFromLocalDB(String area_code, String type_code,
                                            String severity_code) {
        Cursor cursor = null;
        try {
            String filter = "area_code = " + area_code + " and type_code = " + type_code + " and severity_code = " + severity_code;

            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_SPECIAL_CODE, new String[] {
                    "specialCode" ,
                    "special_code_id",
                    "area_code",
                    "type_code",
                    "severity_code",
                    "special_code_remote_id",
                    "description"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getAreaCodeFromLocalDB(String areaString) {
        Cursor cursor = null;

        String filter = "code = " + areaString;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_AREA_CODE, new String[] {
                    "area_code_id",
                    "area_code_remote_id",
                    "parent_area_code_id",
                    "code",
                    "description"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getAreaCodeByRemoteIdFromLocalDB(int areaCodeId) {
        return getAreaCodeByAnIdFromLocalDB("area_code_remote_id = '" + areaCodeId + "'");
    }

    public Cursor getAreaCodeByIdFromLocalDB(int areaCodeId) {
        return getAreaCodeByAnIdFromLocalDB("area_code_id = " + areaCodeId);
    }

    private Cursor getAreaCodeByAnIdFromLocalDB(String filter) {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_AREA_CODE, new String[] {
                    "area_code_id",
                    "area_code_remote_id",
                    "parent_area_code_id",
                    "code",
                    "description"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getTypeCodeFromLocalDB(String typeString) {
        Cursor cursor = null;

        String filter = "code = " + typeString;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_TYPE_CODE, new String[] {
                    "type_code_id",
                    "active",
                    "type_code_remote_id",
                    "code",
                    "description"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getSeverityCodeFromLocalDB(String svrtyString) {
        String filter = null;
        Cursor cursor = null;
        if(svrtyString != null) {
            filter = "code = " + svrtyString;
        }

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_SVRTY_CODE, new String[]{
                    "severity_code_id",
                    "severity_code_remote_id",
                    "active",
                    "code",
                    "description"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public boolean deleteDamage(int damage_id) {
        log.debug(Logs.TRANSACTIONS, "id:" + damage_id);

        boolean b = false;
        //String KEY_NAME = "_id";
        String whereClause = "damage_id"+"=?";
        String[]whereArgs = new String[] {String.valueOf(damage_id)};
        try{
            sqliteDb = appDb.getWritableDatabase();
            if(sqliteDb.isOpen())
            {
                int rowsAffected = sqliteDb.delete(AutotranDB.TABLE_DAMAGE, whereClause, whereArgs);
                System.out.println("ROWS AFFECTED : "+rowsAffected);
                b = true;
            }
        }catch(Exception ex)
        {
            b = false;
            ex.printStackTrace();
        }
        return b;
    }

    public void deleteDamages(int delivery_vin_id) {

        log.debug(Logs.TRANSACTIONS, "id: " + delivery_vin_id);

        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        try {
            if (sqliteDb.isOpen())

            {
                x = sqliteDb.delete(AutotranDB.TABLE_DAMAGE, "delivery_vin_id = " + delivery_vin_id, null);
                if (x >= 0) {
                    log.info(Logs.TRANSACTIONS, "Damages deleted");
                }
                //sqliteDb.close();
            }
        } catch (Exception exce) {

        }
    }

    public void deleteImages(int delivery_vin_id) {

        log.debug(Logs.TRANSACTIONS, "id: " + delivery_vin_id);

        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        try {
            if (sqliteDb.isOpen())

            {
                x = sqliteDb.delete(AutotranDB.TABLE_IMAGE, "delivery_vin_id = " + delivery_vin_id, null);
                if (x >= 0) {
                    log.info(Logs.TRANSACTIONS, "Images deleted");
                }
                //sqliteDb.close();
            }
        } catch (Exception exce) {

        }
    }

    public void insertYardExit(YardExit yardExit) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();

        if(yardExit.yard_exit_id != -1)
            values.put("_id", yardExit.yard_exit_id);

        values.put("vin", yardExit.VIN);
        values.put("inspector", yardExit.inspector);
        values.put("inbound", yardExit.inbound ? 0 : 1);
        values.put("upload_status", yardExit.uploadStatus);
        values.put("scac_code_id", yardExit.scacCode.scac_code_id);
        values.put("terminal_id", yardExit.terminal.terminal_id);
        values.put("upload_status", yardExit.uploadStatus);

        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_YARD_EXIT,
                        new String[] {"_id"},
                        "_id = " + yardExit.yard_exit_id ,
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){
                    duplicateCursor.moveToFirst();
                    long duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("_id"));

                    //update record
                    sqliteDb.update(AutotranDB.TABLE_YARD_EXIT, values,
                            "_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    x = duplicate_id;
                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insert(AutotranDB.TABLE_YARD_EXIT, null, values);
                }
                //sqliteDb.close();
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
    }

    public boolean deleteYardInventory(int id) {
        log.debug(Logs.TRANSACTIONS, "");

        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        try {
            if (sqliteDb.isOpen()) {
                x = sqliteDb.delete(AutotranDB.TABLE_YARD_INVENTORY, "_id = " + id, null);
                if (x >= 0) {
                    log.info(Logs.TRANSACTIONS, "YardInventory " + id + " deleted");
                }
                //sqliteDb.close();
            }
            return true;
        } catch (Exception exce) {
            log.info(Logs.TRANSACTIONS, "YardInventory " + id + " deletion failed");
            return false;
        }
    }

    public void insertYardInventory(YardInventory yardInventory) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        if(yardInventory.yard_inventory_id != -1) {
            values.put("_id", yardInventory.yard_inventory_id);
        }

        if(yardInventory.terminal != null) {
            values.put("terminal_id", yardInventory.terminal.terminal_id);
        }

        values.put("vin", yardInventory.VIN);
        values.put("inspector", yardInventory.inspector);
        values.put("bay", yardInventory.bay);
        values.put("row", yardInventory.row);
        values.put("lot_locate", yardInventory.lotLocate ? 1 : 0);

        if(yardInventory.lotCode != null) {
            values.put("lot_code_id", yardInventory.lotCode.lot_code_id);
        }

        values.put("upload_status", yardInventory.uploadStatus);

        values.put("latitude", yardInventory.latitude);
        values.put("longitude", yardInventory.longitude);
        values.put("ldnbr", yardInventory.ldnbr);
        values.put("delivery_vin_id", yardInventory.delivery_vin_id);


        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_YARD_INVENTORY,
                        new String[] {"_id"},
                        "_id = " + yardInventory.yard_inventory_id ,
                        null, null, null, null);
                //if duplicate
                if(duplicateCursor.getCount() > 0){
                    duplicateCursor.moveToFirst();
                    long duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("_id"));

                    //update record
                    sqliteDb.update(AutotranDB.TABLE_YARD_INVENTORY, values,
                            "_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    x = duplicate_id;
                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insert(AutotranDB.TABLE_YARD_INVENTORY, null, values);
                }
                //sqliteDb.close();
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
    }

    public Cursor getYardInventoriesFromLocalDB(boolean getForUpload) {
        return getYardInventoriesFromLocalDB(getForUpload, false, false);
    }

    public Cursor getYardInventoryFromLocalDBForDeliveryVin(int delivery_vin_id) {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();

            cursor = sqliteDb.query(AutotranDB.TABLE_YARD_INVENTORY, new String[] {"*"},
                    "delivery_vin_id = ?", new String[] { String.valueOf(delivery_vin_id) } ,
                    null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getYardInventoriesFromLocalDB(boolean getForUpload, boolean filterRecords, boolean lotLocate) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = "";
            if(getForUpload) {
                filter = "(upload_status = " + Constants.SYNC_STATUS_NOT_UPLOADED + " OR " + "upload_status = " + Constants.SYNC_STATUS_UPLOAD_FAILED + ")";
                if(filterRecords) {
                    filter += " AND ";
                }
            }

            if(filterRecords) {
                filter += "lot_locate = " + (lotLocate ? "1" : "0");
            }

            cursor = sqliteDb.query(AutotranDB.TABLE_YARD_INVENTORY, new String[] {"*"},filter, null , null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

	public int getDeliveryVinCountForVin(int vin_id) {
		int count = 0;
		try {
			sqliteDb = appDb.getReadableDatabase();

			count = (int) DatabaseUtils.queryNumEntries(sqliteDb, AutotranDB.TABLE_DELIVERY_VIN,
					"vin_id=?", new String[]{String.valueOf(vin_id)});

		} catch (Exception exce) {
			exce.printStackTrace();
		}
		return count;
	}

    public boolean deleteLoadEvent(int id) {
        log.debug(Logs.TRANSACTIONS, "");

        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        try {
            if (sqliteDb.isOpen())

            {
                x = sqliteDb.delete(AutotranDB.TABLE_LOAD_EVENTS, "_id = " + id, null);
                if (x >= 0) {
                    log.info(Logs.TRANSACTIONS, "LoadEvent " + id + " deleted");
                }
                //sqliteDb.close();
            }
            return true;
        } catch (Exception exce) {
            log.info(Logs.TRANSACTIONS, "LoadEvent " + id + " deletion failed");
            return false;
        }
    }

    public void insertLoadEvent(LoadEvent LoadEvent) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        if(LoadEvent.load_event_id != -1) {
            values.put("_id", LoadEvent.load_event_id);
        }

        values.put("upload_status", LoadEvent.uploadStatus);
        values.put("csv", LoadEvent.csv);

        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_LOAD_EVENTS,
                        new String[] {"_id"},
                        "_id = " + LoadEvent.load_event_id ,
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){

                    duplicateCursor.moveToFirst();
                    long duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("_id"));

                    //update record
                    sqliteDb.update(AutotranDB.TABLE_LOAD_EVENTS, values,
                            "_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    x = duplicate_id;
                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insert(AutotranDB.TABLE_LOAD_EVENTS, null, values);
                }
                //sqliteDb.close();
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
    }

	public Cursor getLoadEventFromLocalDb(int load_event_id) {
		Cursor cursor = null;
		try {
			sqliteDb = appDb.getReadableDatabase();
			cursor = sqliteDb.query(AutotranDB.TABLE_LOAD_EVENTS, new String[] {"*"}, "_id = " + load_event_id, null, null,
					null, null);
		} catch (Exception exce) {
			exce.printStackTrace();
		}
		return cursor;
	}

	public Cursor getLoadEventsFromLocalDB(boolean getForUpload) {
		return getLoadEventsFromLocalDB(getForUpload, false);
	}

    public Cursor getLoadEventsFromLocalDB(boolean getForUpload, boolean filterRecords) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = "";
            if(getForUpload) {
                filter = "(upload_status = " + Constants.SYNC_STATUS_NOT_UPLOADED + " OR " + "upload_status = " + Constants.SYNC_STATUS_UPLOAD_FAILED + ")";
                if(filterRecords) {
                    filter += " AND ";
                }
            }

            cursor = sqliteDb.query(AutotranDB.TABLE_LOAD_EVENTS, new String[] {"*"},filter, null , null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getQuestionnaireFromLocalDB(Questionnaire.Type type, boolean getForUpload) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = "";
            if(getForUpload) {
                filter = "(type = '" + type.name() + "')";
            }

            cursor = sqliteDb.query(AutotranDB.TABLE_QUESTIONNAIRE, new String[] {"*"}, filter, null , null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public void insertPlantReturn(PlantReturn plantReturn) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();

        if(plantReturn.plant_return_id != -1)
            values.put("_id", plantReturn.plant_return_id);

        values.put("terminal_id", plantReturn.terminal.terminal_id);
        values.put("vin", plantReturn.VIN);
        values.put("inspector", plantReturn.inspector);
        values.put("delay_code", plantReturn.delayCode);
        values.put("upload_status", plantReturn.uploadStatus);

        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_PLANT_RETURN,
                        new String[] {"_id"},
                        "_id = " + plantReturn.plant_return_id ,
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){

                    duplicateCursor.moveToFirst();
                    long duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("_id"));

                    //update record
                    sqliteDb.update(AutotranDB.TABLE_PLANT_RETURN, values,
                            "_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    x = duplicate_id;
                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insert(AutotranDB.TABLE_PLANT_RETURN, null, values);
                }
                //sqliteDb.close();
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
    }

    public void insertReceivedVehicle(ReceivedVehicle receivedVehicle) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();

        if(receivedVehicle.received_vehicle_id != -1)
            values.put("_id", receivedVehicle.received_vehicle_id);

        values.put("terminal_id", receivedVehicle.terminal.terminal_id);
        values.put("vin", receivedVehicle.VIN);
        values.put("inspector", receivedVehicle.inspector);
        values.put("upload_status", receivedVehicle.uploadStatus);

        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_RECEIVED_VEHICLE,
                        new String[] {"_id"},
                        "_id = " + receivedVehicle.received_vehicle_id ,
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){

                    duplicateCursor.moveToFirst();
                    long duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("_id"));

                    //update record
                    sqliteDb.update(AutotranDB.TABLE_RECEIVED_VEHICLE, values,
                            "_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    x = duplicate_id;
                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insert(AutotranDB.TABLE_RECEIVED_VEHICLE, null, values);
                }
                //sqliteDb.close();
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
    }

    public void insertLotCode(LotCode lotCode) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("lot_code_id", lotCode.lot_code_id);
        values.put("code", lotCode.code);
        values.put("terminal_id", lotCode.terminal_id);
        values.put("description", lotCode.description);
        values.put("shuttleMoveCode", lotCode.shuttleMoveCode);
        if (lotCode.modified != null) {
            values.put("modified", lotCode.modified.getTime());
        }
        values.put("active", lotCode.active);

        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_LOT_CODE,
                        new String[] {"lot_code_id"},
                        "lot_code_id = " + lotCode.lot_code_id ,
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){

                    duplicateCursor.moveToFirst();
                    long duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("lot_code_id"));

                    //update record
                    sqliteDb.update(AutotranDB.TABLE_LOT_CODE, values,
                            "lot_code_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    x = duplicate_id;
                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insert(AutotranDB.TABLE_LOT_CODE, null, values);
                }
                //sqliteDb.close();
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
    }

    public void insertScacCode(ScacCode scacCode) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("scac_code_id", scacCode.scac_code_id);
        values.put("code", scacCode.getCode());
        values.put("terminal_id", scacCode.terminal_id);
        values.put("description", scacCode.getDescription());
        if (scacCode.modified != null) {
            values.put("modified", scacCode.modified.getTime());
        }
        values.put("active", scacCode.active);

        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_SCAC_CODE,
                        new String[] {"scac_code_id"},
                        "scac_code_id = " + scacCode.scac_code_id ,
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){
                    duplicateCursor.moveToFirst();
                    long duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("scac_code_id"));

                    //update record
                    sqliteDb.update(AutotranDB.TABLE_SCAC_CODE, values,
                            "scac_code_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    x = duplicate_id;
                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insert(AutotranDB.TABLE_SCAC_CODE, null, values);
                }
                //sqliteDb.close();
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
    }

    public void insertTerminal(Terminal terminal) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("terminal_id", terminal.terminal_id);
        values.put("description", terminal.description);
        values.put("popupMessage", terminal.popupMessage);
        if (terminal.phoneNumber != null) {
            values.put("phoneNumber", terminal.phoneNumber);
        }
        if (terminal.countryCode != null) {
            values.put("countryCode", terminal.countryCode);
        }
        if (terminal.canToUsPhoneNumber != null) {
            values.put("canToUsPhoneNumber", terminal.canToUsPhoneNumber);
        }
        if (terminal.usToCanPhoneNumber != null) {
            values.put("usToCanPhoneNumber", terminal.usToCanPhoneNumber);
        }
        if (terminal.dispatchPhoneNumber != null) {
            values.put("dispatchPhoneNumber", terminal.dispatchPhoneNumber);
        }
        if (terminal.modified != null) {
            values.put("modified", terminal.modified.getTime());
        }
        values.put("active", terminal.active);

        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_TERMINAL,
                        new String[] {"terminal_id"},
                        "terminal_id = " + terminal.terminal_id ,
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){

                    duplicateCursor.moveToFirst();
                    long duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("terminal_id"));

                    //update record
                    sqliteDb.update(AutotranDB.TABLE_TERMINAL, values,
                            "terminal_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    x = duplicate_id;
                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insert(AutotranDB.TABLE_TERMINAL, null, values);
                }
                //sqliteDb.close();
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
    }

    public Cursor getScacCodeList(int terminal_id) {
        Cursor cursor = null;

        String filter = "(active = 1)";
        if (terminal_id != -1) {
            filter += " and terminal_id = " + terminal_id;
        }

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_SCAC_CODE, new String[] {"*"},filter, null , null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getLotCodeListForTerminal(int terminal_id) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_LOT_CODE, new String[] {"*"},
                    "terminal_id = " + terminal_id + " and active = 1  and shuttlemovecode is null", null , null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getLotCodeListForShuttleMove(String terminal, String shuttleMoveCode) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_LOT_CODE, new String[] {"*"},
                    "terminal_id = ? and shuttleMoveCode = ?  and active = 1", new String[] {terminal, shuttleMoveCode} , null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getTerminalList() {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_TERMINAL, new String[] {"*"}, "active = 1", null, null,
                    null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getShuttleTerminalList() {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            ArrayList<String> parameters = new ArrayList();

            String filter = "active = 1";

            log.debug(Logs.TRANSACTIONS, filter);

            //Grab a distinct set
            cursor = sqliteDb.query(true, AutotranDB.TABLE_SHUTTLE_MOVE, new String[] {"terminal"}, filter, null, null, null, "terminal asc", null);

        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public int insertInspection(Inspection inspection) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("vin", inspection.vin);
        values.put("guid", inspection.guid);
        values.put("inspector", inspection.inspector);
        values.put("notes", inspection.notes);
        values.put("terminal_id", inspection.terminal.terminal_id);
        values.put("lot_code_id", inspection.lotCode.lot_code_id);
        values.put("damageCount", inspection.damageCount);
        values.put("imageCount", inspection.imageCount);
        values.put("type", inspection.type);
        values.put("latitude", inspection.latitude);
        values.put("longitude", inspection.longitude);
        values.put("timestamp", inspection.timestamp.getTime());
        if (inspection.scacCode == null) {
            values.put("scac_code_id", -1);
        }
        else {
            values.put("scac_code_id", inspection.scacCode.scac_code_id);
        }
        values.put("upload_status", inspection.uploadStatus);

        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_INSPECTION,
                        new String[] {"_id"},
                        "_id = " + inspection.inspection_id ,
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0 && inspection.inspection_id != -1){

                    duplicateCursor.moveToFirst();
                    long duplicate_id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("_id"));

                    //update record
                    sqliteDb.update(AutotranDB.TABLE_INSPECTION, values,
                            "_id = ?" ,
                            new String[] {String.valueOf(duplicate_id)});

                    x = duplicate_id;
                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insert(AutotranDB.TABLE_INSPECTION, null, values);
                }
                //sqliteDb.close();
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }

        return (int) x;
    }

    public Cursor getInspectionsFromLocalDB(boolean getForUpload) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = "";
            if(getForUpload) {
                filter = "upload_status = " + Constants.SYNC_STATUS_NOT_UPLOADED + " OR " + "upload_status = " + Constants.SYNC_STATUS_UPLOAD_FAILED;
            }

            cursor = sqliteDb.query(AutotranDB.TABLE_INSPECTION, new String[] {"*"}, filter, null , null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getInspectionFromLocalDB(int inspection_id) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            String filter = "_id = " + inspection_id;
            cursor = sqliteDb.query(AutotranDB.TABLE_INSPECTION, new String[] {"*"}, filter, null, null, null, null);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return cursor;
    }

    public Cursor getInspectionFromLocalDB(String inspection_guid) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            String filter = "guid = '" + inspection_guid + "'";
            cursor = sqliteDb.query(AutotranDB.TABLE_INSPECTION, new String[] {"*"}, filter, null, null, null, null);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return cursor;
    }

    public Cursor getYardExitsFromLocalDB(boolean getForUpload) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = "";
            if(getForUpload) {
                filter = "upload_status = " + Constants.SYNC_STATUS_NOT_UPLOADED + " OR " + "upload_status = " + Constants.SYNC_STATUS_UPLOAD_FAILED;
            }

            cursor = sqliteDb.query(AutotranDB.TABLE_YARD_EXIT, new String[] {"*"},filter, null , null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }


    public Cursor getPlantReturnsFromLocalDB(boolean getForUpload) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = "";
            if(getForUpload) {
                filter = "upload_status = " + Constants.SYNC_STATUS_NOT_UPLOADED + " OR " + "upload_status = " + Constants.SYNC_STATUS_UPLOAD_FAILED;
            }

            cursor = sqliteDb.query(AutotranDB.TABLE_PLANT_RETURN, new String[] {"*"},filter, null , null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getReceivedVehiclesFromLocalDB(boolean getForUpload) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = "";
            if(getForUpload) {
                filter = "upload_status = " + Constants.SYNC_STATUS_NOT_UPLOADED + " OR " + "upload_status = " + Constants.SYNC_STATUS_UPLOAD_FAILED;
            }

            cursor = sqliteDb.query(AutotranDB.TABLE_RECEIVED_VEHICLE, new String[] {"*"},filter, null , null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getTerminal(int terminal_id) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_TERMINAL, new String[] {"*"}, "terminal_id = " + terminal_id, null, null,
                    null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getLotCode(int lot_code_id) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_LOT_CODE, new String[] {"*"}, "lot_code_id = " + lot_code_id, null, null,
                    null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getLotCodeMsg(String terminal, String code) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            String query = "SELECT msg.* "
                    + "FROM " + AutotranDB.TABLE_LOT_CODE + " lot "
                    + " INNER JOIN " + AutotranDB.TABLE_LOT_CODE_MSGS + " msg ON lot.lot_code_id = msg.lot_code_id "
                    + " WHERE msg.active = 1 AND lot.terminal_id = ? AND lot.code = ?";
            cursor = sqliteDb.rawQuery(query, new String[]{terminal, code});
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getLotCode(int terminal_id, String code) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_LOT_CODE, new String[] {"*"},
                    "code = ? and terminal_id = ?", new String[] {code, String.valueOf(terminal_id)},
                    null,
                    null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getScacCode(int terminal_id, String description) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_SCAC_CODE, new String[] {"*"}, "terminal_id = ? AND description = ?", new String[] {String.valueOf(terminal_id), description}, null,
                    null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getScacCode(int scac_code_id) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_SCAC_CODE, new String[] {"*"}, "scac_code_id = " + scac_code_id, null, null,
                    null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getShuttleMovesFromDb(String terminal, String origin, String destination) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            ArrayList<String> parameters = new ArrayList();

            String filter = " (active = 1) ";

            //String filter = "terminal = ?";
            if (terminal !=null) {
                filter += " and terminal = ?";
                parameters.add(terminal);
            }

            if (origin != null) {
                filter += " and origin = ?";
                parameters.add(origin);
            }

            if(destination != null) {
                filter += " and destination = ?";
                parameters.add(destination);
            }

            String[] parameterArray = new String[parameters.size()];
            parameterArray = (String[]) parameters.toArray(new String[parameters.size()]);


            log.debug(Logs.TRANSACTIONS, filter);

            log.debug(Logs.TRANSACTIONS, "parameter size: " + parameterArray.length);

            //Grab a distinct set
            cursor = sqliteDb.query(true, AutotranDB.TABLE_SHUTTLE_MOVE, new String[] {"*"}, filter, parameterArray, null, null, "orgDestString asc", null);

        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getShuttleMovesForLoadFromDb(int shuttle_move_id) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            String filter = "shuttle_move_id = " + shuttle_move_id;

            log.debug(Logs.TRANSACTIONS, filter);

            //Grab a distinct set
            cursor = sqliteDb.query(true, AutotranDB.TABLE_SHUTTLE_MOVE, new String[] {"*"}, filter, null, null, null, "orgDestString asc", null);


        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public long upsertDriverActionToLocalDB(DriverAction driverAction) {

        long x = -1;

        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", driverAction.getId());
        values.put("action", driverAction.getAction());
        values.put("status", driverAction.getStatus());
        values.put("data", driverAction.getData());
        values.put("driver_id", driverAction.getDriver_id());
        values.put("sender_id", driverAction.getSender_id());
        values.put("created", driverAction.getCreated());
        values.put("received", driverAction.getReceived());
        values.put("processed", driverAction.getProcessed());
        values.put("upload_status", driverAction.getUploadStatus());

        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_DRIVER_ACTIONS,
                        new String[]{"id"},
                        "id = " + driverAction.getId(),
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){
                    sqliteDb.update(AutotranDB.TABLE_DRIVER_ACTIONS, values, "id = ?", new String[]{String.valueOf(driverAction.getId())});
                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insert(AutotranDB.TABLE_DRIVER_ACTIONS, null, values);
                }
            } else {
                log.debug(Logs.TRANSACTIONS, "db was closed...");
            }
        } catch (Exception ex) {
            log.debug(Logs.TRANSACTIONS, "Caught an error: " + ex.getMessage());
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }

        return x;
    }



    public Cursor getDriverActionFromDb(int id) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_DRIVER_ACTIONS, new String[] {"*"}, "id = " + id, null, null,
                    null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getDriverActionsFromDb(String driverNumber, boolean includeCompletedActions, String typeFilter, boolean includeUploadedActions) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = "driver_id = " + driverNumber;

            if(!includeCompletedActions) {
                filter += " and status = 'ready'";
            }

            if(!includeUploadedActions) {
                filter += " and upload_status = " + Constants.SYNC_STATUS_NOT_UPLOADED + " or upload_status is null";
            }

            if(typeFilter != null) {
                filter += " and action = '" + typeFilter + "'";
            }

            log.debug(Logs.TRANSACTIONS, filter);

            //Grab a distinct set
            log.debug(Logs.TRANSACTIONS, "DRIVER_ACTION_SYNC - getting driver actions with this filter: " + filter);


			cursor = sqliteDb.query(true, AutotranDB.TABLE_DRIVER_ACTIONS, new String[] {"*"}, filter, null, null, null, "id DESC", null);


        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public void insertShuttleMove(ShuttleMove shuttleMove) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("shuttle_move_id", shuttleMove.shuttleMoveId);
        values.put("orgDestString", shuttleMove.orgDestString);
        values.put("terminal", shuttleMove.terminal);
        values.put("origin", shuttleMove.origin);
        values.put("destination", shuttleMove.destination);
        values.put("active", shuttleMove.active);
        if (shuttleMove.modified != null) {
            values.put("modified", shuttleMove.modified.getTime());
        }

        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_SHUTTLE_MOVE,
                        new String[]{"shuttle_move_id"},
                        "shuttle_move_id = " + shuttleMove.shuttleMoveId,
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){
                    //do nothing since this is a single-row table...
                    sqliteDb.update(AutotranDB.TABLE_SHUTTLE_MOVE, values, "shuttle_move_id = ?", new String[]{String.valueOf(shuttleMove.shuttleMoveId)});
                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insert(AutotranDB.TABLE_SHUTTLE_MOVE, null, values);
                    //log.debug(Logs.TRANSACTIONS, "inserted " + x);
                }
                //sqliteDb.close();
            } else {
                log.debug(Logs.TRANSACTIONS, "db was closed...");
            }
        } catch (Exception ex) {
            log.debug(Logs.TRANSACTIONS, "Caught an error: " + ex.getMessage());
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
    }

    public Cursor getDeliveryFromLocalDBForRemote(String delivery_remote_id) {
        Cursor cursorDelivery = null;
        String filter = "not status = 'cleared' ";
        filter += " and delivery_remote_id = '" + delivery_remote_id + "'";

        try {
            //CommonUtility.dispatchUploadLog("Using Transaction.cursorDelivery cursor for getDeliveryFromLocalDBForRemote()");
            sqliteDb = appDb.getReadableDatabase();
            cursorDelivery = sqliteDb.query(AutotranDB.TABLE_DELIVERY, new String[]{
                    "delivery_id",
                    "delivery_remote_id",
                    "load_id",
                    "dealer_id",
                    "timestamp",
                    "token",
                    "ship_date",
                    "estdeliverdate	",
                    "dealerSignature",
                    "dealerSignatureSignedAt",
                    "dealerContact",
                    "dealerEmail",
                    "dealerSignatureLat",
                    "dealerSignatureLon",
                    "dealerComment",
                    "driverSignature",
                    "driverSignatureSignedAt",
                    "driverSignatureLat",
                    "driverSignatureLon",
                    "driverComment",
                    "driverContact",
                    "sti",
                    "afrhrs",
                    "delivery",
                    "callback",
                    "status", "preloadUploadStatus", "deliveryUploadStatus",
                    "userType",
                    "shuttleLoad",
                    "notes",
                    "dockTerm",
                    "uploaded",
                    "safeDelivery"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursorDelivery;
    }

    public Cursor getAreaCodesLastModified() {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.rawQuery("SELECT MAX(modified) as modified FROM " + AutotranDB.TABLE_AREA_CODE, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getTypeCodesLastModified() {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.rawQuery("SELECT MAX(modified) as modified FROM " + AutotranDB.TABLE_TYPE_CODE, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getSeverityCodesLastModified() {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.rawQuery("SELECT MAX(modified) as modified FROM " + AutotranDB.TABLE_SVRTY_CODE, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getSpecialCodesLastModified() {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.rawQuery("SELECT MAX(modified) as modified FROM " + AutotranDB.TABLE_SPECIAL_CODE, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getTerminalsLastModified() {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.rawQuery("SELECT MAX(modified) as modified FROM " + AutotranDB.TABLE_TERMINAL, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getScacCodesLastModified() {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.rawQuery("SELECT MAX(modified) as modified FROM " + AutotranDB.TABLE_SCAC_CODE, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getLotCodeMsgsLastModified() {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.rawQuery("SELECT MAX(modified) as modified FROM " + AutotranDB.TABLE_LOT_CODE_MSGS, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getPredefinedNotesLastModified() {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.rawQuery("SELECT MAX(modified) as modified FROM " + AutotranDB.TABLE_PREDEFINED_NOTES, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getDamageNoteTemplatesLastModified() {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.rawQuery("SELECT MAX(modified) as modified FROM " + AutotranDB.TABLE_DAMAGE_NOTE_TEMPLATES, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getLotCodesLastModified() {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.rawQuery("SELECT MAX(modified) as modified FROM " + AutotranDB.TABLE_LOT_CODE, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getShuttleLotCodesLastModified() {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.rawQuery("SELECT MAX(modified) as modified FROM " + AutotranDB.TABLE_LOT_CODE + " where shuttleMoveCode is not null", null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getDriverActionsLastModified() {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.rawQuery("SELECT MAX(modified) as modified FROM " + AutotranDB.TABLE_DRIVER_ACTIONS, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getShuttleMovesLastModified() {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.rawQuery("SELECT MAX(modified) as modified FROM " + AutotranDB.TABLE_SHUTTLE_MOVE, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public int getDeliveryUploadQueueCountFromLocalDB() {
        int count = 0;
        Cursor cursor = null;
        try {
            cursor = sqliteDb.rawQuery("SELECT DISTINCT d.delivery_id " +
                            "FROM " + AutotranDB.TABLE_DELIVERY + " d, " +
                            AutotranDB.TABLE_DELIVERY_VIN + " dv, " + AutotranDB.TABLE_IMAGE + " i " +
                            "WHERE d.delivery_id = dv.delivery_id " +
                            "AND (d.deliveryUploadStatus = ? OR " +
                            "((dv.delivery_vin_id = i.delivery_vin_id OR d.delivery_id = i.delivery_id) " +
                            "AND i.deliveryUploadStatus = ?))",
                    new String[]{Integer.toString(Constants.SYNC_STATUS_UPLOADING_FOR_DELIVERY), Integer.toString(Constants.SYNC_STATUS_UPLOADING_FOR_DELIVERY)});
        } catch (Exception exce) {
            exce.printStackTrace();
        } finally {
            if (cursor != null) {
                count = cursor.getCount();
                cursor.close();
            }
        }
        return count;
    }

    public int getLoadUploadQueueCountFromLocalDB(boolean shuttleLoad) {
        int count = 0;
        Cursor cursor = null;
        try {
            cursor = sqliteDb.rawQuery("SELECT DISTINCT l.load_id " +
                            "FROM " + AutotranDB.TABLE_LOAD + " l, " + AutotranDB.TABLE_DELIVERY + " d, " +
                            AutotranDB.TABLE_DELIVERY_VIN + " dv, " + AutotranDB.TABLE_IMAGE + " i " +
                            "WHERE l.load_id = d.load_id " +
                            "AND d.delivery_id = dv.delivery_id " +
                            "AND l.shuttleLoad = ? " +
                            "AND (l.preloadUploadStatus = ? OR ((dv.delivery_vin_id = i.delivery_vin_id OR l.load_id = i.load_id) AND i.preloadUploadStatus = ?))",
                    new String[]{shuttleLoad ? "1" : "0", Integer.toString(Constants.SYNC_STATUS_UPLOADING_FOR_PRELOAD), Integer.toString(Constants.SYNC_STATUS_UPLOADING_FOR_PRELOAD)});
        } catch (Exception exce) {
            exce.printStackTrace();
        } finally {
            if (cursor != null) {
                count = cursor.getCount();
                cursor.close();
            }
        }
        return count;
    }

    public int updateShuttleLoadNumber(String oldNumber, String newNumber) {

        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("loadnumber", newNumber);
        log.debug(Logs.TRANSACTIONS, "Updating load number to " + getContentValuesString(values));

        try {
            if (sqliteDb.isOpen()) {
                //update record
                int numRows = sqliteDb.update(AutotranDB.TABLE_LOAD, values,
                        "loadnumber = ?" ,
                        new String[] {String.valueOf(oldNumber)});

                if(DEBUG) log.debug(Logs.TRANSACTIONS, "Rows updated: " + String.valueOf(numRows));
                return numRows;
            }
        } catch (Exception ex) {
            System.err.print(ex);
        }

        return 0;
    }

    public long insertDamageNoteTemplate(DamageNoteTemplate damageNoteTemplate) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", damageNoteTemplate.id);
        values.put("comment", damageNoteTemplate.comment);
        values.put("driver_prompt", damageNoteTemplate.driver_prompt);
        values.put("driver_prompt_type", damageNoteTemplate.driver_prompt_type);
        values.put("dealer_prompt", damageNoteTemplate.dealer_prompt);
        values.put("dealer_prompt_type", damageNoteTemplate.dealer_prompt_type);
        values.put("area_code", damageNoteTemplate.area_code);
        values.put("type_code", damageNoteTemplate.type_code);
        values.put("severity_code", damageNoteTemplate.severity_code);
        values.put("originTerminal", damageNoteTemplate.originTerminal);
        values.put("mfg", damageNoteTemplate.mfg);
        values.put("active", damageNoteTemplate.active);
        if (damageNoteTemplate.modified != null) {
            values.put("modified", damageNoteTemplate.modified.getTime());
        }

        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_DAMAGE_NOTE_TEMPLATES,
                        new String[]{"id"},
                        "id = " + damageNoteTemplate.id,
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){
                    //do nothing since this is a single-row table...

                    log.debug("UPDATING");

                    sqliteDb.update(AutotranDB.TABLE_DAMAGE_NOTE_TEMPLATES, values, "id = ?", new String[]{String.valueOf(damageNoteTemplate.id)});
                    x = damageNoteTemplate.id;
                } else {
                    //if not duplicate
                    //insert new record
                    log.debug("INSERTING");

                    x = sqliteDb.insert(AutotranDB.TABLE_DAMAGE_NOTE_TEMPLATES, null, values);
                    //log.debug(Logs.TRANSACTIONS, "inserted " + x);
                }
                //sqliteDb.close();
            } else {
                log.debug(Logs.TRANSACTIONS, "db was closed...");
            }
        } catch (Exception ex) {
            log.debug(Logs.TRANSACTIONS, "Caught an error: " + ex.getMessage());
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }

        return x;
    }

    public long insertDamageNote(DamageNote damageNote) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("damage_guid", damageNote.damage_guid);
        values.put("preload_driver_comment", damageNote.preload_driver_comment);
        values.put("delivery_driver_comment", damageNote.delivery_driver_comment);
        values.put("delivery_dealer_comment", damageNote.delivery_dealer_comment);
        values.put("damage_note_template_id", damageNote.damage_note_template_id);

        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_DAMAGE_NOTES,
                        new String[]{"id"},
                        "damage_note_template_id = " + damageNote.damage_note_template_id + " and damage_guid = '" + damageNote.damage_guid + "'",
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){
                    //do nothing since this is a single-row table...

                    duplicateCursor.moveToNext();
                    damageNote.id = duplicateCursor.getInt(duplicateCursor.getColumnIndex("id"));

                    sqliteDb.update(AutotranDB.TABLE_DAMAGE_NOTES, values, "id = ?", new String[]{String.valueOf(damageNote.id)});
                    x = damageNote.id;
                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insert(AutotranDB.TABLE_DAMAGE_NOTES, null, values);
                    //log.debug(Logs.TRANSACTIONS, "inserted " + x);
                }
                //sqliteDb.close();
            } else {
                log.debug(Logs.TRANSACTIONS, "db was closed...");
            }
        } catch (Exception ex) {
            log.debug(Logs.TRANSACTIONS, "Caught an error: " + ex.getMessage());
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }

        return x;
    }

    public long insertLotCodeMsg(LotCodeMessage msg) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", msg.id);
        values.put("lot_code_id", msg.lot_code_id);
        values.put("message", msg.message);
        values.put("prompt", msg.prompt);
        values.put("response", msg.response);
        values.put("active", msg.active ? 1 : 0);
        if (msg.modified != null) {
            values.put("modified", msg.modified.getTime());
        }

        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_LOT_CODE_MSGS,
                        new String[]{"id"},
                        "id = " + msg.id,
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){
                    //do nothing since this is a single-row table...
                    sqliteDb.update(AutotranDB.TABLE_LOT_CODE_MSGS, values, "id = ?", new String[]{String.valueOf(msg.id)});
                    x = msg.id;
                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insert(AutotranDB.TABLE_LOT_CODE_MSGS, null, values);
                    //log.debug(Logs.TRANSACTIONS, "inserted " + x);
                }
                //sqliteDb.close();
            } else {
                log.debug(Logs.TRANSACTIONS, "db was closed...");
            }
        } catch (Exception ex) {
            log.debug(Logs.TRANSACTIONS, "Caught an error: " + ex.getMessage());
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }

        return x;
    }

    public Cursor getLotCodeMsgFromDb(int lot_code_id) {
        Cursor cursor = null;
        String filter = "lot_code_id = " + lot_code_id + " AND active = 1";

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_LOT_CODE_MSGS, new String[]{
                    "id",
                    "lot_code_id",
                    "message",
                    "prompt",
                    "response",
                    "active",
                    "modified"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public void markDriverActionUploaded(int id, boolean success) {
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("upload_status", success ? Constants.SYNC_STATUS_UPLOADED : Constants.SYNC_STATUS_NOT_UPLOADED);
        log.debug(Logs.TRANSACTIONS, "Updating driver action to " + getContentValuesString(values));

        try {
            if (sqliteDb.isOpen())
            {
                //update record
                int numRows = sqliteDb.update(AutotranDB.TABLE_DRIVER_ACTIONS, values,
                        "id = ?" ,
                        new String[] {String.valueOf(id)});

                if(DEBUG) log.debug(Logs.TRANSACTIONS, "Rows updated: " + String.valueOf(numRows));
            }
        } catch (Exception ex) {
            System.err.print(ex);
        }
    }

    public void saveDeliveryDealerComment(String id, String comment) {
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("dealerComment", comment);
        log.debug(Logs.TRANSACTIONS, "Updating dealerComment to " + getContentValuesString(values));

        try {
            if (sqliteDb.isOpen())
            {
                //update record
                int numRows = sqliteDb.update(AutotranDB.TABLE_DELIVERY, values,
                        "delivery_id = ?" ,
                        new String[] {String.valueOf(id)});

                if(DEBUG) log.debug(Logs.TRANSACTIONS, "Rows updated: " + String.valueOf(numRows));
            }
        } catch (Exception ex) {
            System.err.print(ex);
        }
    }

    public void saveDeliveryDealerContact(String id, String comment) {
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("dealerContact", comment);
        log.debug(Logs.TRANSACTIONS, "Updating dealerContact to " + getContentValuesString(values));

        try {
            if (sqliteDb.isOpen())
            {
                //update record
                int numRows = sqliteDb.update(AutotranDB.TABLE_DELIVERY, values,
                        "delivery_id = ?" ,
                        new String[] {String.valueOf(id)});

                if(DEBUG) log.debug(Logs.TRANSACTIONS, "Rows updated: " + String.valueOf(numRows));
            }
        } catch (Exception ex) {
            System.err.print(ex);
        }
    }

    public void saveDeliverySti(String id, int sti) {
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sti", sti);
        log.debug(Logs.TRANSACTIONS, "Updating sti: " + getContentValuesString(values));

        try {
            if (sqliteDb.isOpen())
            {
                //update record
                int numRows = sqliteDb.update(AutotranDB.TABLE_DELIVERY, values,
                        "delivery_id = ?" ,
                        new String[] {String.valueOf(id)});

                if(DEBUG) log.debug(Logs.TRANSACTIONS, "Rows updated: " + String.valueOf(numRows));
            }
        } catch (Exception ex) {
            System.err.print(ex);
        }
    }
    public void saveDeliveryStiAndAfrhs(String id, int sti, int afrhrs, String dealerContact) {
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sti", sti);
        values.put("afrhrs", afrhrs);
        values.put("dealerContact", HelperFuncs.noNull(dealerContact));

        log.debug(Logs.TRANSACTIONS, "Updating sti, afrhrs, and dealerContact: " + getContentValuesString(values));

        try {
            if (sqliteDb.isOpen())
            {
                //update record
                int numRows = sqliteDb.update(AutotranDB.TABLE_DELIVERY, values,
                        "delivery_id = ?" ,
                        new String[] {String.valueOf(id)});

                if(DEBUG) log.debug(Logs.TRANSACTIONS, "Rows updated: " + String.valueOf(numRows));
            }
        } catch (Exception ex) {
            System.err.print(ex);
        }
    }

    public void savePreloadDriverComment(String id, String comment) {
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("driverPreLoadComment", comment);
        log.debug(Logs.TRANSACTIONS, "driverPreLoadComment to " + getContentValuesString(values));

        try {
            if (sqliteDb.isOpen())
            {
                //update record
                int numRows = sqliteDb.update(AutotranDB.TABLE_LOAD, values,
                        "load_id = ?" ,
                        new String[] {String.valueOf(id)});

                if(DEBUG) log.debug(Logs.TRANSACTIONS, "Rows updated: " + String.valueOf(numRows));
            }
        } catch (Exception ex) {
            System.err.print(ex);
        }

    }

    public void saveDeliveryDriverComment(String id, String comment) {
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("driverComment", comment);
        log.debug(Logs.TRANSACTIONS, "set dealerComment to " + getContentValuesString(values));

        try {
            if (sqliteDb.isOpen())
            {
                //update record
                int numRows = sqliteDb.update(AutotranDB.TABLE_DELIVERY, values,
                        "delivery_id = ?" ,
                        new String[] {String.valueOf(id)});

                if(DEBUG) log.debug(Logs.TRANSACTIONS, "Rows updated: " + String.valueOf(numRows));
            }
        } catch (Exception ex) {
            System.err.print(ex);
        }
    }

    public void savePreloadSupervisorComment(int id, String comment) {
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("supervisorComment", comment);
        log.debug(Logs.TRANSACTIONS, "set supervisorComment to " + getContentValuesString(values));

        try {
            if (sqliteDb.isOpen())
            {
                //update record
                int numRows = sqliteDb.update(AutotranDB.TABLE_DELIVERY_VIN, values,
                        "delivery_vin_id = ?" ,
                        new String[] {String.valueOf(id)});

                if(DEBUG) log.debug(Logs.TRANSACTIONS, "Rows updated: " + String.valueOf(numRows));
            }
        } catch (Exception ex) {
            System.err.print(ex);
        }
    }

    public Cursor getProblemReportsFromDb() {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = "";

			/*
			 = "driver_id = " + driverNumber;

			if(!includeCompletedActions) {
				filter += " and status = 'ready'";
			}

			if(!includeUploadedActions) {
				filter += " and upload_status = " + Constants.SYNC_STATUS_NOT_UPLOADED + " or upload_status is null";
			}

			if(typeFilter != null) {
				filter += " and action = '" + typeFilter + "'";
			}
			*/

            log.debug(Logs.TRANSACTIONS, filter);


            //Grab a distinct set
            cursor = sqliteDb.query(true, AutotranDB.TABLE_PROBLEM_REPORT, new String[] {"*"}, filter, null, null, null, null, null);


        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public long saveProblemReport(ProblemReport report) {
        long x = -1;

        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("guid", report.getGuid());
        values.put("driver_id", report.getDriver_id());
        values.put("category", report.getCategory());
        values.put("description", report.getDescription());
        values.put("timestamp", report.getTimestamp());
        values.put("latitude", report.getLatitude());
        values.put("longitude", report.getLongitude());
        values.put("imageCount", report.getImageCount());
        values.put("upload_status", report.getUpload_status());

        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_PROBLEM_REPORT,
                        new String[]{"id"},
                        "id = " + report.getId(),
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){
                    sqliteDb.update(AutotranDB.TABLE_PROBLEM_REPORT, values, "id = ?", new String[]{String.valueOf(report.getId())});
                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insert(AutotranDB.TABLE_PROBLEM_REPORT, null, values);
                }
            } else {
                log.debug(Logs.TRANSACTIONS, "db was closed...");
            }
        } catch (Exception ex) {
            log.debug(Logs.TRANSACTIONS, "Caught an error: " + ex.getMessage());
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }

        return x;
    }

    public void markProblemReportUploaded(int id, boolean success) {
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("upload_status", success ? Constants.SYNC_STATUS_UPLOADED : Constants.SYNC_STATUS_NOT_UPLOADED);
        log.debug(Logs.TRANSACTIONS, "Updating PROBLEM REPORT upload status " + id + " to " + getContentValuesString(values));

        try {
            if (sqliteDb.isOpen())
            {
                //update record
                int numRows = sqliteDb.update(AutotranDB.TABLE_PROBLEM_REPORT, values,
                        "id = ?" ,
                        new String[] {String.valueOf(id)});

                if(DEBUG) log.debug(Logs.TRANSACTIONS, "Rows updated: " + String.valueOf(numRows));
            }
        } catch (Exception ex) {
            System.err.print(ex);
        }

    }

    public Cursor getProblemReportFromDb(String guid) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = "guid = '" + guid + "'";

            log.debug(Logs.TRANSACTIONS, filter);

            //Grab a distinct set
            cursor = sqliteDb.query(true, AutotranDB.TABLE_PROBLEM_REPORT, new String[] {"*"}, filter, null, null, null, null, null);


        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getProblemReportFromDb(int id) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = "id = " + id;

            log.debug(Logs.TRANSACTIONS, filter);

            //Grab a distinct set
            cursor = sqliteDb.query(true, AutotranDB.TABLE_PROBLEM_REPORT, new String[] {"*"}, filter, null, null, null, null, null);


        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getNextProblemReportImage(String guid) {
        String filter = null;
        Cursor cursor = null;
        filter = "problem_report_guid = '" + guid+ "' and uploadStatus = " + Constants.SYNC_STATUS_NOT_UPLOADED + " and uploadIndex < " + (Constants.NUMBER_OF_IMAGE_CHUNKS -1);

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_IMAGE, new String[] {
                    "image_id",
                    "image_bytes",
                    "delivery_vin_id",
                    "inspection_guid",
                    "load_id",
                    "delivery_id",
                    "problem_report_guid",
                    "uploaded", "preloadUploadStatus", "deliveryUploadStatus", "uploadStatus","uploadIndex","s3_upload_status",
                    "imageLat",
                    "imageLon",
                    "uploaded",
                    "preloadImage",
                    "foreignKey",
                    "foreignKeyLabel",
                    "filename",
                    "retries"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;

    }

    public Cursor getProblemReportImagesFromDb(String guid) {
        String filter = null;
        Cursor cursor = null;
        filter = "problem_report_guid = '" + guid + "'";

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_IMAGE, new String[] {
                    "image_id",
                    "image_bytes",
                    "delivery_vin_id",
                    "inspection_guid",
                    "load_id",
                    "delivery_id",
                    "problem_report_guid",
                    "uploaded", "preloadUploadStatus", "deliveryUploadStatus", "uploadStatus","uploadIndex","s3_upload_status",
                    "imageLat",
                    "imageLon",
                    "uploaded",
                    "preloadImage",
                    "foreignKey",
                    "foreignKeyLabel",
                    "filename",
                    "preauth_url",
                    "retries"
            }, filter, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getDamageNotesForDamage(String damage_guid) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = "damage_guid = '" + damage_guid + "'";

            cursor = sqliteDb.query(true, AutotranDB.TABLE_DAMAGE_NOTES, new String[] {"*"}, filter, null, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getDamageNoteTemplates() {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(true, AutotranDB.TABLE_DAMAGE_NOTE_TEMPLATES, new String[] {"*"}, "active = 1", null, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getPredefinedNotes() {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(true, AutotranDB.TABLE_PREDEFINED_NOTES, new String[] {"*"}, "active = 1", null, null, null, null, null);
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getOriginTerminalForLoadId(int load_id) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = "not status = 'cleared'";

            if(load_id != -1)
                filter += " and load_id = " + String.valueOf(load_id);

            cursor = sqliteDb.query(AutotranDB.TABLE_LOAD,

                    new String[] {"originTerminal"}, filter, null, null,
                    null, "load_id" );
        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Long insertPredefinedNote(PredefinedNote predefinedNote) {
        long x = -1;
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", predefinedNote.id);
        values.put("note", predefinedNote.note);
        values.put("active", predefinedNote.active ? 1 : 0);
        values.put("damageNote", predefinedNote.damageNote ? 1 : 0);
        values.put("signatureNote", predefinedNote.signatureNote ? 1 : 0);
        values.put("mfg", predefinedNote.mfg);
        if (predefinedNote.modified != null) {
            values.put("modified", predefinedNote.modified.getTime());
        }

        log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_PREDEFINED_NOTES,
                        new String[]{"id"},
                        "id = " + predefinedNote.id,
                        null, null, null, null);

                //if duplicate
                if(duplicateCursor.getCount() > 0){
                    //do nothing since this is a single- ???
                    sqliteDb.update(AutotranDB.TABLE_PREDEFINED_NOTES, values, "id = ?", new String[]{String.valueOf(predefinedNote.id)});
                    x = predefinedNote.id;
                    Log.d(TAG, "updated predefined note " + x);
                } else {
                    //if not duplicate
                    //insert new record
                    x = sqliteDb.insert(AutotranDB.TABLE_PREDEFINED_NOTES, null, values);
                    Log.d(TAG, "inserted predefined note " + values);
                }
                //sqliteDb.close();
            } else {
                log.debug(Logs.TRANSACTIONS, "db was closed...");
            }
        } catch (Exception ex) {
            log.debug(Logs.TRANSACTIONS, "Caught an error: " + ex.getMessage());
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }

        return x;
    }

    public Cursor getPhotosToUpload() {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            String filter = "s3_upload_status = " + Constants.SYNC_STATUS_NOT_UPLOADED;

            cursor = sqliteDb.query(AutotranDB.TABLE_IMAGE, new String[] {"*"}, filter, null, null, null, null, null);

        } catch (Exception ex) {
            log.debug(Logs.TRANSACTIONS, "Caught an error: " + ex.getMessage());
        }

        return cursor;
    }

    public Cursor getNextPhotoToUpload(boolean hires) {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            String filter = "s3_upload_status = " + Constants.SYNC_STATUS_NOT_UPLOADED;
            if(hires) {
                filter += " and filename LIKE '%hires%'";
            } else {
                filter += " and filename NOT LIKE '%hires%'";
            }

            long count = DatabaseUtils.queryNumEntries(sqliteDb, AutotranDB.TABLE_IMAGE, "s3_upload_status = " + Constants.SYNC_STATUS_NOT_UPLOADED);
            log.debug(Logs.UPLOAD, "getting photo to upload, total photos to upload: " + count);

            cursor = sqliteDb.query(AutotranDB.TABLE_IMAGE, new String[] {"*"}, filter, null, null, null, null, "1");

        } catch (Exception ex) {
            log.debug(Logs.TRANSACTIONS, "Caught an error: " + ex.getMessage());
        }

        return cursor;
    }

    public Cursor getPhotoUploadInProgress() {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            String filter = "s3_upload_status = " + Constants.SYNC_STATUS_UPLOADING;

            cursor = sqliteDb.query(AutotranDB.TABLE_IMAGE, new String[] {"*"}, filter, null, null, null, null, null);

        } catch (Exception ex) {
            log.debug(Logs.TRANSACTIONS, "Caught an error: " + ex.getMessage());
        }

        return cursor;
    }

    public Cursor getPhotoUploadMaxRetriesExceeded() {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            String filter = "s3_upload_status = " + Constants.SYNC_STATUS_MAX_RETRIES_EXCEEDED;

            cursor = sqliteDb.query(AutotranDB.TABLE_IMAGE, new String[] {"*"}, filter, null, null, null, null, null);

        } catch (Exception ex) {
            log.debug(Logs.TRANSACTIONS, "Caught an error: " + ex.getMessage());
        }

        return cursor;
    }



    public Cursor getLoadChildPositions(int parent_load_id) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = "not status = 'cleared' and parent_load_id = " + parent_load_id;

            log.debug(Logs.TRANSACTIONS, "fetching loads: " + filter);

            String query = "SELECT delivery_vin.position "
                    + "FROM delivery_vin "
                    + " INNER JOIN delivery ON delivery.delivery_id = delivery_vin.delivery_id "
                    + " INNER JOIN load ON load.load_id = delivery.load_id "
                    + " WHERE load.parent_load_id = " + parent_load_id;
            cursor = sqliteDb.rawQuery(query, null);

        } catch (Exception ex) {

        }

        return cursor;
    }

    public Cursor getLoadsFromDBForParent(int parent_load_id) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

            String filter = "not status = 'cleared' and parent_load_id = " + parent_load_id;

            log.debug(Logs.TRANSACTIONS, "fetching loads: " + filter);

            cursor = sqliteDb.query(AutotranDB.TABLE_LOAD,

                    new String[] {"loadNumber", "load_id", "load_remote_id", "driverPreLoadSignature",
                            "driverPreLoadSignatureLat", "driverPreLoadSignatureLon",
                            "driverPreLoadContact", "driverPreLoadComment", "status", "driver_id", "uploaded",
                            "preloadUploadStatus", "deliveryUploadStatus", "driverPreLoadSignatureSignedAt","helpTerminal",
                            "shuttleLoad", "driver_number", "truck_number", "shuttleMoveId", "lastUpdated", "relayLoad",
                            "originLoad", "originLoadNumber", "relayLoadNumber", "relayLoadDealerName", "ldtyp","notes",
                            "originTerminal","supervisorSignature","supervisorSignedAt","supervisorSignatureLat","supervisorSignatureLon",
                            "driverHighClaimsAudit","nextDispatch","firstDrop",
                            "lastDrop","lotCodeMsgSeen","parentLoad","parent_load_id","preloadSupervisorSignature",
                            "preloadSupervisorSignedAt","preloadSupervisorSignatureLat","preloadSupervisorSignatureLon",
                            "trailerNumber", "pickSheetImageRequired", "extraDocImageRequired"}, filter, null, null,
                    null, null, null);

            log.debug(Logs.TRANSACTIONS, "fetched loads: " + filter);

        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return cursor;
    }

    public Cursor getChildLoadIds(int driver_id) {
        Cursor cursor = null;
        try {
            sqliteDb = appDb.getReadableDatabase();

			String query = "SELECT load.load_id, load.parent_load_id"
					+ " FROM " + AutotranDB.TABLE_LOAD
					+ " WHERE load.driver_id = " + String.valueOf(driver_id) + " AND NOT load.parent_load_id = -1 and not load.parent_load_id is null";

			cursor = sqliteDb.rawQuery(query, null);
		} catch (Exception exce) {
			exce.printStackTrace();
		}

		return cursor;
	}

    public Cursor getPickSheetAndExtraImages(String load_remote_id, String extraImageTag) {
		Cursor cursor = null;
		try {
			String filter = "load_id = " + load_remote_id + " and (filename LIKE '%PICK_SHEET%'";
			if (HelperFuncs.isNullOrWhitespace(extraImageTag)) {
			    filter += ")";
            }
			else {
			    filter += " or filename LIKE '%" + extraImageTag + "%')";
            }

			sqliteDb = appDb.getReadableDatabase();
			cursor = sqliteDb.query(AutotranDB.TABLE_IMAGE, new String[] {
					"image_id",
					"image_bytes",
					"delivery_vin_id",
					"inspection_guid",
					"load_id",
					"delivery_id",
					"problem_report_guid",
					"uploaded", "preloadUploadStatus", "deliveryUploadStatus", "uploadStatus","uploadIndex","s3_upload_status",
					"imageLat",
					"imageLon",
					"uploaded",
					"preloadImage",
					"foreignKey",
					"foreignKeyLabel",
					"filename",
					"preauth_url",
                    "retries"
			}, filter, null, null, null, null);

		} catch (Exception exce) {
			exce.printStackTrace();
		}

		return cursor;
	}

    public void saveSafeDelivery(String id, String comment) {
		sqliteDb = appDb.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("safeDelivery", comment);
		log.debug(Logs.TRANSACTIONS, "Updating safeDelivery to " + getContentValuesString(values));

		try {
			if (sqliteDb.isOpen())
			{
				//update record
				int numRows = sqliteDb.update(AutotranDB.TABLE_DELIVERY, values,
						"delivery_id = ?" ,
						new String[] {String.valueOf(id)});

				if(DEBUG) log.debug(Logs.TRANSACTIONS, "Rows updated: " + String.valueOf(numRows));
			}
		} catch (Exception ex) {
			System.err.print(ex);
		}
    }

    public int getLoadIdForDeliveryVinId(int delivery_vin_id) {
		int loadId = -1;
        Cursor cursor = null;
		try {
			sqliteDb = appDb.getReadableDatabase();

			String query = "SELECT load.load_id"
					+ " FROM " + AutotranDB.TABLE_LOAD + "," + AutotranDB.TABLE_DELIVERY + "," + AutotranDB.TABLE_DELIVERY_VIN
					+ " WHERE load.load_id = delivery.load_id and delivery.delivery_id = delivery_vin.delivery_id and delivery_vin.delivery_vin_id = " + String.valueOf(delivery_vin_id);
			cursor = sqliteDb.rawQuery(query, null);
		} catch (Exception exce) {
			exce.printStackTrace();
		}
		finally {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    loadId = cursor.getInt(cursor.getColumnIndex("load_id"));
                }
                cursor.close();
            }
        }
		return loadId;
    }

    public int getUserCount(String userType) {
        int count = 0;
        try {
            sqliteDb = appDb.getReadableDatabase();

            if (HelperFuncs.isNullOrEmpty(userType)) {
                count = (int) DatabaseUtils.queryNumEntries(sqliteDb, AutotranDB.TABLE_USER);
            }
            else {
                count = (int) DatabaseUtils.queryNumEntries(sqliteDb, AutotranDB.TABLE_USER,
                        "userType=?", new String[]{userType});
            }

        } catch (Exception exce) {
            exce.printStackTrace();
        }
        return count;
    }

    public long insertTrainingTypeToLocalDB(TrainingType type) {
        sqliteDb = appDb.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("id", type.id);
        values.put("name", type.name);
        values.put("defaultRequiredProgress", type.defaultRequiredProgress);

        // capture changes to requirements from remote
        return sqliteDb.replace(AutotranDB.TABLE_TRAINING_TYPE, null, values);
    }

    public long insertTrainingRequirementToLocalDB(TrainingRequirement requirement) {
        sqliteDb = appDb.getWritableDatabase();

        ContentValues values = new ContentValues();

        // For ad-hoc requirements, use the autoincrement
        if(requirement.adHoc == 0) {
            values.put("id", requirement.id);
        }
        values.put("supervisor_id", requirement.supervisor_id);
        values.put("load_id", requirement.load_id);
        values.put("user_id", requirement.user_id);
        values.put("type", requirement.type);
        values.put("assigned", requirement.getAssignedAsTimestamp());
        values.put("started", requirement.getStartedAsTimestamp());
        values.put("completed", requirement.getCompletedAsTimestamp());
        values.put("progress", requirement.progress);
        values.put("requiredProgress", requirement.requiredProgress);
        values.put("vin", requirement.vin);
        values.put("supplementalReference", requirement.getSupplementalReference());
        values.put("supplementalData", requirement.supplementalData);
        values.put("startedLatitude", requirement.startedLatitude);
        values.put("startedLongitude", requirement.startedLongitude);
        values.put("completedLatitude", requirement.completedLatitude);
        values.put("completedLongitude", requirement.completedLongitude);
        values.put("adHoc", requirement.adHoc);

        return sqliteDb.insert(AutotranDB.TABLE_TRAINING_REQUIREMENT, null, values);
    }

    public long updateTrainingRequirement(TrainingRequirement requirement) {
        sqliteDb = appDb.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("id", requirement.id);
        values.put("supervisor_id", requirement.supervisor_id);
        values.put("load_id", requirement.load_id);
        values.put("user_id", requirement.user_id);
        values.put("type", requirement.type);
        values.put("assigned", requirement.getAssignedAsTimestamp());
        values.put("started", requirement.getStartedAsTimestamp());
        values.put("completed", requirement.getCompletedAsTimestamp());
        values.put("progress", requirement.progress);
        values.put("requiredProgress", requirement.requiredProgress);
        values.put("vin", requirement.vin);
        values.put("supplementalReference", requirement.getSupplementalReference());
        values.put("supplementalData", requirement.supplementalData);
        values.put("startedLatitude", requirement.startedLatitude);
        values.put("startedLongitude", requirement.startedLongitude);
        values.put("completedLatitude", requirement.completedLatitude);
        values.put("completedLongitude", requirement.completedLongitude);
        values.put("adHoc", requirement.adHoc);

        return sqliteDb.update(AutotranDB.TABLE_TRAINING_REQUIREMENT, values, "id = ?", new String[]{Long.toString(requirement.id)});
    }

    public long markTrainingRequirementAdHoc(long id) {
        sqliteDb = appDb.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("adHoc", 1);

        return sqliteDb.update(AutotranDB.TABLE_TRAINING_REQUIREMENT, values, "id = ?", new String[]{Long.toString(id)});
    }

    public long markTrainingRequirementUploaded(long id) {
        sqliteDb = appDb.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("uploaded", 1);

        return sqliteDb.update(AutotranDB.TABLE_TRAINING_REQUIREMENT, values, "id = ?", new String[]{Long.toString(id)});
    }

    public long deleteOrphanTrainingRequirements(List<String> currentLoadIds) {
        sqliteDb = appDb.getWritableDatabase();

        if(currentLoadIds.size() == 0) {
            // If we have no current loads, there can't be any useful training requirements unless
            // they have a non-null user.
            return sqliteDb.delete(AutotranDB.TABLE_TRAINING_REQUIREMENT,
                    "user_id IS NULL",
                    null);
        }
        else {
            // Delete any training requirements not associated with a user or a current
            // load ID.
            String inClause = buildInClause(currentLoadIds);
            return sqliteDb.delete(AutotranDB.TABLE_TRAINING_REQUIREMENT,
                    "user_id IS NULL AND load_id NOT " + inClause,
                    null);
        }
    }

    public Cursor getTrainingTypeListFromLocalDB() {
        sqliteDb = appDb.getReadableDatabase();

        return sqliteDb.query(AutotranDB.TABLE_TRAINING_TYPE, null, null, null, null, null, null);
    }

    public Cursor getTrainingRequirementFromLocalDB(long id) {
        sqliteDb = appDb.getReadableDatabase();

        return sqliteDb.query(AutotranDB.TABLE_TRAINING_REQUIREMENT,
                null,
                "id = ?",
                new String[]{Long.toString(id)},
                null,
                null,
                null,
                null);
    }

    public Cursor getTrainingRequirementsInListFromLocalDB(long[] ids) {
        sqliteDb = appDb.getReadableDatabase();

        if(ids.length < 1) throw new IllegalArgumentException();

        String inClause = buildInClause(ids);

        return sqliteDb.query(AutotranDB.TABLE_TRAINING_REQUIREMENT,
                null,
                 "id " + inClause,
                null,
                null,
                null,
                null,
                null);
    }

    private String buildInClause(long[] ids) {
        StringBuilder inClause = new StringBuilder();
        inClause.append("IN (");
        inClause.append(ids[0]);

        for(int i = 1; i < ids.length; i++) {
            inClause.append(",");
            inClause.append(ids[i]);
        }

        inClause.append(")");
        return inClause.toString();
    }

    private String buildInClause(List<String> ids) {
        StringBuilder inClause = new StringBuilder();
        inClause.append("IN (");
        inClause.append(ids.get(0));

        for(int i = 1; i < ids.size(); i++) {
            inClause.append(",");
            inClause.append(ids.get(i));
        }

        inClause.append(")");
        return inClause.toString();
    }

    public Cursor getCompletedTrainingRequirementsFromLocalDB() {
        sqliteDb = appDb.getReadableDatabase();

        return sqliteDb.query(AutotranDB.TABLE_TRAINING_REQUIREMENT,
                null,
                "completed IS NOT NULL",
                null,
                null,
                null,
                null,
                null);
    }

    public Cursor getTrainingRequirementsByLoadFromLocalDB(String loadRemoteId) {
        sqliteDb = appDb.getReadableDatabase();

        return sqliteDb.query(AutotranDB.TABLE_TRAINING_REQUIREMENT,
                null,
                "load_id = ?",
                new String[]{loadRemoteId},
                null,
                null,
                null,
                null);
    }

    public Cursor getUnfinishedTrainingRequirementsByLoadFromLocalDB(String loadRemoteId) {
        sqliteDb = appDb.getReadableDatabase();

        return sqliteDb.query(AutotranDB.TABLE_TRAINING_REQUIREMENT,
                null,
                "load_id = ? AND completed IS NULL",
                new String[]{loadRemoteId},
                null,
                null,
                null,
                null);
    }

    public Cursor getCompletedTrainingRequirementsByLoadFromLocalDB(String loadRemoteId) {
        sqliteDb = appDb.getReadableDatabase();

        return sqliteDb.query(AutotranDB.TABLE_TRAINING_REQUIREMENT,
                null,
                "load_id = ? AND completed IS NOT NULL",
                new String[]{loadRemoteId},
                null,
                null,
                null,
                null);
    }

    public Cursor getTrainingRequirementsByUserFromLocalDB(long userId) {
        sqliteDb = appDb.getReadableDatabase();

        return sqliteDb.query(AutotranDB.TABLE_TRAINING_REQUIREMENT,
                null,
                "load_id = ?",
                new String[]{Long.toString(userId)},
                null,
                null,
                null,
                null);
    }

    public long insertTrendingAlertToLocalDB(TrendingAlert trendingAlert) {
        sqliteDb = appDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", trendingAlert.id);
        values.put("load_id", trendingAlert.load_id);
        values.put("loadNumber", trendingAlert.ldnbr);
        values.put("vin_id", trendingAlert.vin_id);
        values.put("driver_id", trendingAlert.driver_id);
        values.put("alert", trendingAlert.alert);
        values.put("count", trendingAlert.count);
        values.put("ordr", trendingAlert.order);
        values.put("type", trendingAlert.type);

        return sqliteDb.insert(AutotranDB.TABLE_TRENDING_ALERT, null, values);
    }

    public Cursor getTrendingAlertsForLoadRemoteId(int load_remote_id) {
        sqliteDb = appDb.getReadableDatabase();

        String filter = "load_id = " + String.valueOf(load_remote_id) +" and vin_id is null";

        return sqliteDb.query(AutotranDB.TABLE_TRENDING_ALERT, new String[] {
                "id",
                "load_id",
                "driver_id",
                "loadNumber",
                "vin_id",
                "alert",
                "count",
                "ordr",
                "type"},
                filter,
                null,
                null,
                null,
                null);
    }

    public Cursor getTrendingAlertsForVinId(int load_remote_id, int vin_id) {
        sqliteDb = appDb.getReadableDatabase();

        String filter = "vin_id = " + String.valueOf(vin_id) + " and load_id = " + load_remote_id;

        return sqliteDb.query(AutotranDB.TABLE_TRENDING_ALERT, new String[] {
                "id",
                "load_id",
                "driver_id",
                "loadNumber",
                "vin_id",
                "alert",
                "count",
                "ordr",
                "type"},
                filter,
                null,
                null,
                null,
                null);
    }

    public long insertWMRequestToLocalDB(WMRequest wmRequest) {
        sqliteDb = appDb.getReadableDatabase();

        long id = -1;

        //duplicate if request exists
        Cursor duplicateCursor = null;
        try {
            if (sqliteDb.isOpen()) {
                duplicateCursor = sqliteDb.query(
                        AutotranDB.TABLE_WM_REQUESTS,
                        new String[] {"id", "uuid", "jsonData", "retries"},
                        "id = " + wmRequest.getId(),
                        null, null, null, null);

                ContentValues values = new ContentValues();
                values.put("uuid", wmRequest.getUuid());
                values.put("jsonData", wmRequest.getJsonData());
                values.put("retries", wmRequest.getRetries());

                log.debug(Logs.TRANSACTIONS, getContentValuesString(values));

                //if duplicate
                if(duplicateCursor.getCount() > 0){

                    duplicateCursor.moveToFirst();
                    long duplicateId = duplicateCursor.getInt(duplicateCursor.getColumnIndex("id"));

                    //update request
                    sqliteDb.update(AutotranDB.TABLE_WM_REQUESTS, values,
                            "id = " + duplicateId, null);

                    id = duplicateId;
                } else {
                    //if not duplicate
                    //insert request
                    id = sqliteDb.insert(AutotranDB.TABLE_WM_REQUESTS, null, values);
                }
            }
        } catch (Exception ex) {
            System.err.print(ex);
        } finally {
            if (duplicateCursor != null) {
                duplicateCursor.close();
            }
        }
        return id;
    }

    public Cursor getWMRequestFromLocalDB(int id) {
        Cursor cursor = null;

        try {
            sqliteDb = appDb.getReadableDatabase();
            cursor = sqliteDb.query(AutotranDB.TABLE_WM_REQUESTS, new String[] {
                    "id",
                    "uuid",
                    "jsonData",
                    "retries"
            }, "id = " + id, null, null, null, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return cursor;
    }

    public boolean deleteWMRequest(int id) {
        log.debug(Logs.TRANSACTIONS, "id:" + id);

        boolean retVal = false;
        //String KEY_NAME = "_id";
        String whereClause = "id = " + id;
        try{
            sqliteDb = appDb.getWritableDatabase();
            if(sqliteDb.isOpen())
            {
                int rowsAffected = sqliteDb.delete(AutotranDB.TABLE_WM_REQUESTS, whereClause, null);
                System.out.println("ROWS AFFECTED : "+rowsAffected);
                retVal = true;
            }
        }catch(Exception ex)
        {
            retVal = false;
            ex.printStackTrace();
        }
        return retVal;
    }

    public void closeAndReopenDatabase() {
        // NOTE: This is probably not a good idea. Database transactions may be in
        //       progress in background tasks even if this is called from the UI.
        sqliteDb = appDb.getWritableDatabase();
        if(sqliteDb.isOpen())
        {
            appDb.close();
            sqliteDb = appDb.getWritableDatabase();
        }
    }
}
