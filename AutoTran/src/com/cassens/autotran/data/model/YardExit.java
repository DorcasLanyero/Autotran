package com.cassens.autotran.data.model;

import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.model.lookup.ScacCode;
import com.cassens.autotran.data.model.lookup.Terminal;

public class YardExit {
  public int yard_exit_id;
  public Terminal terminal;
  public String inspector;
  public String VIN;
  public ScacCode scacCode;
  public boolean inbound = true;
  public int uploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED;
   
  public YardExit() {
      yard_exit_id = -1; // Initialize to -1 so SQL autoincrement will set it.
  }
}
