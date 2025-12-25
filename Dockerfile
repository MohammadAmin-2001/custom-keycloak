FROM quay.io/keycloak/keycloak:26.2.4

# Copy any custom providers (if you have them)
COPY target/*.jar /opt/keycloak/providers/

# Build Keycloak with configuration
RUN /opt/keycloak/bin/kc.sh build

# Set working directory
WORKDIR /opt/keycloak

# Entrypoint
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]

