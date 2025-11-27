package com.example.healthylifehub.patterns.factory;

/**
 * Factory Pattern - Base Notification (Product Interface)
 */
public abstract class BaseNotification {
    protected String title;
    protected String message;
    protected String channelId;
    protected int priority;
    protected int iconResId;

    public abstract void configure();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getChannelId() { return channelId; }
    public int getPriority() { return priority; }
    public int getIconResId() { return iconResId; }
}
