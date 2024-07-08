package com.cassens.autotran.data.model;

import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.model.lookup.Terminal;

public class ReceivedVehicle {
  public int received_vehicle_id = -1;
  public Terminal terminal;
  public String inspector;
  public String VIN;
  public int uploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED;
}
