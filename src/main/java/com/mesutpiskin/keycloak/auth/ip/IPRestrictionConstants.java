package com.mesutpiskin.keycloak.auth.ip;

public class IPRestrictionConstants {
    // Configuration keys
    public static final String IP_RULES = "ip-rules";
    public static final String CHECK_X_FORWARDED_FOR = "check-x-forwarded-for";
    public static final String ERROR_MESSAGE_BLOCKED = "error-message-blocked";
    public static final String ERROR_MESSAGE_NOT_ALLOWED = "error-message-not-allowed";
    
    // IP rule prefixes
    public static final String PREFIX_ALLOW = "+";
    public static final String PREFIX_DENY = "-";
    
    // Default values
    public static final String DEFAULT_IP_RULES = "";
    public static final String DEFAULT_CHECK_X_FORWARDED_FOR = "true";
    public static final String DEFAULT_ERROR_MESSAGE_BLOCKED = "Access from your IP address is blocked";
    public static final String DEFAULT_ERROR_MESSAGE_NOT_ALLOWED = "Access from your IP address is not allowed";
    
    // Event details keys
    public static final String EVENT_DETAIL_CLIENT_IP = "client_ip";
    public static final String EVENT_DETAIL_MATCHED_RULE = "matched_rule";
    public static final String EVENT_DETAIL_RULE_TYPE = "rule_type";
    public static final String EVENT_DETAIL_ALL_RULES = "all_rules";
    public static final String EVENT_DETAIL_X_FORWARDED_FOR = "x_forwarded_for";
    
    // Rule types for events
    public static final String RULE_TYPE_DENY = "DENY";
    public static final String RULE_TYPE_ALLOW = "ALLOW";
    public static final String RULE_TYPE_NO_MATCH = "NO_MATCH";
    
    // Error reasons
    public static final String RESTRICTION_REASON_BLOCKED = "IP Restriction: IP address is explicitly blocked";
    public static final String RESTRICTION_REASON_NOT_ALLOWED = "IP Restriction: IP address is not in allowed list";
}
