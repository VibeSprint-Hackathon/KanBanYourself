# API бэкенда

## Назначение

Этот документ описывает HTTP API, доступный фронтенду и демонстрационным адаптерам. Базовый адрес локального приложения: `http://localhost:8080`.

Точные реализованные DTO определяются кодом в `backend/src/main/java/com/vibesprint/backend/api/` и HTTP-тестами. Замороженный контракт MVP находится в [CONTRACTS.md](../stages/00-contracts/CONTRACTS.md).

## Текущий статус

| Метод и путь | Статус | Назначение |
|---|---|---|
| `GET /api/health` | Готов | Проверка запуска приложения |
| `GET /api/demo/state` | Готов | Получение всего состояния для интерфейса |
| `POST /api/demo/quests/{questId}/complete` | Готов | Завершение Квеста и применение прогресса |
| `POST /api/demo/reset` | Готов | Атомарное восстановление исходного состояния |

Для JSON-запросов используется `Content-Type: application/json`. Аутентификации в MVP нет.

## Общие значения

- Статус Квеста: `TODO | IN_PROGRESS | DONE`.
- Статус рейда: `ACTIVE | DEFEATED`.
- Состояние персонажа: `idle | coding`.
- Реакция после завершения: `happy | level-up`.
- Источник команды: `DEMO | GITHUB`.
- Nullable-поля присутствуют в JSON со значением `null`.

## Проверка доступности

### `GET /api/health`

Успешный ответ: HTTP 200 и строка `OK`.

```bash
curl http://localhost:8080/api/health
```

## Получение состояния

### `GET /api/demo/state`

Возвращает игрока, все Квесты, текущий рейд и следующую ещё не полученную награду. Квесты отсортированы по `id`.

```bash
curl http://localhost:8080/api/demo/state
```

Пример исходного ответа:

```json
{
  "player": {
    "id": 1,
    "name": "Andrei",
    "totalXp": 920,
    "level": 4,
    "nextLevelXp": 1000,
    "title": "Code Adventurer",
    "cosmeticKey": "base",
    "characterState": "coding"
  },
  "quests": [
    {
      "id": 101,
      "title": "Fix payment validation",
      "status": "IN_PROGRESS",
      "xpReward": 180,
      "assigneeId": 1,
      "externalReference": null
    }
  ],
  "raid": {
    "id": 201,
    "name": "Merge Conflict Hydra",
    "maxHp": 1000,
    "currentHp": 180,
    "status": "ACTIVE"
  },
  "nextUnlock": {
    "level": 5,
    "cosmeticKey": "rare-hoodie",
    "displayName": "Rare Hoodie"
  }
}
```

Реальный seed содержит три Квеста. После получения `rare-hoodie` поле `nextUnlock` равно `null`. Если подготовленный игрок или рейд отсутствует, API возвращает `409 DEMO_STATE_NOT_READY`.

## Завершение Квеста

### `POST /api/demo/quests/{questId}/complete`

`questId` — положительный числовой идентификатор. Endpoint вызывает атомарный сервис прогресса: переводит Квест в `DONE`, начисляет XP, вычисляет уровень и награду, наносит урон рейду.

```bash
curl -X POST http://localhost:8080/api/demo/quests/101/complete \
  -H "Content-Type: application/json" \
  -d '{"eventId":"demo-payment-validation-1","source":"DEMO"}'
```

Тело запроса:

| Поле | Тип | Правило |
|---|---|---|
| `eventId` | string | Обязательно, не может быть пустым |
| `source` | enum | Обязательно: `DEMO` или `GITHUB` |

Основные поля успешного ответа:

| Поле | Значение |
|---|---|
| `applied` | Применена ли награда в этом вызове |
| `reason` | `null` или `ALREADY_COMPLETED` |
| `xpGained` | Начисленный XP |
| `raidDamage` | Нанесённый урон |
| `levelUp` | Повысился ли уровень |
| `unlockedCosmetic` | Полученная косметика или `null` |
| `reaction` | `happy`, `level-up` или `null` |
| `bossDefeated` | Равен ли итоговый HP рейда нулю |
| `quest`, `player`, `raid` | Полные актуальные снимки объектов |

Для Квеста 101 первый вызов возвращает 180 XP, уровень 5, `rare-hoodie` и побеждённый рейд. Полный JSON-пример приведён в [контракте](../stages/00-contracts/CONTRACTS.md#успешный-результат).

### Повтор команды

Повторное завершение Квеста безопасно и отвечает HTTP 200:

- `applied=false`;
- `reason=ALREADY_COMPLETED`;
- `xpGained=0` и `raidDamage=0`;
- награда и реакция равны `null`;
- `quest`, `player` и `raid` содержат полное текущее состояние.

`eventId` возвращается в ответе, но в MVP не хранится. От повторной награды защищает статус Квеста `DONE`.

## Ошибки

Все контролируемые ошибки имеют одинаковую форму:

```json
{
  "code": "QUEST_NOT_FOUND",
  "message": "Quest 999 was not found"
}
```

| HTTP | `code` | Причина |
|---:|---|---|
| 400 | `INVALID_REQUEST` | Некорректный id, JSON, `eventId` или `source` |
| 404 | `QUEST_NOT_FOUND` | Квест не существует |
| 409 | `DEMO_STATE_NOT_READY` | Игрок или рейд не подготовлен |
| 500 | `INTERNAL_ERROR` | Неожиданный внутренний сбой |

Внутренние исключения, SQL и stack trace клиенту не возвращаются.

## Рекомендуемый поток фронтенда

1. При открытии страницы вызвать `GET /api/demo/state`.
2. Для демо-завершения вызвать `POST .../complete`.
3. Сразу применить `quest`, `player` и `raid` из ответа POST.
4. При сомнении в актуальности перечитать `GET /api/demo/state`.
5. Не рассчитывать XP, уровни, награды или урон на фронтенде.

## Сброс демо

### `POST /api/demo/reset`

Запрос не содержит тела. Endpoint атомарно удаляет изменённые и лишние демо-данные, восстанавливает исходного игрока, три Квеста и рейд, затем возвращает тот же объект, что `GET /api/demo/state`.

```bash
curl -X POST http://localhost:8080/api/demo/reset
```

## Realtime

- Протокол: STOMP поверх WebSocket.
- Подключение: `ws://localhost:8080/ws`.
- Подписка: `/topic/progression`.
- Payload: тот же JSON, что возвращает успешный `POST .../complete`.
- Сообщение отправляется только при `applied=true`; повтор не создаёт событие.
- Reset не отправляет другой тип сообщения.
- При ошибке или переподключении клиент перечитывает `GET /api/demo/state`.

Vue-клиент подключён при старте приложения, использует одну STOMP-сессию и передаёт типизированный payload подписчикам через `progressionRealtime.subscribe(...)`. В разработке адрес можно переопределить переменной `VITE_WS_URL`. REST остаётся основным путём инициирующего экрана и способом полной повторной синхронизации.
