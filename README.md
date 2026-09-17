# Система записи на приём к врачу

Учебный проект для стажировки в Республиканском центре электронного здравоохранения (РЦЭЗ).
Модуль амбулаторно-поликлинической системы: справочник врачей, расписание приёма,
картотека пациентов и запись на приём с контролем занятости слотов.

## Стек

Java 21 · Spring Boot 3.3 · Spring Data JPA · Spring Security (JWT) · PostgreSQL 16 ·
Flyway · Lombok · MapStruct · SpringDoc OpenAPI · Bean Validation · Docker Compose

## Структура

```
kz.rcez.appointment
├── config          AppConfig (Clock), OpenApiConfig, DataSeeder
├── security        JwtService, JwtAuthenticationFilter, SecurityConfig, AppUserDetails, CurrentUser
├── entity          BaseEntity, UserAccount, Doctor, Patient, TimeSlot, Appointment
│   └── enums       Role, Specialty, Gender, SlotStatus, AppointmentStatus
├── repository      Spring Data JPA + JPQL-запросы поиска
├── dto             auth/, doctor/, patient/, slot/, appointment/, common/
├── mapper          MapStruct-мапперы entity ↔ DTO
├── service         AuthService, DoctorService, PatientService, TimeSlotService, AppointmentService
├── controller      REST-контроллеры /api/v1/**
└── exception       ApiException + GlobalExceptionHandler
```

## Модель данных

```
UserAccount 1 ─── 1 Doctor 1 ─── N TimeSlot 1 ─── 1 Appointment N ─── 1 Patient
```

| Сущность | Назначение |
|---|---|
| `UserAccount` | учётная запись, роль `ADMIN` / `DOCTOR` / `REGISTRAR` |
| `Doctor` | врач: ФИО, специальность, кабинет, длительность приёма по умолчанию |
| `Patient` | пациент: ИИН (12 цифр, уникален), ФИО, дата рождения, пол, контакты |
| `TimeSlot` | слот расписания: врач, дата, интервал, статус `FREE` / `BOOKED` / `BLOCKED` |
| `Appointment` | запись: слот, пациент, врач, статус `SCHEDULED` / `COMPLETED` / `CANCELLED` / `NO_SHOW` |

Все сущности наследуют `BaseEntity`: `id`, аудит (`created_at` / `updated_at`),
флаг мягкого удаления `deleted` и `@Version` для оптимистичной блокировки.
Мягко удалённые строки отсекаются на уровне Hibernate через `@SQLRestriction("deleted = false")`.

## Бизнес-правила

**Расписание**
- слот нельзя создать в прошлом и с интервалом, где начало не раньше окончания;
- пересечение с существующим слотом того же врача отклоняется (`409 SLOT_OVERLAP`);
  на уровне БД дубль страхует частичный уникальный индекс `uq_time_slot_doctor_interval`;
- пакетная генерация нарезает рабочий день на слоты, вычитает перерыв, умеет пропускать выходные;
  пересекающиеся и прошедшие интервалы пропускаются, а не роняют операцию;
- `DOCTOR` управляет только своим расписанием, `ADMIN` — любым.

**Запись на приём**
- создание и отмена выполняются в одной транзакции со сменой статуса слота;
- слот берётся под `PESSIMISTIC_WRITE` (`findByIdForUpdate`), чтобы два регистратора
  не заняли один и тот же слот; на уровне БД это дублирует частичный уникальный индекс
  `uq_appointment_active_slot` (одна активная запись на слот);
- отклоняются занятый, заблокированный и прошедший слот, а также повторная активная
  запись пациента к тому же врачу в тот же день;
- при отмене слот возвращается в `FREE`; при `COMPLETED` / `NO_SHOW` остаётся занятым
  как след состоявшегося визита;
- запись в терминальном статусе больше не изменяется.

## Роли и доступ

| Операция | ADMIN | DOCTOR | REGISTRAR |
|---|:---:|:---:|:---:|
| CRUD врачей, учётные записи | ✅ | — | — |
| Расписание врача | любое | только своё | — |
| CRUD пациентов | ✅ | — | ✅ (кроме удаления) |
| Создать запись | ✅ | — | ✅ |
| Отменить запись | ✅ | ✅ | ✅ |
| Завершить приём | ✅ | ✅ (свой) | — |
| Поиск слотов, история | ✅ | ✅ | ✅ |

## Запуск

### Docker Compose (всё сразу)

```bash
docker compose up --build
```

Поднимается PostgreSQL и приложение на `http://localhost:8080`.

### Локально

```bash
docker compose up -d db
mvn spring-boot:run
```

Тесты:

```bash
mvn test
```

### Переменные окружения

| Переменная | По умолчанию | Назначение |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/appointment_db` | адрес БД |
| `DB_USERNAME` / `DB_PASSWORD` | `appointment` | доступ к БД |
| `SERVER_PORT` | `8080` | порт приложения |
| `JWT_SECRET` | dev-значение | секрет HMAC-SHA256, **обязательно задать в проде** (≥32 символов) |
| `JWT_EXPIRATION` | `3600` | время жизни токена, секунд |
| `SEED_ENABLED` | `true` | загрузка демо-данных при старте |

### Документация API

- Swagger UI — http://localhost:8080/swagger-ui.html
- OpenAPI JSON — http://localhost:8080/v3/api-docs

### Демо-учётные записи

| Логин | Пароль | Роль |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `registrar` | `registrar123` | REGISTRAR |
| `doctor` | `doctor123` | DOCTOR (Ахметов Д.С., терапевт) |
| `doctor2` | `doctor123` | DOCTOR (Исаева Г.Б., кардиолог) |

## Примеры curl

### 1. Вход в систему

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"registrar","password":"registrar123"}'
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "username": "registrar",
  "fullName": "Сериккали Айгуль Маратовна",
  "role": "REGISTRAR"
}
```

Дальше токен подставляется в заголовок:

```bash
TOKEN=<accessToken>
```

### 2. Генерация расписания врача на неделю

```bash
curl -X POST http://localhost:8080/api/v1/doctors/1/slots/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
        "dateFrom": "2026-09-21",
        "dateTo": "2026-09-25",
        "workStart": "09:00",
        "workEnd": "17:00",
        "slotMinutes": 15,
        "breakStart": "13:00",
        "breakEnd": "14:00",
        "skipWeekends": true
      }'
```

```json
{ "created": 140, "skipped": 0, "slots": [ ... ] }
```

### 3. Одиночный слот

```bash
curl -X POST http://localhost:8080/api/v1/doctors/1/slots \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"slotDate":"2026-09-26","startTime":"09:00","endTime":"09:15"}'
```

Пересечение с существующим слотом:

```json
{ "status": 409, "code": "SLOT_OVERLAP", "message": "Слот пересекается с существующим слотом расписания врача" }
```

### 4. Поиск свободных слотов

```bash
# по специальности и диапазону дат
curl "http://localhost:8080/api/v1/slots/free?specialty=THERAPIST&dateFrom=2026-09-21&dateTo=2026-09-25" \
  -H "Authorization: Bearer $TOKEN"

# по конкретному врачу
curl "http://localhost:8080/api/v1/slots/free?doctorId=1" -H "Authorization: Bearer $TOKEN"
```

### 5. Регистрация пациента

```bash
curl -X POST http://localhost:8080/api/v1/patients \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
        "iin": "900101300123",
        "lastName": "Нурланов",
        "firstName": "Асхат",
        "middleName": "Бекович",
        "birthDate": "1990-01-01",
        "gender": "MALE",
        "phone": "+77051234567",
        "address": "г. Астана, ул. Кенесары, 42"
      }'
```

### 6. Запись на приём

```bash
curl -X POST http://localhost:8080/api/v1/appointments \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"patientId":1,"timeSlotId":1,"complaint":"Головная боль, температура 37.5"}'
```

```json
{
  "id": 1,
  "patientFullName": "Нурланов Асхат Бекович",
  "doctorFullName": "Ахметов Данияр Серикович",
  "cabinet": "312",
  "slotDate": "2026-09-21",
  "startTime": "09:00:00",
  "status": "SCHEDULED"
}
```

Попытка занять тот же слот повторно:

```json
{ "status": 409, "code": "SLOT_ALREADY_BOOKED", "message": "Слот уже занят другой записью" }
```

### 7. Отмена записи (слот освобождается)

```bash
curl -X POST http://localhost:8080/api/v1/appointments/1/cancel \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"reason":"Пациент перенёс визит"}'
```

### 8. Завершение приёма (роль DOCTOR)

```bash
curl -X POST http://localhost:8080/api/v1/appointments/2/complete \
  -H "Authorization: Bearer $DOCTOR_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"conclusion":"ОРВИ. Назначено симптоматическое лечение."}'
```

### 9. История записей пациента

```bash
curl "http://localhost:8080/api/v1/appointments/patient/1?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

### 10. Приёмы врача на день

```bash
curl "http://localhost:8080/api/v1/appointments/doctor/1?date=2026-09-21&status=SCHEDULED" \
  -H "Authorization: Bearer $TOKEN"
```

## Формат ошибок

Все ошибки приходят в едином виде:

```json
{
  "timestamp": "2026-09-17T08:28:59Z",
  "status": 409,
  "code": "SLOT_ALREADY_BOOKED",
  "message": "Слот уже занят другой записью",
  "path": "/api/v1/appointments",
  "violations": [{ "field": "iin", "message": "ИИН должен состоять из 12 цифр" }]
}
```

| Код | HTTP | Когда |
|---|---|---|
| `VALIDATION_ERROR` | 400 | не прошла валидация полей |
| `SLOT_IN_PAST` | 400 / 409 | слот в прошлом |
| `INVALID_DATE_RANGE`, `RANGE_TOO_LONG` | 400 | некорректный период |
| `UNAUTHORIZED`, `BAD_CREDENTIALS` | 401 | нет токена или неверный пароль |
| `ACCESS_DENIED` | 403 | нет прав, в т.ч. чужое расписание |
| `NOT_FOUND` | 404 | сущность не найдена |
| `SLOT_OVERLAP` | 409 | пересечение слотов |
| `SLOT_ALREADY_BOOKED`, `SLOT_BLOCKED` | 409 | слот недоступен |
| `DUPLICATE_APPOINTMENT` | 409 | повторная запись к тому же врачу в тот же день |
| `APPOINTMENT_NOT_ACTIVE` | 409 | запись уже закрыта |
| `IIN_ALREADY_EXISTS` | 409 | ИИН занят |
| `CONCURRENT_MODIFICATION` | 409 | конфликт оптимистичной блокировки |

## Тесты

25 unit-тестов на бизнес-логику (`mvn test`), «сейчас» подменяется фиксированным `Clock`:

- **`AppointmentServiceTest`** — занятие слота при создании записи, отказ на занятом,
  заблокированном и прошедшем слоте, отказ на дубле записи, освобождение слота при отмене,
  запрет повторной отмены, завершение приёма и неявка (слот остаётся занятым);
- **`TimeSlotServiceTest`** — отказ на пересекающемся слоте, слоте в прошлом и перевёрнутом
  интервале, запрет чужого расписания, нарезка дня с вычетом перерыва, пропуск выходных
  и пересечений при генерации, запрет блокировки и удаления занятого слота, мягкое удаление,
  фильтрация прошедших слотов в поиске.
