package com.sdgsystems.util;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

public class AuditResponse {

    @Expose
    public int questionnaireVersion;

    @Expose
    public List<Check> checkList;

    @Expose
    public String notes;

    public AuditResponse() {
    }

    public AuditResponse(int questionnaireVersion, List<Check> checkList, String notes) {
        this.questionnaireVersion = questionnaireVersion;
        this.checkList = checkList;
        this.notes = notes;
    }
}
