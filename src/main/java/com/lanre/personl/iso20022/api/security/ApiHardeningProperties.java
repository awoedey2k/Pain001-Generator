package com.lanre.personl.iso20022.api.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "iso20022.api")
public class ApiHardeningProperties {

    private boolean enabled = true;
    private final RequestLimits requestLimits = new RequestLimits();
    private final RateLimit rateLimit = new RateLimit();
    private final Auth auth = new Auth();

    @Getter
    @Setter
    public static class RequestLimits {
        private long xmlBytes = 262_144L;
        private long jsonBytes = 65_536L;
        private long textBytes = 131_072L;
    }

    @Getter
    @Setter
    public static class RateLimit {
        private boolean enabled = true;
        private int requestsPerWindow = 60;
        private long windowSeconds = 60;
        private boolean trustForwardedFor = false;
    }

    @Getter
    @Setter
    public static class Auth {
        private String realm = "iso20022-api";
        private final User writer = new User("api-writer", "changeit-writer-password", "WRITER");
        private final User auditor = new User("api-auditor", "changeit-auditor-password", "AUDITOR");
        private final User admin = new User("api-admin", "changeit-admin-password", "ADMIN");
    }

    @Getter
    @Setter
    public static class User {
        private String username;
        private String password;
        private String role;

        public User() {
        }

        public User(String username, String password, String role) {
            this.username = username;
            this.password = password;
            this.role = role;
        }
    }
}
