package com.meinelaterne.godot_gpgs.util;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.meinelaterne.godot_gpgs.GooglePlayGameServices;

public class Client extends ConcreteSubject implements Subscriber {

    public static final String SIGNAL_SIGN_IN_COMPLETE = "sign_in_complete";
    public static final String SIGNAL_SIGN_IN_FAILED = "sign_in_failed";
    public static final String SIGNAL_SIGN_OUT = "sign_out_complete";
    public static final String SIGNAL_GET_PLAYER_INFO_FAILED = "get_player_info_failed";

    private static final String TAG = "gpgs";

    private static final int SIGN_IN_SILENT = 0;
    private static final int SIGN_IN_INTERACTIVE = 1;

    // Request code used to invoke sign in user interactions.
    private static final int RC_SIGN_IN = 9001;

    // Client used to sign in with Google APIs
    private GoogleSignInClient mGoogleSignInClient;

    // The currently signed in account, used to check the account has changed outside of this activity when resuming.
    GoogleSignInAccount mSignedInAccount;

    // The activity that this code will run in when the game is deployed
    private Activity activity;

    // The main class object for this module
    private GooglePlayGameServices gpgs;

    private PlayerInfo currentPlayer = null;

    public Client(final Activity activity, GooglePlayGameServices gpgs, boolean buildSnapshots) {

        this.activity = activity;
        this.gpgs = gpgs;

        Log.d(TAG, "Client()");

        // Create the client used to sign in.
        if (buildSnapshots){
            Log.d(TAG, "Creating sign in client with Saved Games functionality");
            GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                    .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(activity, signInOptions);
        }else{
            Log.d(TAG, "Creating sign in client");
            mGoogleSignInClient = GoogleSignIn.getClient(activity, GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);
        }
    }

    /**
     * Start a sign in activity.
     */
    public void signInInteractive() {
        Log.d(TAG, "signInInteractive()");
        activity.startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }


    /**
     * Try to sign in without displaying dialogs to the user.
     * If the user has already signed in previously, it will not show dialog.
     */
    public void signInSilent() {
        Log.d(TAG, "signInSilent()");
        mGoogleSignInClient.silentSignIn().addOnCompleteListener(activity, new OnCompleteListener<GoogleSignInAccount>() {
            @Override
            public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "signInSilent(): success");
                    onConnected(task.getResult(), SIGN_IN_SILENT);
                } else {
                    Log.d(TAG, "signInSilent(): failure", task.getException());
                    onDisconnected();

                    updateSubscribers(Client.SIGNAL_SIGN_IN_FAILED);

                    //GodotLib.calldeferred(instance_id, GODOT_CALLBACK_FUNCTIONS[2], new Object[] { SIGN_IN_SILENT });
                }
            }
        });
    }

    public void signOut() {
        Log.d(TAG, "signOut()");

        mGoogleSignInClient.signOut().addOnCompleteListener(activity, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "signOut(): success");
                    onDisconnected();

                    updateSubscribers(Client.SIGNAL_SIGN_OUT, true);

                } else {
                    int code = ((ApiException) task.getException()).getStatusCode();
                    Log.d(TAG, "signOut() failed with API Exception status code: " + code);

                    updateSubscribers(Client.SIGNAL_SIGN_OUT, false);

                }
            }
        });
    }

    public void onMainActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(intent);

            if (result == null) {
                Log.d(TAG, "Client.onMainActivityResult: condition result == null is true. aborting.");
                return;
            }

            if (result.isSuccess()) {
                GoogleSignInAccount signedInAccount = result.getSignInAccount();
                if (signedInAccount == null) {
                    Log.d(TAG, "Client.onMainActivityResult: Received null from result.getSignInAccount(). Aborting.");
                    return;
                }

                onConnected(signedInAccount, SIGN_IN_INTERACTIVE);

                // This line was needed to show the popups for "Welcome back", "achievement unlocked", etc.
                // Ugh, this thing was annoying to figure out haha.
                Games.getGamesClient(activity, signedInAccount).setViewForPopups(activity.findViewById(android.R.id.content));
            } else {
                String message = result.getStatus().getStatusMessage();
                if (message != null) {
                    Log.d(TAG, "Connection error. ApiException message: " + message);
                }

                onDisconnected();

                updateSubscribers(Client.SIGNAL_SIGN_IN_FAILED, SIGN_IN_INTERACTIVE);

            }
        }
    }

    private void onConnected(GoogleSignInAccount googleSignInAccount, int signInType) {
        Log.d(TAG, "onConnected(): connected to Google APIs");
        if (mSignedInAccount != googleSignInAccount) {
            mSignedInAccount = googleSignInAccount;
            gpgs.setClient(mSignedInAccount);
            getSignedInPlayer(signInType);
        }
    }

    public void onDisconnected() {
        Log.d(TAG, "onDisconnected()");
        mSignedInAccount = null;
        currentPlayer = null;
        gpgs.removeClient();
    }

    public void getSignedInPlayer(final int signInType){
        PlayersClient playersClient = Games.getPlayersClient(activity, mSignedInAccount);

        playersClient.getCurrentPlayer().addOnSuccessListener(new OnSuccessListener<Player>() {
            @Override
            public void onSuccess(Player p) {
                setCurrentPlayer(new PlayerInfo(activity, p));
                updateSubscribers(Client.SIGNAL_SIGN_IN_COMPLETE, signInType, p.getPlayerId());
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG,"There was a problem getting the player id!");
                updateSubscribers(Client.SIGNAL_SIGN_IN_FAILED, signInType);
            }
        });
    }

    @Override
    public void update(String message, Object... args) {
        updateSubscribers(message, args);
    }

    @Override
    public void updateSubscribers(String message, Object... args) {
        Log.d(TAG, "Client.updateSubscribers(): updating " + subscribers.size() + " subscribers");
        super.updateSubscribers(message, args);
    }

    public void setCurrentPlayer(PlayerInfo value) {
        currentPlayer = value;
        currentPlayer.addSubscriber(this);
        Log.d(TAG, "Client.setCurrentPlayer(): " + currentPlayer.player.getDisplayName());
    }

    public PlayerInfo getCurrentPlayer() {
        return currentPlayer;
    }

}