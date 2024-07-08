package com.cassens.autotran;

import android.content.Context;

import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Image;
import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class AutoTranUpgrades {
    private static final Logger log = LoggerFactory.getLogger(AutoTranUpgrades.class.getSimpleName());

    /**
     * This fixes a bug where there were images with erroneous shuttle load ids in their load_id field
     * There are two passes, here:
     * -anything that has a load id but ALSO has a delivery vin, delivery or problem report id needs to have the load id unset
     * -every image w/ a load id needs to check the filename to make sure that the load number of the load id exists in the filename
     * @param context
     */
    public static void fix_shuttle_load_id_propogation_error(Context context) {
        log.debug(Logs.UPGRADES, "fix_shuttle_load_id_propogation_error");

        //Pull all image ids, load_ids, filenames where load id is not null
        ArrayList<Image> loadIdImages = DataManager.getAllImagesWithLoadIds(context, false);

        Gson gson = new Gson();

        for (Image image : loadIdImages) {

            log.debug(Logs.UPGRADES, "Checking image: " + gson.toJson(image));

            //Remove load ids from data that also has a different id
            if(image.delivery_vin_id != -1 || image.delivery_id != -1 || image.problem_report_guid != null || image.inspection_guid != null) {
                log.debug(Logs.UPGRADES, "Removing load id from " + image.filename);
                removeImageLoadId(context, image);
            } else {
                //Get the load number for the load id (checking both remote and local)
                String ldnbr = DataManager.getLoadNumberForLoadId(context, image.load_id);
                log.debug(Logs.UPGRADES, "Image " + image.filename + " claims to be for load " + ldnbr + ", checking");

                if (ldnbr == null) {
                    log.debug(Logs.UPGRADES, "loadnumber is null, skipping");
                    continue;
                }

                //If the load number for the load id is NOT in the image filename
                if(image.filename.contains(ldnbr)) {
                    log.debug(Logs.UPGRADES, image.filename + " checks out for load " + ldnbr);
                } else {
                    log.debug(Logs.UPGRADES, "that filename doesn't match with the load");

                    //IF this is a PRELOAD image, try to get the load number
                    if(image.filename.contains(Constants.PRELOAD_IMAGE_FILE_PREFIX)) {
                        log.debug(Logs.UPGRADES, "This is a preload image, trying to get the original load id for that load number");

                        String filename = image.filename;
                        filename = filename.substring(Constants.PRELOAD_IMAGE_FILE_PREFIX.length());
                        int indexOfDelimiter = -1;
                        if(filename.contains(Constants.IMAGE_FILE_DELIM)) {
                            indexOfDelimiter = filename.indexOf(Constants.IMAGE_FILE_DELIM);
                            String testLoadNumber = filename.substring(0,indexOfDelimiter);

                            log.debug(Logs.UPGRADES, "Looking for ldnbr " + testLoadNumber);
                            int load_id = DataManager.getRemoteLoadIdForLoadNumber(context, testLoadNumber);
                            if(load_id != -1) {
                                log.debug(Logs.UPGRADES, "Found " + testLoadNumber + " as " + load_id + " setting it into the image");
                                image.load_id = load_id;
                                DataManager.insertImageToLocalDB(context, image);
                                //set the load id in the image and save it
                            } else {
                                log.debug(Logs.UPGRADES, "Couldn't find " + testLoadNumber + " removing the load id");
                                removeImageLoadId(context, image);
                            }

                        } else {
                            log.debug(Logs.UPGRADES, "No additional filename delimeter in " + filename);
                            removeImageLoadId(context, image);
                        }
                    }

                }
            }
        }
    }

    private static void removeImageLoadId(Context context, Image image) {
        image.load_id = -1;
        DataManager.insertImageToLocalDB(context, image);
    }
}
