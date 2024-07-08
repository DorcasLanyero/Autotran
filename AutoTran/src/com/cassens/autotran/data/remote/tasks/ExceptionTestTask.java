package com.cassens.autotran.data.remote.tasks;

import android.os.AsyncTask;

import java.util.ArrayList;

/**
 * Created by john on 11/30/17.
 * for async exception testing
 */

public class ExceptionTestTask extends AsyncTask {
    @Override
    protected Object doInBackground(Object[] objects) {
        generateConcurrentModificationException();
        return null;
    }

    public static void generateConcurrentModificationException() {
        ArrayList<String> list = new ArrayList<String>();

        list.add("A");
        list.add("B");
        list.add("C");
        list.add("D");
        list.add("E");

        for(String string : list) {
            if(string.equals("C")) {
                list.remove(string);
            }
        }
    }
}
