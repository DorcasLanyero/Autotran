package com.cassens.autotran.data.model.lookup;

import com.google.gson.annotations.SerializedName;
import com.sdgsystems.util.HelperFuncs;

import java.util.Date;

public class LotCode {

  @SerializedName("id")
  public int lot_code_id;

  public String code;
  public int terminal_id;
  public String description;

  @SerializedName("shuttle_move_code")
  public String shuttleMoveCode;
  public Date modified;
  public int active;

  public boolean codesMatch(LotCode lc) {
      if (this.lot_code_id == 0 || lc.lot_code_id == 0) {
          return true;
      }
      else {
          return HelperFuncs.noNull(this.code).equalsIgnoreCase(HelperFuncs.noNull(lc.code));
      }
  }
}
