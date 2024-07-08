package com.cassens.autotran.data.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.cassens.autotran.R;
import com.cassens.autotran.data.model.DeliveryVinModel;
import com.sdgsystems.util.HelperFuncs;

import java.util.List;


/**
 * Created by john on 3/6/18.
 */

public class DeliveryVinModelAdapter extends ArrayAdapter<DeliveryVinModel> {
    private Context mContext;
    private List<DeliveryVinModel> vinList;

    public DeliveryVinModelAdapter(@NonNull Context context, List<DeliveryVinModel> vins) {
        super(context, 0, vins);
        mContext = context;
        vinList = vins;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if(listItem == null) {
            listItem = LayoutInflater.from(mContext).inflate(R.layout.damage_popup_list_element, parent, false);
        }

        DeliveryVinModel deliveryVinModel = vinList.get(position);

        TextView vin = (TextView)listItem.findViewById(R.id.vinTextView);
        vin.setText(deliveryVinModel.getVin());

        TextView color = (TextView)listItem.findViewById(R.id.colorTextView);
        color.setText(deliveryVinModel.getColor());

        TextView description = (TextView)listItem.findViewById(R.id.descriptionTextView);
        description.setText(deliveryVinModel.getDescription());

        TextView damages = (TextView)listItem.findViewById(R.id.damageTextView);
        damages.setText(deliveryVinModel.getDamages());

        return listItem;
    }
}
