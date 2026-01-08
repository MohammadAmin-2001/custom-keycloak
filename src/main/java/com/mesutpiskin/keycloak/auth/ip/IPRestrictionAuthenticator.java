package com.mesutpiskin.keycloak.auth.ip;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * IP Restriction Authenticator
 * Checks client IP against configured allow/deny rules before authentication
 * Supports single IPs and CIDR notation
 * Uses + prefix for allow, - prefix for deny
 */
public class IPRestrictionAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(IPRestrictionAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        
        // If no configuration, allow access
        if (config == null) {
            logger.debug("IP restriction authenticator has no configuration, allowing access");
            context.success();
            return;
        }

        Map<String, String> configMap = config.getConfig();
        
        // Get client IP address
        String clientIP = getClientIP(context, configMap);
        
        if (clientIP == null || clientIP.isEmpty()) {
            logger.warn("Could not determine client IP address, allowing access");
            context.success();
            return;
        }

        logger.debugf("Checking IP restriction for client IP: %s", clientIP);

        // Get IP rules from configuration
        List<String> ipRules = getIPRules(configMap);
        
        logger.infof("Loaded %d IP rules: %s", ipRules.size(), ipRules);
        
        if (ipRules.isEmpty()) {
            logger.debug("No IP rules configured, allowing access");
            context.success();
            return;
        }

        // Check IP against rules
        IPCheckResult result = checkIPAgainstRules(clientIP, ipRules);
        
        if (result.isAllowed()) {
            logger.debugf("IP %s is allowed (matched rule: %s)", clientIP, result.getMatchedRule());
            context.success();
        } else {
            logger.infof("IP %s is blocked (reason: %s, matched rule: %s)", 
                clientIP, result.getReason(), result.getMatchedRule());
            
            // Log event for failed login due to IP restriction
            logIPRestrictionEvent(context, clientIP, result, ipRules, configMap);
            
            // Get appropriate error message
            String errorMessage = getErrorMessage(configMap, result.isExplicitDeny());
            
            // Block access
            context.failure(AuthenticationFlowError.INVALID_USER, 
                createErrorResponse(context, errorMessage));
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // This authenticator doesn't require user interaction
        context.success();
    }

    @Override
    public boolean requiresUser() {
        // This runs before user authentication, so no user is required
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // No required actions
    }

    @Override
    public void close() {
        // Nothing to close
    }

    /**
     * Get client IP address, checking X-Forwarded-For if configured
     */
    private String getClientIP(AuthenticationFlowContext context, Map<String, String> config) {
        HttpRequest request = context.getHttpRequest();
        
        boolean checkXForwardedFor = Boolean.parseBoolean(
            config.getOrDefault(IPRestrictionConstants.CHECK_X_FORWARDED_FOR, 
                               IPRestrictionConstants.DEFAULT_CHECK_X_FORWARDED_FOR)
        );

        String clientIP = null;
        String remoteAddr = context.getConnection().getRemoteAddr();
        
        logger.infof("checkXForwardedFor setting: %b", checkXForwardedFor);
        logger.infof("Remote address from connection: %s", remoteAddr);

        // Check X-Forwarded-For header if enabled
        if (checkXForwardedFor) {
            String forwardedFor = request.getHttpHeaders().getHeaderString("X-Forwarded-For");
            logger.infof("X-Forwarded-For header raw value: '%s'", forwardedFor);
            
            if (forwardedFor != null && !forwardedFor.isEmpty()) {
                clientIP = IPUtils.extractIPFromForwardedHeader(forwardedFor);
                logger.infof("Extracted IP from X-Forwarded-For: '%s' (original header: '%s')", clientIP, forwardedFor);
            } else {
                logger.infof("X-Forwarded-For header is null or empty");
            }
        }

        // Fallback to remote address from connection
        if (clientIP == null || clientIP.isEmpty()) {
            clientIP = remoteAddr;
            logger.infof("Using remote address as client IP: %s", clientIP);
        }
        
        logger.infof("Final client IP determined: '%s'", clientIP);

        return clientIP;
    }

    /**
     * Get IP rules from configuration as a list
     * Supports multivalued configuration
     */
    private List<String> getIPRules(Map<String, String> config) {
        List<String> rules = new ArrayList<>();
        
        logger.infof("Reading IP rules from config. Config map keys: %s", config.keySet());
        
        // Check for multivalued configuration (key with index: ip-rules##0, ip-rules##1, etc.)
        int index = 0;
        while (true) {
            String key = IPRestrictionConstants.IP_RULES + "##" + index;
            String value = config.get(key);
            
            logger.debugf("Checking key '%s': value='%s'", key, value);
            
            if (value == null || value.trim().isEmpty()) {
                // No more values
                break;
            }
            
            rules.add(value.trim());
            logger.infof("Added multivalued rule #%d: '%s'", index, value.trim());
            index++;
        }
        
        // If no multivalued entries found, check for single value
        if (rules.isEmpty()) {
            String singleValue = config.get(IPRestrictionConstants.IP_RULES);
            logger.infof("No multivalued rules found. Single value: '%s'", singleValue);
            if (singleValue != null && !singleValue.trim().isEmpty()) {
                // Keycloak stores multivalued strings with ## separator
                // Also support newlines and commas for backward compatibility
                String[] splitRules = singleValue.split("##|[\\r\\n,]+");
                for (String rule : splitRules) {
                    if (!rule.trim().isEmpty()) {
                        rules.add(rule.trim());
                        logger.infof("Added split rule: '%s'", rule.trim());
                    }
                }
            }
        }
        
        logger.infof("Final rules list: %s", rules);
        return rules;
    }

    /**
     * Check IP against all rules
     * Returns the result with allow/deny status and matched rule
     */
    private IPCheckResult checkIPAgainstRules(String clientIP, List<String> rules) {
        boolean hasAllowRules = false;
        boolean hasExplicitDeny = false;
        String matchedRule = null;

        logger.infof("Checking IP '%s' against %d rules", clientIP, rules.size());

        // First pass: Check for explicit deny rules (-)
        for (String rule : rules) {
            logger.debugf("Checking rule: '%s' (starts with deny: %b)", rule, rule.startsWith(IPRestrictionConstants.PREFIX_DENY));
            if (rule.startsWith(IPRestrictionConstants.PREFIX_DENY)) {
                String ipPattern = rule.substring(1).trim();
                boolean matches = IPUtils.matchesRule(clientIP, ipPattern);
                logger.infof("Deny rule '%s' -> IP pattern '%s' matches '%s': %b", rule, ipPattern, clientIP, matches);
                if (matches) {
                    logger.infof("IP '%s' BLOCKED by deny rule: '%s'", clientIP, rule);
                    return new IPCheckResult(false, true, rule, 
                        IPRestrictionConstants.RESTRICTION_REASON_BLOCKED);
                }
            } else if (rule.startsWith(IPRestrictionConstants.PREFIX_ALLOW)) {
                hasAllowRules = true;
            }
        }

        // Second pass: Check for allow rules (+)
        for (String rule : rules) {
            if (rule.startsWith(IPRestrictionConstants.PREFIX_ALLOW)) {
                String ipPattern = rule.substring(1).trim();
                if (IPUtils.matchesRule(clientIP, ipPattern)) {
                    return new IPCheckResult(true, false, rule, "Allowed");
                }
            }
        }

        // If there are allow rules but IP didn't match any, deny
        if (hasAllowRules) {
            return new IPCheckResult(false, false, "none", 
                IPRestrictionConstants.RESTRICTION_REASON_NOT_ALLOWED);
        }

        // No rules matched and no allow rules exist - allow by default
        return new IPCheckResult(true, false, "default", "No restrictions");
    }

    /**
     * Log event for IP restriction failure
     */
    private void logIPRestrictionEvent(AuthenticationFlowContext context, String clientIP, 
                                      IPCheckResult result, List<String> allRules, 
                                      Map<String, String> config) {
        EventBuilder event = context.getEvent();
        
        // Get X-Forwarded-For for logging if it exists
        String forwardedFor = context.getHttpRequest().getHttpHeaders().getHeaderString("X-Forwarded-For");
        
        event.detail(IPRestrictionConstants.EVENT_DETAIL_CLIENT_IP, clientIP)
             .detail(IPRestrictionConstants.EVENT_DETAIL_MATCHED_RULE, result.getMatchedRule())
             .detail(IPRestrictionConstants.EVENT_DETAIL_RULE_TYPE, 
                    result.isExplicitDeny() ? IPRestrictionConstants.RULE_TYPE_DENY : 
                                             IPRestrictionConstants.RULE_TYPE_NO_MATCH)
             .detail(IPRestrictionConstants.EVENT_DETAIL_ALL_RULES, String.join(", ", allRules))
             .detail("reason", result.getReason());
        
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            event.detail(IPRestrictionConstants.EVENT_DETAIL_X_FORWARDED_FOR, forwardedFor);
        }
        
        event.error(Errors.NOT_ALLOWED);
    }

    /**
     * Get error message based on deny type
     */
    private String getErrorMessage(Map<String, String> config, boolean isExplicitDeny) {
        if (isExplicitDeny) {
            return config.getOrDefault(IPRestrictionConstants.ERROR_MESSAGE_BLOCKED, 
                                      IPRestrictionConstants.DEFAULT_ERROR_MESSAGE_BLOCKED);
        } else {
            return config.getOrDefault(IPRestrictionConstants.ERROR_MESSAGE_NOT_ALLOWED, 
                                      IPRestrictionConstants.DEFAULT_ERROR_MESSAGE_NOT_ALLOWED);
        }
    }

    /**
     * Create error response
     */
    private Response createErrorResponse(AuthenticationFlowContext context, String errorMessage) {
        return context.form()
            .setError(errorMessage)
            .createErrorPage(Response.Status.FORBIDDEN);
    }

    /**
     * Inner class to hold IP check result
     */
    private static class IPCheckResult {
        private final boolean allowed;
        private final boolean explicitDeny;
        private final String matchedRule;
        private final String reason;

        public IPCheckResult(boolean allowed, boolean explicitDeny, String matchedRule, String reason) {
            this.allowed = allowed;
            this.explicitDeny = explicitDeny;
            this.matchedRule = matchedRule;
            this.reason = reason;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public boolean isExplicitDeny() {
            return explicitDeny;
        }

        public String getMatchedRule() {
            return matchedRule;
        }

        public String getReason() {
            return reason;
        }
    }
}
