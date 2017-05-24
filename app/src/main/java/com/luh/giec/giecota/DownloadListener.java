package com.luh.giec.giecota;

/**
 * Created by Administrator on 2017/5/23.
 */

public interface DownloadListener {
    void onProgress(int progress);

    void onSuccess();

    void onFailed();

    void onPaused();

    void onCanceled();
}
