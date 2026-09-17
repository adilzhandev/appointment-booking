package kz.rcez.appointment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    /** Явный Clock — позволяет подменять «сейчас» в тестах бизнес-логики. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
