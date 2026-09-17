package kz.rcez.appointment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import kz.rcez.appointment.dto.common.PageResponse;
import kz.rcez.appointment.dto.patient.PatientRequest;
import kz.rcez.appointment.dto.patient.PatientResponse;
import kz.rcez.appointment.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Validated
@Tag(name = "Пациенты", description = "Картотека пациентов")
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','REGISTRAR')")
    @Operation(summary = "Зарегистрировать пациента")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Пациент создан"),
            @ApiResponse(responseCode = "409", description = "ИИН уже зарегистрирован", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<PatientResponse> create(@Valid @RequestBody PatientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','REGISTRAR')")
    @Operation(summary = "Обновить данные пациента")
    public PatientResponse update(@PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        return patientService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить пациента", description = "Мягкое удаление")
    @ApiResponse(responseCode = "204", description = "Пациент удалён")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        patientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Карточка пациента")
    public PatientResponse getById(@PathVariable Long id) {
        return patientService.getById(id);
    }

    @GetMapping("/by-iin/{iin}")
    @Operation(summary = "Поиск пациента по ИИН")
    public PatientResponse getByIin(
            @Parameter(description = "12 цифр", example = "900101300123")
            @PathVariable @Pattern(regexp = "^[0-9]{12}$", message = "ИИН должен состоять из 12 цифр") String iin) {
        return patientService.getByIin(iin);
    }

    @GetMapping
    @Operation(summary = "Поиск пациентов", description = "По части ИИН или ФИО")
    public PageResponse<PatientResponse> search(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable) {
        return patientService.search(q, pageable);
    }
}
