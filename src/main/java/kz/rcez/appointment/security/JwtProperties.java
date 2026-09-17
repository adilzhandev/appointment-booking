package kz.rcez.appointment.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** Секрет HMAC-SHA256, не короче 32 символов. */
    private String secret;

    /** Время жизни токена в секундах. */
    private long expirationSeconds = 3600;

    private String issuer = "rcez-appointment";
}
