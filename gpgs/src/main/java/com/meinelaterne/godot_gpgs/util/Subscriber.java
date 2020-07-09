package com.meinelaterne.godot_gpgs.util;

public interface Subscriber {
    void update(String message, Object... args);
}
