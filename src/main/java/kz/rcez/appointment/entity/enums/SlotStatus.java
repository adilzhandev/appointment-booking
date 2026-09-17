package kz.rcez.appointment.entity.enums;

/** Состояние слота расписания. */
public enum SlotStatus {
    /** Свободен, доступен для записи. */
    FREE,
    /** Занят активной записью. */
    BOOKED,
    /** Заблокирован врачом/админом (отпуск, совещание и т.п.). */
    BLOCKED
}
