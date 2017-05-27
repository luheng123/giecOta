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
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
    public final static String CURRENT_VERSION = "1.0.8";
    public final static String DOWNLOAD_URL = "http://10.0.2.2/";
    private ProgressDialog progressDialog;
    SharedPreferences prefs;
    private ProgressBar progressBar;
    private DownloadTask downloadTask;
    private long firstTime = 0;
    Button pauseDownload;
    private DrawerLayout mDrawerLayout;
    private ImageButton btn_menu;
    private Button checkUpdate_bt;
    private Button cancelDownload;
    private TextView tv_version_tittle, tv_phoneBrand, tv_phoneModel, show_size;
    private String versionName, phoneBrand, phoneModel;


    private DownloadListener listener = new DownloadListener() {
        @Override
        public void onProgress(Integer... values) {
            getNotificationManager().notify(1, getNotification("Downloading...", values[0]));
            progressBar.setProgress(values[0]);
            show_size.setText(values[1] + "/" + values[2] + " MB");
        }

        @Override
        public void onSuccess() {
            downloadTask = null;
            getNotificationManager().notify(1, getNotification("Download Success,please check " +
                    "and" + " install", -1));

        }

        @Override
        public void onFailed() {
            downloadTask = null;
            getNotificationManager().notify(1, getNotification("Download Failed", -1));
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
        //初始化控件
        show_size = (TextView) findViewById(R.id.show_size);
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
                    if (downloadTask != null) {
                        downloadTask.pauseDownload();
                        String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                        String directory = Environment.getExternalStoragePublicDirectory
                                (Environment.DIRECTORY_DOWNLOADS).getPath();
                        File file = new File(directory + fileName);
                        if (file.exists()) {
                            file.delete();
                        }
                    } else {
                        String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                        String directory = Environment.getExternalStoragePublicDirectory
                                (Environment.DIRECTORY_DOWNLOADS).getPath();
                        File file = new File(directory + fileName);
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
                show_size.setVisibility(View.INVISIBLE);
                break;
            case R.id.pause_download:
                if (downloadTask != null) {
                    downloadTask.pauseDownload();
                } else {
                    downloadTask = new DownloadTask(listener);
                    downloadTask.execute(prefs.getString("url", null));
                    getNotificationManager().notify(1, getNotification("Downloading...", 0));
                }
                break;
            case R.id.btn_menu:
                mDrawerLayout.openDrawer(GravityCompat.END);
                break;
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
                    Toast.makeText(this, "拒绝权限将无法使用程序", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            default:
        }
    }


    /**
     * 展示一个是否升级的对话框
     */
    private void showNeedUpdateDialog() {
        if (isNeedUpdate(CURRENT_VERSION, prefs.getString("version", null))) {

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("检查到新版本");
            dialog.setMessage(prefs.getString("description", null));
            dialog.setCancelable(false);
            dialog.setPositiveButton("升级", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    downloadTask = new DownloadTask(listener);
                    downloadTask.execute(prefs.getString("url", null));
                    pauseDownload.setVisibility(View.VISIBLE);
                    cancelDownload.setVisibility(View.VISIBLE);
                    checkUpdate_bt.setVisibility(View.INVISIBLE);
                    progressBar.setVisibility(View.VISIBLE);
                    show_size.setVisibility(View.VISIBLE);
                    getNotificationManager().notify(1, getNotification("Downloading...", 0));
                }
            });
            dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            dialog.show();
        } else {
            Toast.makeText(MainActivity.this, "无可用升级", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 与远程版本进行比较判断是否需要升级
     */
    private boolean isNeedUpdate(String currentVersion, String remoteVersion) {
        return remoteVersion != null && currentVersion != null && currentVersion.compareTo
                (remoteVersion) < 0;
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
                        Toast.makeText(MyApplication.getContext(), "无网络", Toast.LENGTH_SHORT)
                                .show();
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
            progressDialog.setMessage("正在从服务器获取更新信息");
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
                Toast.makeText(MainActivity.this, "再按一次退出程序并停止下载", Toast.LENGTH_SHORT).show();
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
