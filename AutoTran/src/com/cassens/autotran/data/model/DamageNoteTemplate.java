package com.cassens.autotran.data.model;

import java.util.Date;

public class DamageNoteTemplate {
    public int id;
    public String comment;
    public String driver_prompt;
    public String driver_prompt_type;
    public String dealer_prompt;
    public String dealer_prompt_type;
    public String area_code;
    public String type_code;
    public String severity_code;
    public String mfg;
    public String originTerminal;
    public Date modified;
    public int active = 0;

    public DamageNoteTemplate() {}
}
