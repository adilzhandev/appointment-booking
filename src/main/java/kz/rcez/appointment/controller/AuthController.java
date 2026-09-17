package kz.rcez.appointment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.rcez.appointment.dto.auth.JwtResponse;
import kz.rcez.appointment.dto.auth.LoginRequest;
import kz.rcez.appointment.dto.auth.RegisterUserRequest;
import kz.rcez.appointment.dto.auth.UserResponse;
import kz.rcez.appointment.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация", description = "Вход в систему и управление учётными записями")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Вход в систему", description = "Возвращает JWT для последующих запросов",
            security = @SecurityRequirement(name = ""))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токен выдан"),
            @ApiResponse(responseCode = "401", description = "Неверный логин или пароль", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public JwtResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать учётную запись", description = "Доступно только администратору")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Учётная запись создана"),
            @ApiResponse(responseCode = "409", description = "Логин уже занят", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Текущий пользователь")
    public UserResponse me() {
        return authService.currentUserInfo();
    }
}
