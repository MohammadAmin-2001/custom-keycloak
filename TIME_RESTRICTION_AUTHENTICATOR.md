# Time/Date Restriction Authenticator

This custom Keycloak authenticator restricts user authentication based on time of day and day of week.

## Features

- **Day of Week Restriction**: Allow login only on specific days (e.g., Monday-Friday for business hours)
- **Time Range Restriction**: Allow login only during specific hours (e.g., 9:00 AM - 5:00 PM)
- **Timezone Support**: Configure timezone for accurate time checking
- **Overnight Support**: Handle time ranges that span midnight (e.g., 22:00 - 06:00)
- **Custom Error Messages**: Display custom messages when access is denied

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| **Timezone** | String | UTC | Timezone for time restriction (e.g., UTC, America/New_York, Europe/London, Asia/Tokyo) |
| **Allowed Days** | String | MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY | Comma-separated list of allowed days of the week |
| **Start Time** | String | 00:00 | Start time in HH:mm format (24-hour) |
| **End Time** | String | 23:59 | End time in HH:mm format (24-hour) |
| **Error Message** | String | Access is not allowed at this time | Message displayed when access is denied |

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

1. **Navigate to Authentication Flows**:
   - Go to your Keycloak Admin Console
   - Select your realm
   - Go to **Authentication** → **Flows**

2. **Create or Copy a Flow**:
   - Click **Copy** on an existing flow (e.g., "Browser") or create a new one
   - Give it a name like "Browser with Time Restriction"

3. **Add Time Restriction**:
   - Click **Add execution**
   - Select **Time/Date Restriction** from the list
   - Set requirement to **REQUIRED**

4. **Configure the Authenticator**:
   - Click the **Settings** (gear icon) next to the Time/Date Restriction execution
   - Configure your restrictions:
     - **Alias**: Give it a descriptive name (e.g., "Business Hours Only")
     - **Timezone**: Your timezone (e.g., America/New_York)
     - **Allowed Days**: MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY
     - **Start Time**: 09:00
     - **End Time**: 17:00
     - **Error Message**: Login is only allowed during business hours (9 AM - 5 PM, Monday-Friday)

5. **Bind the Flow**:
   - Go to **Authentication** → **Bindings**
   - Set your new flow as the **Browser Flow**

## Configuration Examples

### Example 1: Business Hours Only
Restrict access to weekdays, 9 AM to 5 PM (EST):

```
Timezone: America/New_York
Allowed Days: MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY
Start Time: 09:00
End Time: 17:00
Error Message: Access is only allowed during business hours (9 AM - 5 PM, Monday-Friday)
```

### Example 2: Weekend Only
Restrict access to weekends only:

```
Timezone: UTC
Allowed Days: SATURDAY,SUNDAY
Start Time: 00:00
End Time: 23:59
Error Message: Access is only allowed on weekends
```

### Example 3: Night Shift
Restrict access to night hours (10 PM to 6 AM):

```
Timezone: Europe/London
Allowed Days: MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY
Start Time: 22:00
End Time: 06:00
Error Message: Access is only allowed during night shift hours (10 PM - 6 AM)
```

### Example 4: Extended Business Hours
Monday-Friday 8 AM to 8 PM:

```
Timezone: Asia/Tokyo
Allowed Days: MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY
Start Time: 08:00
End Time: 20:00
Error Message: Login is only permitted during extended business hours
```

### Example 5: 24/7 Weekday Access
All day Monday through Friday:

```
Timezone: UTC
Allowed Days: MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY
Start Time: 00:00
End Time: 23:59
Error Message: Access is only allowed on weekdays
```

## Common Timezones

- **UTC**: Coordinated Universal Time
- **America/New_York**: Eastern Time (US)
- **America/Chicago**: Central Time (US)
- **America/Denver**: Mountain Time (US)
- **America/Los_Angeles**: Pacific Time (US)
- **Europe/London**: British Time
- **Europe/Paris**: Central European Time
- **Asia/Tokyo**: Japan Standard Time
- **Asia/Shanghai**: China Standard Time
- **Australia/Sydney**: Australian Eastern Time

For a complete list of timezones, see: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones

## How It Works

1. When a user attempts to authenticate, the authenticator checks:
   - The current day of the week in the configured timezone
   - The current time in the configured timezone

2. If the current day is not in the allowed days list, authentication fails

3. If the current time is outside the allowed time range, authentication fails

4. If both checks pass, authentication continues to the next step in the flow

5. Configuration errors (invalid timezone, time format, etc.) will log warnings but allow access to prevent accidental lockouts

## Testing

To test the authenticator:

1. Configure it with restrictive settings
2. Try to login during allowed times → Should succeed
3. Try to login during restricted times → Should fail with error message
4. Check Keycloak logs for detailed information about time checks

## Troubleshooting

### Users can still login during restricted times
- Verify the authenticator is set to **REQUIRED** in the flow
- Check that the correct flow is bound to the browser
- Verify timezone configuration is correct
- Check Keycloak logs for any errors

### All users are blocked
- Check timezone configuration
- Verify time format is HH:mm (24-hour format)
- Ensure allowed days are spelled correctly (all caps)
- Check Keycloak logs for configuration errors

### Time seems off
- Verify timezone setting matches your location
- Remember that times are checked in the configured timezone, not server time
- Use UTC if you need consistent behavior regardless of location

## Notes

- The authenticator uses the configured timezone for all time checks
- Overnight time ranges (e.g., 22:00 - 06:00) are fully supported
- In case of configuration errors, the authenticator will allow access and log warnings
- The authenticator requires an authenticated user (place it after username/password authentication)
- Multiple time restriction authenticators can be used in the same flow with different configurations

## Security Considerations

- This authenticator should be used in combination with proper authentication (username/password, 2FA, etc.)
- Time restrictions are based on server time in the configured timezone
- Users with valid sessions may remain logged in past restricted times
- Consider using session timeouts to enforce logout during restricted periods
