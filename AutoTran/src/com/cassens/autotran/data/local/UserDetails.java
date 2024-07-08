package com.cassens.autotran.data.local;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cassens.autotran.CommonUtility;

public class UserDetails
{
	private static UserDetails mUserDetails;
	Transactions trans;
	SQLiteDatabase sqliteDb;
	AutotranDB appDb;
	public String id, firstName, lastName, email, driverNumber, 
	deviceToken, deviceID, password, role, userType,
	activationLink, status, created, modified, fullName, autoInspectLastDelivery;
	
	public static synchronized UserDetails getInstance() {
		if (mUserDetails == null) {
			mUserDetails = new UserDetails();
		}
		return mUserDetails;
	}
	
	
	public boolean UploadUserData(Context context, String driverNumber) {
		trans = new Transactions(context);
		// Get User Details
		Cursor cursor = trans.getUserFromDBForDriverNumber(driverNumber);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				id = cursor.getString(cursor.getColumnIndex("user_id"));
				firstName = cursor.getString(cursor.getColumnIndex("firstName"));
				lastName = cursor.getString(cursor.getColumnIndex("lastName"));
				email = cursor.getString(cursor.getColumnIndex("email"));
				driverNumber = cursor.getString(cursor.getColumnIndex("driverNumber"));
				deviceToken = cursor.getString(cursor.getColumnIndex("deviceID"));
				deviceID = cursor.getString(cursor.getColumnIndex("deviceID"));
				password = cursor.getString(cursor.getColumnIndex("password"));
				role = cursor.getString(cursor.getColumnIndex("role"));
				userType = cursor.getString(cursor.getColumnIndex("userType"));
				activationLink = cursor.getString(cursor.getColumnIndex("activationLink"));
				status = cursor.getString(cursor.getColumnIndex("status"));
				created = cursor.getString(cursor.getColumnIndex("created"));
				modified = cursor.getString(cursor.getColumnIndex("modified"));
				fullName = cursor.getString(cursor.getColumnIndex("fullName"));
				autoInspectLastDelivery = cursor.getString(cursor.getColumnIndex("autoInspectLastDelivery"));
			}
			cursor.close();
		} else {
			CommonUtility.showText("Cursor found null.");
		}
		
		//trans.closeDatabase();
		return true;
	}
}
