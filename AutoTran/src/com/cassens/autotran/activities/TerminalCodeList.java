package com.cassens.autotran.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.cassens.autotran.CodeEntry;
import com.cassens.autotran.CodeEntryCallback;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.adapters.CodeEntryAdapter;
import com.cassens.autotran.data.model.lookup.Terminal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Project : AUTOTRAN Description : DamageCodeList class show list of area code main
 * 
 * @author Hemant Creation Date : 12-11-2013
 */
public class TerminalCodeList extends AutoTranActivity implements CodeEntryCallback
{
    private static final Logger log = LoggerFactory.getLogger(TerminalCodeList.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	ListView list;
	TextView title;
    TextView prompt;
	CodeEntryAdapter adapter;
	ArrayList<CodeEntry> codeEntryList = new ArrayList<CodeEntry>();

	public static final String EXTRA_IS_SHUTTLE_LIST = "is_shuttle_list";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		boolean isShuttleList = getIntent().getBooleanExtra(EXTRA_IS_SHUTTLE_LIST, false);

		setContentView(R.layout.code_list);
		list = (ListView) findViewById(R.id.list);
        title = (TextView) findViewById(R.id.title);
        prompt = (TextView) findViewById(R.id.prompt);
		
		ArrayList<Terminal> terminalList;
		if (isShuttleList) {
			terminalList = (ArrayList<Terminal>) DataManager.getShuttleTerminalList(this);
		}
		else {
			terminalList = (ArrayList<Terminal>) DataManager.getTerminalList(this);
		}

		prompt.setText("Enter Terminal #");
		
        if (terminalList != null) {
            
            log.debug(Logs.DEBUG, "Getting terminal list: " + terminalList.size());
            for (Terminal terminal: terminalList) {
                    log.debug(Logs.DEBUG, "terminal=" + terminal.terminal_id);
                    CodeEntry codeEntry = new CodeEntry();

                    codeEntry.id = terminal.terminal_id;
                    codeEntry.description = terminal.description;
                    codeEntryList.add(codeEntry);
            }
            adapter = new CodeEntryAdapter(TerminalCodeList.this, R.layout.horizontal_list_with_color_text, codeEntryList, this);
            list.setAdapter(adapter);
        }
	}

	public void menuList(View v)
	{
		// TODO Auto-generated method stub

	}

	public void back(View v)
	{
	    Intent i = new Intent();
        setResult(RESULT_CANCELED, i);
		finish();
	}

	@Override
	protected void onStart()
	{
		// TODO Auto-generated method stub
		super.onStart();
	}

	@Override
	public void callbackCall(String id, String description) {
		Intent intent = new Intent();
		intent.putExtra("code", id);
		intent.putExtra("description", description);
		setResult(RESULT_OK, intent);
		finish();
	}


}
