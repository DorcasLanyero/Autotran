package com.cassens.autotran.constants;

/**
 * Project : AUTOTRAN Description : URLS class keep all urls of app
 *
 * @author Hemant Creation Date : 12-11-2013
 **/
public class URLS
{
	public static final String HOST_URL_CONSTANT = ServerHost.HOST_URL_CONSTANT;

	//public static final String IP_OVERRIDE = "http://54.84.80.4/";
	public static final String IP_OVERRIDE = "http://52.73.230.71/";

	public static String download_diagnostic_data = HOST_URL_CONSTANT + "api/download_diagnostic_data.json";
	public static String upload_diagnostic_data = HOST_URL_CONSTANT + "api/upload_diagnostic_data.json";

	public static String login = HOST_URL_CONSTANT + "api/login.json";
	public static String pick_load = HOST_URL_CONSTANT + "api/pick_load.json";
	public static String poll_loads = HOST_URL_CONSTANT + "api/poll_loads.json";
	public static String poll_supervisors = HOST_URL_CONSTANT + "api/poll_supervisors.json";

	public static String POST_SAVE_VIN_IMAGE = HOST_URL_CONSTANT + "api/save_vin_image.json";
	public static String POST_SAVE_LOAD_FULL = HOST_URL_CONSTANT + "api/save_load_full.json";
	public static String POST_SAVE_DELIVERY_FULL = HOST_URL_CONSTANT + "api/save_delivery_full.json";
	public static final String POST_SAVE_TRAINING_REQUIREMENT = HOST_URL_CONSTANT + "api/save_training_requirement.json";
    public static final String POST_SAVE_ADHOC_TRAINING = HOST_URL_CONSTANT + "api/save_adhoc_training.json";

	public static String POST_SAVE_YARD_INVENTORY = HOST_URL_CONSTANT + "api/save_yard_inventory.json";
	public static String POST_SAVE_LOAD_EVENT = HOST_URL_CONSTANT + "api/save_load_event.json";

    public static String mark_driver_action_status = HOST_URL_CONSTANT + "api/mark_driver_action_status.json";
	public static String consolidated_data_pull = HOST_URL_CONSTANT + "api/consolidated_data_pull.json";
	public static String save_problem_report = HOST_URL_CONSTANT + "api/save_problem_report.json";
	public static String save_inspection = HOST_URL_CONSTANT + "api/save_inspection.json";
}