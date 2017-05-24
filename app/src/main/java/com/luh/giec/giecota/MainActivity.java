package com.luh.giec.giecota;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import com.luh.giec.giecota.util.HttpUtil;
import com.luh.giec.giecota.util.JsonUtil;
import com.luh.giec.giecota.util.MyApplication;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public final static String CURRENT_VERSION = "1.0.0";
    public final static String DOWNLOAD_URL = "http://10.0.2.2/";
    private ProgressDialog progressDialog;
    SharedPreferences prefs;
    private DownloadService.DownloadBinder downloadBinder;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadBinder = (DownloadService.DownloadBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化控件
        Button checkUpdate_bt = (Button) findViewById(R.id.checkUpdate_bt);
        //绑定控件
        checkUpdate_bt.setOnClickListener(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Intent intent = new Intent(this, DownloadService.class);
        startService(intent);
        bindService(intent, connection, BIND_AUTO_CREATE);

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
                    //开始下载
                    downloadBinder.startDownload(prefs.getString("url",null));
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
        unbindService(connection);
    }
}
