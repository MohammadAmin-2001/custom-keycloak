package com.mesutpiskin.keycloak.auth.ip;

import org.jboss.logging.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * Utility class for IP address validation and CIDR matching
 */
public class IPUtils {

    private static final Logger logger = Logger.getLogger(IPUtils.class);
    
    // Regex patterns for IP validation
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$"
    );
    
    private static final Pattern CIDR_PATTERN = Pattern.compile(
        "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}/([0-9]|[1-2][0-9]|3[0-2])$"
    );

    /**
     * Check if an IP address matches a rule (can be single IP or CIDR notation)
     * 
     * @param clientIP The client IP address to check
     * @param rule The rule (e.g., "192.168.1.1" or "192.168.0.0/24")
     * @return true if the IP matches the rule
     */
    public static boolean matchesRule(String clientIP, String rule) {
        if (clientIP == null || rule == null || rule.trim().isEmpty()) {
            return false;
        }

        rule = rule.trim();
        
        // Check if rule is a CIDR notation
        if (rule.contains("/")) {
            return matchesCIDR(clientIP, rule);
        } else {
            // Simple IP match
            return clientIP.equals(rule);
        }
    }

    /**
     * Check if an IP address matches a CIDR range
     * 
     * @param ipAddress The IP address to check
     * @param cidr The CIDR notation (e.g., "192.168.0.0/24")
     * @return true if the IP is within the CIDR range
     */
    public static boolean matchesCIDR(String ipAddress, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                logger.warnf("Invalid CIDR format: %s", cidr);
                return false;
            }

            String networkAddress = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            if (prefixLength < 0 || prefixLength > 32) {
                logger.warnf("Invalid CIDR prefix length: %d", prefixLength);
                return false;
            }

            byte[] ipBytes = InetAddress.getByName(ipAddress).getAddress();
            byte[] networkBytes = InetAddress.getByName(networkAddress).getAddress();

            if (ipBytes.length != 4 || networkBytes.length != 4) {
                // Not IPv4
                return false;
            }

            int mask = 0xffffffff << (32 - prefixLength);

            int ipInt = byteArrayToInt(ipBytes);
            int networkInt = byteArrayToInt(networkBytes);

            return (ipInt & mask) == (networkInt & mask);

        } catch (UnknownHostException | NumberFormatException e) {
            logger.warnf(e, "Error matching CIDR %s for IP %s", cidr, ipAddress);
            return false;
        }
    }

    /**
     * Convert byte array to integer
     */
    private static int byteArrayToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
               ((bytes[1] & 0xFF) << 16) |
               ((bytes[2] & 0xFF) << 8) |
               (bytes[3] & 0xFF);
    }

    /**
     * Validate if a string is a valid IPv4 address
     */
    public static boolean isValidIPv4(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        return IPV4_PATTERN.matcher(ip.trim()).matches();
    }

    /**
     * Validate if a string is a valid CIDR notation
     */
    public static boolean isValidCIDR(String cidr) {
        if (cidr == null || cidr.trim().isEmpty()) {
            return false;
        }
        return CIDR_PATTERN.matcher(cidr.trim()).matches();
    }

    /**
     * Validate if a rule is valid (either IP or CIDR)
     */
    public static boolean isValidRule(String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            return false;
        }
        
        rule = rule.trim();
        
        if (rule.contains("/")) {
            return isValidCIDR(rule);
        } else {
            return isValidIPv4(rule);
        }
    }

    /**
     * Extract the actual IP address from X-Forwarded-For header
     * Takes the first IP if multiple are present
     */
    public static String extractIPFromForwardedHeader(String forwardedFor) {
        if (forwardedFor == null || forwardedFor.trim().isEmpty()) {
            return null;
        }

        // X-Forwarded-For can contain multiple IPs: "client, proxy1, proxy2"
        // We want the first one (the original client)
        String[] ips = forwardedFor.split(",");
        if (ips.length > 0) {
            return ips[0].trim();
        }

        return null;
    }
}
