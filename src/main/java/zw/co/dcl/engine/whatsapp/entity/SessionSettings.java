package zw.co.dcl.engine.whatsapp.entity;

import lombok.Data;

@Data
public class SessionSettings {
    /**
     * timeout of user inactivity in min.
     * Default to 3min
     */
    private int inactivityTimeout = 3;

    private Long debounceTimeoutInMs = 2000L;

    /**
     * time to consider received message as valid
     *
     * if timestamp is > this, consider as old and disregard.
     * Default to 10sec
     */
    private int webhookSecTimestampThreshold = 10;

    private boolean handleSessionInactivity = true;

    /**
     * global session timeout in mins
     */
    private int sessionTTL = 240;

    /**
     * send when user is not logged in.
     * Usually menu with login options and other
     * external (non-session) options for user to select
     */
    private String startMenuStageKey;
}
