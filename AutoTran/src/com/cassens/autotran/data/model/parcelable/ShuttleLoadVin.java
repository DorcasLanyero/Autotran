package com.cassens.autotran.data.model.parcelable;

import android.os.Parcel;
import android.os.Parcelable;

public class ShuttleLoadVin implements Parcelable {
    public String vin, route, productionStatus;

    public ShuttleLoadVin(String vin, String route, String productionStatus) {
        this.vin = vin;
        this.route = route;
        this.productionStatus = productionStatus;
    }

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<ShuttleLoadVin> CREATOR
            = new Parcelable.Creator<ShuttleLoadVin>() {
        public ShuttleLoadVin createFromParcel(Parcel in) {
            return new ShuttleLoadVin(in);
        }

        public ShuttleLoadVin[] newArray(int size) {
            return new ShuttleLoadVin[size];
        }
    };

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.vin);
        out.writeString(this.route);
        out.writeString(this.productionStatus);
    }

    private ShuttleLoadVin(Parcel in) {
        this.vin = in.readString();
        this.route = in.readString();
        this.productionStatus = in.readString();
    }
}



