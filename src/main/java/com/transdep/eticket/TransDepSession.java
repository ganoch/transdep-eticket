package com.transdep.eticket;

import org.apache.http.client.CookieStore;

import java.util.UUID;

/**
 * Tracks TransDep session state, including cookies, route context, and duration.
 */
public class TransDepSession {
    private final long durationMs;
    private String sessionId;
    private long startTimestamp;
    private long lastAccessTimestamp;
    private boolean active;
    private CookieStore cookieJar;
    private String departure;
    private String stop;
    private String destination;
    private String dispatcherId;

    public TransDepSession(CookieStore cookieJar, long durationMs) {
        this.durationMs = durationMs;
        this.cookieJar = cookieJar;
        startSession(System.currentTimeMillis());
    }

    public void startSession(long now) {
        this.sessionId = UUID.randomUUID().toString();
        this.startTimestamp = now;
        this.lastAccessTimestamp = now;
        this.active = true;
    }

    public void touch(long now) {
        this.lastAccessTimestamp = now;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isExpired(long now) {
        return active && (now - startTimestamp) >= durationMs;
    }

    public void clear() {
        this.active = false;
        this.departure = null;
        this.stop = null;
        this.destination = null;
        this.dispatcherId = null;
        this.cookieJar = null;
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getLastAccessTimestamp() {
        return lastAccessTimestamp;
    }

    public CookieStore getCookieJar() {
        return cookieJar;
    }

    public void setCookieJar(CookieStore cookieJar) {
        this.cookieJar = cookieJar;
    }

    public String getDeparture() {
        return departure;
    }

    public void setDeparture(String departure) {
        this.departure = departure;
    }

    public String getStop() {
        return stop;
    }

    public void setStop(String stop) {
        this.stop = stop;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDispatcherId() {
        return dispatcherId;
    }

    public void setDispatcherId(String dispatcherId) {
        this.dispatcherId = dispatcherId;
    }
}
