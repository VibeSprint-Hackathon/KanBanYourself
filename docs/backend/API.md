# API бэкенда

## Назначение

HTTP API для Dashboard и пятиколоночной доски. Базовый адрес: `http://localhost:8080`. Аутентификации в демо нет; JSON-запросы используют `Content-Type: application/json`.

## Endpoint

| Метод и путь | Успех | Назначение |
|---|---:|---|
| `GET /api/health` | 200 | Проверка запуска |
| `GET /api/demo/state` | 200 | Полный снимок демо |
| `POST /api/demo/reset` | 200 | Восстановление точного seed |
| `POST /api/demo/quests` | 201 | Создание Квеста |
| `PUT /api/demo/quests/{id}` | 200 | Обновление Квеста |
| `PATCH /api/demo/quests/{id}/move` | 200 | Перемещение/сортировка |
| `DELETE /api/demo/quests/{id}` | 200 | Удаление незавершённого Квеста |
| `POST /api/demo/quests/{id}/complete` | 200 | Завершение и прогресс игрока/рейда |

Все четыре обычные мутации Квеста возвращают полный актуальный `DemoStateResponse`.

## Квест

Полный объект Квеста имеет один контракт:

```json
{
  "id": 101,
  "title": "Fix payment validation",
  "description": "Fix server-side validation and cover the payment edge cases.",
  "status": "IN_PROGRESS",
  "progress": 72,
  "xpReward": 180,
  "assigneeId": 1,
  "externalReference": null,
  "sortOrder": 100
}
```

Статусы: `BACKLOG | TODO | IN_PROGRESS | TESTING | DONE`. Порядок колонок фиксирован именно так; внутри колонки — `sortOrder ASC`, затем `id ASC`.

- Для `BACKLOG`/`TODO` progress равен `null`.
- Для `IN_PROGRESS`/`TESTING` progress находится в `0..100`; при входе без значения используется `0`.
- Для `DONE` progress равен `100`.
- `sortOrder` положительный; после move значения нормализуются с шагом 100.

## Полное состояние

### `GET /api/demo/state`

Возвращает `player`, десять упорядоченных `quests`, `raid` и nullable `nextUnlock`. Reset возвращает ту же форму. Исходные ключевые значения: игрок 1 с 920 XP, Квест 101 `IN_PROGRESS`/72/180 XP, рейд 201 с 180 из 1000 HP.

## Создание

### `POST /api/demo/quests`

```json
{
  "title": "Add keyboard shortcuts",
  "description": "Speed up common actions.",
  "status": "BACKLOG",
  "progress": null,
  "xpReward": 250,
  "externalReference": null
}
```

Допустимы только незавершённые статусы. Исполнитель всегда demo player 1. Квест получает id из PostgreSQL sequence (начиная с 1000) и добавляется в конец колонки.

## Обновление

### `PUT /api/demo/quests/{id}`

Тело совпадает с create. У незавершённого Квеста разрешены любые незавершённые статусы; смена колонки добавляет его в конец. `DONE` нельзя установить этим endpoint.

У завершённого Квеста разрешено менять только `title`, `description`, `externalReference`; запрос обязан сохранить `status=DONE`, `progress=100` и прежний `xpReward`.

## Перемещение

### `PATCH /api/demo/quests/{id}/move`

```json
{
  "status": "TESTING",
  "beforeQuestId": 108
}
```

`beforeQuestId=null` добавляет в конец. Иначе id должен указывать на другой Квест целевой колонки. Исходная и целевая колонки нормализуются. Завершённый Квест и перемещение в `DONE` запрещены.

## Удаление

### `DELETE /api/demo/quests/{id}`

Удаляет только незавершённый Квест и возвращает оставшееся состояние. `DONE` не удаляется, поскольку XP и урон необратимы.

## Завершение

### `POST /api/demo/quests/{id}/complete`

```json
{"eventId":"demo-payment-validation-1","source":"DEMO"}
```

Источник: `DEMO | GITHUB`. Одна транзакция переводит Квест в `DONE`, ставит progress 100, добавляет его в конец `DONE`, начисляет XP и наносит равный урон рейду. `ProgressionResponse.quest` использует полный контракт Квеста.

Повтор безопасен: `applied=false`, `reason=ALREADY_COMPLETED`, XP/урон равны 0, а снимки Квеста, игрока и рейда остаются полными. Для Квеста 101 первый вызов даёт 1100 XP и 0 HP рейда.

## Валидация и ошибки

- `title`: непустой, максимум 255 символов.
- `description`: обязателен, максимум 2000 символов.
- `xpReward`: положительное целое.
- `externalReference`: nullable, максимум 500 символов.
- Path id и nullable `beforeQuestId`: положительные.

Форма ошибки: `{"code":"QUEST_NOT_FOUND","message":"Quest 999 was not found"}`.

| HTTP | code | Причина |
|---:|---|---|
| 400 | `INVALID_REQUEST` | Невалидный JSON, поля или progress/status |
| 404 | `QUEST_NOT_FOUND` | Нет Квеста или `beforeQuestId` не в целевой колонке |
| 409 | `QUEST_CONFLICT` | Запрещённый переход, удаление или изменение завершённого Квеста |
| 409 | `DEMO_STATE_NOT_READY` | Не подготовлен игрок или рейд |
| 500 | `INTERNAL_ERROR` | Неожиданный сбой без внутренних деталей |

## Reset и realtime

`POST /api/demo/reset` атомарно удаляет пользовательские изменения, восстанавливает игрока, десять Квестов, рейд и sequence Квестов на 1000.

STOMP подключается к `/ws`, topic — `/topic/progression`. Публикуется только применённый `ProgressionResponse`; повтор и reset событий не создают. REST остаётся обязательным источником полного состояния.
