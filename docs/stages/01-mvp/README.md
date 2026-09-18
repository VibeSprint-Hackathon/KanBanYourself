# Этап 01 — MVP

Статус: `ACTIVE`  
Текущий подэтап: `I1 — NEXT: подключить Dashboard к backend API`

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

### Поток B — `B1/B2 DONE`, `B3 PARTIAL`

- Типы `DemoState`/`ProgressionResult` соответствуют REST-контракту и nullable-полям.
- Реализован Dashboard на `/`: layout, игрок, активный Квест, drawer, XP, рейд, toast и адаптивность.
- Mock-complete обновляет визуальное состояние из fixture.
- Не готово: живые GET/POST, loading/error, применение realtime и отдельная доска из трёх колонок.
- Проверка: frontend typecheck и production build проходят.

### Поток C — `DONE`

- `POST /api/demo/reset` атомарно восстанавливает точный seed.
- STOMP работает через `/ws` и `/topic/progression`; публикуются только результаты `applied=true`.
- Vue-клиент подключается при старте и автоматически переподключается.
- Живая проверка подтвердила доставку backend -> browser.

## Текущее ограничение

Dashboard выглядит и реагирует, но доменное состояние берётся из `dashboard.fixture.ts`, а Complete Quest не вызывает backend. STOMP-события принимаются клиентом, но не применяются к Dashboard. Поэтому полный MVP ещё не готов.

## Следующий подэтап I1

Frontend должен:

1. загрузить `GET /api/demo/state` при открытии;
2. хранить один серверный state для Dashboard, layout и realtime;
3. завершить Квест через реальный `POST .../complete`;
4. применить REST-ответ без собственных расчётов;
5. подписаться на готовый realtime-клиент без двойного применения одного события;
6. показать loading, ошибку и повтор запроса;
7. оставить fixture только для presentation-текста и временных визуальных данных.

Точный порядок и критерии описаны в [IMPLEMENTATION_PLAN.md](../../IMPLEMENTATION_PLAN.md#i1--подключить-ui-к-backend-api-next).

## Контрольные точки

1. Контракт 0.1/0.2 — `DONE`.
2. A1/A2/A3 — `DONE`.
3. B1/B2 — `DONE`; B3 — `PARTIAL`.
4. C1/C2 — `DONE`.
5. I1 UI + backend — `NEXT`.
6. B3b минимальная доска из общего state — `PLANNED`.
7. I2 три полных прогона — `PLANNED`.

## Проверка завершения этапа

- `reset -> load -> complete -> repeat -> refresh` проходит три раза.
- Первый complete даёт 1100 XP, уровень 5, `rare-hoodie` и 0 HP.
- Повтор не даёт XP, урон и realtime-событие.
- Финальное состояние переживает refresh; ошибка backend не ломает страницу.
- Frontend typecheck/build и все backend-тесты зелёные.

## Передача дальше

После I1 завершить минимальные колонки Квестов из того же серверного state, затем выполнить I2. Отдельные маршруты, дополнительную анимацию и GitHub webhook обсуждать только после стабильного сценария.
