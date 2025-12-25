# Keycloak Advanced Security Authenticators

Enhanced Keycloak authenticators with IP restriction, time/date control, and email 2FA for advanced access control.

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Keycloak](https://img.shields.io/badge/Keycloak-26.0.0-blue)]()
[![Java](https://img.shields.io/badge/Java-21-orange)]()
[![License](https://img.shields.io/badge/license-MIT-green)]()



## 🙏 Acknowledgments

**Special thanks to [mesutpiskin/keycloak-2fa-email-authenticator](https://github.com/mesutpiskin/keycloak-2fa-email-authenticator)** - This project is forked from and built upon the excellent Email 2FA authenticator created by Mesut Pişkin. The original work provided the foundation for the email authentication functionality.


## 🎯 Overview

This project provides three powerful authenticators for Keycloak to enhance security and access control:

### 1. **IP Address Restriction Authenticator** ⭐
Block or allow users based on their IP address **before** they even see the login form.
- ✅ Runs **before** username/password (prevents credential enumeration)
- ✅ Allow/Deny rules with `+` and `-` prefixes
- ✅ CIDR notation support (`+192.168.0.0/24`)
- ✅ Multi-value configuration with add/remove UI
- ✅ X-Forwarded-For support for reverse proxies
- ✅ Detailed event logging

### 2. **Time/Date Restriction Authenticator**
Restrict login based on business hours, days of the week, and timezone.
- ✅ Day of week restrictions (e.g., Monday-Friday only)
- ✅ Time range restrictions (e.g., 9 AM - 5 PM)
- ✅ Timezone support
- ✅ Overnight time ranges (e.g., 10 PM - 6 AM)
- ✅ Custom error messages

### 3. **Email 2FA Authenticator**
Two-factor authentication via email verification code.
- ✅ Secure email-based 2FA
- ✅ Configurable code length and TTL
- ✅ Resend and cancel options
- ✅ Conditional authentication support

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.6+
- Keycloak 26.0.0+

### Clone and Build


> This project requires **Java 21** and Maven.

1. **Install Java 21**  
   Check your Java version:
   ```bash
   java -version
   ```
   Ensure it shows Java 21. If not, download and install it from [Adoptium](https://adoptium.net/) or another trusted source.

2. **Install Maven**  
   Verify Maven installation:
   ```bash
   mvn -version
   ```
   If Maven is not installed, download it from [Apache Maven](https://maven.apache.org/download.cgi) and follow the installation instructions.

3. **Clone the Repository**  
   Clone this project to your local machine:
   ```bash
   git clone <repository-url>
   cd <repository-directory>
   ```

4. **Build the Project**  
   Run the following command to build the project and generate the JAR file:
   ```bash
   mvn clean package
   ```
   This will create the JAR file `target/custom-keycloak-<version>.jar`.

5. **Deploy the JAR**  
   - For a standard Keycloak installation, copy the generated JAR file to the Keycloak providers directory:
     ```bash
     cp target/custom-keycloak-<version>.jar <keycloak-home>/providers/
     ```
   - For a Dockerized Keycloak setup, copy the JAR to the deployments directory:
     ```bash
     cp target/custom-keycloak-<version>.jar /opt/jboss/keycloak/standalone/deployments/
     ```

6. **Build Keycloak**  
   Ensure Keycloak recognizes the new provider by running:
   ```bash
   bin/kc.sh build
   ```

### Deploy to Keycloak

```bash
# Copy JAR to Keycloak providers directory
cp target/custom-keycloak.jar /opt/keycloak/providers/

# Rebuild Keycloak (for production mode)
/opt/keycloak/bin/kc.sh build

# Restart Keycloak
/opt/keycloak/bin/kc.sh start

# Or for development mode:
/opt/keycloak/bin/kc.sh start-dev
```

### Configure in Keycloak Admin Console

1. Go to **Authentication** → **Flows**
2. Copy the "Browser" flow
3. Add authenticators in this order:
   ```
   1. IP Address Restriction       [REQUIRED]  ← Blocks unauthorized IPs
   2. Cookie                        [ALTERNATIVE]
   3. Username Password Form        [REQUIRED]
   4. Time/Date Restriction         [REQUIRED]  ← Enforces business hours
   5. Email 2FA                     [REQUIRED]  ← Extra security
   ```
4. Configure each authenticator's settings
5. Bind the flow to **Browser Flow** in **Bindings**

## 📖 Documentation

Comprehensive documentation is available in the `docs/` directory:

| Document | Description |
|----------|-------------|
| **[IP Restriction Guide](doc/IP_RESTRICTION_AUTHENTICATOR.md)** | Complete guide for IP-based access control |
| **[Testing Time Restrictions](TESTING_TIME_RESTRICTION.md)** | How to test time/date restrictions |

## 💡 Common Use Cases

### Office Hours Only
```
IP Rules: +192.168.1.0/24
Time Rules: Monday-Friday, 09:00-17:00
Result: Only office network, only during business hours
```

### Admin Access (Maximum Security)
```
IP Rules: +192.168.1.100, +10.8.0.50
Time Rules: 24/7
Email 2FA: Required
Result: Admins can only login from specific IPs with 2FA
```

### Public Access with Threat Protection
```
IP Rules: -185.220.101.0/24, -103.253.145.0/24
Time Rules: None (24/7)
Result: Block known malicious IPs, allow everyone else
```

### Remote + Office Access
```
IP Rules: +192.168.1.0/24, +10.8.0.0/24
Result: Allow office network and VPN range
```

## 🔧 Configuration Examples

### IP Restriction - Allow Office Network
```
IP Rules:
  +192.168.1.0/24

Check X-Forwarded-For: true (if behind proxy)
Error Message: Access is only allowed from office network
```

### Time Restriction - Business Hours
```
Timezone: America/New_York
Allowed Days: MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY
Start Time: 09:00
End Time: 17:00
Error Message: Login is only allowed during business hours
```

### Email 2FA - Standard Setup
```
Code Length: 6
Code TTL: 300 (5 minutes)
```

## 📊 Event Logging

All authenticators log detailed security events that can be viewed in Keycloak Admin Console:

**Events** → **Login Events** → Look for `LOGIN_ERROR` with `NOT_ALLOWED` error type

### IP Restriction Event Example
```json
{
  "type": "LOGIN_ERROR",
  "error": "NOT_ALLOWED",
  "details": {
    "client_ip": "203.0.113.45",
    "matched_rule": "-203.0.113.0/24",
    "rule_type": "DENY",
    "reason": "IP Restriction: IP address is explicitly blocked"
  }
}
```

### Time/Date Restriction Event Example
```json
{
  "type": "LOGIN_ERROR",
  "error": "NOT_ALLOWED",
  "details": {
    "reason": "Time/Date restriction: Current day SATURDAY is not allowed",
    "current_day": "SATURDAY",
    "allowed_days": "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY",
    "timezone": "UTC"
  }
}
```

## 🛡️ Security Best Practices

1. **Layer Your Security**
   - Use IP restriction as the first line of defense
   - Combine with time restrictions for business hours
   - Add 2FA for sensitive accounts

2. **IP Restriction Guidelines**
   - Use **allow lists** (`+`) for high-security environments
   - Use **deny lists** (`-`) to block known threats
   - Regularly review and update IP rules

3. **Monitor Events**
   - Enable event logging in Keycloak
   - Set up alerts for excessive blocked attempts
   - Review login events regularly

4. **Testing**
   - Always test in development/staging first
   - Have a backup access method
   - Don't lock yourself out!

## 🔍 Troubleshooting

### IP Restriction Not Working
- Verify rule format: must start with `+` or `-`
- Check X-Forwarded-For setting if behind proxy
- Review Events → Login Events for actual IP seen
- Ensure authenticator is at the TOP of the flow

### Time Restriction Not Working
- Verify timezone matches your server
- Check current time in configured timezone
- Ensure authenticator is AFTER username/password

### Build Failures
```bash
# Clean and rebuild
mvn clean install -U

# Check Java version
java -version  # Should be 21+
```

## 📦 Features Comparison

| Feature | Email 2FA | Time/Date | IP Restriction |
|---------|-----------|-----------|----------------|
| Runs before login form | ❌ | ❌ | ✅ |
| Prevents credential enumeration | ❌ | ❌ | ✅ |
| User interaction required | ✅ | ❌ | ❌ |
| Event logging | ✅ | ✅ | ✅ |
| CIDR support | N/A | N/A | ✅ |
| Timezone support | N/A | ✅ | N/A |
| Reverse proxy support | N/A | N/A | ✅ |

## 🤝 Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make your changes with tests
4. Update documentation
5. Submit a pull request

### Enhancements Added
- ⭐ **IP Address Restriction Authenticator** - Complete IP-based access control
- ⭐ **Time/Date Restriction Authenticator** - Time-based access control  
- ⭐ **Advanced Event Logging** - Detailed security event tracking
- ⭐ **Multi-value Configuration** - User-friendly add/remove UI for IP rules


## 📈 Version History

### v26.0.0-SNAPSHOT (Current)
- ✅ IP Address Restriction Authenticator
- ✅ Time/Date Restriction Authenticator
- ✅ Enhanced event logging for all authenticators
- ✅ Multi-value configuration support
- ✅ Comprehensive documentation
- ✅ Keycloak 26.0.0 compatibility

## 🌟 Star History

If you find this project useful, please give it a star! ⭐

---

**Built with ❤️ for the Keycloak community**

For detailed setup instructions, see the documentation files or visit the links above.
