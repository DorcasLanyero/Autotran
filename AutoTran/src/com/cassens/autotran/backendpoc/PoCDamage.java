package com.cassens.autotran.backendpoc;

import com.cassens.autotran.data.model.Damage;
import com.cassens.autotran.data.model.DeliveryVin;
import com.sdgsystems.util.HelperFuncs;

public class PoCDamage {
    int area;
    int type;
    int severity;
    int special;       // ToDo: Make special a boolean instead of an int
    boolean preload;
    String source;

    static PoCDamage convertFromV2(Damage oldDamage) {
        PoCDamage damage = new PoCDamage();
        if (oldDamage.specialCode == null) {
            damage.special = -1;
            damage.area = HelperFuncs.stringToInt(oldDamage.getAreaCode(), -1);
            damage.type = HelperFuncs.stringToInt(oldDamage.getTypeCode(), -1);
            damage.severity = HelperFuncs.stringToInt(oldDamage.getSeverityCode(), -1);
        }
        else {
            damage.special = oldDamage.special_code_id; // this id probably isn't meaningful
            damage.area = HelperFuncs.stringToInt(oldDamage.specialCode.getAreaCode(), -1);
            damage.type = HelperFuncs.stringToInt(oldDamage.specialCode.getTypeCode(), -1);
            damage.severity = HelperFuncs.stringToInt(oldDamage.specialCode.getSeverityCode(), -1);
        }
        damage.preload = oldDamage.preLoadDamage;
        damage.source = oldDamage.source;
        return damage;
    }
}