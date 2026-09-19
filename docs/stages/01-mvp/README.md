# Этап 01 — MVP

Статус: `ACTIVE`  
Текущий подэтап: `I2 — NEXT: проверить полный вертикальный сценарий`

## Результат этапа

Работающий вертикальный сценарий: загрузить подготовленное состояние, завершить Квест один раз, получить прогресс игрока и рейда и обновить UI без перезагрузки.

## Входные документы

- Контракт: [../00-contracts/README.md](../00-contracts/README.md) и [CONTRACTS.md](../00-contracts/CONTRACTS.md).
- Текущий порядок работ: [../../IMPLEMENTATION_PLAN.md](../../IMPLEMENTATION_PLAN.md).
- HTTP и realtime: [../../backend/API.md](../../backend/API.md) и [../../INTEGRATIONS.md](../../INTEGRATIONS.md).
- Правила фронтенда: [../../FRONTEND.md](../../FRONTEND.md).

## Владение

| Поток | Участник | Область |
|---|---|---|
| A | Тимофей | Схема, бизнес-логика и REST API |
| B | Андрей | Dashboard, frontend API и состояния интерфейса |
| C | Никита; backend-часть временно закрыл Тимофей | Reset, доставка событий и будущие внешние интеграции |

## Выполнено

### Поток A — `DONE`

- A1: Liquibase-схема, seed, JPA-сущности и репозитории.
- A2: атомарное и идемпотентное завершение Квеста с XP, уровнем, наградой и уроном рейду.
- A3: `GET /api/demo/state`, `POST /api/demo/quests/{questId}/complete`, DTO, validation и единые ошибки.
- Проверка: 40 backend-тестов, 0 ошибок, Java 21 и PostgreSQL 17.

### Поток B — `DONE`

- Типы `DemoState`/`ProgressionResult` соответствуют REST-контракту и nullable-полям.
- Реализован Dashboard на `/`: layout, игрок, активный Квест, drawer, XP, рейд, toast и адаптивность.
- Dashboard загружает state, завершает Квест и сбрасывает демо через backend API.
- Pinia хранит единый state для layout, Dashboard и realtime; loading/error/retry готовы.
- `/quests` показывает серверные Квесты в `TODO`, `IN_PROGRESS`, `DONE` и завершает их через общий API.
- `/raids` показывает серверный рейд, фактический последний урон и realtime-изменения.
- История рейдов и отсутствующие в API метрики не подменяются fixture.
- Проверка: frontend typecheck и production build проходят.

### Поток C — `DONE`

- `POST /api/demo/reset` атомарно восстанавливает точный seed.
- STOMP работает через `/ws` и `/topic/progression`; публикуются только результаты `applied=true`.
- Vue-клиент подключается при старте и автоматически переподключается.
- Живая проверка подтвердила доставку backend -> browser.

## Текущее ограничение

CRUD Квестов и история рейдов отсутствуют в backend и не входят в обязательный MVP. Следующая задача — подтвердить стабильность уже собранного сценария тремя полными прогонами.

## Следующие подэтапы

### I1Q — `DONE`

`/quests` подключён к `demoStore.state.quests`; серверные Create/Edit/Delete/drag скрыты. Живая проверка подтвердила перемещение Квеста и синхронизацию Dashboard.

### I1R — `DONE`

`/raids` подключён к `demoStore.state.raid` и `ProgressionResult`. Живая проверка подтвердила переход 180 HP `ACTIVE` -> 0 HP `DEFEATED` через STOMP без перезагрузки.

### I2 — `NEXT`

Трижды пройти `reset -> load всех вкладок -> complete -> repeat -> refresh`, затем проверить отказ backend и realtime.

Точный порядок описан в [IMPLEMENTATION_PLAN.md](../../IMPLEMENTATION_PLAN.md#i2--проверить-вертикальный-сценарий-next).

## Контрольные точки

1. Контракт 0.1/0.2 — `DONE`.
2. A1/A2/A3 — `DONE`.
3. B1/B2/B3 — `DONE`.
4. C1/C2 — `DONE`.
5. I1 UI + backend — `DONE`.
6. B3b визуальные вкладки — `DONE`.
7. I1Q Quests + backend — `DONE`; I1R Raids + backend — `DONE`.
8. I2 три полных прогона по всем вкладкам — `NEXT`.

## Проверка завершения этапа

- `reset -> load -> complete -> repeat -> refresh` проходит три раза.
- Первый complete даёт 1100 XP, уровень 5, `rare-hoodie` и 0 HP.
- Повтор не даёт XP, урон и realtime-событие.
- Финальное состояние переживает refresh; ошибка backend не ломает страницу.
- Frontend typecheck/build и все backend-тесты зелёные.

## Передача дальше

Выполнить I2. Полный CRUD, история рейдов, дополнительная анимация и GitHub webhook не блокируют MVP.
