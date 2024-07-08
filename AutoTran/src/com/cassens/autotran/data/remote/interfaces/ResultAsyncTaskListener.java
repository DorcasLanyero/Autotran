/**
 * Project : AutoTran
 * Author : Navjot Singh Bedi
 * Creation Date : 17-Dec-2013
 * Description : @TODO
 */
package com.cassens.autotran.data.remote.interfaces;

import org.json.JSONObject;

public interface ResultAsyncTaskListener
{
	public void onSuccess(String result);
    public void onFailure(String message);
}
