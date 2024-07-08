package com.cassens.autotran.data.model;

import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.model.lookup.Terminal;

public class PlantReturn {
  public int plant_return_id;
  public Terminal terminal;
  public String inspector;
  public String delayCode;
  public String VIN;
  public int uploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED;
  
  public PlantReturn() {
      plant_return_id = -1; // Initialize to -1 so SQL autoincrement will set it.
  }
}
