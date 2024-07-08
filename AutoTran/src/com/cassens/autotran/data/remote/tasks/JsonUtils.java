package com.cassens.autotran.data.remote.tasks;

import android.content.Context;

import com.cassens.autotran.Logs;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Damage;
import com.cassens.autotran.data.model.Dealer;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.Image;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.TrainingRequirement;
import com.cassens.autotran.data.model.VIN;
import com.cassens.autotran.data.model.lookup.ShuttleMove;
import com.cassens.autotran.data.remote.GsonTypeAdapters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.HelperFuncs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class JsonUtils {
	private static final Logger log = LoggerFactory.getLogger(JsonUtils.class.getSimpleName());

	public static void parseAndSaveLoad(Context context, Load load, JSONObject loadJson, boolean preserveSignatures)
			throws JSONException {
		load.driver_id = DataManager.getUserForRemoteId(context, loadJson.getInt("user_id")).user_id;
		//TODO: should load have a truck number?
		//TODO: should load have a 'fillers' field?

		boolean relayLoad = false;

		Load existingLoad = null;
		List<DeliveryVin> existingDeliveryVins = null;

		if (loadJson.has("originldnbr") && !loadJson.isNull("originldnbr") && loadJson.getString("originldnbr").trim().length() > 0) {
			relayLoad = true;
			load.originLoadNumber = loadJson.getString("originldnbr");
			load.relayLoad = true;
		}

		if (loadJson.has("originTerm") && !HelperFuncs.isNullOrEmpty(loadJson.getString("originTerm"))) {
			load.originTerminal = loadJson.getString("originTerm");
		}

		if (loadJson.has("helpTerm") && !HelperFuncs.isNullOrEmpty(loadJson.getString("helpTerm"))) {
			load.helpTerminal = loadJson.getString("helpTerm");
		}

		if (loadJson.has("relayldnbr")
				&& !loadJson.isNull("relayldnbr")
				&& loadJson.getString("relayldnbr").trim().length() > 0
				&& !loadJson.getString("relayldnbr").trim().equals("0")) {
			load.originLoad = true;

			load.relayLoadNumber = loadJson.getString("relayldnbr");
			relayLoad = true;
		}

		if (loadJson.has("ldtyp") && !loadJson.isNull("ldtyp")) {
			load.loadType = loadJson.getString("ldtyp");
		} else {
			load.loadType = "";
		}

		if (loadJson.has("notes") && !HelperFuncs.isNullOrEmpty(loadJson.getString("notes"))
				&& !loadJson.getString("notes").equalsIgnoreCase("null")) {
			load.notes = loadJson.getString("notes");
		}

		log.debug(Logs.DEBUG, "next dispatch" + loadJson.getString("nextDispatch"));
		if (loadJson.has("nextDispatch") && !HelperFuncs.isNullOrEmpty(loadJson.getString("nextDispatch"))
				&& !loadJson.getString("nextDispatch").equalsIgnoreCase("null")) {
			load.nextDispatch = loadJson.getString("nextDispatch");
		}

		log.debug(Logs.DEBUG, "first drop" + loadJson.getString("firstDrop"));
		if (loadJson.has("firstDrop") && !HelperFuncs.isNullOrEmpty(loadJson.getString("firstDrop"))
				&& !loadJson.getString("firstDrop").equalsIgnoreCase("null")) {
			load.firstDrop = loadJson.getString("firstDrop");
		}

		log.debug(Logs.DEBUG, "last drop" + loadJson.getString("lastDrop"));
		if (loadJson.has("lastDrop") && !HelperFuncs.isNullOrEmpty(loadJson.getString("lastDrop"))
				&& !loadJson.getString("lastDrop").equalsIgnoreCase("null")) {
			load.lastDrop = loadJson.getString("lastDrop");
		}

		if (loadJson.has("driver_preload_signature_signedat") && !HelperFuncs.isNullOrEmpty(loadJson.getString("driver_preload_signature_signedat"))
				&& !loadJson.getString("driver_preload_signature_signedat").equalsIgnoreCase("null")) {
			load.driverPreLoadSignature = Constants.DUMMY_SIGNATURE;
			load.setUploaded(true);

			load.driverPreLoadSignatureSignedAt = loadJson.getString("driver_preload_signature_signedat");
			load.preloadUploadStatus = Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD;
		}

		if (loadJson.has("trknbr") && !HelperFuncs.isNullOrEmpty(loadJson.getString("trknbr"))) {
			load.truckNumber = loadJson.getString("trknbr");
		}

		if (loadJson.has("trailerNumber") && !HelperFuncs.isNullOrEmpty(loadJson.getString("trailerNumber"))) {
			load.trailerNumber = loadJson.getString("trailerNumber");
		}

		if (loadJson.has("parentLoad") && !HelperFuncs.isNullOrEmpty(loadJson.getString("parentLoad"))
				&& !loadJson.getString("parentLoad").equalsIgnoreCase("null")) {
			load.parentLoad = loadJson.getInt("parentLoad") == 1;
		}

		if (loadJson.has("parent_load_id") && !HelperFuncs.isNullOrEmpty(loadJson.getString("parent_load_id"))
				&& !loadJson.getString("parent_load_id").equalsIgnoreCase("null")) {
			load.parent_load_id = loadJson.getInt("parent_load_id");
		}

		if (loadJson.has("pickSheetImageRequired")) {
			load.setPickSheetImageRequired(loadJson.getBoolean("pickSheetImageRequired"));
		}
		if (loadJson.has("extraDocImageRequired")) {
			load.setExtraDocImageRequired(loadJson.getString("extraDocImageRequired"));
		}

		//Get all VINs and store them for the delivery vin insertion
		HashMap vinMap = new HashMap();

		JSONObject vinsJson = loadJson.getJSONObject("Vin");
		for (int vinsIndex = 0; vinsIndex < vinsJson.names().length(); vinsIndex++) {
			JSONObject vinJson = vinsJson.getJSONObject(vinsJson.names().get(vinsIndex).toString());
			VIN vin = new VIN();

			//TODO: should all of the vin fields get populated or are they mostly unused?
			vin.vin_remote_id = vinJson.getString("id");
			vin.vin_number = vinJson.getString("vin_number");
			vin.status = vinJson.getString("status");
			vin.body = vinJson.getString("body");
			vin.weight = vinJson.getString("weight");
			vin.color = vinJson.getString("color");
			vin.colordes = vinJson.getString("colordes");
			vin.type = vinJson.getString("type");

			vinMap.put(vin.vin_remote_id, vin);
		}

		//Get existing load and existing deliveryVins and preserve needed load values values
		if (load.load_remote_id != null) {
			existingLoad = DataManager.getLoadForLoadNumber(context, load.loadNumber);

			if (existingLoad != null) {
				// preserve this even when signatures are not being preserved, since it
				// happens at the very beginning of the preload
				load.load_id = existingLoad.load_id;
				// Note: If the dispatched remote ID is different, it means the load was deleted and
				// re-added on the server. In that case, we need to update the load_id in all  of
				// the supplemental images, since the load_id in that case is the *remote* load id,
				// NOT the local load id.  (This is probably a design flaw that should be fixed
				// eventually.
				if (load.load_remote_id != null && existingLoad.load_remote_id != null
						&& !load.load_remote_id.equalsIgnoreCase(existingLoad.load_remote_id)) {
					load.images = existingLoad.images;
					for (Image image : load.images) {
						int load_id;
						try {
							load_id = Integer.parseInt(load.load_remote_id);
							image.load_id = load_id;
						} catch (NumberFormatException nfe) {

						}
					}
				}
				load.preloadSupervisorSignature = existingLoad.preloadSupervisorSignature;
				load.preloadSupervisorSignedAt = existingLoad.preloadSupervisorSignedAt;
				load.notes = existingLoad.notes; // Supplemental notes
				load.supervisorSignature = null;
				load.driverHighClaimsAudit = existingLoad.driverHighClaimsAudit;
				load.supervisorSignature = existingLoad.supervisorSignature; // for audit
				load.supervisorSignedAt = existingLoad.supervisorSignedAt;
				load.supervisorSignatureLat = existingLoad.supervisorSignatureLat;
				load.supervisorSignatureLon = existingLoad.supervisorSignatureLon;
				if (preserveSignatures) {
					load.driverPreLoadSignature = existingLoad.driverPreLoadSignature;
					load.driverPreLoadSignatureSignedAt = existingLoad.driverPreLoadSignatureSignedAt;
					load.driverPreLoadSignatureLat = existingLoad.driverPreLoadSignatureLat;
					load.driverPreLoadSignatureLon = existingLoad.driverPreLoadSignatureLon;
					load.driverPreLoadContact = existingLoad.driverPreLoadContact;
				}
				load.driverPreLoadComment = existingLoad.driverPreLoadComment;

				existingDeliveryVins = existingLoad.getDeliveryVinList(true);
			}
		}

		//Get all Deliveries
		JSONObject deliveriesJson = loadJson.getJSONObject("Delivery");

		log.debug(Logs.DEBUG, "pulled " + deliveriesJson.names().length() + " deliveries");

		for (int deliveriesIndex = 0; deliveriesIndex < deliveriesJson.names().length(); deliveriesIndex++) {
			Delivery delivery = new Delivery();

			JSONObject deliveryJSON = deliveriesJson.getJSONObject(deliveriesJson.names().getString(deliveriesIndex).toString());
			//Get Delivery meta data

			log.debug(Logs.DEBUG, "Parsing Delivery " + deliveryJSON.getString("id"));

			delivery.delivery_remote_id = deliveryJSON.getString("id");
			delivery.ship_date = deliveryJSON.getString("ship_date");
			delivery.estdeliverdate = deliveryJSON.getString("estdeliverdate");
			delivery.status = deliveryJSON.getString("status");
			delivery.delivery = deliveryJSON.getString("delivery");
			delivery.callback = deliveryJSON.getString("callback");
			if (deliveryJSON.has("dockTerm") && !HelperFuncs.isNullOrEmpty(deliveryJSON.getString("dockTerm"))
					&& !deliveryJSON.getString("dockTerm").equalsIgnoreCase("null")) {
				delivery.dockTerm = deliveryJSON.getInt("dockTerm");
			}

			if (deliveryJSON.has("driver_signature_signedat") && !HelperFuncs.isNullOrEmpty(deliveryJSON.getString("driver_signature_signedat"))
					&& !deliveryJSON.getString("driver_signature_signedat").equalsIgnoreCase("null")) {
				log.debug(Logs.DEBUG, "driver signed at: " + deliveryJSON.getString("driver_signature_signedat"));
				delivery.dealerSignature = Constants.DUMMY_SIGNATURE;
				delivery.driverSignature = Constants.DUMMY_SIGNATURE;
				delivery.setUploaded(true);

				delivery.dealerSignatureSignedAt = deliveryJSON.getString("dealer_signature_signedat");
				delivery.driverSignatureSignedAt = deliveryJSON.getString("driver_signature_signedat");
				delivery.deliveryUploadStatus = Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY;
			}

			if (deliveryJSON.has("notes") && !HelperFuncs.isNullOrEmpty(deliveryJSON.getString("notes"))
					&& !deliveryJSON.getString("notes").equalsIgnoreCase("null")) {
				delivery.notes = deliveryJSON.getString("notes");
			}

			//Get Dealer
			JSONObject dealerJson = deliveryJSON.getJSONObject("Dealer");
			delivery.dealer = setDealerFromJson(dealerJson);

			if (deliveryJSON.has("DealerNameOverride")) {
				log.debug(Logs.DEBUG, "Adding dealer name override of " + deliveryJSON.getString("DealerNameOverride"));
				load.relayLoadDealerName = deliveryJSON.getString("DealerNameOverride");
			}


			// If this is an overwrite, preserve existing delivery values, as needed
			if (existingLoad != null) {
				Delivery existingDelivery = existingLoad.getDeliveryForDealerRemoteId(delivery.dealer.dealer_remote_id);
				if (existingDelivery != null) {
					handleDealerUpdatedFields(delivery.dealer, existingDelivery.dealer);

					// Never overwrite existing dealer signature information

					// Note: If the dispatched remote ID is different, it means the load was
					// deleted and re-added on the server. In that case, we need to update the
					// delivery_id in all of the supplemental images, since the delivery_id in that
					// case is the *remote* load id, NOT the local load id.  (This is probably a
					// design flaw that should be fixed eventually.
					if (delivery.delivery_remote_id != null && existingDelivery.delivery_remote_id != null
							&& !delivery.delivery_remote_id.equalsIgnoreCase(existingDelivery.delivery_remote_id)) {
						delivery.images = existingDelivery.images;
						for (Image image : delivery.images) {
							int delivery_id;
							try {
								delivery_id = Integer.parseInt(delivery.delivery_remote_id);
								image.delivery_id = delivery_id;
							} catch (NumberFormatException nfe) {

							}
						}
					}
					delivery.delivery_id = existingDelivery.delivery_id;
					delivery.dealerSignature = existingDelivery.dealerSignature;
					delivery.dealerSignatureSignedAt = existingDelivery.dealerSignatureSignedAt;
					delivery.dealerSignatureLat = existingDelivery.dealerSignatureLat;
					delivery.dealerSignatureLon = existingDelivery.dealerSignatureLon;
					delivery.dealerComment = existingDelivery.dealerComment;
					delivery.dealerContact = existingDelivery.dealerContact;
					delivery.sti = existingDelivery.sti;
					delivery.afrhrs = existingDelivery.afrhrs;
					delivery.notes = existingDelivery.notes; // supplemental notes
					delivery.safeDelivery = existingDelivery.safeDelivery;

					if (preserveSignatures) {
						delivery.driverSignature = existingDelivery.driverSignature;
						delivery.driverSignatureSignedAt = existingDelivery.driverSignatureSignedAt;
						delivery.driverSignatureLat = existingDelivery.driverSignatureLat;
						delivery.driverSignatureLon = existingDelivery.driverSignatureLon;
						delivery.driverContact = existingDelivery.driverContact;
					}
					delivery.driverComment = existingDelivery.driverComment;
				}
			}

			if (deliveryJSON.has("DeliveryVin")) {
				JSONObject deliveryVinsJson = deliveryJSON.getJSONObject("DeliveryVin");

				log.debug(Logs.DEBUG, "parsing " + deliveryVinsJson.names().length() + " vins for the delivery");


				for (int deliveryVinIndex = 0; deliveryVinIndex < deliveryVinsJson.names().length(); deliveryVinIndex++) {
					JSONObject deliveryVinJson = deliveryVinsJson.getJSONObject(deliveryVinsJson.names().getString(deliveryVinIndex).toString());
					DeliveryVin deliveryVin = new DeliveryVin();

					deliveryVin.delivery_vin_remote_id = deliveryVinJson.getString("id");

					log.debug(Logs.DEBUG, "parsing delivery vin " + deliveryVin.delivery_vin_remote_id);

					deliveryVin.status = deliveryVinJson.getString("status");
					deliveryVin.user_type = deliveryVinJson.getString("user_type");
					deliveryVin.rejected_by = deliveryVinJson.getString("rejected_by");
					deliveryVin.ldseq = deliveryVinJson.getString("ldseq");
					deliveryVin.pro = deliveryVinJson.getString("pro");
					deliveryVin.position = deliveryVinJson.getString("position");
					if (!DeliveryVin.isValidLoadPosition(deliveryVin.position)) {
						deliveryVin.position = null;
					}
					deliveryVin.backdrv = deliveryVinJson.getString("backdrv");
					deliveryVin.rldspickup = deliveryVinJson.getString("rldspickup");
					deliveryVin.bckhlnbr = deliveryVinJson.getString("bckhlnbr");
					deliveryVin.lot = deliveryVinJson.getString("lot");
					deliveryVin.rowbay = deliveryVinJson.getString("rowbay");
					deliveryVin.rte1 = deliveryVinJson.getString("rte1");
					deliveryVin.rte2 = deliveryVinJson.getString("rte2");
					deliveryVin.von = deliveryVinJson.getString("von");
					if (deliveryVinJson.has("finalMfg")) {
						deliveryVin.finalMfg = deliveryVinJson.getString("finalMfg");
					}
					if (deliveryVinJson.has("finalDealer")) {
						deliveryVin.finalDealer = deliveryVinJson.getString("finalDealer");
					}

					if (deliveryVinJson.has("do_lotlocate")) {
						deliveryVin.do_lotlocate = deliveryVinJson.getString("do_lotlocate");
					}

					deliveryVin.vin = (VIN) vinMap.get(deliveryVinJson.getString("vin_id"));

					//If this is a relay load, the deliveryVin was already inspected.
					if (relayLoad && load.relayLoad) {
						deliveryVin.inspectedPreload = true;
					}

					// Go through the list of existing delivery vins and preserve values the driver
					// has already generated
					if (existingDeliveryVins != null) {
						// Use iterator to traverse existing, since we're deleting as we go
						for (Iterator<DeliveryVin> itr = existingDeliveryVins.iterator(); itr.hasNext(); ) {
							DeliveryVin existingDeliveryVin = itr.next();
							if (deliveryVinsMatch(existingDeliveryVin, deliveryVin)) {
								deliveryVin.delivery_vin_id = existingDeliveryVin.delivery_vin_id;
								deliveryVin.inspectedPreload = existingDeliveryVin.inspectedPreload;
								deliveryVin.inspectedDelivery = existingDeliveryVin.inspectedDelivery;
								deliveryVin.position = existingDeliveryVin.position;
								deliveryVin.backdrv = existingDeliveryVin.backdrv;
								deliveryVin.preloadNotes = existingDeliveryVin.preloadNotes;
								deliveryVin.deliveryNotes = existingDeliveryVin.deliveryNotes;
								deliveryVin.damages = existingDeliveryVin.damages;
								deliveryVin.images = existingDeliveryVin.images;
								deliveryVin.ats = existingDeliveryVin.ats;
								deliveryVin.supervisorSignature = existingDeliveryVin.supervisorSignature;
								deliveryVin.supervisorComment = existingDeliveryVin.supervisorComment;
								deliveryVin.supervisorContact = existingDeliveryVin.supervisorContact;
								deliveryVin.supervisorSignatureLat = existingDeliveryVin.supervisorSignatureLat;
								deliveryVin.supervisorSignatureLon = existingDeliveryVin.supervisorSignatureLon;

								// Note: deliveryVin.key has not been being preserved on load updates
								//       for a long time (if ever), which was probably an oversight.
								//       However, since, contrary to its name, it's actually used
								//       to track selection VIN selection history and can, therefore,
								//       grow without bounds, I'm reluctant to begin preserving it
								//       here.  The field on the server is 500 characters and if
								//       it should grow beyond that size, it will result in an
								//       error on the upload.  We should put some checks on the
								//       length before including this here. And, while we are at
								//       it, we should change the name from key to something
								//       meaningful.
								// deliveryVin.key = existingDeliveryVin.key;


								/*
								if (existingDeliveryVin.delivery_vin_remote_id.equals(deliveryVin.delivery_vin_remote_id)) {
									deliveryVin.images = existingDeliveryVin.images;
								}
								else {
									// A remote ID mismatch can occur if the VIN is moved to a
									// different delivery within the same load, in that case, we
									// want to preserve any images that have already been taken for
									// the preload.
									for (Image img : existingLoad.getImages()) {
										if (img.delivery_vin_id == existingDeliveryVin.delivery_vin_id) {
											// set the delivery_vin_id to the new deliveryVin instead?
											load.images.add(img);
										}
									}
								}
								*/
								itr.remove();
							}
						}
					}


					//parse the damages
					//make sure to mark them as preload, readonly AND uploaded for preload and delivery
					if (deliveryVinJson.has("Damage")) {
						JSONArray damagesJson = deliveryVinJson.getJSONArray("Damage");
						log.debug(Logs.DEBUG, "parsing " + damagesJson.length() + " damages for the deliveryvin");
						for (int damageIndex = 0; damageIndex < damagesJson.length(); damageIndex++) {
							JSONObject damageJson = damagesJson.getJSONObject(damageIndex);
							Damage damage = new Damage();
							// TODO: Investigate whether it makes sense to set delivery_vin_id here. Seems to be wrong.
							damage.delivery_vin_id = damageJson.getInt("delivery_vin_id");
							damage.source = damageJson.getString("source");
							damage.guid = damageJson.getString("id");

							if (!damageJson.isNull("typecode_id")) {
								try {
									damage.type_code_id = DataManager.getTypeCodeByRemoteId(context, damageJson.getInt("typecode_id")).type_code_id;
									damage.svrty_code_id = DataManager.getSeverityCodeByRemoteId(context, damageJson.getInt("svrtycode_id")).severity_code_id;
									damage.area_code_id = DataManager.getAreaCodeByRemoteId(context, damageJson.getInt("areacode_id")).area_code_id;

									damage.areaCode = DataManager.getAreaCode(context, damage.area_code_id);
									damage.typeCode = DataManager.getTypeCode(context, damage.type_code_id);
									damage.severityCode = DataManager.getSeverityCode(context, damage.svrty_code_id);
								} catch (Exception e) {
									log.debug(Logs.DEBUG, "Error reading historical/relay damage: " + e.getMessage());
								}
							} else if (!damageJson.isNull("specialcode_id")) {
								try {
									damage.specialCode = DataManager.getSpecialCodeByCode(context, damageJson.getString("specialcode_id"));
									damage.special_code_id = damage.specialCode.special_code_id;
								} catch (Exception e) {
									log.debug(Logs.DEBUG, "Error reading historical/relay special damage: " + e.getMessage());
								}
							}

							//BFF I THINK that we need to ALWAYS mark incoming damages as ready only
							damage.readonly = true;//damageJson.getBoolean("readonly");
							//damage.readonly = !HelperFuncs.noNull(damage.source, "").equalsIgnoreCase("driver");

							//PDK - Changed this to use the preload_damage flag from the server in 2.6.0 pilot,
							//      but that seems to have caused pre-entered damagaes to begin showing up on
							//      deliveries. I THINK preLoadDamage should always be forced to true here because
							//      for pre-entered damages entered from other inspections, we want them to show
							//      up on the preload inspection for this load, regardless of whether they were
							//      previously entered at preload or delivery.
							damage.preLoadDamage = true;

							/*
							if (!damageJson.isNull("preload_damage")) {
								damage.preLoadDamage = damageJson.getBoolean("preload_damage");
							}
							else{
								damage.preLoadDamage = true;
							}
							*/

							//Since these are read-only set the upload status manually
							/*
							 * I think that setting the upload status for these is bad.  maybe I should just skip them during the upload process?
							if(damage.readonly) {
								if(damage.preLoadDamage) {
									damage.preloadUploadStatus = Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD;
								}
								else {
									damage.deliveryUploadStatus = Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY;
								}
							}


							//incoming preload damage flag is a string
							if(damageJson.has("preload_damage")) {
								log.debug(Logs.DEBUG, "preload_damage: " + damageJson.getString("preload_damage"));
								damage.preLoadDamage = damageJson.getString("preload_damage").equals("1") || damageJson.getString("preload_damage").equals("true");
							}
							*/
							addDamageIfNotPresent(deliveryVin, damage);
						}
					}

					delivery.deliveryVins.add(deliveryVin);
				}
			}

			load.deliveries.add(delivery);
		}

		// If this is a first leg relay load, get finalDealers
		if (loadJson.has("FinalDealer")) {
			JSONObject finalDealersJson = loadJson.getJSONObject("FinalDealer");
			if (finalDealersJson != null) {
				for (int i = 0; i < finalDealersJson.names().length(); i++) {
					JSONObject finalDealerJson = finalDealersJson.getJSONObject(finalDealersJson.names().getString(i).toString());
					Dealer finalDealer = setDealerFromJson(finalDealerJson);
					/*log.debug(Logs.DEBUG, String.format("JUNK: Got final dealer: %s-%s ('%s')",
										   finalDealer.customer_number, finalDealer.mfg,
										   finalDealer.customer_name)); */
					Dealer existingFinalDealer = DataManager.getDealer(context, finalDealer.customer_number, finalDealer.mfg);
					if (existingFinalDealer != null) {
						handleDealerUpdatedFields(finalDealer, existingFinalDealer);
					}
					DataManager.insertDealerToLocalDB(context, finalDealer);
				}
			}
		}

		log.debug(Logs.DEBUG, "pulled " + deliveriesJson.names().length() + " deliveries");


		log.debug(Logs.DEBUG, "inserting load into database");
		//Insert the load and all of the objects beneath it
		long load_id = DataManager.insertLoadToLocalDB(context, load, true);

		// Remove deliveryVins that didn't have a match in the new load. (Any deliveryVins that
		// didn't exist in the new load have been deleted from existingDeliveryVins at this point.)
		if (existingDeliveryVins != null) {
			for (DeliveryVin edv : existingDeliveryVins) {
				if (edv.vin != null && DataManager.getDeliveryVinCountForVin(context, edv.vin) == 1) {
					//vin is not used anywhere else, ok to delete
					DataManager.deleteVinDataFromDB(context, edv.vin.vin_id);
				}
				DataManager.deleteDeliveryVinDataFromDB(context, edv.delivery_vin_id);
				DataManager.deleteDamages(context, edv.delivery_vin_id);
				DataManager.deleteImages(context, edv.delivery_vin_id);
			}
		}

		//get the load that we just inserted and trim out deliveries that are empty
		DataManager.deleteEmptyDeliveriesFromLoad(context, (int) load_id);
	}

	private static Dealer setDealerFromJson(JSONObject dealerJson) throws JSONException {
		Dealer dealer = new Dealer();
		dealer.dealer_remote_id = dealerJson.getString("id");
		dealer.mfg = dealerJson.getString("mfg");
		dealer.customer_number = dealerJson.getString("customer_number");
		dealer.customer_name = dealerJson.getString("customer_name");
		dealer.city = dealerJson.getString("city");
		dealer.state = dealerJson.getString("state");
		dealer.address = dealerJson.getString("address");
		dealer.zip = dealerJson.getString("zip");
		dealer.contact_name = dealerJson.getString("contact_name");
		dealer.email = dealerJson.getString("email");
		dealer.countryCode = dealerJson.getString("countryCode");


		// Be default, we use the last_updated column for dealer.lastUpdated. The last_updated
		// set on the server by the updated field that comes in via the dealer csv file.
		// However, in test mode, we use the modified column from the dealer table for
		// dealerLastUpdated. This allows us to test dealer update notifications by modifying the
		// dealer from the console.
		if (AppSetting.DEALER_UPDATE_TEST_MODE.getBoolean()) {
			if (dealerJson.has("modified") && dealerJson.getString("modified") != "null") {
				SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
				try {
					// Time from server does not include time zone, so we must append " GMT"
					dealer.lastUpdated = dFormat.parse(dealerJson.getString("modified") + " GMT");
				} catch (ParseException e) {
				}
			}
		}
		else {
			if (dealerJson.has("last_updated") && dealerJson.getString("last_updated") != "null") {
				SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
				try {
					// Time from server does not include time zone, so we must append " GMT"
					dealer.lastUpdated = dFormat.parse(dealerJson.getString("last_updated") + " GMT");
				} catch (ParseException e) {
				}
			}
		}

		if (dealerJson.has("phone")) {
			dealer.phone = dealerJson.getString("phone");
			log.debug(Logs.DEBUG, "saving phone " + dealer.phone + " for dealer");
		} else {
			log.debug(Logs.DEBUG, "not saving phone");
		}

		if (dealerJson.has("monam") && dealerJson.getString("monam") != "null")
			dealer.monam = dealerJson.getInt("monam");

		if (dealerJson.has("monpm") && dealerJson.getString("monpm") != "null")
			dealer.monpm = dealerJson.getInt("monpm");

		if (dealerJson.has("tueam") && dealerJson.getString("tueam") != "null")
			dealer.tueam = dealerJson.getInt("tueam");

		if (dealerJson.has("tuepm") && dealerJson.getString("tuepm") != "null")
			dealer.tuepm = dealerJson.getInt("tuepm");

		if (dealerJson.has("wedam") && dealerJson.getString("wedam") != "null")
			dealer.wedam = dealerJson.getInt("wedam");

		if (dealerJson.has("wedpm") && dealerJson.getString("wedpm") != "null")
			dealer.wedpm = dealerJson.getInt("wedpm");

		if (dealerJson.has("thuam") && dealerJson.getString("thuam") != "null")
			dealer.thuam = dealerJson.getInt("thuam");

		if (dealerJson.has("thupm") && dealerJson.getString("thupm") != "null")
			dealer.thupm = dealerJson.getInt("thupm");

		if (dealerJson.has("friam") && dealerJson.getString("friam") != "null")
			dealer.friam = dealerJson.getInt("friam");

		if (dealerJson.has("fripm") && dealerJson.getString("fripm") != "null")
			dealer.fripm = dealerJson.getInt("fripm");

		if (dealerJson.has("satam") && dealerJson.getString("satam") != "null")
			dealer.satam = dealerJson.getInt("satam");

		if (dealerJson.has("satpm") && dealerJson.getString("satpm") != "null")
			dealer.satpm = dealerJson.getInt("satpm");

		if (dealerJson.has("sunam") && dealerJson.getString("sunam") != "null")
			dealer.sunam = dealerJson.getInt("sunam");


		if (dealerJson.has("sunpm") && dealerJson.getString("sunpm") != "null")
			dealer.sunpm = dealerJson.getInt("sunpm");

		if (dealerJson.has("lotLocateRequired") && dealerJson.getString("lotLocateRequired") != "null") {
			dealer.lotLocateRequired = dealerJson.getInt("lotLocateRequired") == 0 ? false : true;
		}

		if(dealerJson.has("lot_code_id") && dealerJson.getString("lot_code_id") != "null") {
			dealer.lot_code_id = dealerJson.getInt("lot_code_id");
		}

		dealer.afthr = dealerJson.getString("afthr");
		dealer.comments = dealerJson.getString("comments");
		dealer.status = dealerJson.getString("status");

		if (dealerJson.has("high_claims")) {
			dealer.high_claims = dealerJson.getBoolean("high_claims");
		}

		if (dealerJson.has("alwaysUnattended")) {
			dealer.alwaysUnattended = dealerJson.getBoolean("alwaysUnattended");
		}

		if (dealerJson.has("photosOnUnattended")) {
			dealer.photosOnUnattended = dealerJson.getBoolean("photosOnUnattended");
		}
		return dealer;
	}

	private static void handleDealerUpdatedFields(Dealer dealer, Dealer existingDealer) {
		dealer.setUpdatedFields(existingDealer.getUpdatedFields());
		/* log.debug(Logs.DEBUG, "JUNK: Existing updatedFields ("
					+ existingDealer.customer_number + "): "
					+ existingDealer.getUpdatedFieldsCsv()); */

		//long dateModified = dealer.lastUpdated.getTime();
		long dateModified = System.currentTimeMillis();
		if (fieldUpdated(dealer.afthr, existingDealer.afthr)) {
			dealer.insertUpdatedField("afthr", dateModified);
		}

		if (fieldUpdated(dealer.address, existingDealer.address)
				|| fieldUpdated(dealer.city, existingDealer.city)
				|| fieldUpdated(dealer.state, existingDealer.state)
				|| fieldUpdated(dealer.zip, existingDealer.zip)) {
			dealer.insertUpdatedField("address", dateModified);
		}

		if (fieldUpdated(dealer.phone, existingDealer.phone)) {
			dealer.insertUpdatedField("phone", dateModified);
		}

		if (fieldUpdated(dealer.comments, existingDealer.comments)) {
			dealer.insertUpdatedField("comments", dateModified);
		}

		if (operatingHoursUpdated(dealer, existingDealer)) {
			dealer.insertUpdatedField("hours", dateModified);
		}
					/* log.debug(Logs.DEBUG, "JUNK: Combined updatedFields ("
								+ dealer.customer_number + "): "
								+ dealer.getUpdatedFieldsCsv()); */
	}

	private static void addDamageIfNotPresent(DeliveryVin dvin, Damage damage) {
		for (Damage dmg : dvin.damages) {
			if (dmg.type_code_id == damage.type_code_id
					&& dmg.svrty_code_id == damage.svrty_code_id
					&& dmg.area_code_id == damage.area_code_id
					&& dmg.special_code_id == damage.special_code_id) {
				return;
			}
			// TODO: Fix up foreignKey on damage images to match the new damage_id
			// If there is an existing image of type Constants.IMAGE_DAMAGE, its foreignKey
			// will be set to the damage_id of the existing damage.  Need to find it and set its
			// foreignKey to the new damage_id.
		}
		// TODO: Figure out best way to keep driver-added images sorted to the top.
		// Probably need to just make two passes through the list.
		dvin.damages.add(damage);
	}

	public static TrainingRequirement parseTrainingRequirement(Context context, JSONObject trainingHolder) {
		JSONObject trainingJson = null;
		try {
			trainingJson = trainingHolder.getJSONObject("TrainingRequirement");
		} catch (JSONException e) {
			log.warn(Logs.DISPATCH, "Failed to parse training requirement", e);
			log.debug(Logs.DEBUG, "Failed to parse training requirement: " + trainingHolder.toString(), e);
			return null;
		}

		Gson gson = new GsonBuilder()
				.registerTypeAdapter(ShuttleMove.class, new GsonTypeAdapters.ShuttleMoveSerializer())
				.registerTypeAdapter(Date.class, new GsonTypeAdapters.DateSerializer())
				.create();

		TrainingRequirement req = gson.fromJson(trainingJson.toString(), TrainingRequirement.class);

		if (req.load_id == null && req.user_id == null) {
			// Bad requirement—assigned to neither a user or a load.
			return null;
		}

		return req;
	}

	private static boolean deliveryVinsMatch(DeliveryVin existingDeliveryVin, DeliveryVin incomingDeliveryVin) {
		if (existingDeliveryVin.delivery_vin_remote_id.equals(incomingDeliveryVin.delivery_vin_remote_id)) {
			return true;
		} else if (existingDeliveryVin.vin == null || incomingDeliveryVin.vin == null ||
				HelperFuncs.isNullOrWhitespace(existingDeliveryVin.vin.vin_number) ||
				HelperFuncs.isNullOrWhitespace(incomingDeliveryVin.vin.vin_number)) {
			return false;
		}
		return existingDeliveryVin.vin.vin_number.equals(incomingDeliveryVin.vin.vin_number);
	}

	private static boolean fieldUpdated(String existingDeliveryValue, String JsonValue) {
		if (!existingDeliveryValue.equals(JsonValue)) {
			return true;
		}
		return false;
	}

	public static boolean operatingHoursUpdated(Dealer d1, Dealer d2) {
		if (d1.monam.equals(d2.monam) && d1.monpm.equals(d2.monpm) &&
				d1.tueam.equals(d2.tueam) && d1.tuepm.equals(d2.tuepm) &&
				d1.wedam.equals(d2.wedam) && d1.wedpm.equals(d2.wedpm) &&
				d1.thuam.equals(d2.thuam) && d1.thupm.equals(d2.thupm) &&
				d1.friam.equals(d2.friam) && d1.fripm.equals(d2.fripm) &&
				d1.satam.equals(d2.satam) && d1.satpm.equals(d2.satpm) &&
				d1.sunam.equals(d2.sunam) && d1.sunpm.equals(d2.sunpm))
		{
			return false;
		}
		return true;
	}

}
