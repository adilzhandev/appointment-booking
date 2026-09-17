package kz.rcez.appointment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.rcez.appointment.dto.common.PageResponse;
import kz.rcez.appointment.dto.doctor.DoctorRequest;
import kz.rcez.appointment.dto.doctor.DoctorResponse;
import kz.rcez.appointment.entity.enums.Specialty;
import kz.rcez.appointment.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
@Tag(name = "Врачи", description = "Справочник врачей организации")
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Добавить врача")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Врач создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<DoctorResponse> create(@Valid @RequestBody DoctorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить карточку врача")
    public DoctorResponse update(@PathVariable Long id, @Valid @RequestBody DoctorRequest request) {
        return doctorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить врача", description = "Мягкое удаление: история приёмов сохраняется")
    @ApiResponse(responseCode = "204", description = "Врач удалён")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        doctorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Карточка врача")
    public DoctorResponse getById(@PathVariable Long id) {
        return doctorService.getById(id);
    }

    @GetMapping
    @Operation(summary = "Список врачей", description = "Фильтрация по специальности и поиск по ФИО")
    public PageResponse<DoctorResponse> search(
            @Parameter(description = "Специальность") @RequestParam(required = false) Specialty specialty,
            @Parameter(description = "Часть ФИО") @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        return doctorService.search(specialty, q, pageable);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Карточка текущего врача")
    public DoctorResponse me() {
        return doctorService.getById(doctorService.getCurrentDoctor().getId());
    }
}
