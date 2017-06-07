package com.luh.giec.giecota;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.luh.giec.giecota.MainActivity.DIRECTORY;


/**
 * Created by Administrator on 2017/5/23.
 */

class DownloadTask extends AsyncTask<String, Integer, Integer> {

    private static final int TYPE_SUCCESS = 0;
    private static final int TYPE_FAILED = 1;
    private static final int TYPE_PAUSED = 2;
    private static final int TYPE_CANCELED = 3;

    private DownloadListener listener;

    private boolean isCanceled = false;

    private boolean isPaused = false;

    private int lastProgress;


    DownloadTask(DownloadListener listener) {
        this.listener = listener;
    }

    @Override
    protected Integer doInBackground(String... params) {
        InputStream is = null;
        RandomAccessFile saveFile = null;
        File file = null;
        try {
            long downloadedLength = 0;//已下载的长度
            String downloadUrl = params[0];
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            /* String directory = Environment.getExternalStoragePublicDirectory
                                (Environment.DIRECTORY_DOWNLOADS).getPath();*/
            file = new File(DIRECTORY + fileName);
            String TAG = "luh-giec";
            Log.d(TAG, "file路径" + DIRECTORY + fileName);
            if (file.exists()) {
                downloadedLength = file.length();
                Log.d(TAG, "file exists" + " downloadedLength= " + downloadedLength);
            }
            long contentLength = getContentLength(downloadUrl);
            Log.d(TAG, "contentLength=" + contentLength);
            if (contentLength == 0) {
                Log.d(TAG, "contentLength=0");
                return TYPE_FAILED;
            } else if (contentLength == downloadedLength) {
                Log.d(TAG, "contentLength==downloadedLength没有进行下载，已存在");
                return TYPE_SUCCESS;
            }
            if (downloadedLength > contentLength) {
                return TYPE_FAILED;
            }

            //downloadStart
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().addHeader("RANGE", "bytes=" +
                    downloadedLength + "-").url(downloadUrl).build();
            Response response = client.newCall(request).execute();
            if (response != null) {
                is = response.body().byteStream();
                saveFile = new RandomAccessFile(file, "rw");
                saveFile.seek(downloadedLength);
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while ((len = is.read(b)) != -1) {
                    if (isCanceled) {
                        Log.d(TAG, "isCanceled");
                        return TYPE_CANCELED;
                    } else if (isPaused) {
                        Log.d(TAG, "isPaused");
                        return TYPE_PAUSED;
                    } else {
                        total += len;
                        saveFile.write(b, 0, len);

                        Integer values[] = {(int) ((total + downloadedLength) * 100 /
                                contentLength), (int) (total + downloadedLength) / 1024 / 1024,
                                (int) contentLength / 1024 / 1024};
                        publishProgress(values);
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (saveFile != null) {
                    saveFile.close();
                }
                if (isCanceled && file != null) {
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
        //downloadEnd
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if (progress > lastProgress) {
            listener.onProgress(values);
            lastProgress = progress;
        }
    }

    @Override
    protected void onPostExecute(Integer integer) {
        switch (integer) {
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
            default:
                break;
        }
    }

    void pauseDownload() {
        isPaused = true;
    }

    void cancelDownload() {
        isCanceled = true;
    }


    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(downloadUrl).build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.body().close();
            return contentLength;
        }
        return 0;
    }
}
