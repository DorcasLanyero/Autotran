package com.cassens.autotran.data.model;

import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.model.lookup.LotCode;
import com.cassens.autotran.data.model.lookup.Terminal;

public class YardInventory {
    public int yard_inventory_id;
    public Terminal terminal;
    public String inspector;
    public String VIN;
    public LotCode lotCode;
    public String row;
    public String bay;
    public Double latitude;
    public Double longitude;
    public String ldnbr;
    public int delivery_vin_id = -1;
    public int uploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED;
    public boolean lotLocate = false;

    public YardInventory() {
        yard_inventory_id = -1; // Initialize to -1 so SQL autoincrement will set it.
    }

    public com.cassens.autotran.data.model.dto.YardInventory getDTO(String download_status) {
        com.cassens.autotran.data.model.dto.YardInventory dto = new com.cassens.autotran.data.model.dto.YardInventory();
        dto.setId(this.yard_inventory_id);
        dto.setInspector(this.inspector);
        if (this.terminal != null) {
            dto.setTerminal_id(this.terminal.terminal_id);
        }
        dto.setVIN(this.VIN);
        dto.setLot_locate(this.lotLocate);
        dto.setBay(this.bay);
        dto.setRow(this.row);

        if (this.lotCode != null) {
            dto.setLot_code_id(this.lotCode.lot_code_id);
        }

        if(this.latitude != null) {
            dto.setLatitude(this.latitude);
        }

        if(this.longitude != null) {
            dto.setLongitude(this.longitude);
        }

        if(this.ldnbr != null) {
            dto.setLdnbr(this.ldnbr);
        }

        dto.setDownload_status(download_status);

        return dto;
    }

}