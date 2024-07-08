package com.cassens.autotran.data.model;

import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.model.lookup.LotCode;
import com.cassens.autotran.data.model.lookup.Terminal;

public class LoadEvent {
    public int load_event_id;
    public String csv;
    public int uploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED;

    public LoadEvent() {
        load_event_id = -1; // Initialize to -1 so SQL autoincrement will set it.
    }

    public com.cassens.autotran.data.model.dto.LoadEvent getDTO(String download_status) {
        com.cassens.autotran.data.model.dto.LoadEvent dto = new com.cassens.autotran.data.model.dto.LoadEvent();
        dto.setId(this.load_event_id);
        dto.setCsv(this.csv);
        dto.setDownload_status(download_status);

        return dto;
    }

}