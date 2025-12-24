package com.mesutpiskin.keycloak.auth.time;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class TimeRestrictionAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "time-restriction-authenticator";
    private static final TimeRestrictionAuthenticator SINGLETON = new TimeRestrictionAuthenticator();

    @Override
    public String getDisplayType() {
        return "Time/Date Restriction";
    }

    @Override
    public String getReferenceCategory() {
        return "time-restriction";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Restricts authentication based on time of day and day of week";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> configProperties = new ArrayList<>();

        // Timezone configuration
        ProviderConfigProperty timezoneProperty = new ProviderConfigProperty();
        timezoneProperty.setName(TimeRestrictionConstants.TIMEZONE);
        timezoneProperty.setLabel("Timezone");
        timezoneProperty.setType(ProviderConfigProperty.STRING_TYPE);
        timezoneProperty.setDefaultValue(TimeRestrictionConstants.DEFAULT_TIMEZONE);
        timezoneProperty.setHelpText("Timezone for time restriction (e.g., UTC, America/New_York, Europe/London)");
        configProperties.add(timezoneProperty);

        // Allowed days configuration
        ProviderConfigProperty allowedDaysProperty = new ProviderConfigProperty();
        allowedDaysProperty.setName(TimeRestrictionConstants.ALLOWED_DAYS);
        allowedDaysProperty.setLabel("Allowed Days");
        allowedDaysProperty.setType(ProviderConfigProperty.STRING_TYPE);
        allowedDaysProperty.setDefaultValue(TimeRestrictionConstants.DEFAULT_ALLOWED_DAYS);
        allowedDaysProperty.setHelpText("Comma-separated list of allowed days (MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY)");
        configProperties.add(allowedDaysProperty);

        // Start time configuration
        ProviderConfigProperty startTimeProperty = new ProviderConfigProperty();
        startTimeProperty.setName(TimeRestrictionConstants.START_TIME);
        startTimeProperty.setLabel("Start Time");
        startTimeProperty.setType(ProviderConfigProperty.STRING_TYPE);
        startTimeProperty.setDefaultValue(TimeRestrictionConstants.DEFAULT_START_TIME);
        startTimeProperty.setHelpText("Start time in HH:mm format (e.g., 09:00)");
        configProperties.add(startTimeProperty);

        // End time configuration
        ProviderConfigProperty endTimeProperty = new ProviderConfigProperty();
        endTimeProperty.setName(TimeRestrictionConstants.END_TIME);
        endTimeProperty.setLabel("End Time");
        endTimeProperty.setType(ProviderConfigProperty.STRING_TYPE);
        endTimeProperty.setDefaultValue(TimeRestrictionConstants.DEFAULT_END_TIME);
        endTimeProperty.setHelpText("End time in HH:mm format (e.g., 17:00). Can be before start time for overnight ranges.");
        configProperties.add(endTimeProperty);

        // Error message configuration
        ProviderConfigProperty errorMessageProperty = new ProviderConfigProperty();
        errorMessageProperty.setName(TimeRestrictionConstants.ERROR_MESSAGE);
        errorMessageProperty.setLabel("Error Message");
        errorMessageProperty.setType(ProviderConfigProperty.STRING_TYPE);
        errorMessageProperty.setDefaultValue(TimeRestrictionConstants.DEFAULT_ERROR_MESSAGE);
        errorMessageProperty.setHelpText("Message to display when access is denied");
        configProperties.add(errorMessageProperty);

        return configProperties;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {
        // Nothing to initialize
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Nothing to post-initialize
    }

    @Override
    public void close() {
        // Nothing to close
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
