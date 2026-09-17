package kz.rcez.appointment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.rcez.appointment.dto.slot.CreateSlotRequest;
import kz.rcez.appointment.dto.slot.GenerateScheduleRequest;
import kz.rcez.appointment.dto.slot.GenerateScheduleResponse;
import kz.rcez.appointment.dto.slot.SlotResponse;
import kz.rcez.appointment.entity.enums.Specialty;
import kz.rcez.appointment.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Расписание", description = "Слоты приёма врачей")
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    @PostMapping("/doctors/{doctorId}/slots")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    @Operation(summary = "Создать слот",
            description = "Врач может создавать слоты только в собственном расписании")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Слот создан"),
            @ApiResponse(responseCode = "400", description = "Слот в прошлом или некорректный интервал", content = @Content),
            @ApiResponse(responseCode = "403", description = "Чужое расписание", content = @Content),
            @ApiResponse(responseCode = "409", description = "Пересечение с существующим слотом", content = @Content)
    })
    public ResponseEntity<SlotResponse> createSlot(@PathVariable Long doctorId,
                                                   @Valid @RequestBody CreateSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timeSlotService.createSlot(doctorId, request));
    }

    @PostMapping("/doctors/{doctorId}/slots/generate")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    @Operation(summary = "Сгенерировать расписание на период",
            description = "Нарезает рабочие дни на слоты заданной длительности; "
                    + "пересекающиеся и прошедшие интервалы пропускаются")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Расписание сгенерировано"),
            @ApiResponse(responseCode = "400", description = "Некорректный период", content = @Content)
    })
    public ResponseEntity<GenerateScheduleResponse> generate(@PathVariable Long doctorId,
                                                             @Valid @RequestBody GenerateScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timeSlotService.generateSchedule(doctorId, request));
    }

    @GetMapping("/slots/free")
    @Operation(summary = "Поиск свободных слотов",
            description = "Фильтры: врач, специальность, диапазон дат. "
                    + "По умолчанию — ближайшие 7 дней. Прошедшие слоты не возвращаются")
    public List<SlotResponse> findFree(
            @Parameter(description = "Идентификатор врача") @RequestParam(required = false) Long doctorId,
            @Parameter(description = "Специальность") @RequestParam(required = false) Specialty specialty,
            @Parameter(example = "2026-09-20") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(example = "2026-09-27") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return timeSlotService.findFreeSlots(doctorId, specialty, dateFrom, dateTo);
    }

    @GetMapping("/doctors/{doctorId}/slots")
    @Operation(summary = "Расписание врача на день", description = "Все слоты дня, включая занятые и заблокированные")
    public List<SlotResponse> daySchedule(
            @PathVariable Long doctorId,
            @Parameter(example = "2026-09-20") @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return timeSlotService.getDoctorDaySchedule(doctorId, date);
    }

    @PostMapping("/slots/{slotId}/block")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    @Operation(summary = "Заблокировать слот", description = "Слот с активной записью заблокировать нельзя")
    public SlotResponse block(@PathVariable Long slotId) {
        return timeSlotService.block(slotId);
    }

    @PostMapping("/slots/{slotId}/unblock")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    @Operation(summary = "Разблокировать слот")
    public SlotResponse unblock(@PathVariable Long slotId) {
        return timeSlotService.unblock(slotId);
    }

    @DeleteMapping("/slots/{slotId}")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    @Operation(summary = "Удалить слот", description = "Мягкое удаление; занятый слот удалить нельзя")
    @ApiResponse(responseCode = "204", description = "Слот удалён")
    public ResponseEntity<Void> delete(@PathVariable Long slotId) {
        timeSlotService.delete(slotId);
        return ResponseEntity.noContent().build();
    }
}
