package com.luh.giec.giecota.util;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Administrator on 2017/5/23.
 */

public class JsonUtil {
    public static void parseAndSaveJson(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            if (jsonObject.getString("status").equals("ok")) {
                JSONObject jsonObjectUpdate = jsonObject.getJSONObject("update");
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences
                        (MyApplication.getContext()).edit();
                editor.putString("version", jsonObjectUpdate.getString("version"));
                editor.putString("description", jsonObjectUpdate.getString("description"));
                editor.putString("url", jsonObjectUpdate.getString("url"));
                editor.apply();

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
