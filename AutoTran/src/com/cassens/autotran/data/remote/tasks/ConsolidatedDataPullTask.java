package com.cassens.autotran.data.remote.tasks;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.activities.ViewMessagesActivity;
import com.cassens.autotran.backendpoc.PoCUtils;
import com.cassens.autotran.constants.URLS;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.DamageNoteTemplate;
import com.cassens.autotran.data.model.DriverAction;
import com.cassens.autotran.data.model.LotCodeMessage;
import com.cassens.autotran.data.model.PredefinedNote;
import com.cassens.autotran.data.model.TrendingAlert;
import com.cassens.autotran.data.model.lookup.AreaCode;
import com.cassens.autotran.data.model.lookup.LotCode;
import com.cassens.autotran.data.model.lookup.ScacCode;
import com.cassens.autotran.data.model.lookup.SeverityCode;
import com.cassens.autotran.data.model.lookup.ShuttleMove;
import com.cassens.autotran.data.model.lookup.SpecialCode;
import com.cassens.autotran.data.model.lookup.Terminal;
import com.cassens.autotran.data.model.lookup.TrainingType;
import com.cassens.autotran.data.model.lookup.TypeCode;
import com.cassens.autotran.data.remote.CallWebServices;
import com.sdgsystems.app_config.AppConfig;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.cassens.autotran.receivers.DriverActionQueueReceiver;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.sdgsystems.util.HelperFuncs;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import com.google.gson.reflect.TypeToken;


public class ConsolidatedDataPullTask extends AsyncTask<Void, String, Void> {
    private static final Logger log = LoggerFactory.getLogger(ConsolidatedDataPullTask.class.getSimpleName());

    private static String TAG = "ConsolidatedDataPullTask";
    private IConsolidatedDataPullCallback _callback;
    private Context context;

    private boolean mGetAll = false;

    public ConsolidatedDataPullTask(Context context, boolean getAll, IConsolidatedDataPullCallback callback) {
        this.context = context;
        mGetAll = getAll;

        if(callback != null) {
            _callback = callback;
        }
    }

    public interface IConsolidatedDataPullCallback {
        void updateProgress(String status);
        void complete();
    }

    public void setContext(Context context) {
        this.context = context;
    }

    private class ResponseObject {
        public String status;
        public String message;
        public AreaCodeResults areaCodeResults;
        public TypeCodeResults typeCodeResults;
        public SeverityCodeResults severityCodeResults;
        public SpecialCodeResults specialCodeResults;
        public TerminalResults terminalResults;
        public ScacCodeResults scacCodeResults;
        public LotCodeResults lotCodeResults;
        public LotCodeMsgResults lotCodeMsgResults;
        public DriverActionResults driverActionResults;
        public ShuttleMoveResults shuttleMoveResults;
        public DamageNoteTemplateResults damageNoteTemplateResults;
        public PredefinedNoteResults predefinedNoteResults;
        public TrainingTypeResults trainingTypeResults;
        public TrendingAlertResults trendingAlertResults;
        public ConfigSettingsResults configSettingsResults;
    }

    private class RemoteAreaCode {
        String id;
        String group;
        String description;
        String code;
        String active;
        Date created;
        Date modified;
    }

    private class AreaCodeRec {
        public RemoteAreaCode AreaCode;
        public RemoteAreaCode[] child;
    }

    private class AreaCodeResults {
        public String tableLastModified;
        public AreaCodeRec[] recs;
    }

    private boolean isStatusActive(String status) {
        return !(HelperFuncs.isNullOrEmpty(status) || status.equals("0"));
    }

    private String zeroIfNullOrEmpty(String value) {
        return HelperFuncs.isNullOrEmpty(value) ? "0" : value;
    }

    private void processAreaCodes(AreaCodeResults results) {
        if (results.recs == null) {
            log.debug(Logs.DEBUG, "Returned null area codes list");
            return;
        }
        if (results.recs.length == 0) {
            return;
        }

        updateLastModified(AREA_CODES_LAST_MODIFIED, results.tableLastModified);

        // NOTE: Area codes are handled differently from the other tables in that any time
        // one area code changes, ALL area codes are returned.  Because of that, we don't check
        // the mGetAll flag here.
        DataManager.setAreaCodeTableRowsInactive(context);

        for (int i = 0; i < results.recs.length; i++)
        {
            RemoteAreaCode rAreaCode = results.recs[i].AreaCode;
            RemoteAreaCode[] rChildren = results.recs[i].child;
            AreaCode areaCode = new AreaCode();

            areaCode.parent_area_code_id =-1;
            areaCode.area_code_remote_id = rAreaCode.id;
            areaCode.setDescription(rAreaCode.group);
            areaCode.modified = rAreaCode.modified;
            areaCode.active = isStatusActive(rAreaCode.active);

            for (int x = 0; x < rChildren.length; x++) {
                RemoteAreaCode rChild = rChildren[x];
                AreaCode childCode = new AreaCode();

                childCode.area_code_remote_id = rChild.id;
                childCode.setDescription(rChild.description);
                childCode.setCode(rChild.code);
                childCode.modified = rChild.modified;
                childCode.active = isStatusActive(rChild.active);
                areaCode.childAreaCodes.add(childCode);
            }
            DataManager.insertAreaCodeToLocalDB(context, areaCode);
        }
    }

    private class RemoteTypeCode {
        String id;
        String description;
        String code;
        String active;
        Date created;
        Date modified;
    }


    private class TypeCodeRec {
        public RemoteTypeCode TypeCode;
    }

    private class TypeCodeResults {
        public String tableLastModified;
        public TypeCodeRec[] recs;
    }

    private void processTypeCodes(TypeCodeResults results) {
        if (results.recs == null) {
            log.debug(Logs.DEBUG, "Returned null TypeCodes list");
            return;
        }
        updateLastModified(TYPE_CODES_LAST_MODIFIED, results.tableLastModified);

        // If we retrieved all rows, mark all rows in the local table as not active.  This handles
        // the case where a row on the server table gets deleted.
        if (mGetAll) {
            DataManager.setTypeCodeTableRowsInactive(context);
        }

        for (int i = 0; i < results.recs.length; i++) {
            RemoteTypeCode rTypeCode = results.recs[i].TypeCode;
            TypeCode code = new TypeCode();
            code.type_code_remote_id = rTypeCode.id;
            code.setCode(rTypeCode.code);
            code.setDescription(rTypeCode.description);
            code.modified = rTypeCode.modified;
            code.active = isStatusActive(rTypeCode.active);
            DataManager.insertTypeCodeToLocalDB(context, code);
        }
    }

    private class RemoteSeverityCode {
        String id;
        String description;
        String code;
        String active;
        Date created;
        Date modified;
    }

    private class SeverityCodeRec {
        public RemoteSeverityCode SeverityCode;
    }

    private class SeverityCodeResults {
        public String tableLastModified;
        public SeverityCodeRec[] recs;
    }

    private void processSeverityCodes(SeverityCodeResults results) {
        if (results.recs == null) {
            log.debug(Logs.DEBUG, "Returned null SeverityCodes list");
            return;
        }
        updateLastModified(SEVERITY_CODES_LAST_MODIFIED, results.tableLastModified);

        // If we retrieved all rows, mark all rows in the local table as not active.  This handles
        // the case where a row on the server table gets deleted.
        if (mGetAll) {
            DataManager.setSvrtyCodeTableRowsInactive(context);
        }

        if (results.recs.length > 0) {
            for (int i = 0; i < results.recs.length; i++) {
                RemoteSeverityCode rSeverityCode = results.recs[i].SeverityCode;
                SeverityCode code = new SeverityCode();
                code.severity_code_remote_id = rSeverityCode.id;
                code.setCode(rSeverityCode.code);
                code.setDescription(rSeverityCode.description);
                code.modified = rSeverityCode.modified;
                code.active = isStatusActive(rSeverityCode.active);
                DataManager.insertSvrtyCodeToLocalDB(context, code);
            }
        }
    }

    private class RemoteSpecialCode {
        String id;
        String area_code;
        String type_code;
        String severity_code;
        String description;
        String active;
        Date created;
        Date modified;
    }

    private class SpecialCodeRec {
        public RemoteSpecialCode SpecialCode;
    }

    private class SpecialCodeResults {
        public String tableLastModified;
        public SpecialCodeRec[] recs;
    }

    private void processSpecialCodes(SpecialCodeResults results) {
        if (results.recs == null) {
            log.debug(Logs.DEBUG, "Returned null SpecialCodes list");
            return;
        }
        updateLastModified(SPECIAL_CODES_LAST_MODIFIED, results.tableLastModified);

        // If we retrieved all rows, mark all rows in the local table as not active.  This handles
        // the case where a row on the server table gets deleted.
        if (mGetAll) {
            DataManager.setSpecialCodeTableRowsInactive(context);
        }

        for (int i = 0; i < results.recs.length; i++) {
            RemoteSpecialCode rSpecialCode = results.recs[i].SpecialCode;
            SpecialCode code = new SpecialCode();
            code.special_code_remote_id = rSpecialCode.id;
            code.setAreaCode(zeroIfNullOrEmpty(rSpecialCode.area_code));
            code.setTypeCode(zeroIfNullOrEmpty(rSpecialCode.type_code));
            code.setSeverityCode(zeroIfNullOrEmpty(rSpecialCode.severity_code));
            code.setDescription(rSpecialCode.description);
            code.modified = rSpecialCode.modified;
            code.active = isStatusActive(rSpecialCode.active);
            DataManager.insertSpecialCodeToLocalDB(context, code);
        }
    }

    private class RemoteTerminal {
        public int id;
        public String description;
        public String rowCharacters;
        public String bayCharacters;
        public String popupMessage;
        public String phoneNumber;
        public String usToCanPhoneNumber;
        public String canToUsPhoneNumber;
        public String dispatchPhoneNumber;
        public String countryCode;
        public boolean restrictedDispatch;
        public boolean highClaims;
        String active;
        public Date created;
        public Date modified;
    }
    private class TerminalRec {
        public RemoteTerminal Terminal;
    }

    private class TerminalResults {
        public String tableLastModified;
        public TerminalRec[] recs;
    }

    private class RemoteScacCode {
        int id;
        int terminal_id;
        String code;
        String description;
        String active;
        Date created;
        Date modified;
    }

    private class ScacCodeRec {
        public RemoteScacCode ScacCode;
    }

    private class ScacCodeResults {
        public String tableLastModified;
        public ScacCodeRec[] recs;
    }

    private void processScacCodes(ScacCodeResults results) {
        if (results.recs == null) {
            log.debug(Logs.DEBUG, "Returned null ScacCodes list");
            return;
        }
        updateLastModified(SCAC_CODES_LAST_MODIFIED, results.tableLastModified);

        // If we retrieved all rows, mark all rows in the local table as not active.  This handles
        // the case where a row on the server table gets deleted.
        if (mGetAll) {
            DataManager.setScacCodeTableRowsInactive(context);
        }

        for (int i = 0; i < results.recs.length; i++) {
            RemoteScacCode rScacCode = results.recs[i].ScacCode;
            ScacCode code = new ScacCode();
            code.scac_code_id = rScacCode.id;
            code.terminal_id = rScacCode.terminal_id;
            code.setCode(rScacCode.code);
            code.setDescription(rScacCode.description);
            code.modified = rScacCode.modified;
            code.active = isStatusActive(rScacCode.active);
            DataManager.insertScacCode(context, code);
        }
    }

    private class LotCodeRec {
        public LotCode LotCode;
    }

    private class LotCodeResults {
        public String tableLastModified;
        public LotCodeRec[] recs;
    }

    private class LotCodeMsgRec {
        public LotCodeMessage LotCodeMsg;
    }

    private class LotCodeMsgResults {
        public String tableLastModified;
        public LotCodeMsgRec[] recs;
    }

    private class DriverActionRec {
        public DriverAction DriverAction;
    }

    private class DriverActionResults {
        public String tableLastModified;
        public DriverActionRec[] recs;
    }

    private class ShuttleMoveRec {
        public ShuttleMove ShuttleMove;
    }

    private class ShuttleMoveResults {
        public String tableLastModified;
        public ShuttleMoveRec[] recs;
    }

    private class DamageNoteTemplateRec {
        public DamageNoteTemplate DamageNoteTemplate;
    }

    private class DamageNoteTemplateResults {
        public String tableLastModified;
        public DamageNoteTemplateRec[] recs;
    }

    private class PredefinedNoteRec {
        public PredefinedNote PredefinedNote;
    }

    private class PredefinedNoteResults {
        public String tableLastModified;
        public PredefinedNoteRec[] recs;
    }

    private class TrainingTypeResults {
        public String tableLastModified;
        public TrainingTypeRec[] recs;
    }

    private class TrainingTypeRec {
        public TrainingType TrainingType;
    }

    private class TrendingAlertResults {
        public String tableLastModified;
        public TrendingAlertRec[] recs;
    }

    private class TrendingAlertRec {
        public TrendingAlert TrendingAlert;
    }

    private Gson getGsonMapper() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(boolean.class, new ConsolidatedDataPullTask.BooleanTypeAdapter());
        builder.registerTypeAdapter(Date.class, new ConsolidatedDataPullTask.DateTypeAdapter());
        return builder.create();
    }

    private static final String AREA_CODES_LAST_MODIFIED = "area_codes_LastModified";
    private static final String TYPE_CODES_LAST_MODIFIED = "type_codes_LastModified";
    private static final String SEVERITY_CODES_LAST_MODIFIED = "severity_codes_LastModified";
    private static final String SPECIAL_CODES_LAST_MODIFIED = "special_codes_LastModified";
    private static final String TERMINALS_LAST_MODIFIED = "terminals_LastModified";
    private static final String SCAC_CODES_LAST_MODIFIED = "scac_codes_LastModified";
    private static final String LOT_CODES_LAST_MODIFIED = "lot_codes_LastModified";
    private static final String LOT_CODE_MSGS_LAST_MODIFIED = "lot_code_msgs_LastModified";
    private static final String DRIVER_ACTIONS_LAST_MODIFIED = "driver_actions_LastModified";
    private static final String SHUTTLE_MOVES_LAST_MODIFIED = "shuttle_moves_LastModified";
    private static final String DAMAGE_NOTE_TEMPLATES_LAST_MODIFIED = "damage_note_templates_LastModified";
    private static final String PREDEFINED_NOTES_LAST_MODIFIED = "predefined_notes_LastModified";
    private static final String TRAINING_TYPES_LAST_MODIFIED = "training_types_LastModified";
    private static final String TRENDING_ALERTS_LAST_MODIFIED = "trending_alerts_LastModified";
    private static final String CONFIG_SETTINGS_LAST_MODIFIED = "config_settings_LastModified";

    private void setLastModified(String name, Date lastModified, List <NameValuePair> nameValuePairs) {
        if (lastModified != null) {
            nameValuePairs.add(new BasicNameValuePair(name, String.valueOf(formatter.format(lastModified))));
        } else {
            nameValuePairs.add(new BasicNameValuePair(name, null));
        }
    }

    SharedPreferences sharedPrefs;

    private void setLastModified(String name, List <NameValuePair> nameValuePairs) {
        nameValuePairs.add(new BasicNameValuePair(name, sharedPrefs.getString(name, null)));
    }

    private void updateLastModified(String name, String lastModified) {
        sharedPrefs.edit().putString(name, lastModified).commit();
    }

    private void processLotCodes(LotCodeResults results) {
        if (results.recs == null) {
            log.debug(Logs.DEBUG, "Returned null lot codes list");
            return;
        }
        updateLastModified(LOT_CODES_LAST_MODIFIED, results.tableLastModified);

        // If we retrieved all rows, mark all rows in the local table as not active.  This handles
        // the case where a row on the server table gets deleted.
        if (mGetAll) {
            DataManager.setLotCodeTableRowsInactive(context);
        }

        for (int i = 0; i < results.recs.length; i++) {
            LotCode code = results.recs[i].LotCode;
            DataManager.insertLotCode(context, code);
        }
    }

    private void processLotCodeMsgs(LotCodeMsgResults results) {
        if (results.recs == null) {
            log.debug(Logs.DEBUG, "Returned null lot code messages list");
            return;
        }
        updateLastModified(LOT_CODE_MSGS_LAST_MODIFIED, results.tableLastModified);

        // If we retrieved all rows, mark all rows in the local table as not active.  This handles
        // the case where a row on the server table gets deleted.
        if (mGetAll) {
            DataManager.setLotCodeMsgsTableRowsInactive(context);
        }

        for (int i = 0; i < results.recs.length; i++) {
            LotCodeMessage code = results.recs[i].LotCodeMsg;
            DataManager.insertLotCodeMsgToLocalDB(context, code);
        }
    }

    private void processDriverActions(DriverActionResults results) {
        //process unread messages
        String driverNumber = CommonUtility.getDriverNumber(context);

        if (results.recs == null) {
            log.debug(Logs.DEBUG, "Returned null driver actions list");
            return;
        }
        updateLastModified(DRIVER_ACTIONS_LAST_MODIFIED, results.tableLastModified);

        if (results.recs.length == 0) {
            //log.debug(Logs.DEBUG, "Returned empty driver actions list");
            // No new messages, but there might be pending messages to re-display.
            ViewMessagesActivity.processPendingMessages(context, driverNumber, true);
            return;
        }

        for (int i = 0; i < results.recs.length; i++) {
            DriverAction action = results.recs[i].DriverAction;
            DataManager.upsertDriverActionToLocalDB(context, action);
        }

        // Get all of the message actions and display them as an aggregate popup.
        ViewMessagesActivity.processPendingMessages(context, driverNumber, true);

        //If we have any non-message actions remaining, queue them.
        if(DataManager.getDriverActions(context, driverNumber, false, null).size() > 0) {
            log.debug(Logs.DEBUG, "There are pending driver actions, processing the driver action queue");
            Intent intent = new Intent(context, DriverActionQueueReceiver.class);
            context.sendBroadcast(intent);
        }
    }

    private void processShuttleMoves(ShuttleMoveResults results) {
        if (results.recs == null) {
            log.debug(Logs.DEBUG, "Returned null shuttle moves list");
            return;
        }
        updateLastModified(SHUTTLE_MOVES_LAST_MODIFIED, results.tableLastModified);

        // If we retrieved all rows, mark all rows in the local table as not active.  This handles
        // the case where a row on the server table gets deleted.
        if (mGetAll) {
            DataManager.setShuttleMoveTableRowsInactive(context);
        }

        for (int i = 0; i < results.recs.length; i++) {
            ShuttleMove move = results.recs[i].ShuttleMove;
            move.setProps(move.orgDestString);
            DataManager.insertShuttleMove(context, move);
        }
    }

    private void processDamageNoteTemplates(DamageNoteTemplateResults results) {
        if (results.recs == null) {
            log.debug(Logs.DEBUG, "Returned null damage note template list");
            return;
        }
        updateLastModified(DAMAGE_NOTE_TEMPLATES_LAST_MODIFIED, results.tableLastModified);

        // If we retrieved all rows, mark all rows in the local table as not active.  This handles
        // the case where a row on the server table gets deleted.
        if (mGetAll) {
            DataManager.setDamageNoteTemplatesTableRowsInactive(context);
        }

        for (int i = 0; i < results.recs.length; i++) {
            DamageNoteTemplate template = results.recs[i].DamageNoteTemplate;
            DataManager.insertDamageNoteTemplateToLocalDB(context, template);
        }
    }

    private void processPredefinedNotes(PredefinedNoteResults results) {
        if (results.recs == null) {
            log.debug(Logs.DEBUG, "Returned null predefined notes list");
            return;
        }
        updateLastModified(PREDEFINED_NOTES_LAST_MODIFIED, results.tableLastModified);

        // If we retrieved all rows, mark all rows in the local table as not active.  This handles
        // the case where a row on the server table gets deleted.
        if (mGetAll) {
            DataManager.setPredefinedNotesTableRowsInactive(context);
        }

        if (results.recs.length > 0) {
            for (int i = 0; i < results.recs.length; i++) {
                DataManager.insertPredefinedNoteToLocalDB(context, results.recs[i].PredefinedNote);
            }
        }
    }

    private void processTrainingTypes(TrainingTypeResults results) {
        if(results.recs == null) {
            log.debug(Logs.DEBUG, "Returned null training types list");
            return;
        }
        updateLastModified(TRAINING_TYPES_LAST_MODIFIED, results.tableLastModified);

        if(results.recs.length > 0) {
            for(int i = 0; i < results.recs.length; i++) {
                DataManager.insertTrainingTypeToLocalDB(context, results.recs[i].TrainingType);
            }
        }
    }

    private void processTrendingAlerts(TrendingAlertResults results) {
        if (results.recs == null) {
            log.debug(Logs.DEBUG, "Returned null trending alerts list");
            return;
        }
        updateLastModified(TRENDING_ALERTS_LAST_MODIFIED, results.tableLastModified);
        if(results.recs.length > 0) {
            for(int i = 0; i < results.recs.length; i++) {
                //Log.d("NARF", "alert " + results.recs.length);
                DataManager.insertTrendingAlertToLocalDB(context, results.recs[i].TrendingAlert);
            }
        }
    }

    private void processTerminals(TerminalResults results) {
        if (results.recs == null) {
            log.debug(Logs.DEBUG, "Returned null terminals list");
            return;
        }
        updateLastModified(TERMINALS_LAST_MODIFIED, results.tableLastModified);

        // If we retrieved all rows, mark all rows in the local table as not active.  This handles
        // the case where a row on the server table gets deleted.
        if (mGetAll) {
            DataManager.setTerminalTableRowsInactive(context);
        }

        for (int i = 0; i < results.recs.length; i++) {
            RemoteTerminal rTerminal = results.recs[i].Terminal;
            Terminal terminal = new Terminal();
            terminal.terminal_id = rTerminal.id;
            terminal.description = rTerminal.description;
            terminal.popupMessage = rTerminal.popupMessage;
            // TODO: Are Terminal rowCharacters and bayCharacters needed on the tablet?
            //       (They weren't being populated in the prior (GetAllCodes) implementation.)
            //terminal.rowCharacters = rTerminal.rowCharacters;
            //terminal.bayCharacters = rTerminal.bayCharacters;
            terminal.countryCode = rTerminal.countryCode;
            terminal.phoneNumber = rTerminal.phoneNumber;
            terminal.usToCanPhoneNumber = rTerminal.usToCanPhoneNumber;
            terminal.canToUsPhoneNumber = rTerminal.canToUsPhoneNumber;
            terminal.dispatchPhoneNumber = rTerminal.dispatchPhoneNumber;
            terminal.modified = rTerminal.modified;
            terminal.active = isStatusActive(rTerminal.active);
            DataManager.insertTerminal(context, terminal);
        }
    }

    private class RemoteConfigSetting {
        int id;
        String name;
        String value;
        String description;
        String active;
        Date created;
        Date modified;
    }

    private class ConfigSettingsRec {
        public RemoteConfigSetting ConfigSetting;
    }

    private class ConfigSettingsResults {
        public String tableLastModified;
        public ConfigSettingsRec[] recs;
    }

    private void processConfigSettings(ConfigSettingsResults results) {
        if (results.recs == null) {
            log.debug(Logs.DEBUG, "Returned null ConfigSettings list");
            return;
        }
        updateLastModified(CONFIG_SETTINGS_LAST_MODIFIED, results.tableLastModified);

        // Config settings are saved to AppSettings instead of the database.  AppSettings
        // saves the settings to SharePreferences to provide persistence.
        HashMap<String, String> newSettingsMap = new HashMap<String, String>();
        for (int i = 0; i < results.recs.length; i++) {
            RemoteConfigSetting rConfigSetting = results.recs[i].ConfigSetting;
            if (!isStatusActive(rConfigSetting.active)  || HelperFuncs.isNullOrEmpty(rConfigSetting.name)) {
                log.debug(Logs.DEBUG, "JUNK: processConfigSettings(): Ignoring inactive config setting: " + rConfigSetting.name);
                continue;
            }
            String name = HelperFuncs.noNull(rConfigSetting.name);
            String value = HelperFuncs.noNull(rConfigSetting.value);

            String putRetVal = newSettingsMap.put(name, value);
            log.debug(Logs.DEBUG, "JUNK: processConfigSettings(): Added " + name + " to map retval='" + putRetVal + "'");
        }
        AppConfig.updateSettingsOverrides(AutoTranApplication.getAppContext(), newSettingsMap);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        CommonUtility.dispatchLogThreadStartStop("Started ConsolidatedDataPullTask", true);
        if(this.context == null) {
            log.error(Logs.DEBUG, "The context for the driver data update task was null, not executing");
            return null;
        }

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        NetworkInfo networkInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnectedOrConnecting()) {
            return null;
        }

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

        if(!mGetAll) {
            setLastModified(AREA_CODES_LAST_MODIFIED, nameValuePairs);
            setLastModified(TYPE_CODES_LAST_MODIFIED, nameValuePairs);
            setLastModified(SEVERITY_CODES_LAST_MODIFIED, nameValuePairs);
            setLastModified(SPECIAL_CODES_LAST_MODIFIED, nameValuePairs);
            setLastModified(TERMINALS_LAST_MODIFIED, nameValuePairs);
            setLastModified(SCAC_CODES_LAST_MODIFIED, nameValuePairs);
            setLastModified(LOT_CODES_LAST_MODIFIED, nameValuePairs);
            setLastModified(LOT_CODE_MSGS_LAST_MODIFIED, nameValuePairs);
            setLastModified(SHUTTLE_MOVES_LAST_MODIFIED, nameValuePairs);
            setLastModified(DAMAGE_NOTE_TEMPLATES_LAST_MODIFIED, nameValuePairs);
            setLastModified(PREDEFINED_NOTES_LAST_MODIFIED, nameValuePairs);
            setLastModified(TRAINING_TYPES_LAST_MODIFIED, nameValuePairs);
            setLastModified(TRENDING_ALERTS_LAST_MODIFIED, nameValuePairs);
            setLastModified(CONFIG_SETTINGS_LAST_MODIFIED, nameValuePairs);
        }
        // Don't attempt to pull all on the driver_actions table.
        setLastModified(DRIVER_ACTIONS_LAST_MODIFIED, nameValuePairs);

        Gson gson = this.getGsonMapper();

        try {
            publishProgress("Retrieving remote data");
            Type listType = new TypeToken<List<NameValuePair>>() {}.getType();
            PoCUtils.logHttpRequest(URLS.consolidated_data_pull, gson.toJson(nameValuePairs, listType));
            ResponseObject response = gson.fromJson(CallWebServices.sendJson(URLS.consolidated_data_pull, nameValuePairs, this.context), ResponseObject.class);
            if (response == null) {
                log.debug(Logs.DEBUG, "Null Gson response returned");
                return null;
            }
            //log.debug(Logs.DEBUG, "status=" + response.status);
            //log.debug(Logs.DEBUG, "message=" + response.message);
            Log.d(TAG, "status=" + response.status);
            Log.d(TAG, "message=" + response.message);

            publishProgress("Updating area codes");
            processAreaCodes(response.areaCodeResults);
            publishProgress("Updating type codes");
            processTypeCodes(response.typeCodeResults);
            publishProgress("Updating severity codes");
            processSeverityCodes(response.severityCodeResults);
            publishProgress("Updating special codes");
            processSpecialCodes(response.specialCodeResults);
            publishProgress("Updating terminals");
            processTerminals(response.terminalResults);
            publishProgress("Updating scac codes");
            processScacCodes(response.scacCodeResults);
            publishProgress("Updating lot codes");
            processLotCodes(response.lotCodeResults);
            publishProgress("Updating lot code messages");
            processLotCodeMsgs(response.lotCodeMsgResults);
            publishProgress("Updating driver actions");
            processDriverActions(response.driverActionResults);
            publishProgress("Updating shuttle moves");
            processShuttleMoves(response.shuttleMoveResults);
            publishProgress("Updating damage notes");
            processDamageNoteTemplates(response.damageNoteTemplateResults);
            publishProgress("Updating predefined notes");
            processPredefinedNotes(response.predefinedNoteResults);
            publishProgress("Updating training types");
            processTrainingTypes(response.trainingTypeResults);
            publishProgress("Updating trending alerts");
            processTrendingAlerts(response.trendingAlertResults);
            publishProgress("Updating config settings");
            processConfigSettings(response.configSettingsResults);

            PoCUtils.logHttpResponse(URLS.consolidated_data_pull, 0, gson.toJson(response, ResponseObject.class));

            CommonUtility.uploadLogMessage("Calling pushLocalDataToRemoteServer from ConsolidatedDataPullTask");
            SyncManager.pushLocalDataToRemoteServer(context, CommonUtility.getDriverNumberAsInt(context),false);
        } catch (Exception e) {
            log.debug(Logs.DEBUG, "Failed to get driver data update: " + e.getMessage());
            e.printStackTrace();
            // Consider logging some information to Crashlytics here.
        }

        return null;
    }

    @Override
    public void onPostExecute(Void nothing) {
        CommonUtility.dispatchLogThreadStartStop("Completed ConsolidatedDataPullTask", false);
        if(_callback != null) {
            _callback.complete();
        }
    }

    class BooleanTypeAdapter implements JsonDeserializer<Boolean> {
        public Boolean deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            int code = 0;
            if (typeOfT == String.class) {
                code = Integer.parseInt(json.getAsString());
            } else if (typeOfT == int.class) {
                code = json.getAsInt();
            } else if (typeOfT == boolean.class) {
                code = json.getAsInt();
            } else {
                code = 1;
            }
            return code == 1;
        }
    }

    public static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    class DateTypeAdapter implements JsonDeserializer<Date> {
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String code = json.getAsString();
            try {
                return formatter.parse(code);
            } catch (ParseException e) {
                return null;
            }
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if(_callback != null && values != null && values.length > 0) {
            _callback.updateProgress(values[0]);
        }

    }
}
