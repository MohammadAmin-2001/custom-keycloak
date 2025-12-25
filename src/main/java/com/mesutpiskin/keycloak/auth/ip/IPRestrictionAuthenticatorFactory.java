package com.mesutpiskin.keycloak.auth.ip;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

public class IPRestrictionAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "ip-restriction-authenticator";
    private static final IPRestrictionAuthenticator SINGLETON = new IPRestrictionAuthenticator();

    @Override
    public String getDisplayType() {
        return "IP Address Restriction";
    }

    @Override
    public String getReferenceCategory() {
        return "ip-restriction";
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
        return "Restricts authentication based on client IP address. Use + prefix to allow IPs and - prefix to deny IPs. Supports CIDR notation (e.g., +192.168.0.0/24, -10.0.0.1).";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
            // IP Rules - Multivalued list with add/remove buttons
            .property()
                .name(IPRestrictionConstants.IP_RULES)
                .label("IP Rules")
                .helpText("IP address rules. Use + to allow (e.g., +192.168.1.1 or +192.168.0.0/24) and - to deny (e.g., -10.0.0.5). You can add multiple rules using the + button.")
                .type(ProviderConfigProperty.MULTIVALUED_STRING_TYPE)
                .add()
            
            // Check X-Forwarded-For header
            .property()
                .name(IPRestrictionConstants.CHECK_X_FORWARDED_FOR)
                .label("Check X-Forwarded-For Header")
                .helpText("Enable this if Keycloak is behind a reverse proxy (nginx, Apache, load balancer). The authenticator will use the IP from the X-Forwarded-For header.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(IPRestrictionConstants.DEFAULT_CHECK_X_FORWARDED_FOR)
                .add()
            
            // Error message for blocked IPs
            .property()
                .name(IPRestrictionConstants.ERROR_MESSAGE_BLOCKED)
                .label("Error Message (Blocked IP)")
                .helpText("Message to display when an IP is explicitly blocked (matched a - rule)")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(IPRestrictionConstants.DEFAULT_ERROR_MESSAGE_BLOCKED)
                .add()
            
            // Error message for not allowed IPs
            .property()
                .name(IPRestrictionConstants.ERROR_MESSAGE_NOT_ALLOWED)
                .label("Error Message (Not Allowed)")
                .helpText("Message to display when an IP is not in the allowed list (no + rule matched)")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(IPRestrictionConstants.DEFAULT_ERROR_MESSAGE_NOT_ALLOWED)
                .add()
            
            .build();
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
