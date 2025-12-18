package org.dcl.jawce.server.constant;

public class Constant {
    public static final String LIVE_MODE_CACHE_KEY = "liveMode";

    public static final String CHANNEL_CHAT_LIST = "CHAT_LIST";
    public static final String CHANNEL_CHAT_UPDATE = "CHAT_UPDATE";
    public static final String CHANNEL_MESSAGE = "MESSAGE";

    public static String getTicket(Long id) {
        return String.format("TICKET-%05d", id);
    }
}
