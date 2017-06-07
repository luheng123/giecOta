package com.luh.giec.giecota;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.File;

import static com.luh.giec.giecota.MainActivity.DIRECTORY;

public class BootCompleteReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        File file = new File(DIRECTORY + "/update.zip");
        if (file.exists()) {
            file.delete();
        }
        Intent intentService = new Intent(context, DownloadService.class);
        context.startService(intentService);
    }
}
