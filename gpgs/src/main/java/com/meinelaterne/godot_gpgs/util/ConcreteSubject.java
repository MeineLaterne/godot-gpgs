package com.meinelaterne.godot_gpgs.util;

import java.util.ArrayList;

public class ConcreteSubject implements Subject {
    protected ArrayList<Subscriber> subscribers;

    public ConcreteSubject() {}


    @Override
    public void addSubscriber(Subscriber subscriber) {
        if (subscribers == null) {
            subscribers = new ArrayList<>();
        }

        if (!subscribers.contains(subscriber)) {
            subscribers.add(subscriber);
        }
    }

    @Override
    public void updateSubscribers(String message, Object... args) {
        for (Subscriber s : subscribers) s.update(message, args);
    }
}
