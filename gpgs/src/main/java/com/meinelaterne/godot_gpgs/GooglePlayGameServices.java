package com.meinelaterne.godot_gpgs;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.games.PlayerLevelInfo;
import com.meinelaterne.godot_gpgs.util.Achievements;
import com.meinelaterne.godot_gpgs.util.Client;
import com.meinelaterne.godot_gpgs.util.GodotCache;
import com.meinelaterne.godot_gpgs.util.Leaderboard;
import com.meinelaterne.godot_gpgs.util.Network;
import com.meinelaterne.godot_gpgs.util.PlayerInfo;
import com.meinelaterne.godot_gpgs.util.SavedGames;
import com.meinelaterne.godot_gpgs.util.Subscriber;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class GooglePlayGameServices extends GodotPlugin implements Subscriber {

    public static final String GODOT_SUB_FOLDER = "files";
    public static final String CACHE_FOLDER = "gpgs_lib_cache";

    private static final String TAG = "gpgs";

    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private Activity activity;

    private GoogleSignInAccount signedInAccount;
    private Client client;
    private Network network;
    private Achievements achievements;
    private Leaderboard leaderboard;
    private SavedGames savedGames;

    private boolean savedGamesEnabled = false;

    public GooglePlayGameServices(Godot godot) {
        super(godot);
        activity = getActivity();
    }

    /**
     * @param useSavedGames Specifies whether or not play game services will be compiled with saved games functionality or not
     */
    public void init(boolean useSavedGames) {
        client = new Client(activity, this, useSavedGames);
        network = new Network(activity);
        savedGamesEnabled = useSavedGames;

        client.addSubscriber(this);
    }

    public void clearCache(){
        GodotCache.clearCache(activity);
    }

    public void keepScreenOn(final boolean keepOn){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (keepOn) activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
    }

    public void setClient(GoogleSignInAccount signedInAccount) {
        this.signedInAccount = signedInAccount;
        this.achievements = new Achievements(activity, signedInAccount);
        this.leaderboard = new Leaderboard(activity, signedInAccount);
        this.savedGames = new SavedGames(activity, signedInAccount);

        savedGames.addSubscriber(this);

        Log.d(TAG, "setClient: " + signedInAccount.toString());
    }

    public void removeClient(){
        this.signedInAccount = null;
        this.achievements = null;
        this.leaderboard = null;
        this.savedGames = null;
    }

    @Override
    public void onMainActivityResult(int requestCode, int resultCode, Intent data) {
        client.onMainActivityResult(requestCode, resultCode, data);
        savedGames.onMainActivityResult(requestCode, resultCode, data);
    }

    //region Connection methods -----------------------------------------------------------------------------

    /**
     * Sign In while showing the full UI
     *
     * @godot_callback _on_play_game_services_sign_in_success(signInType, playerId)
     * @godot_callback _on_play_game_services_player_info_failure
     * @godot_callback _on_play_game_services_sign_in_failure(signInType)
     * @return true if sign in process is started
     */
    public void signInInteractive() {
        if (client == null) return;
        client.signInInteractive();
    }

    /**
     * Sign In silently with minimal interference
     *
     * @godot_callback _on_play_game_services_sign_in_success(signInType, playerId)
     * @godot_callback _on_play_game_services_sign_in_failure(signInType)
     * @return true if sign in process is started
     */
    public void signInSilent() {
        if (client == null) return;
        client.signInSilent();
    }

    /**
     * Sign Out method
     *
     * @godot_callback _on_play_game_services_sign_out_success()
     * @godot_callback _on_play_game_services_sign_out_failure()
     * @return true if sign out process is started
     */
    public void signOut() {
        if (client == null) return;
        client.signOut();
    }

    //endregion

    //region Network Methods --------------------------------------------------------------------------------

    /**
     * Is the user connected to the internet
     *
     * @return true if user is online
     */
    public boolean isOnline() {
        if (network == null) return false;
        return network.isOnline();
    }

    /**
     * Is the user connected to the WiFi
     *
     * @return true if user is connected to WiFi
     */
    public boolean isWifiConnected() {
        if (network == null) return false;
        return network.isWifiConnected();
    }

    /**
     * Is the user connected to the Mobile network
     *
     * @return true if user is connected to Mobile network
     */
    public boolean isMobileConnected() {
        if (network == null) return false;
        return network.isMobileConnected();
    }

    //endregion

    //region Currently Signed In Player Information Methods -------------------------------------------------

    public String getCurrentPlayerID(){
        if (client != null && client.getCurrentPlayer() != null)
            return client.getCurrentPlayer().player.getPlayerId();
        return "";
    }

    public String getCurrentPlayerDisplayName(){
        if (client != null && client.getCurrentPlayer() != null)
            return client.getCurrentPlayer().player.getDisplayName();
        return "";
    }

    public String getCurrentPlayerTitle(){
        if (client != null && client.getCurrentPlayer() != null)
            return client.getCurrentPlayer().player.getTitle();
        return "";
    }

    public int getCurrentPlayerLevel(){
        if (client != null && client.getCurrentPlayer() != null) {
            PlayerLevelInfo levelInfo = client.getCurrentPlayer().player.getLevelInfo();
            if (levelInfo != null)
                return levelInfo.getCurrentLevel().getLevelNumber();
        }
        return 0;
    }

    public String getCurrentPlayerXP(){
        if (client != null && client.getCurrentPlayer() != null)
            return "" + client.getCurrentPlayer().player.getLevelInfo().getCurrentXpTotal();
        return "";
    }

    public String getCurrentPlayerMaxXP(){
        if (client != null && client.getCurrentPlayer() != null)
            return "" + client.getCurrentPlayer().player.getLevelInfo().getCurrentLevel().getMaxXp();
        return "";
    }

    public String getCurrentPlayerMinXP(){
        if (client != null && client.getCurrentPlayer() != null)
            return "" + client.getCurrentPlayer().player.getLevelInfo().getCurrentLevel().getMinXp();
        return "";
    }

    /**
     * @godot_callback _on_play_game_services_player_icon_requested(id, folder, fileName)
     */
    public boolean requestCurrentPlayerIcon(boolean hiRes){
        if (client != null)
            return client.getCurrentPlayer().requestPlayerIcon(hiRes);
        return false;
    }

    /**
     * @godot_callback _on_play_game_services_player_banner_portrait_requested(id, folder, fileName)
     * @godot_callback _on_play_game_services_player_banner_landscape_requested(id, folder, fileName)
     */
    public boolean requestCurrentPlayerBanner(boolean portrait){
        if (client != null)
            return client.getCurrentPlayer().requestPlayerBanner(portrait);
        return false;
    }

    //endregion

    //region Achievements -----------------------------------------------------------------------------------

    public boolean showAchievementsUI() {
        return achievements != null && achievements.showAchievementsUI();
    }

    public void unlockAchievement(String achievementID){
        if (achievements != null) achievements.unlockAchievement(achievementID);
    }

    public void incrementAchievement(String achievementID, int incrementBy){
        if (achievements != null) achievements.incrementAchievement(achievementID, incrementBy);
    }

    //endregion

    //region Leaderboard ------------------------------------------------------------------------------------

    public boolean showLeaderboardUI(String leaderboardID){
        return leaderboard != null && leaderboard.showLeaderboardUI(leaderboardID);
    }

    public void submitScore(String leaderboardID, int score){
        if (leaderboard != null) leaderboard.submitScore(leaderboardID, score);
    }

    //endregion

    //region Saved Games (Snapshots) ------------------------------------------------------------------------

    public void showSavedGamesUI(String title, boolean allowAddButton, boolean allowDelete, int maxSavedGamesToShow){
        Log.d(TAG, "showSavedGamesUI()");
        if (savedGamesEnabled) {
            if (savedGames != null)
                savedGames.showSavedGamesUI(title, allowAddButton, allowDelete, maxSavedGamesToShow);
        }else {
            Log.d(TAG, "Saved Games not enabled. Need to pass in true for the second input of the singleton's init() function to use this functionality.");
        }
    }

    public void requestWriteSnapshot(String snapshotName, String data, String description, String imageFileName){
        if (savedGamesEnabled) {
            if (savedGames != null)
                savedGames.requestWriteSnapshot(snapshotName, data, description, imageFileName);
        }else {
            Log.d(TAG, "Saved Games not enabled. Need to pass in true for the second input of the singleton's init() function to use this functionality.");
        }
    }

    public void requestLoadSnapshot(String snapshotName){
        if (savedGamesEnabled) {
            if (savedGames != null)
                savedGames.requestLoadSnapshot(snapshotName);
        }else {
            Log.d(TAG, "Saved Games not enabled. Need to pass in true for the second input of the singleton's init() function to use this functionality.");
        }
    }

    //endregion
    @NonNull
    @Override
    public String getPluginName() {
        return "GooglePlayGameServices";
    }

    @NonNull
    @Override
    public List<String> getPluginMethods() {
        return Arrays.asList(
                "init", "clearCache","keepScreenOn","getDelimiter",
                "signInInteractive", "signInSilent", "signOut",
                "isOnline", "isWifiConnected", "isMobileConnected",
                "getCurrentPlayerID","getCurrentPlayerDisplayName","getCurrentPlayerTitle",
                "getCurrentPlayerLevel","getCurrentPlayerXP","getCurrentPlayerMaxXP","getCurrentPlayerMinXP",
                "requestCurrentPlayerIcon","requestCurrentPlayerBanner",
                "showAchievementsUI","unlockAchievement","incrementAchievement",
                "showLeaderboardUI","submitScore",
                "showSavedGamesUI","requestWriteSnapshot","requestLoadSnapshot"
        );
    }

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signals = new ArraySet<>();

        signals.add(new SignalInfo(Client.SIGNAL_SIGN_IN_COMPLETE, int.class, String.class));
        signals.add(new SignalInfo(Client.SIGNAL_SIGN_IN_FAILED, int.class));
        signals.add(new SignalInfo(Client.SIGNAL_SIGN_OUT, Boolean.class));
        signals.add(new SignalInfo(Client.SIGNAL_GET_PLAYER_INFO_FAILED, int.class));

        signals.add(new SignalInfo(SavedGames.SIGNAL_SAVED_GAME_LOADING_STARTED));
        signals.add(new SignalInfo(SavedGames.SIGNAL_SAVED_GAME_LOADED, String.class, Boolean.class));
        signals.add(new SignalInfo(SavedGames.SIGNAL_SAVED_GAME_READY, String.class, String.class));
        signals.add(new SignalInfo(SavedGames.SIGNAL_SAVED_GAME_SAVED, Boolean.class));

        signals.add(new SignalInfo(PlayerInfo.SIGNAL_ICON_REQUESTED, String.class, String.class, String.class));
        signals.add(new SignalInfo(PlayerInfo.SIGNAL_BANNER_REQUESTED, String.class, String.class, String.class));

        return signals;
    }

    @Override
    public void onMainPause() {
        Log.d(TAG, "Main Activity paused.");
    }

    @Override
    public void onMainResume() {
        Log.d(TAG, "Main Activity resumed.");
    }

    @Override
    public void update(String message, Object... args) {
        Log.d(TAG, "GPGS: emitting signal " + message);
        emitSignal(message, args);
    }

}
