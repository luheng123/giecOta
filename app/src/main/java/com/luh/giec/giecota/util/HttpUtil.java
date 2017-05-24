package com.luh.giec.giecota.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by Administrator on 2017/5/22.
 */

public class HttpUtil {
    public static void sendOkHttpRequest(String address, long downloadedLength, okhttp3.Callback
            callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().addHeader("RANGE", "bytes=" + downloadedLength +
                "-").url(address).build();
        client.newCall(request).enqueue(callback);
    }
}