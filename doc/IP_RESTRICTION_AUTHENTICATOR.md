# IP Address Restriction Authenticator

This custom Keycloak authenticator restricts user authentication based on client IP address. It runs **BEFORE** the username/password form to block unauthorized IPs early in the authentication flow.

## Features

- ✅ **Early Blocking**: Runs before username/password to prevent credential enumeration
- ✅ **Allow Rules**: Use `+` prefix to allow specific IPs or ranges
- ✅ **Deny Rules**: Use `-` prefix to block specific IPs or ranges
- ✅ **CIDR Support**: Allow or deny entire IP ranges (e.g., `+192.168.0.0/24`)
- ✅ **Multi-Value Configuration**: Add multiple rules with add/remove buttons in Admin UI
- ✅ **Reverse Proxy Support**: Reads X-Forwarded-For header for real client IP
- ✅ **Event Logging**: All blocked attempts are logged with detailed information
- ✅ **Custom Error Messages**: Different messages for blocked vs not-allowed IPs

## How It Works

### Rule Processing Logic

1. **Deny rules are checked first** (`-` prefix)
   - If IP matches a deny rule → **BLOCKED** immediately
   
2. **Then allow rules are checked** (`+` prefix)
   - If IP matches an allow rule → **ALLOWED**
   
3. **Default behavior**
   - If **allow rules exist** but IP doesn't match any → **BLOCKED**
   - If **no allow rules exist** and IP doesn't match deny rules → **ALLOWED**

### Rule Format

Each rule must start with:
- `+` = Allow this IP/range
- `-` = Deny this IP/range

Examples:
```
+192.168.1.1          # Allow single IP
+192.168.0.0/24       # Allow IP range (CIDR)
-10.0.0.5             # Block single IP
-185.220.101.0/24     # Block IP range (known malicious range)
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| **IP Rules** | Multi-value | (empty) | List of IP rules with `+` or `-` prefix. Click + button to add more rules |
| **Check X-Forwarded-For Header** | Boolean | true | Enable if behind reverse proxy (nginx, Apache, load balancer) |
| **Error Message (Blocked IP)** | String | Access from your IP address is blocked | Message for explicitly blocked IPs (`-` rules) |
| **Error Message (Not Allowed)** | String | Access from your IP address is not allowed | Message when IP doesn't match any `+` rule |

## Installation

1. Build the JAR file:
   ```bash
   mvn clean package
   ```

2. Copy the JAR to Keycloak:
   ```bash
   cp target/keycloak-2fa-email-authenticator.jar /opt/keycloak/providers/
   ```

3. Restart Keycloak:
   ```bash
   /opt/keycloak/bin/kc.sh build
   /opt/keycloak/bin/kc.sh start
   ```

## Usage in Authentication Flow

### Step 1: Create/Copy Authentication Flow

1. Go to Keycloak Admin Console
2. Select your realm
3. Go to **Authentication** → **Flows**
4. Click **Copy** on "Browser" flow (or create new)
5. Name it "Browser with IP Restriction"

### Step 2: Add IP Restriction (AT THE BEGINNING!)

1. At the **very top** of the flow, click **Add execution**
2. Select **IP Address Restriction**
3. Set requirement to **REQUIRED**
4. **Move it to the TOP** using drag-and-drop (before cookie, username/password, etc.)

**Correct flow order:**
```
1. IP Address Restriction         [REQUIRED]
2. Cookie                          [ALTERNATIVE]
3. Username Password Form          [REQUIRED]
4. Time/Date Restriction           [REQUIRED]  (optional)
5. Email 2FA                       [REQUIRED]  (optional)
```

### Step 3: Configure IP Rules

1. Click **Settings** (gear icon) next to IP Address Restriction
2. Give it an alias (e.g., "Office IP Only")
3. Add IP rules:
   - Click the **+** button to add a new rule
   - Enter rule with prefix (e.g., `+192.168.1.0/24`)
   - Click **+** again to add more rules
   - Use **-** button to remove unwanted rules

4. Configure other settings:
   - Enable **Check X-Forwarded-For** if behind proxy
   - Customize error messages
5. Click **Save**

### Step 4: Bind the Flow

1. Go to **Authentication** → **Bindings**
2. Set **Browser Flow** to your new flow
3. Click **Save**

## Configuration Examples

### Example 1: Allow Office IP Range Only

**Use Case**: Only employees from office network can login

```
IP Rules:
  +192.168.1.0/24

Check X-Forwarded-For: false (if Keycloak is on local network)
Error Message (Not Allowed): Login is only allowed from office network
```

**Result**: Only IPs in 192.168.1.0-192.168.1.255 can login

---

### Example 2: Allow Multiple Offices

**Use Case**: Company has offices in multiple locations

```
IP Rules:
  +192.168.1.0/24     (New York office)
  +10.20.30.0/24      (London office)
  +172.16.0.0/16      (Tokyo office)

Error Message (Not Allowed): Login is only allowed from company offices
```

---

### Example 3: Allow All But Block Known Malicious IPs

**Use Case**: Allow everyone except known bad actors

```
IP Rules:
  -185.220.101.0/24   (Known Tor exit nodes)
  -103.253.145.0/24   (Known botnet range)
  -45.142.212.0/22    (Known malicious range)

Error Message (Blocked IP): Your IP address has been blocked due to security concerns
```

**Result**: All IPs allowed EXCEPT those in deny list

---

### Example 4: VPN + Office Access

**Use Case**: Allow office network and VPN range

```
IP Rules:
  +192.168.1.0/24     (Office network)
  +10.8.0.0/24        (VPN network)

Check X-Forwarded-For: true (if using VPN gateway)
Error Message (Not Allowed): Login requires VPN or office network connection
```

---

### Example 5: Admin IP Restriction (Advanced)

**Use Case**: Create a separate flow for admin users

1. Create flow "Admin Login Flow"
2. Add IP restriction:
   ```
   IP Rules:
     +192.168.1.100    (Admin workstation)
     +10.0.0.50        (Admin VPN)
   
   Error Message: Admin login is restricted to authorized workstations
   ```

3. Use **Authentication → Required Actions** or conditional flows to apply this only to admin users

---

### Example 6: Mixed Allow and Deny

**Use Case**: Allow office but block specific problem user's home IP

```
IP Rules:
  +192.168.1.0/24     (Allow office)
  -203.0.113.45       (Block specific IP)
  -198.51.100.20      (Block another IP)

Error Message (Blocked IP): Your IP address has been blocked. Contact IT support.
```

**Result**: Office IPs are allowed, but specific IPs are still blocked (deny takes precedence)

---

## Adding Rules in Admin UI

When you configure the IP restriction, you'll see an interface like this:

```
┌─────────────────────────────────────────────────────┐
│ IP Rules                                            │
├─────────────────────────────────────────────────────┤
│ [+192.168.1.0/24        ] [+] [-]                  │
│ [+10.0.0.0/24           ] [+] [-]                  │
│ [-185.220.101.0/24      ] [+] [-]                  │
│                             [+] <-- Click to add    │
└─────────────────────────────────────────────────────┘
```

- Click **[+]** on the right to **add a new row**
- Click **[-]** to **remove a row**
- Enter each rule with `+` or `-` prefix

## CIDR Notation Quick Reference

| CIDR | IP Range | # of IPs | Use Case |
|------|----------|----------|----------|
| /32 | Single IP | 1 | Specific workstation |
| /24 | x.x.x.0 - x.x.x.255 | 256 | Small office |
| /16 | x.x.0.0 - x.x.255.255 | 65,536 | Large organization |
| /8 | x.0.0.0 - x.255.255.255 | 16,777,216 | Entire class A network |

**Examples:**
- `192.168.1.100/32` = Only 192.168.1.100
- `192.168.1.0/24` = 192.168.1.0 to 192.168.1.255
- `10.0.0.0/8` = 10.0.0.0 to 10.255.255.255

## Reverse Proxy / Load Balancer Setup

### If Keycloak is Behind a Proxy

When using nginx, Apache, or a load balancer:

1. **Enable X-Forwarded-For** in your proxy:

   **Nginx:**
   ```nginx
   location / {
       proxy_pass http://keycloak:8080;
       proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
       proxy_set_header X-Forwarded-Proto $scheme;
       proxy_set_header Host $host;
   }
   ```

   **Apache:**
   ```apache
   ProxyPass / http://keycloak:8080/
   ProxyPassReverse / http://keycloak:8080/
   ProxyPreserveHost On
   RequestHeader set X-Forwarded-For "%{REMOTE_ADDR}s"
   ```

2. **Enable in Keycloak authenticator config:**
   - Check ✅ **Check X-Forwarded-For Header**

3. **Verify it's working:**
   - Check Keycloak events to see if correct IP is logged
   - Test from different IPs

### Without Reverse Proxy

If Keycloak is directly accessible:
- Uncheck ☐ **Check X-Forwarded-For Header**
- Authenticator will use direct connection IP

## Event Logging

When an IP is blocked, detailed events are logged:

### Event Details

| Detail Key | Description | Example |
|------------|-------------|---------|
| `client_ip` | The client's IP address | 203.0.113.45 |
| `matched_rule` | The rule that was matched | -203.0.113.0/24 |
| `rule_type` | Type of match | DENY or NO_MATCH |
| `all_rules` | All configured rules | +192.168.1.0/24, -10.0.0.5 |
| `x_forwarded_for` | Original X-Forwarded-For header | 203.0.113.45, 10.0.0.1 |
| `reason` | Human-readable reason | IP Restriction: IP address is explicitly blocked |

### Viewing Events

1. Go to **Events** → **Login Events**
2. Look for events with:
   - **Type**: LOGIN_ERROR
   - **Error**: NOT_ALLOWED
   - **Details**: Contains IP restriction information

### Event Examples

**Blocked IP:**
```
Type: LOGIN_ERROR
Error: NOT_ALLOWED
IP Address: 185.220.101.50
Details:
  - client_ip: 185.220.101.50
  - matched_rule: -185.220.101.0/24
  - rule_type: DENY
  - reason: IP Restriction: IP address is explicitly blocked
```

**Not in Whitelist:**
```
Type: LOGIN_ERROR
Error: NOT_ALLOWED
IP Address: 203.0.113.45
Details:
  - client_ip: 203.0.113.45
  - matched_rule: none
  - rule_type: NO_MATCH
  - reason: IP Restriction: IP address is not in allowed list
  - all_rules: +192.168.1.0/24, +10.0.0.0/24
```

## Testing

### Test Allow Rule

1. Configure:
   ```
   +YOUR.IP.ADDRESS.HERE
   ```

2. Try to login → Should succeed

3. Change to different IP or remove rule → Should fail

### Test Deny Rule

1. Configure:
   ```
   -YOUR.IP.ADDRESS.HERE
   ```

2. Try to login → Should fail with blocked message

### Test CIDR Range

1. Find your IP (e.g., 192.168.1.100)
2. Configure:
   ```
   +192.168.1.0/24
   ```
3. Try to login → Should succeed

### Test from Different IP

Use VPN or mobile hotspot to test from different IP address

### Check Events

After each test:
1. Go to **Events** → **Login Events**
2. Verify IP is logged correctly
3. Check event details for troubleshooting

## Troubleshooting

### Issue: Wrong IP Address in Logs

**Cause**: X-Forwarded-For configuration mismatch

**Solution**:
- If behind proxy: Enable "Check X-Forwarded-For"
- If not behind proxy: Disable "Check X-Forwarded-For"
- Verify proxy is setting X-Forwarded-For header

---

### Issue: All Users Blocked

**Cause**: No allow rules and user IP doesn't match any deny rules

**Solution**:
- Add allow rules: `+YOUR.IP.RANGE`
- Or remove all deny rules if you want to allow everyone

---

### Issue: Can't Add Multiple Rules

**Cause**: Not using the [+] button

**Solution**:
- Click the **[+]** button on the right side of each rule
- This adds a new input field
- Don't use comma or newline in a single field

---

### Issue: CIDR Not Working

**Cause**: Invalid CIDR notation

**Solution**:
- Format must be: `IP/MASK` (e.g., `192.168.1.0/24`)
- Network address must match the mask
- Verify with CIDR calculator

---

### Issue: Rule Not Taking Effect

**Cause**: Rule missing `+` or `-` prefix

**Solution**:
- Every rule MUST start with `+` or `-`
- `192.168.1.1` ❌ (invalid)
- `+192.168.1.1` ✅ (valid)

---

## Security Best Practices

1. **Combine with Other Authenticators**
   - IP restriction alone is not enough
   - Always use with password authentication
   - Consider adding 2FA for sensitive accounts

2. **Use Allow Lists for High Security**
   - For admin accounts, use allow lists only
   - Deny lists can be bypassed with new IPs

3. **Regular Review**
   - Review IP rules regularly
   - Remove outdated rules
   - Monitor failed login events

4. **VPN Recommendation**
   - For remote access, use VPN
   - Whitelist VPN IP range
   - More secure than whitelisting individual home IPs

5. **Logging and Monitoring**
   - Enable event logging
   - Set up alerts for excessive blocks
   - Monitor for unusual patterns

6. **Test Before Production**
   - Test rules in development first
   - Have a backup access method
   - Don't lock yourself out!

## Integration with Time Restriction

You can combine IP and Time restrictions:

**Flow order:**
```
1. IP Address Restriction      [REQUIRED]
2. Cookie                       [ALTERNATIVE]
3. Username Password Form       [REQUIRED]
4. Time/Date Restriction        [REQUIRED]
5. Email 2FA                    [REQUIRED]
```

**Example Use Case**: 
- Office hours (9-5): Allow office IP + any VPN
- After hours: Require specific admin IPs only

## Common Patterns

### Pattern 1: Office Only
```
+192.168.1.0/24
```

### Pattern 2: Office + VPN
```
+192.168.1.0/24
+10.8.0.0/24
```

### Pattern 3: Allow All, Block Bad Actors
```
-185.220.101.0/24
-103.253.145.0/24
```

### Pattern 4: Specific Workstations
```
+192.168.1.100
+192.168.1.101
+192.168.1.102
```

### Pattern 5: Cloud + Office
```
+192.168.1.0/24     (Office)
+34.120.0.0/16      (Google Cloud)
+52.0.0.0/8         (AWS)
```

## Notes

- Rules are processed in order: **deny first, then allow**
- CIDR notation is fully supported for IPv4
- X-Forwarded-For uses the **first IP** in the header (original client)
- Empty configuration = no restrictions (all IPs allowed)
- Invalid rules are logged and skipped
- Configuration changes take effect immediately (no restart needed)

## Support

For issues or questions:
1. Check Keycloak server logs
2. Review event logs in Admin Console
3. Verify IP rules format
4. Test with simple single IP first
5. Check reverse proxy configuration
