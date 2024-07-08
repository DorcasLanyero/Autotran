package com.cassens.autotran.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import com.cassens.autotran.CodeEntry;
import com.cassens.autotran.CodeEntryCallback;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.data.adapters.CodeEntryAdapter;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.lookup.Terminal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class SetTerminalActivity extends AutoTranActivity implements CodeEntryCallback {
  private static final int REQ_CODE_TERMINAL = 0;

  private static final Logger log = LoggerFactory.getLogger(SetTerminalActivity.class.getSimpleName());

  @Override
  public Logger getLogger() {
      return log;
  }

  ListView list;
  CodeEntryAdapter adapter;
  ArrayList<CodeEntry> codeEntryList = new ArrayList<CodeEntry>();

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_set_defaults);
      
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      EditText defaultTerminal = (EditText) findViewById(R.id.defaultTerminal);
      
      defaultTerminal.setText(CommonUtility.getDefaultTerminalPref(this));

      list = (ListView) findViewById(R.id.list);

      ArrayList<Terminal> terminalList = (ArrayList<Terminal>) DataManager.getTerminalList(this);

      if (terminalList != null) {

          log.debug(Logs.DEBUG, "Getting terminal list: " + terminalList.size());
          //Log.d("narf", "Getting terminal list: " + terminalList.size());
          for (Terminal terminal : terminalList) {
              log.debug(Logs.DEBUG, "terminal=" + terminal.terminal_id);
              CodeEntry codeEntry = new CodeEntry();

              codeEntry.id = terminal.terminal_id;
              codeEntry.description = terminal.description;
              codeEntryList.add(codeEntry);
          }
          adapter = new CodeEntryAdapter(this, R.layout.horizontal_list_with_color_text, codeEntryList, this);
          list.setAdapter(adapter);
      }
  }
  
  public void okClicked(View v) {
      CommonUtility.logButtonClick(log, v);
    try {
        CommonUtility.setDefaultTerminalPref(this, ((EditText) findViewById(R.id.defaultTerminal)).getText().toString());
        finish();
    }
    catch (NumberFormatException ne) {
        CommonUtility.simpleMessageDialog(this, "Invalid terminal specified");
    }
  }

  public void clearClicked(View v) {
      CommonUtility.logButtonClick(log, v);
          ((EditText) findViewById(R.id.defaultTerminal)).setText("");
  }
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
      super.onActivityResult(requestCode, resultCode, data);
      
      Intent intent = new Intent();
      
      if (resultCode == RESULT_OK) {
          if (requestCode == REQ_CODE_TERMINAL) {
              String code = data.getStringExtra("code");
              ((EditText) findViewById(R.id.defaultTerminal)).setText(code);
          }
      }
  } 
  
  public void back(View v) {
          this.finish();
  }

    @Override
    public void callbackCall(String id, String description) {
        ((EditText) findViewById(R.id.defaultTerminal)).setText(id);
    }
}
