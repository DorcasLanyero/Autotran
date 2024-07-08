package com.cassens.autotran.activities;

import static com.sdgsystems.util.HelperFuncs.hideSoftKeyboard;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.data.local.DataManager;
import com.sdgsystems.util.Check;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class NotesActivity extends AutoTranActivity implements OnClickListener
{
	private static final Logger log = LoggerFactory.getLogger(NotesActivity.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

    // State values which control the notes list displayed.
    public static final int GENERIC_NOTES = 0;
    public static final int PRELOAD_VIN_DAMAGE = 1;
    public static final int PRELOAD_SUPERVISOR_VIN_SIGNOFF = 2;
    public static final int PRELOAD_DRIVER_LOAD_SIGNOFF = 3;
    public static final int DELIVERY_VIN_DAMAGE = 4;
    public static final int DELIVERY_VIN_REJECTION = 5;
    public static final int DELIVERY_DEALER_SIGNOFF = 6;
    public static final int DELIVERY_DRIVER_SIGNOFF = 7;
    public static final int GENERIC_VIN_DAMAGE = 8;
    public static final int HIGH_CLAIMS_AUDIT = 9;

	// Intent Extra keys
	public static final String EXTRA_IS_EDITABLE = "is_editable";
	public static final String EXTRA_STATE = "state";
	public static final String EXTRA_TITLE = "title";
	public static final String EXTRA_PROMPT = "prompt";
	public static final String EXTRA_OLD_NOTES = "old_notes";
	public static final String EXTRA_NOTES = "notes";
	public static final String EXTRA_MFG = "mfg";
	public static final String EXTRA_MAX_LENGTH = "maxLength";
	public static final String EXTRA_IS_REQUIRED = "is_required";
	public static final String EXTRA_TOP_BAR_COLOR = "top_bar_color";

    // Request Codes for Launched Activities
    private static final int REQ_CODE_NOTES_LIST = 1001;

    private EditText notesEditText;
	private int notesMaxLen;
	private int topBarColor = -1;
	private TextView charCount;

    int state;
    boolean isEditable;
    boolean requireNote;
    String mfg;
    String initialNotesText = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notes);
		
		Intent intent = getIntent();
	        
		isEditable = intent.getBooleanExtra(EXTRA_IS_EDITABLE, true);

		requireNote = intent.getBooleanExtra(EXTRA_IS_REQUIRED, false);

        state = intent.getIntExtra(EXTRA_STATE, GENERIC_NOTES);

        String checkPrompts = "";

        if(state == HIGH_CLAIMS_AUDIT) {
        	ArrayList<Check> checks = intent.getParcelableArrayListExtra("checks");

        	for(Check check : checks) {
        		if(!check.getMarked()) {
        			if(checkPrompts.length() > 0) {
        				checkPrompts += "\n";
					}

					checkPrompts += "X - " + check.prompt;
				}
			}

			findViewById(R.id.prompt_list).setVisibility(View.VISIBLE);
			((TextView)findViewById(R.id.prompt_list)).setText(checkPrompts);
			((Button) findViewById(R.id.btn_notes_predefined)).setVisibility(View.GONE);
		}

		
		((ImageView) findViewById(R.id.img_back)).setOnClickListener(this);
		if (isEditable) {             
            ((Button) findViewById(R.id.btn_notes_save)).setOnClickListener(this);
            ((Button) findViewById(R.id.btn_notes_predefined)).setOnClickListener(this);
		    ((ImageView) findViewById(R.id.img_menu)).setOnClickListener(this);

            if(state == PRELOAD_DRIVER_LOAD_SIGNOFF || state == PRELOAD_SUPERVISOR_VIN_SIGNOFF) {
                ((Button) findViewById(R.id.btn_notes_predefined)).setVisibility(View.GONE);
                ((ImageView) findViewById(R.id.img_menu)).setVisibility(View.GONE);
            }
		}
		else {
            hideSoftKeyboard(this);
		    ((Button) findViewById(R.id.btn_notes_save)).setVisibility(View.GONE);
		    ((ImageView) findViewById(R.id.img_menu)).setVisibility(View.GONE);
			((Button) findViewById(R.id.btn_notes_predefined)).setVisibility(View.GONE);
		}
		

		String str = intent.getStringExtra(EXTRA_TITLE);
		if (str != null) {
		    ((TextView) findViewById(R.id.ACTIVITY_TITLE)).setText(str);
		}
		
		TextView tv = (TextView) findViewById(R.id.ACTIVITY_PROMPT);
		if (isEditable) {
            str = intent.getStringExtra(EXTRA_PROMPT);
            if (str != null) {
                tv.setText(str);
            }
            tv.setVisibility(View.VISIBLE);
		}
		else {
		    tv.setVisibility(View.GONE);
		}

		String oldNotes = intent.getStringExtra(EXTRA_OLD_NOTES);
		if (!HelperFuncs.isNullOrEmpty(oldNotes)) {
			//((TextView) findViewById(R.id.oldNotes)).setText(oldNotes);
			TextView oldNotesTv = (TextView)findViewById(R.id.oldNotes);
			oldNotesTv.setText(formatPredefNotesForTextView(oldNotes));
			oldNotesTv.setMovementMethod(new ScrollingMovementMethod());
			oldNotesTv.setVisibility(View.VISIBLE);
		}

		notesEditText = (EditText) findViewById(R.id.et_notes_message);
		notesMaxLen = intent.getIntExtra(EXTRA_MAX_LENGTH, getResources().getInteger(R.integer.max_note_length));
		topBarColor = intent.getIntExtra(EXTRA_TOP_BAR_COLOR, -1);
		if (topBarColor >= 0) {
			setTopBarColor(R.id.lin_layout, R.id.img_back, R.drawable.back_button_dealer, R.id.img_menu, R.drawable.menu_icon_dealer, R.color.DealerIndicatorColor);
		}

		if (isEditable) {
			initialNotesText = unmarkupPredefNotes(intent.getStringExtra(EXTRA_NOTES));
			notesEditText.setText(initialNotesText);
			notesEditText.requestFocus();
			setNotesMaxLen(notesMaxLen);
			notesEditText.setMaxLines(20);
			// Surprisingly, there's no way to get the maxLen of an EditText field programmatically,
			// so we have to retrieve it from a resource.
			notesEditText.setHint("Add notes here (" + notesMaxLen + " characters remaining)...");
			charCount = (TextView) findViewById(R.id.charCount);
			charCount.setVisibility(View.VISIBLE);
			updateNotesCharCount();
			notesEditText.addTextChangedListener(notesMessageTextWatcher);
			if (CommonUtility.isHoneywellLargeDisplaySet()) {
				hideSoftKeyboard(this);
			}
		}
		else {
			notesEditText.setKeyListener(null);
			notesEditText.setFocusable(false);
			notesEditText.setCursorVisible(false);
			notesEditText.setText(formatPredefNotesForTextView(intent.getStringExtra(EXTRA_NOTES)));
			hideSoftKeyboard(this);
		}
		mfg = intent.getStringExtra(EXTRA_MFG);
	}

	private void updateNotesCharCount() {
		int remaining = notesMaxLen - notesEditText.length();
		charCount.setText(remaining + " characters remaining");
		if (remaining <= 0) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			//builder.setTitle((title == null) ? "" : title);
			builder.setMessage("No additional notes allowed. Total notes size limit has been reached.");
			builder.setPositiveButton("Ok", null);
			builder.setCancelable(true);
			builder.create().show();
		}
	}

	private final TextWatcher notesMessageTextWatcher = new TextWatcher() {
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			//This sets a textview to the current length
		}

		public void onTextChanged(CharSequence s, int start, int before, int count) {
		}

		public void afterTextChanged(Editable s) {
			updateNotesCharCount();
		}
	};

	@Override
	public void onClick(View v)
	{
	    Intent intent;

		int id = v.getId();
		if (id == R.id.img_back) {
			CommonUtility.logButtonClick(log, "Back");
			this.onBackPressed();
			return;
		}

		CommonUtility.logButtonClick(log, v);
		if (id == R.id.btn_notes_save) {
			String notes = notesEditText.getText().toString();
			if (!isNoteUpdated()) {
				log.debug(Logs.INTERACTION, "User made no updates to notes");
				setResultAndFinish();
				return;
			}
			if (!notes.equals("")) {
				notes = markupPredefNotes(notes);
			} else if (requireNote) {
				new AlertDialog.Builder(NotesActivity.this)
						.setMessage("Please enter an explanation before continuing.")
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								return;
							}
						}).show();
				return;
			}
			intent = new Intent();
			intent.putExtra("notes", notes);
			this.setResult(RESULT_OK, intent);
			this.finish();
		} else if (id == R.id.img_menu || id == R.id.btn_notes_predefined) {
		    ArrayList<String> noteList = getNoteList(state);
			intent = new Intent(NotesActivity.this, NotesListActivity.class);
			intent.putExtra(NotesListActivity.EXTRA_TITLE, "Notes");
			intent.putExtra(NotesListActivity.EXTRA_OPTIONS, noteList.toArray(new String[0]));
			if (topBarColor >= 0) {
				intent.putExtra(NotesActivity.EXTRA_TOP_BAR_COLOR, R.color.DealerIndicatorColor);
			}
			startActivityForResult(intent, REQ_CODE_NOTES_LIST);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK)
		{
			int newLength = notesEditText.getText().toString().length() + data.getStringExtra(NotesListActivity.RESPONSE_SELECTION).length() + PREDEF_PREFIX.length() + 1;
			if (newLength > notesMaxLen) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                //builder.setTitle((title == null) ? "" : title);
                builder.setMessage("Selected note will cause comment to exceed maximum character limit");
                builder.setPositiveButton("Ok", null);
                builder.setCancelable(true);
                builder.create().show();
                return;
			}
			notesMaxLen -= PREDEF_PREFIX.length();
		    if (notesEditText.getText().toString().trim().length() > 0) {
		        notesEditText.append("\n");
		    }
			notesEditText.append(data.getStringExtra(NotesListActivity.RESPONSE_SELECTION));
			setNotesMaxLen(notesMaxLen);
			updateNotesCharCount();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private ArrayList<String> getNoteList(int state) {
		ArrayList<String> noteList;

		switch (state) {
			case PRELOAD_VIN_DAMAGE:
			case DELIVERY_VIN_DAMAGE:
			case GENERIC_VIN_DAMAGE:
				noteList = DataManager.getPredefinedNotesByMfg(this, true, false, mfg);
				break;

			case PRELOAD_SUPERVISOR_VIN_SIGNOFF:
			case PRELOAD_DRIVER_LOAD_SIGNOFF:
				noteList = DataManager.getPredefinedNotesByMfg(this, false, true, mfg);
				break;

//		    case DELIVERY_VIN_REJECTION:
//                break;

			case DELIVERY_DEALER_SIGNOFF:
			case DELIVERY_DRIVER_SIGNOFF:
				noteList = DataManager.getPredefinedNotesByMfg(this, false, true, mfg);
				break;

			case GENERIC_NOTES:
			default:
				noteList = new ArrayList<>(); //check this
		}
		return noteList;
	}

	private static final String PREDEF_PREFIX="#";

	private String markupPredefNotes(String notes) {
		if (notes == null) {
			return null;
		}
		StringBuilder str = new StringBuilder("");
		String[] lines = notes.split("\n");

		ArrayList<String> noteList = getNoteList(state);
		if (noteList.size() == 0) {
			return notes;
		}

		int i = lines.length;
		for (String line : lines) {
			if (noteList.contains(line)) {
				str.append(PREDEF_PREFIX + line);
			}
			else {
				str.append(line);
			}
			if (--i > 0) {
				str.append("\n");
			}
		}
		return str.toString();
	}


	private String unmarkupPredefNotes(String notes) {
		if (notes == null) {
			return "";
		}
		StringBuilder str = new StringBuilder("");
		String[] lines = notes.split("\n");
		int prefLen = PREDEF_PREFIX.length();
		int i = lines.length;
		for (String line : lines) {
			if (line.length() > prefLen && line.substring(0, prefLen).equalsIgnoreCase(PREDEF_PREFIX)) {
				str.append(line.substring(PREDEF_PREFIX.length()));
				notesMaxLen -= PREDEF_PREFIX.length();
			}
			else {
				str.append(line);
			}
			if (--i > 0) {
				str.append("\n");
			}
		}
		return str.toString();
	}

	public static Spanned formatPredefNotesForTextView(String notes) {
		if (notes == null) {
			return null;
		}
		//StringBuilder str = new StringBuilder("\"![CDATA[");
		StringBuilder str = new StringBuilder("");
		String[] lines = notes.split("\n");
		int prefLen = PREDEF_PREFIX.length();
		int i = lines.length;
		for (String line : lines) {
			if (line.length() > prefLen && line.substring(0, prefLen).equalsIgnoreCase(PREDEF_PREFIX)) {
				str.append("<font color=\"blue\">" + line.substring(PREDEF_PREFIX.length()) + "</font>");
			}
			else {
				str.append(line);
			}

			if (--i > 0) {
				str.append("<br>");
			}
		}
		//str.append("]]");

		return Html.fromHtml(str.toString());
	}

	private void setNotesMaxLen(int maxLen) {
		notesEditText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLen)});
	}

	boolean isDealerMode() {
		return topBarColor >= 0;
	}

	private void setResultAndFinish() {
		this.setResult(RESULT_CANCELED, new Intent());
		this.finish();
	}

	private boolean isNoteUpdated() {
		if (isEditable) {
			String initialNote = HelperFuncs.isNullOrWhitespace(initialNotesText) ? "" : initialNotesText;
			String currentNote = notesEditText.getText().toString();
			currentNote = HelperFuncs.noNull(currentNote);
			return !currentNote.equalsIgnoreCase(initialNote);
		}
		else {
			return false;
		}
	}

	@Override
	public void onBackPressed()
	{
		if (isNoteUpdated()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(NotesActivity.this);
			builder.setMessage("Do you wish leave this screen and discard your changes?");
			builder.setPositiveButton("Discard", (dialog, which) -> setResultAndFinish());
			builder.setNegativeButton("Cancel", null);
			builder.setCancelable(true);
			builder.create().show();
		}
		else {
			setResultAndFinish();
		}
	}
}
