package com.sdgsystems.util;

import android.os.Parcel;
import android.os.Parcelable;
import android.widget.EditText;

import com.cassens.autotran.CommonUtility;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by adam on 5/20/16.
 */
public class Check implements Parcelable {
    @Expose
    @SerializedName("i")
    final public int id;

    @Expose
    @SerializedName("question")
    final public String prompt;

    @Expose
    @SerializedName("m")
    private boolean marked = false;

    //@Expose
    public String note;

    public boolean showNote;

    public Check(int id, String prompt, Boolean marked) {
        this.id = id;
        this.prompt = prompt;
        this.marked = marked != null && marked;
        note = "";
        showNote = false;
    }

    public Check(Parcel parcel) {
        this.id = parcel.readInt();
        this.marked = (parcel.readInt() == 1);
        this.prompt = parcel.readString();
    }

    public boolean getMarked() {
        return this.marked;
    }

    public void setMarked(boolean mark) {
        this.marked = mark;
    }

    public boolean hasNotes() {
        return !CommonUtility.isNullOrBlank(note);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeInt(marked ? 1 : 0);
        parcel.writeString(prompt);
    }

    public static final Parcelable.Creator<Check> CREATOR
            = new Parcelable.Creator<Check>() {

        // This simply calls our new constructor (typically private) and
        // passes along the unmarshalled `Parcel`, and then returns the new object!
        @Override
        public Check createFromParcel(Parcel in) {
            return new Check(in);
        }

        // We just need to copy this and change the type to match our class.
        @Override
        public Check[] newArray(int size) {
            return new Check[size];
        }
    };
}

