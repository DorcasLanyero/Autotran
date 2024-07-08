package com.cassens.autotran.data.model;

import java.util.Date;

/**
 * Created by adam on 7/7/16.
 */
public class LotCodeMessage {
    public int id;
    public int lot_code_id;
    public String message;
    public String prompt;
    public String response;
    public Date modified;
    public boolean active = false;

    public String getMessage() {
        return message;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getResponse() {
        return response;
    }
}
