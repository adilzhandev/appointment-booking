package kz.rcez.appointment.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Запись на приём к врачу — API")
                        .version("1.0.0")
                        .description("""
                                Сервис записи пациентов на приём в амбулаторно-поликлинической организации.

                                **Роли:**
                                - `ADMIN` — справочники, врачи, учётные записи, любое расписание;
                                - `DOCTOR` — своё расписание и свои приёмы;
                                - `REGISTRAR` — пациенты, создание и отмена записей.

                                **Аутентификация:** `POST /api/v1/auth/login` → полученный `accessToken`
                                передавайте в заголовке `Authorization: Bearer <token>`.
                                """)
                        .contact(new Contact().name("РЦЭЗ").email("support@rcez.kz"))
                        .license(new License().name("Internal use")))
                .servers(List.of(new Server().url("http://localhost:8080").description("Локальный запуск")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT, полученный в /api/v1/auth/login")));
    }
}
