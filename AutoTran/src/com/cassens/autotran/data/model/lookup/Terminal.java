package com.cassens.autotran.data.model.lookup;

import java.util.ArrayList;
import java.util.Date;

public class Terminal {
  public int terminal_id;
  public String description;
  public String popupMessage;
  public String phoneNumber;
  public String usToCanPhoneNumber;
  public String canToUsPhoneNumber;
  public String dispatchPhoneNumber;
  public String countryCode;

  //Used for custom row/bay keyboards
  public ArrayList<String> rowCharacters;
  public ArrayList<String> bayCharacters;
  public Date modified;
  public boolean active=true;
  
  public Terminal() {
    rowCharacters = new ArrayList<String>();
    bayCharacters = new ArrayList<String>();
  }
}
