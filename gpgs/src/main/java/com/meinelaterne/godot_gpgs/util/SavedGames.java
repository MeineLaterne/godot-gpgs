package com.meinelaterne.godot_gpgs.util;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

public class SavedGames extends ConcreteSubject  {

    public static final String SIGNAL_SAVED_GAME_LOADING_STARTED = "saved_game_loading_started";
    public static final String SIGNAL_SAVED_GAME_LOADED = "saved_game_loaded";
    public static final String SIGNAL_SAVED_GAME_READY = "saved_game_ready";
    public static final String SIGNAL_SAVED_GAME_SAVED = "saved_game_saved";

    private static final String TAG = "gpgs";

    private static final String SNAPSHOT_NAME_PREFIX = "snapshot-";
    private static final int RC_SAVED_GAMES = 9009;

    private Activity activity = null;
    private GoogleSignInAccount signedInAccount = null;

    private SnapshotsClient snapshotsClient;
    private boolean savingFile = false;
    private int conflictResolutionPolicy = SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED;
    private GodotCache imageCache;

    public SavedGames(Activity activity, GoogleSignInAccount signedInAccount) {
        this.signedInAccount = signedInAccount;
        this.activity = activity;
        imageCache = new GodotCache(activity);
    }

    // If allowAddButton is true then depending on the user selection, a previous save can be overwritten or a new save can be created
    // If allowAddButton is false then depending on the user selection, a previous save can be loaded
    public void showSavedGamesUI(String title, boolean allowAddButton, boolean allowDelete, int maxSavedGamesToShow){
        savingFile = allowAddButton;
        snapshotsClient = Games.getSnapshotsClient(activity, signedInAccount);
        Task<Intent> intentTask = snapshotsClient.getSelectSnapshotIntent(title, allowAddButton, allowDelete, maxSavedGamesToShow);

        intentTask.addOnSuccessListener(new OnSuccessListener<Intent>() {
            @Override
            public void onSuccess(Intent intent) {
                activity.startActivityForResult(intent, RC_SAVED_GAMES);
            }
        });
    }

    public void onMainActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == RC_SAVED_GAMES && intent != null){
            if (intent.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA)){
                // If the user selects an existing saved game file
                SnapshotMetadata snapshotMetadata = intent.getParcelableExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA);
                String snapshotName = snapshotMetadata.getUniqueName();

                if (savingFile){
                    String suggestedImagePath = GodotCache.CACHE_FOLDER + "/" + snapshotName + "_img.png";
                    updateSubscribers(SavedGames.SIGNAL_SAVED_GAME_READY, snapshotName, suggestedImagePath);
                }else{
                    Log.d(TAG, "Loading existing save. unique id: " + snapshotName);
                    updateSubscribers(SavedGames.SIGNAL_SAVED_GAME_LOADING_STARTED);

                    requestLoadSnapshot(snapshotName);
                }
            }else if (intent.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_NEW)){
                // If the user selects to create a new saved game
                if (savingFile){
                    Log.d(TAG, "Creating new save");
                    String snapshotName = SNAPSHOT_NAME_PREFIX + new BigInteger(281, new Random()).toString(13);
                    String suggestedImagePath = GodotCache.CACHE_FOLDER + "/" + snapshotName + "_img.png";
                    updateSubscribers(SavedGames.SIGNAL_SAVED_GAME_READY, snapshotName, suggestedImagePath);
                }
            }
        }
    }

    public void requestWriteSnapshot(String snapshotName, final String data, final String description, final String imageFileName){

        if (snapshotsClient == null) {
            snapshotsClient = Games.getSnapshotsClient(activity, signedInAccount);
        }

        snapshotsClient.open(snapshotName, true, conflictResolutionPolicy)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "ERROR while opening snapshot for saving: ", e);
                        updateSubscribers(SavedGames.SIGNAL_SAVED_GAME_SAVED, false);
                    }
                })
                .continueWith(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, Object>() {
                    @Override
                    public Object then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                        Snapshot snapshot = task.getResult().getData();

                        snapshot.getSnapshotContents().writeBytes(data.getBytes("UTF-8"));

                        Bitmap coverImage = imageCache.getBitmap(imageFileName);
                        SnapshotMetadataChange metadata;
                        if (coverImage != null){
                            metadata = new SnapshotMetadataChange.Builder()
                                    .setCoverImage(coverImage)
                                    .setDescription(description)
                                    .build();
                        }else{
                            metadata = new SnapshotMetadataChange.Builder()
                                    .setDescription(description)
                                    .build();
                        }

                        snapshotsClient.commitAndClose(snapshot, metadata);
                        return null;
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<Object>() {
                    @Override
                    public void onSuccess(Object s) {
                        updateSubscribers(SavedGames.SIGNAL_SAVED_GAME_SAVED, true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "ERROR while writing to snapshot for saving: ", e);
                        updateSubscribers(SavedGames.SIGNAL_SAVED_GAME_SAVED, false);
                    }
                });
    }

    public void requestLoadSnapshot(String snapshotName){

        if (snapshotsClient == null) {
            snapshotsClient = Games.getSnapshotsClient(activity, signedInAccount);
        }

        snapshotsClient.open(snapshotName, true, conflictResolutionPolicy)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "ERROR while opening snapshot for loading: ", e);
                        updateSubscribers(SavedGames.SIGNAL_SAVED_GAME_LOADED, "", false);
                    }
                })
                .continueWith(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, String>() {
                    @Override
                    public String then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                        Snapshot snapshot = task.getResult().getData();

                        try{
                            return new String(snapshot.getSnapshotContents().readFully(),"UTF-8");
                        } catch (IOException e){
                            Log.e(TAG, "ERROR while opening snapshot for loading: ", e);
                            updateSubscribers(SavedGames.SIGNAL_SAVED_GAME_LOADED, "", false);
                        }

                        return null;
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        updateSubscribers(SavedGames.SIGNAL_SAVED_GAME_LOADED, task.getResult(), true);
                    }
                });
    }

    public void setConflictResolutionPolicy(int value){
        conflictResolutionPolicy = value;
    }
}
