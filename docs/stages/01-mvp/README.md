# Этап 01 — MVP

Статус: `ACTIVE`  
Текущий подэтап: `A3 — DONE`

## Результат этапа

Работающий вертикальный сценарий: загрузить подготовленное состояние, завершить Квест один раз, получить прогресс игрока и рейда, обновить UI без перезагрузки.

## Входные документы

- Контракты и правила прогресса: [../00-contracts/README.md](../00-contracts/README.md).
- Точные REST payload: [../00-contracts/CONTRACTS.md](../00-contracts/CONTRACTS.md).
- План и порядок работ: [../../IMPLEMENTATION_PLAN.md](../../IMPLEMENTATION_PLAN.md).
- Постоянные правила бэкенда: [../../BACKEND.md](../../BACKEND.md).

Не перечитывать остальные документы без конкретной необходимости.

## Владение

| Поток | Участник | Область |
|---|---|---|
| A | Тимофей | Схема, доменная логика, чтение состояния, REST DTO и ошибки |
| B | Андрей | Vue UI, типы контракта, fixture и анимации |
| C | Никита | Reset, внешние триггеры, доставка событий и необязательный realtime |

## Завершённые подэтапы A1 и A2

- A1: схема, seed, сущности и репозитории готовы; детали — [BACKEND_A1.md](BACKEND_A1.md).
- A2: `ProgressionService.completeQuest` атомарно завершает Квест, начисляет XP, выдаёт награду и наносит урон рейду.
- Повтор и две параллельные команды дают только одно начисление; `eventId` пока не хранится.
- A2 возвращает внутренний `ProgressionResult`; это не REST DTO.
- Схема и зависимости в A2 не менялись.
- Проверка A1+A2 на Java 21 и PostgreSQL 17: 26 тестов, 0 ошибок.

## Подэтап A3 — API чтения и завершения

Цель: открыть HTTP-границу, которая точно соответствует замороженному JSON-контракту и не содержит бизнес-расчётов.

Владелец: Тимофей. Основная область: новый пакет `com.vibesprint.backend.api` и HTTP-тесты. Пакеты `integration` и `realtime` не изменяются.

### Область A3

A3 реализует:

- `GET /api/demo/state` — агрегированное текущее состояние;
- `POST /api/demo/quests/{questId}/complete` — перевод HTTP-запроса во внутреннюю команду A2;
- DTO, явное преобразование внутренних enum в согласованные JSON-значения;
- единый формат ошибок и HTTP-статусы;
- contract-тесты и один сценарий `GET -> complete -> GET` на реальной БД.

`POST /api/demo/reset` остаётся C2: ему нужна отдельная транзакция восстановления seed. C2 переиспользует DTO и mapper A3 и не создаёт второй endpoint завершения.

### Поток запроса

```text
HTTP request -> validation -> DemoController -> query service или ProgressionService
             -> mapper -> REST DTO -> JSON response
```

Контроллер не читает репозитории и не рассчитывает XP, уровень, награду, состояние персонажа или статус рейда.

### Read-модель состояния

`DemoStateQueryService` выполняет read-only транзакцию и получает:

1. демонстрационного игрока с id `1`;
2. все Квесты по возрастанию id;
3. рейд с наименьшим id;
4. вычисляемый прогресс через `ProgressionRules`.

`characterState` равен `coding`, пока существует хотя бы один `IN_PROGRESS`, иначе `idle`. `nextUnlock` содержит `rare-hoodie` до достижения 1000 XP; после получения единственной награды MVP возвращается `null`, новая награда не придумывается.

Если игрок или рейд отсутствует, запрос возвращает `409 DEMO_STATE_NOT_READY`. Пустой список Квестов допустим.

### Команда завершения

- Path `questId` должен быть положительным.
- Тело содержит непустой `eventId` и обязательный `source` из `DEMO | GITHUB`.
- После HTTP validation контроллер создаёт `CompleteQuestCommand` и один раз вызывает `ProgressionService.completeQuest`.
- Первый вызов и повтор отвечают HTTP 200; семантика определяется полями `applied` и `reason`.
- Поля снимков берутся из `ProgressionResult`, без повторного чтения БД.

### DTO и JSON

- `DemoStateResponse` содержит `player`, `quests`, `raid`, nullable `nextUnlock` и общие вложенные снимки.
- `CompleteQuestRequest` содержит `eventId` и `source`.
- `ProgressionResponse` повторяет успешный результат из `CONTRACTS.md`.
- `ApiErrorResponse` содержит только `code` и `message`.
- В JSON статусы остаются `IN_PROGRESS`, `DONE`, `ACTIVE`, `DEFEATED`.
- `characterState` сериализуется как `idle | coding`, реакция — `happy | level-up`.
- Nullable-поля не удаляются из JSON: клиент всегда видит стабильную форму ответа.
- JPA-сущности и внутренний `ProgressionResult` напрямую не сериализуются.

### Ошибки

| Условие | HTTP | `code` |
|---|---:|---|
| Невалидный path/body, пустой `eventId`, неизвестный `source` | 400 | `INVALID_REQUEST` |
| Квест отсутствует | 404 | `QUEST_NOT_FOUND` |
| Игрок или рейд не подготовлен | 409 | `DEMO_STATE_NOT_READY` |
| Неожиданная ошибка | 500 | `INTERNAL_ERROR` |

`@RestControllerAdvice` скрывает имена Java-классов, SQL и stack trace. Для ожидаемых ошибок сообщение остаётся коротким и полезным; для `500` используется нейтральный текст.

### План файлов

```text
backend/src/main/java/com/vibesprint/backend/api/
├── DemoController.java
├── DemoStateQueryService.java
├── DemoStateResponse.java
├── CompleteQuestRequest.java
├── ProgressionResponse.java
├── DemoApiMapper.java
├── ApiErrorResponse.java
└── ApiExceptionHandler.java
```

Тесты разместить в зеркальном пакете `backend/src/test/java/com/vibesprint/backend/api/`. Новые зависимости и Liquibase changeset не нужны.

### Проверка A3

- GET на seed возвращает игрока 920 XP, три Квеста в стабильном порядке, рейд 180 HP и следующую награду.
- Complete Квеста 101 возвращает точные имена, регистр и nullable-поля контракта.
- Повтор возвращает HTTP 200, `applied=false`, нулевые начисления и полные снимки.
- Следующий GET показывает сохранённые 1100 XP, Квест `DONE`, рейд `DEFEATED` и `nextUnlock=null`.
- Ошибочные path/body/source, отсутствующий Квест и неполное состояние имеют согласованные status/code.
- Полный Maven test suite остаётся зелёным на PostgreSQL 17.

### Не входит в A3

- Reset и восстановление seed, GitHub webhook, WebSocket и публикация событий.
- Изменение схемы, seed, правил A2 или замороженного JSON-контракта.
- CORS, аутентификация, OpenAPI, универсальный mapper/framework и новые зависимости.
- Изменения фронтенда.

## Точки интеграции

- B может подключить два готовых endpoint после A3; `nextUnlock` в TypeScript должен допускать `null`.
- C получает готовые DTO/mapper и реализует только reset и внешние способы запуска.
- Изменение JSON, seed или формулы требует совместного решения команды.

## Контрольные точки

1. A1: чистая БД мигрирует, seed читается через JPA — `DONE`.
2. A2: завершение Квеста атомарно и идемпотентно — `DONE`.
3. A3: REST точно соответствует `CONTRACTS.md` — `DONE`.
4. I1: UI работает с живым REST вместо fixture.
5. I2: reset -> complete -> повтор -> reset проходит три раза.

## Критерий завершения A3

- Два endpoint A3 работают через DTO и сервисные границы.
- JSON и ошибки совпадают с контрактом, включая повтор команды.
- Refresh показывает сохранённый результат без дополнительных мутаций.
- Все тесты проходят; схема, правила прогресса и зависимости не изменены.

## Передача дальше

A3 реализован в пакете `api`: добавлены агрегированное чтение, endpoint завершения, DTO, mapper, validation и единый формат ошибок. `ProgressionRules` теперь также сообщает следующую ещё не полученную награду MVP. Схема, seed и зависимости не менялись.

Проверка на Java 21 и PostgreSQL 17 завершена: 34 теста, 0 ошибок. HTTP-тесты покрывают исходное состояние, завершение, повтор, refresh, невалидные запросы, отсутствующий Квест, игрока и рейд.

Следующий шаг: Никита реализует reset в C2 поверх согласованной API-модели, а Андрей заменяет fixture живыми GET/POST вызовами в I1.
