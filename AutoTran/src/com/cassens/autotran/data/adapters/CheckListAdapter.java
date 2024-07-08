package com.cassens.autotran.data.adapters;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.cassens.autotran.R;
import com.sdgsystems.util.Check;
import com.sdgsystems.util.ExtendedEditText;

import java.util.List;

/**
 * Created by adam on 5/20/16.
 */
public class CheckListAdapter extends ArrayAdapter<Check> {
    private Context context;
    private int layoutResourceId;
    private List<Check> checks;
    private boolean locked = false;
    LayoutInflater inflater;

    public CheckListAdapter(Context context, int textViewResourceId, List<Check> checks) {
        super(context, textViewResourceId, checks);
        this.context = context;
        this.layoutResourceId = textViewResourceId;
        this.checks = checks;
        inflater = ((Activity) context).getLayoutInflater();
    }

    @Override
    public View getView(final int position, View row, ViewGroup parent) {
        if (row == null) {
            row = inflater.inflate(layoutResourceId, parent, false);
        }

        final Check currentCheck = this.checks.get(position);
        final CheckBox checkBox = (CheckBox) row.findViewById(R.id.checkBox);
        final ExtendedEditText noteBox = (ExtendedEditText) row.findViewById(R.id.noteBox);

        checkBox.setOnCheckedChangeListener(null);
        noteBox.clearTextChangedListeners();
        checkBox.setFocusable(false);

        checkBox.setText(currentCheck.prompt);
        checkBox.setTag(currentCheck.id);
        checkBox.setChecked(currentCheck.getMarked());

        String oldText = currentCheck.note;
        noteBox.setText(oldText == "" ? "" : oldText);
        noteBox.setVisibility(currentCheck.showNote ? View.VISIBLE : View.GONE);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (locked) {
                    compoundButton.setChecked(!b);
                } else {
                    currentCheck.setMarked(b);
                    if(!currentCheck.getMarked()) {
//                        noteBox.setVisibility(View.VISIBLE);
//                        currentCheck.showNote = true;
                    } else {
                        noteBox.setVisibility(View.GONE);
                        currentCheck.showNote = false;
                    }
                }
            }
        });

        noteBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                currentCheck.note = editable.toString();
            }
        });

        return row;
    }

    public void Lock() {
        this.locked = true;
    }

    public void Unlock() {
        this.locked = false;
    }

    public boolean allChecksMarked() {
        for (Check c : this.checks) {
            if (!c.getMarked()) {
                return false;
            }
        }
        return true;
    }

    public boolean checklistComplete() {
        for (Check c : this.checks) {
            if (!c.getMarked() && !c.hasNotes()) {
                return false;
            }
        }
        return true;
    }


    public List<Check> getCheckListResults() {
        return this.checks;
    }

    @Override
    public int getCount()
    {
        return this.checks.size();
    }
}
