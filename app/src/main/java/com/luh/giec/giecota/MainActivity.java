package com.luh.giec.giecota;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import util.HttpUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public final static String CURRENT_VERSION = "1.0.0";
    public final static String DOWNLOAD_URL = "http://10.0.2.2/";
    private ProgressDialog progressDialog;
    private String remoteVersion;
    private String description;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button checkUpdate_bt = (Button) findViewById(R.id.checkUpdate_bt);

        checkUpdate_bt.setOnClickListener(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        remoteVersion = prefs.getString("version", null);


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
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                parseAndSaveJson(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                    }
                });
            }
        });
    }



    /**
     * 将从服务器上获取的Json解析并且保存在本地的sharePreference
     */
    private void parseAndSaveJson(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            if (jsonObject.getString("status").equals("ok")) {
                JSONObject jsonObjectUpdate = jsonObject.getJSONObject("update");
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences
                        (this).edit();
                editor.putString("version", jsonObjectUpdate.getString("version"));
                editor.putString("description", jsonObjectUpdate.getString("description"));
                editor.putString("url", jsonObjectUpdate.getString("url"));
                editor.apply();

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
}
