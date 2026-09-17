package kz.rcez.appointment.entity.enums;

/** Статус записи на приём. */
public enum AppointmentStatus {
    /** Запись создана, приём предстоит. */
    SCHEDULED,
    /** Приём состоялся. */
    COMPLETED,
    /** Запись отменена, слот освобождён. */
    CANCELLED,
    /** Пациент не явился. */
    NO_SHOW;

    /** Статус, после которого запись больше не меняется. */
    public boolean isTerminal() {
        return this != SCHEDULED;
    }
}
