package com.luh.giec.giecota;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.luh.giec.giecota.util.HttpUtil;
import com.luh.giec.giecota.util.JsonUtil;
import com.luh.giec.giecota.util.SystemUtils;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.luh.giec.giecota.MainActivity.CURRENT_VERSION;
import static com.luh.giec.giecota.MainActivity.DIRECTORY;
import static com.luh.giec.giecota.MainActivity.DOWNLOAD_URL;

public class DownloadService extends Service {


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkJson();

        //检查更新频率
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int hour = prefs.getInt("frequency", 8);

        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int mTime = 1000 * 60 * 60 * hour;
        long triggerAtTime = SystemClock.elapsedRealtime() + mTime;
        Intent i = new Intent(this, DownloadService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);

        //判断是否需要更新还是已经更新完毕
        String remoteVersion = prefs.getString("version", null);
        if (SystemUtils.isNeedUpdate(CURRENT_VERSION, remoteVersion)) {
            Intent intentDownload = new Intent(this, MainActivity.class);
            intentDownload.putExtra("canUpdate", true);
            PendingIntent piDowndload = PendingIntent.getActivity(this, 0, intentDownload, 0);
            NotificationManager managerNotif = (NotificationManager) getSystemService
                    (NOTIFICATION_SERVICE);
            Notification notification = new NotificationCompat.Builder(this).setContentTitle
                    ("system update").setContentText("new version to update").setWhen(System
                    .currentTimeMillis()).setSmallIcon(R.mipmap.ic_launcher).setLargeIcon
                    (BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                    .setContentIntent(piDowndload).setAutoCancel(true).build();
            managerNotif.notify(2, notification);

        } else {
            String downloadUrl = prefs.getString("url", null);
            if (downloadUrl != null) {
                String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                /*String directory = Environment.getExternalStoragePublicDirectory(Environment
                        .DIRECTORY_DOWNLOADS).getPath();*/
                File file = new File(DIRECTORY + fileName);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void checkJson() {
        Log.d("luh-service", "checkJson");
        HttpUtil.sendOkHttpRequest(DOWNLOAD_URL + CURRENT_VERSION + "/update.json", 0, new
                Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response != null && response.isSuccessful()) {
                    String responseText = response.body().string();
                    JsonUtil.parseAndSaveJson(responseText);
                }
            }
        });
    }
}
