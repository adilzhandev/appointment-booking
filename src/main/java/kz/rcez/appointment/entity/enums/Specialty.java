package kz.rcez.appointment.entity.enums;

/** Врачебная специальность (упрощённый справочник). */
public enum Specialty {
    THERAPIST("Терапевт"),
    PEDIATRICIAN("Педиатр"),
    SURGEON("Хирург"),
    CARDIOLOGIST("Кардиолог"),
    NEUROLOGIST("Невролог"),
    OPHTHALMOLOGIST("Офтальмолог"),
    OTOLARYNGOLOGIST("Оториноларинголог"),
    ENDOCRINOLOGIST("Эндокринолог"),
    GYNECOLOGIST("Гинеколог"),
    DENTIST("Стоматолог");

    private final String title;

    Specialty(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
