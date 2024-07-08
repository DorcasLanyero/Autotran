package com.cassens.autotran.data.model;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.gson.annotations.Expose;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class TrainingRequirement {
    @SuppressLint("SimpleDateFormat")
    private static final DateFormat ISO_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // For internal bookkeeping only.
    public int uploaded;
    public int adHoc;

    @Expose public int id;
    @Expose public String supervisor_id; // who signed off?
    @Expose public String load_id;
    @Expose public String user_id;
    @Expose public int type;
    @Expose public int progress;
    @Expose public int requiredProgress;
    @Expose public String vin;
    @Expose private String supplementalReference;

    public String getSupplementalReference() {
        return supplementalReference;
    }

    public void setSupplementalReference(String supplementalReference) {
        this.supplementalReference = supplementalReference;
        if(this.supplementalReference != null && this.supplementalReference.length() > 256) {
            this.supplementalReference = this.supplementalReference.substring(0, 256);
        }
    }

    @Expose public String supplementalData;
    @Expose public double startedLatitude;
    @Expose public double startedLongitude;
    @Expose public double completedLatitude;
    @Expose public double completedLongitude;

    @Expose public Date assigned;
    
    public String getAssignedAsTimestamp() {
        if(assigned == null) return null;
        return ISO_DATE.format(assigned);
    }
    
    public void setAssignedFromTimestamp(String timestamp) {
        if(timestamp == null || timestamp.equals("null")) {
            assigned = null;
            return;
        }
        try {
            assigned = ISO_DATE.parse(timestamp);
        } catch (ParseException e) {
            throw new IllegalArgumentException();
        }
    }

    @Expose public Date started;

    public String getStartedAsTimestamp() {
        if(started == null) return null;
        return ISO_DATE.format(started);
    }

    public void setStartedFromTimestamp(String timestamp) {
        if(timestamp == null || timestamp.equals("null")) {
            started = null;
            return;
        }
        try {
            started = ISO_DATE.parse(timestamp);
        } catch (ParseException e) {
            throw new IllegalArgumentException();
        }
    }


    @Expose public Date completed;

    public String getCompletedAsTimestamp() {
        if(completed == null) return null;
        return ISO_DATE.format(completed);
    }

    public void setCompletedFromTimestamp(String timestamp) {
        if(timestamp == null || timestamp.equals("null")) {
            completed = null;
            return;
        }
        try {
            completed = ISO_DATE.parse(timestamp);
        } catch (ParseException e) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof TrainingRequirement)) {
           return false;
        }

        // I think this is a sufficient set of fields—the others are for
        // reference/completion purposes, and shouldn't be reset by
        // server-side changes.
        TrainingRequirement other = (TrainingRequirement) obj;
        return
                this.id == other.id
                && (Objects.equals(this.supervisor_id, other.supervisor_id))
                && (Objects.equals(this.load_id, other.load_id))
                && (Objects.equals(this.user_id, other.user_id))
                && this.type == other.type
                && this.progress == other.progress
                && this.requiredProgress == other.requiredProgress
                && Objects.equals(this.assigned, other.assigned)
                && Objects.equals(this.started, other.started)
                && Objects.equals(this.completed, other.completed);
    }

    public static class ByStatus {
        public List<TrainingRequirement> finished = new ArrayList<>();
        public List<TrainingRequirement> unfinished = new ArrayList<>();

        @Override
        public String toString() {
            return "Finished: " + finished.size() + " Unfinished: " + unfinished.size();
        }
    }

    public static ByStatus filterList(List<TrainingRequirement> requirements) {
        ByStatus result = new ByStatus();

        for(TrainingRequirement r : requirements) {
            if(r.completed != null) result.finished.add(r);
            else result.unfinished.add(r);
        }

        return result;
    }
}
