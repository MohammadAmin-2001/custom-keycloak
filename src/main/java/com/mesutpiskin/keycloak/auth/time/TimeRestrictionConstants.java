package com.mesutpiskin.keycloak.auth.time;

public class TimeRestrictionConstants {
    // Configuration keys
    public static final String ALLOWED_DAYS = "allowed-days";
    public static final String START_TIME = "start-time";
    public static final String END_TIME = "end-time";
    public static final String TIMEZONE = "timezone";
    public static final String ERROR_MESSAGE = "error-message";
    
    // Event details keys
    public static final String EVENT_DETAIL_REASON = "reason";
    public static final String EVENT_DETAIL_ALLOWED_DAYS = "allowed_days";
    public static final String EVENT_DETAIL_ALLOWED_TIME_RANGE = "allowed_time_range";
    public static final String EVENT_DETAIL_CURRENT_DAY = "current_day";
    public static final String EVENT_DETAIL_CURRENT_TIME = "current_time";
    public static final String EVENT_DETAIL_TIMEZONE = "timezone";
    
    // Default values
    public static final String DEFAULT_TIMEZONE = "UTC";
    public static final String DEFAULT_ERROR_MESSAGE = "Access is not allowed at this time";
    public static final String DEFAULT_START_TIME = "00:00";
    public static final String DEFAULT_END_TIME = "23:59";
    public static final String DEFAULT_ALLOWED_DAYS = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY";
    
    // Error reasons
    public static final String RESTRICTION_REASON_DAY = "Time/Date restriction: Day not allowed";
    public static final String RESTRICTION_REASON_TIME = "Time/Date restriction: Time not allowed";
}
