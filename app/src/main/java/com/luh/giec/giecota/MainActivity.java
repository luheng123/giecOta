package com.luh.giec.giecota;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.RecoverySystem;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import com.luh.giec.giecota.util.HttpUtil;
import com.luh.giec.giecota.util.JsonUtil;
import com.luh.giec.giecota.util.MyApplication;
import com.luh.giec.giecota.util.SystemUtils;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public final static String CURRENT_VERSION = SystemProperties.get("ro.product.version");
    public final static String DOWNLOAD_URL = SystemProperties.get("ro.product.ota.host");
    //    public final static String CURRENT_VERSION = "1.0.0";
//    public final static String DOWNLOAD_URL = "http://10.0.2.2/";
    public final static String DIRECTORY = "/data/media/0";
    private ProgressDialog progressDialog;
    private SharedPreferences prefs;
    private ProgressBar progressBar;
    private DownloadTask downloadTask;
    private long firstTime = 0;
    private Button pauseDownload;
    private DrawerLayout mDrawerLayout;
    private ImageButton btn_menu;
    private Button checkUpdate_bt;
    private Button cancelDownload;
    private TextView tv_version_tittle, tv_phoneBrand, tv_phoneModel, show_size,
            tv_update_frequency;
    private String versionName, phoneBrand, phoneModel;
    private int checkHour = 8;


    private DownloadListener listener = new DownloadListener() {
        @Override
        public void onProgress(Integer... values) {
            getNotificationManager().notify(1, getNotification(MainActivity.this.getResources()
                    .getString(R.string.Downloading), values[0]));
            progressBar.setProgress(values[0]);
            show_size.setText(values[1] + "/" + values[2] + " MB");
        }

        @Override
        public void onSuccess() {
            downloadTask = null;
            getNotificationManager().notify(1, getNotification(MainActivity.this.getResources()
                    .getString(R.string.download_success), -1));
            String downloadUrl = prefs.getString("url", null);
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            /* String directory = Environment.getExternalStoragePublicDirectory
                                (Environment.DIRECTORY_DOWNLOADS).getPath();*/
            File file = new File(DIRECTORY + fileName);
            try {
                RecoverySystem.installPackage(MainActivity.this, file);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onFailed() {
            downloadTask = null;
            getNotificationManager().notify(1, getNotification(MainActivity.this.getResources()
                    .getString(R.string.download_fail), -1));
        }

        @Override
        public void onPaused() {
            downloadTask = null;
        }

        @Override
        public void onCanceled() {
            downloadTask = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent getIntent = getIntent();
        boolean canUpdate = getIntent.getBooleanExtra("canUpdate", false);

        Log.d("luh-notification", "can update =" + canUpdate);

        //初始化控件
        show_size = (TextView) findViewById(R.id.show_size);
        tv_update_frequency = (TextView) findViewById(R.id.tv_update_frequency);
        checkUpdate_bt = (Button) findViewById(R.id.checkUpdate_bt);
        progressBar = (ProgressBar) findViewById(R.id.down_progress);
        cancelDownload = (Button) findViewById(R.id.cancel_download);
        pauseDownload = (Button) findViewById(R.id.pause_download);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.layout_drawer);
        btn_menu = (ImageButton) findViewById(R.id.btn_menu);
        tv_version_tittle = (TextView) findViewById(R.id.tv_version_tittle);
        tv_phoneBrand = (TextView) findViewById(R.id.tv_phoneBrand);
        tv_phoneModel = (TextView) findViewById(R.id.tv_phoneModel);
        //绑定控件
        checkUpdate_bt.setOnClickListener(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        cancelDownload.setOnClickListener(this);
        pauseDownload.setOnClickListener(this);
        btn_menu.setOnClickListener(this);
        tv_update_frequency.setOnClickListener(this);

        versionName = SystemUtils.getBuildVersion();
        phoneModel = SystemUtils.getPhoneModel();
        phoneBrand = SystemUtils.getPhoneBrand();
        String str = String.format(getResources().getString(R.string.version_number_tittle),
                versionName);
        tv_version_tittle.setText(str);
        tv_phoneBrand.setText(phoneBrand);
        tv_phoneModel.setText(getResources().getString(R.string.phone_model) + phoneModel);
        cancelDownload.setText(R.string.bt_cancel);
        pauseDownload.setText(R.string.bt_pause);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission
                .WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission
                    .WRITE_EXTERNAL_STORAGE}, 1);
        }
        if (canUpdate) {
            showNeedUpdateDialog();
        }


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.checkUpdate_bt:
                getRemoteJson();
                break;
            case R.id.cancel_download:
                String downloadUrl = prefs.getString("url", null);
                if (downloadUrl != null) {
                    if (downloadTask != null) {//正在下载时点击取消
                        downloadTask.cancelDownload();
                    } else {//暂停时点击取消
                        String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                       /* String directory = Environment.getExternalStoragePublicDirectory
                                (Environment.DIRECTORY_DOWNLOADS).getPath();*/
                        File file = new File(DIRECTORY + fileName);
                        if (file.exists()) {
                            file.delete();
                        }
                    }
                    getNotificationManager().cancel(1);
                }
                progressBar.setProgress(0);
                cancelDownload.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
                pauseDownload.setVisibility(View.INVISIBLE);
                checkUpdate_bt.setVisibility(View.VISIBLE);
                pauseDownload.setText(R.string.bt_pause);
                show_size.setVisibility(View.INVISIBLE);
                break;
            case R.id.pause_download:
                if (downloadTask != null) {
                    downloadTask.pauseDownload();
                    pauseDownload.setText(R.string.bt_continue);
                } else {
                    downloadTask = new DownloadTask(listener);
                    downloadTask.execute(prefs.getString("url", null));
                    getNotificationManager().notify(1, getNotification("Downloading...", 0));
                    pauseDownload.setText(R.string.bt_pause);
                }
                break;
            case R.id.btn_menu:
                mDrawerLayout.openDrawer(GravityCompat.END);
                break;
            case R.id.tv_update_frequency:
                int h = prefs.getInt("frequency", 8);
                switch (h) {
                    case 4:
                        h = 0;
                        break;
                    case 8:
                        h = 1;
                        break;
                    case 12:
                        h = 2;
                        break;
                    case 24:
                        h = 3;
                        break;
                    default:
                        break;
                }
                showUpdateFrequencyDailog(h);
            default:
                break;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] != PackageManager
                        .PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.Denial_permission, Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            default:
        }
    }


    /**
     * 展示自动跟新选项dailog
     */
    private void showUpdateFrequencyDailog(int hour) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.Update_Dialog_Title);
        String[] hourList = {"4 " + MainActivity.this.getResources().getString(R.string
                .frequency_dialog_hour), "8 " + MainActivity.this.getResources().getString(R
                .string.frequency_dialog_hour), "12 " + MainActivity.this.getResources()
                .getString(R.string.frequency_dialog_hour), "24 " + MainActivity.this
                .getResources().getString(R.string.frequency_dialog_hour)};

        builder.setSingleChoiceItems(hourList, hour, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        checkHour = 4;
                        break;
                    case 1:
                        checkHour = 8;
                        break;
                    case 2:
                        checkHour = 12;
                        break;
                    case 3:
                        checkHour = 24;
                        break;
                    default:
                        break;
                }
            }
        });
        builder.setPositiveButton(R.string.frequency_dialog_Positive, new DialogInterface
                .OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences
                        (MainActivity.this).edit();
                editor.putInt("frequency", checkHour);
                Log.d("luh--", "onClick: " + checkHour);
                editor.apply();
                Intent intent = new Intent(MainActivity.this, DownloadService.class);
                startService(intent);
            }
        });
        builder.setNegativeButton(R.string.bt_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }


    /**
     * 展示一个是否升级的对话框
     */
    private void showNeedUpdateDialog() {
        if (SystemUtils.isNeedUpdate(CURRENT_VERSION, prefs.getString("version", null))) {

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(R.string.Update_Dialog_Title);
            dialog.setMessage(prefs.getString("description", null));
            dialog.setCancelable(false);
            dialog.setPositiveButton(R.string.bt_update, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    downloadTask = new DownloadTask(listener);
                    downloadTask.execute(prefs.getString("url", null));
                    pauseDownload.setVisibility(View.VISIBLE);
                    cancelDownload.setVisibility(View.VISIBLE);
                    checkUpdate_bt.setVisibility(View.INVISIBLE);
                    progressBar.setVisibility(View.VISIBLE);
                    show_size.setVisibility(View.VISIBLE);
                    getNotificationManager().notify(1, getNotification(MainActivity.this
                            .getResources().getString(R.string.Downloading), 0));
                }
            });
            dialog.setNegativeButton(R.string.bt_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            dialog.show();
        } else {
            Toast.makeText(MainActivity.this, R.string.no_useable_update, Toast.LENGTH_SHORT)
                    .show();
        }
    }


    /**
     * 访问服务器下载当前版本对应的Json文件
     */
    private void getRemoteJson() {

        showProgressDialog();
        HttpUtil.sendOkHttpRequest(DOWNLOAD_URL + CURRENT_VERSION + "/update.json", 0, new
                Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(MyApplication.getContext(), R.string.no_internation, Toast
                                .LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response != null && response.isSuccessful()) {
                    String responseText = response.body().string();
                    JsonUtil.parseAndSaveJson(responseText);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        showNeedUpdateDialog();
                    }
                });
            }
        });
    }

    /**
     * 显示一个缓冲框
     */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(this.getResources().getString(R.string.showProgress_Dialog));
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 取消缓冲框
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unbindService(connection);
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private Notification getNotification(String title, int progress) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setContentIntent(pi);
        builder.setContentTitle(title);
        if (progress > 0) {
            builder.setContentText(progress + "%");
            builder.setProgress(100, progress, false);
        }
        return builder.build();

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (System.currentTimeMillis() - firstTime > 2000) {
                Toast.makeText(MainActivity.this, R.string.press_exit, Toast.LENGTH_SHORT).show();
                firstTime = System.currentTimeMillis();
            } else {
                if (downloadTask != null) {
                    downloadTask.pauseDownload();
                }
                getNotificationManager().cancel(1);
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
