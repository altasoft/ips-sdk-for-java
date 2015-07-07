package com.ips.api.client;

import com.google.api.client.util.Key;

import java.util.List;

/**
 * IPS Message Collection
 */
public final class MessageCollection {

    @Key
    private List<Message> items;

    @Key
    private String next;

    public final List<Message> getItems() {
        return items;
    }

    public final void setItems(List<Message> items) {
        this.items = items;
    }

    public final String getNext() {
        return next;
    }

    public final void setNext(String next) {
        this.next = next;
    }
}