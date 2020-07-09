package com.meinelaterne.godot_gpgs.util;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class Network {

    private Activity activity = null;
    private ConnectivityManager connectivityManager = null;

    private static final String TAG = "gpgs";

    public Network(final Activity activity) {
        this.activity = activity;

        connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);

        Log.d(TAG, "GPGS: Network init");
    }

    private boolean isConnected(int type) {
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(type);

        if (networkInfo != null)
            return networkInfo.isConnected();

        return false;
    }

    public boolean isOnline() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return (networkInfo != null && networkInfo.isConnected());
    }

    public boolean isWifiConnected() {
        return isConnected(ConnectivityManager.TYPE_WIFI);
    }

    public boolean isMobileConnected() {
        return isConnected(ConnectivityManager.TYPE_MOBILE);
    }
}
