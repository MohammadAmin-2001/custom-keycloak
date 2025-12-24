package com.mesutpiskin.keycloak.auth.time;

public class TimeRestrictionConstants {
    public static final String ALLOWED_DAYS = "allowed-days";
    public static final String START_TIME = "start-time";
    public static final String END_TIME = "end-time";
    public static final String TIMEZONE = "timezone";
    public static final String ERROR_MESSAGE = "error-message";
    
    public static final String DEFAULT_TIMEZONE = "UTC";
    public static final String DEFAULT_ERROR_MESSAGE = "Access is not allowed at this time";
    public static final String DEFAULT_START_TIME = "00:00";
    public static final String DEFAULT_END_TIME = "23:59";
    public static final String DEFAULT_ALLOWED_DAYS = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY";
}
