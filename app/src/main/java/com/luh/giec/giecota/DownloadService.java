package com.luh.giec.giecota;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import java.io.File;

public class DownloadService extends Service {


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
