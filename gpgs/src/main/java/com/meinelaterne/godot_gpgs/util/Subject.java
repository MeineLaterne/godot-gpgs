package com.meinelaterne.godot_gpgs.util;

public interface Subject {
    void addSubscriber(Subscriber subscriber);
    void updateSubscribers(String message, Object... args);
}
