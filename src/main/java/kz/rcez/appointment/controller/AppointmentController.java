package kz.rcez.appointment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.rcez.appointment.dto.appointment.AppointmentResponse;
import kz.rcez.appointment.dto.appointment.CancelAppointmentRequest;
import kz.rcez.appointment.dto.appointment.CompleteAppointmentRequest;
import kz.rcez.appointment.dto.appointment.CreateAppointmentRequest;
import kz.rcez.appointment.dto.common.PageResponse;
import kz.rcez.appointment.entity.enums.AppointmentStatus;
import kz.rcez.appointment.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Записи на приём", description = "Создание, отмена и закрытие записей")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','REGISTRAR')")
    @Operation(summary = "Записать пациента на приём",
            description = "Слот переводится в статус BOOKED в той же транзакции")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Запись создана"),
            @ApiResponse(responseCode = "404", description = "Пациент или слот не найдены", content = @Content),
            @ApiResponse(responseCode = "409", description = "Слот занят, заблокирован или уже прошёл", content = @Content)
    })
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.create(request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','REGISTRAR','DOCTOR')")
    @Operation(summary = "Отменить запись", description = "Слот возвращается в статус FREE")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Запись отменена"),
            @ApiResponse(responseCode = "409", description = "Запись уже закрыта", content = @Content)
    })
    public AppointmentResponse cancel(@PathVariable Long id,
                                      @Valid @RequestBody CancelAppointmentRequest request) {
        return appointmentService.cancel(id, request);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    @Operation(summary = "Завершить приём", description = "Доступно врачу этой записи и администратору")
    public AppointmentResponse complete(@PathVariable Long id,
                                        @Valid @RequestBody CompleteAppointmentRequest request) {
        return appointmentService.complete(id, request);
    }

    @PostMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','REGISTRAR')")
    @Operation(summary = "Отметить неявку пациента")
    public AppointmentResponse markNoShow(@PathVariable Long id) {
        return appointmentService.markNoShow(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Карточка записи")
    public AppointmentResponse getById(@PathVariable Long id) {
        return appointmentService.getById(id);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "История записей пациента", description = "Свежие записи сверху")
    public PageResponse<AppointmentResponse> patientHistory(
            @PathVariable Long patientId,
            @Parameter(description = "Фильтр по статусу") @RequestParam(required = false) AppointmentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return appointmentService.patientHistory(patientId, status, pageable);
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Приёмы врача на день")
    public List<AppointmentResponse> doctorDaySchedule(
            @PathVariable Long doctorId,
            @Parameter(example = "2026-09-20") @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) AppointmentStatus status) {
        return appointmentService.doctorDaySchedule(doctorId, date, status);
    }
}
