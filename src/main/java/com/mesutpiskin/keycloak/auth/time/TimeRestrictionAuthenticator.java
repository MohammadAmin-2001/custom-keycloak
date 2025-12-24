package com.mesutpiskin.keycloak.auth.time;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import jakarta.ws.rs.core.Response;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TimeRestrictionAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(TimeRestrictionAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        
        if (config == null) {
            logger.warn("Time restriction authenticator has no configuration, allowing access");
            context.success();
            return;
        }

        // Get configuration values
        String timezone = config.getConfig().getOrDefault(
            TimeRestrictionConstants.TIMEZONE, 
            TimeRestrictionConstants.DEFAULT_TIMEZONE
        );
        
        String allowedDaysStr = config.getConfig().getOrDefault(
            TimeRestrictionConstants.ALLOWED_DAYS, 
            TimeRestrictionConstants.DEFAULT_ALLOWED_DAYS
        );
        
        String startTimeStr = config.getConfig().getOrDefault(
            TimeRestrictionConstants.START_TIME, 
            TimeRestrictionConstants.DEFAULT_START_TIME
        );
        
        String endTimeStr = config.getConfig().getOrDefault(
            TimeRestrictionConstants.END_TIME, 
            TimeRestrictionConstants.DEFAULT_END_TIME
        );
        
        String errorMessage = config.getConfig().getOrDefault(
            TimeRestrictionConstants.ERROR_MESSAGE, 
            TimeRestrictionConstants.DEFAULT_ERROR_MESSAGE
        );

        try {
            // Get current time in configured timezone
            ZoneId zoneId = ZoneId.of(timezone);
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            DayOfWeek currentDay = now.getDayOfWeek();
            LocalTime currentTime = now.toLocalTime();

            // Parse allowed days
            Set<DayOfWeek> allowedDays = parseAllowedDays(allowedDaysStr);

            // Check if current day is allowed
            if (!allowedDays.contains(currentDay)) {
                logger.infof("Access denied for user %s: Current day %s is not in allowed days %s", 
                    context.getUser().getUsername(), currentDay, allowedDays);
                context.failure(AuthenticationFlowError.INVALID_USER, 
                    createErrorResponse(context, errorMessage));
                return;
            }

            // Parse time restrictions
            LocalTime startTime = LocalTime.parse(startTimeStr);
            LocalTime endTime = LocalTime.parse(endTimeStr);

            // Check if current time is within allowed range
            boolean isTimeAllowed = isTimeInRange(currentTime, startTime, endTime);

            if (!isTimeAllowed) {
                logger.infof("Access denied for user %s: Current time %s is not within allowed range %s - %s (timezone: %s)", 
                    context.getUser().getUsername(), currentTime, startTime, endTime, timezone);
                context.failure(AuthenticationFlowError.INVALID_USER, 
                    createErrorResponse(context, errorMessage));
                return;
            }

            logger.debugf("Access granted for user %s at %s %s (timezone: %s)", 
                context.getUser().getUsername(), currentDay, currentTime, timezone);
            context.success();

        } catch (Exception e) {
            logger.errorf(e, "Error in time restriction authenticator configuration: %s", e.getMessage());
            // In case of configuration error, we allow access to prevent lockout
            context.success();
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // This authenticator doesn't require user interaction
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return true;
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
     * Parse allowed days from comma-separated string
     */
    private Set<DayOfWeek> parseAllowedDays(String allowedDaysStr) {
        Set<DayOfWeek> allowedDays = new HashSet<>();
        String[] days = allowedDaysStr.split(",");
        
        for (String day : days) {
            try {
                allowedDays.add(DayOfWeek.valueOf(day.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                logger.warnf("Invalid day of week: %s", day);
            }
        }
        
        return allowedDays;
    }

    /**
     * Check if current time is within the allowed range
     * Handles cases where end time is before start time (overnight range)
     */
    private boolean isTimeInRange(LocalTime current, LocalTime start, LocalTime end) {
        if (start.isBefore(end)) {
            // Normal range (e.g., 09:00 - 17:00)
            return !current.isBefore(start) && !current.isAfter(end);
        } else {
            // Overnight range (e.g., 22:00 - 06:00)
            return !current.isBefore(start) || !current.isAfter(end);
        }
    }

    /**
     * Create error response with custom message
     */
    private Response createErrorResponse(AuthenticationFlowContext context, String errorMessage) {
        return context.form()
            .setError(errorMessage)
            .createErrorPage(Response.Status.FORBIDDEN);
    }
}
