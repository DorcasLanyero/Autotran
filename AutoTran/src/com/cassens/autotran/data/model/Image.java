package com.cassens.autotran.data.model;

import android.content.Context;

import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.sdgsystems.util.HelperFuncs;

import java.util.Comparator;
import java.util.List;

public class Image {
	public int image_id;
	public int delivery_vin_id;
	public String inspection_guid;
	public int load_id;
	public int delivery_id;
	public String problem_report_guid;
	public boolean uploaded;
	public String imageLat;
	public String imageLon;
	public String filename;
	public boolean preloadImage;
	public int foreignKey;
	public String foreignKeyLabel;
	public String preauth_url;
	public int retries;

	public int preloadUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_PRELOAD;
	public int deliveryUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_DELIVERY;
	public int uploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED;
	public int s3_upload_status = Constants.SYNC_STATUS_NOT_READY_FOR_UPLOAD;

	public Image() {
	    image_id = -1;
	    delivery_vin_id = -1;
		load_id = -1;
		delivery_id = -1;
		uploaded = false;
		preloadImage = false;
		foreignKey = -1;
		filename = null;
		preauth_url = "";
		retries = 0;
	}

	@Override
	public boolean equals(Object other) {
		Image otherImage;
		try {
			otherImage = (Image) other;
		} catch (Exception e) {
			return false;
		}
		
		if(this.image_id != -1 && otherImage.image_id != -1) {
			return this.image_id == otherImage.image_id;
		} else if (this.filename != null && otherImage.filename != null) {
			return this.filename.equals(otherImage.filename);
		} else {
			return false;
		}
	}

	public boolean isHires() {
	    return this.filename.endsWith("_hires");
    }

	public static Comparator<Image> HiResLast = new Comparator<Image>() {
	    public int compare(Image image1, Image image2) {
	        if (image1.isHires() == image2.isHires()) {
	            return 0;
            }
            else if (image1.isHires()) {
	            return 1;
            }
            else {
                return -1;
            }
        }
    };

	public com.cassens.autotran.data.model.dto.Image  getDTODetails(Context context) {
		com.cassens.autotran.data.model.dto.Image dto = new com.cassens.autotran.data.model.dto.Image();

		dto.setFilename(this.filename);
		dto.setDelivery_vin_id(this.delivery_vin_id);

		dto.setLoad_id(this.load_id);
		dto.setDelivery_id(this.delivery_id);

		dto.setInspection_id(this.inspection_guid);
		if (!HelperFuncs.isNullOrEmpty(this.imageLat)) {
			dto.setImage_latitude(Double.parseDouble(this.imageLat));
		}
		if (!HelperFuncs.isNullOrEmpty(this.imageLon)) {
			dto.setImage_longitude(Double.parseDouble(this.imageLon));
		}
		dto.setPreload_image((short) (this.preloadImage ? 1 : 0));

		dto.setProblem_report_guid(problem_report_guid);

		dto.setPreauth_url(this.preauth_url);

		return dto;
	}

	public com.cassens.autotran.data.model.dto.Image getDTO(Context context) {
		com.cassens.autotran.data.model.dto.Image dto = this.getDTODetails(context);
		if (this.delivery_vin_id != -1) {
			DeliveryVin dv = DataManager.getDeliveryVin(context, this.delivery_vin_id);
			if (dv != null && !HelperFuncs.isNullOrEmpty(dv.delivery_vin_remote_id)) {
				dto.setDelivery_vin_id(Integer.parseInt(dv.delivery_vin_remote_id));
			}
			dto.setInspection_id(null);

		}

		return dto;
	}

	public boolean isForeignKeyInList(final List<String> extraList) {
		for (int i = extraList.size() - 1; i >= 0; i--) {
			if (this.foreignKeyLabel != null && this.foreignKeyLabel.equals(extraList.get(i))) {
				return true;
			}
		}
		return false;
	}
}
