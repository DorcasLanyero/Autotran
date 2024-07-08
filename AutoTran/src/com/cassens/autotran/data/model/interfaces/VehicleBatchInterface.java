package com.cassens.autotran.data.model.interfaces;

import android.content.Context;

import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.Image;

import java.util.List;


public interface VehicleBatchInterface {
    String getId();
    String getRemoteId();

    void save(Context ctx);

    List<DeliveryVin> getDeliveryVinList();
    List<DeliveryVin> getDeliveryVinList(boolean includeDeleted);

    List<Image> getImages();

    String getNotes();
    void setNotes(String s);

    boolean getUploaded();
    void setUploaded(boolean b);
}
