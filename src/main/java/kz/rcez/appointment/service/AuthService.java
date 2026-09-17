package kz.rcez.appointment.service;

import kz.rcez.appointment.dto.auth.JwtResponse;
import kz.rcez.appointment.dto.auth.LoginRequest;
import kz.rcez.appointment.dto.auth.RegisterUserRequest;
import kz.rcez.appointment.dto.auth.UserResponse;
import kz.rcez.appointment.entity.UserAccount;
import kz.rcez.appointment.exception.ConflictException;
import kz.rcez.appointment.exception.NotFoundException;
import kz.rcez.appointment.repository.UserAccountRepository;
import kz.rcez.appointment.security.CurrentUser;
import kz.rcez.appointment.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUser currentUser;

    /** Проверяет логин/пароль через AuthenticationManager и выдаёт JWT. */
    @Transactional(readOnly = true)
    public JwtResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserAccount user = userAccountRepository.findByUsername(request.username())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + request.username()));

        return new JwtResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getUsername(),
                user.getFullName(),
                user.getRole());
    }

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        if (userAccountRepository.existsByUsername(request.username())) {
            throw new ConflictException("USERNAME_TAKEN", "Логин уже занят: " + request.username());
        }

        UserAccount user = UserAccount.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(request.role())
                .enabled(true)
                .build();

        return toResponse(userAccountRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUserInfo() {
        Long id = currentUser.userId()
                .orElseThrow(() -> new NotFoundException("Текущий пользователь не определён"));
        return userAccountRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Пользователь", id));
    }

    private UserResponse toResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(),
                user.getRole(), user.isEnabled());
    }
}
