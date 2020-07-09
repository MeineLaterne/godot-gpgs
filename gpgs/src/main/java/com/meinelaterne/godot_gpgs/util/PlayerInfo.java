package com.meinelaterne.godot_gpgs.util;

import android.app.Activity;

import com.google.android.gms.games.Player;

public class PlayerInfo extends ConcreteSubject implements Subscriber {

    public static final String SIGNAL_ICON_REQUESTED = "player_icon_request_handled";
    public static final String SIGNAL_BANNER_REQUESTED = "player_banner_request_handled";

    private static final String TAG = "gpgs";

    private Activity activity = null;
    private GodotCache imageCache;

    public Player player = null;

    public PlayerInfo(Activity activity, Player player){
        this.activity = activity;
        this.player = player;
        imageCache = new GodotCache(activity);
        imageCache.addSubscriber(this);
    }

    public boolean requestPlayerIcon(boolean hiRes){
        if (player != null){
            if (hiRes && player.hasHiResImage()){
                imageCache.sendURIImage(
                        player.getHiResImageUri(),
                        player.getPlayerId()+"_hi_res_icon.png",
                        PlayerInfo.SIGNAL_ICON_REQUESTED,
                        player.getPlayerId());
            }else if (player.hasIconImage()){
                imageCache.sendURIImage(
                        player.getIconImageUri(),
                        player.getPlayerId()+"_icon.png",
                        PlayerInfo.SIGNAL_ICON_REQUESTED,
                        player.getPlayerId());
            }

            return true;
        }
        return false;
    }

    public boolean requestPlayerBanner(boolean portrait){
        if (player != null){
            if (portrait){
                imageCache.sendURIImage(
                        player.getBannerImagePortraitUri(),
                        player.getPlayerId()+"_banner_portrait.png",
                        PlayerInfo.SIGNAL_BANNER_REQUESTED,
                        player.getPlayerId());
            }else{
                imageCache.sendURIImage(
                        player.getBannerImageLandscapeUri(),
                        player.getPlayerId()+"_banner_landscape.png",
                        PlayerInfo.SIGNAL_BANNER_REQUESTED,
                        player.getPlayerId());
            }
            return true;
        }
        return false;
    }

    @Override
    public void update(String message, Object... args) {
        updateSubscribers(message, args);
    }

}
