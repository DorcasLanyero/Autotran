package com.cassens.autotran.data.model.lookup;

import java.util.Date;

public class ScacCode {
  public int scac_code_id;
  private String code;
  public int terminal_id;
  private String description;
  public Date modified;
  public boolean active=true;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
