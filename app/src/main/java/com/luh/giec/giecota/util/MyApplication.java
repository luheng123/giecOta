package com.luh.giec.giecota.util;

import android.app.Application;
import android.content.Context;

/**
 * Created by Administrator on 2017/5/23.
 */

public class MyApplication extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }
}
