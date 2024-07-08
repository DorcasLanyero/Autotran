package com.cassens.autotran.data.model;

import android.content.Context;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.model.lookup.LotCode;
import com.cassens.autotran.data.model.lookup.ScacCode;
import com.cassens.autotran.data.model.lookup.Terminal;

public class Inspection {

    public int inspection_id = -1;
    public String vin;
    public String guid;
    public String notes;
    public String inspector;

    public Terminal terminal;
    public LotCode lotCode;
    public int type = Constants.INSPECTION_TYPE_GATE;
    public ScacCode scacCode;
    public ArrayList<Image> images;
    public int imageCount;
    public ArrayList<Damage> damages;
    public int damageCount;
    public Double latitude;
    public Double longitude;
    public Date timestamp;

    public int uploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED;


    public Inspection() {
        this.guid = UUID.randomUUID().toString();
        images = new ArrayList<>();
        damages = new ArrayList<>();
    }

    public com.cassens.autotran.data.model.dto.Inspection getDTO(Context context) {
        com.cassens.autotran.data.model.dto.Inspection dto = new com.cassens.autotran.data.model.dto.Inspection();

        dto.setInspection_id(this.inspection_id);
        dto.setVin(this.vin);
        dto.setGuid(this.guid);
        dto.setNotes(this.notes);
        dto.setInspector(this.inspector);
        dto.setTerminal(this.terminal);
        dto.setLotCode(this.lotCode);
        dto.setScacCode(this.scacCode);
        dto.setImageCount(this.imageCount);
        dto.setDamageCount(this.damageCount);
        dto.setLatitude(this.latitude);
        dto.setLongitude(this.longitude);
        dto.setTimestamp(this.timestamp);

        for(Damage dmg : this.damages) {
            com.cassens.autotran.data.model.dto.Damage dmgDto = dmg.getDTO();
            dmgDto.setGuid(this.guid);
            dto.damages.add(dmgDto);
        }

        for (Image image : images) {
            com.cassens.autotran.data.model.dto.Image imageDto = image.getDTO(context);
            imageDto.setInspection_guid(this.guid);
            dto.images.add(imageDto);
        }

        return dto;
    }
}
