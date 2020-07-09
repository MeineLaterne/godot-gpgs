package com.meinelaterne.godot_gpgs.util;

import android.app.Activity;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.games.Games;
import com.google.android.gms.tasks.OnSuccessListener;

public class Leaderboard {
    private static final String TAG = "gpgs";

    private static final int RC_LEADERBOARD_UI = 9004;

    private Activity activity = null;
    private GoogleSignInAccount signedInAccount = null;

    public Leaderboard(Activity activity, GoogleSignInAccount signedInAccount) {
        this.signedInAccount = signedInAccount;
        this.activity = activity;
    }

    public boolean showLeaderboardUI(String leaderboardID){
        if (signedInAccount != null){
            Games.getLeaderboardsClient(activity,signedInAccount)
                    .getLeaderboardIntent(leaderboardID)
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                            activity.startActivityForResult(intent, RC_LEADERBOARD_UI);
                        }
                    });
            return true;
        }
        return false;
    }

    public void submitScore(String leaderboardID, int score){
        if (signedInAccount != null)
            Games.getLeaderboardsClient(activity, signedInAccount).submitScore(leaderboardID, score);
    }
}
