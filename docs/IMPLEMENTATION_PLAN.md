# План реализации

План ведёт к одному надёжному вертикальному сценарию. Источники истины: границы MVP — [MVP.md](MVP.md), HTTP API — [backend/API.md](backend/API.md), состояние текущего этапа — [stages/01-mvp/README.md](stages/01-mvp/README.md).

Распределение потоков: A — Тимофей, B — Андрей, C — Никита. Тимофей временно закрыл backend-часть C. Статусы: `DONE`, `PARTIAL`, `NEXT`, `PLANNED`.

## Сводка

| Шаг | Статус | Фактический результат |
|---|---|---|
| 0.1 Контракт демо | `DONE` | Зафиксированы JSON, правила прогресса и ошибки |
| 0.2 Владение и запасной путь | `DONE` | Разделены A/B/C, обязательным выбран REST-ответ |
| A1 Схема и seed | `DONE` | PostgreSQL/Liquibase, Player, Quest, Raid |
| A2 Сервис прогресса | `DONE` | Одна транзакция, идемпотентность и блокировки |
| A3 REST API | `DONE` | State, complete, DTO и единые ошибки |
| B1 Типы и fixture | `DONE` | Типы совпадают с backend; fixture собирает UI отдельно |
| B2 Dashboard | `DONE` | Главная сцена, карточки, drawer и адаптивная компоновка |
| B3 Реакция прогресса | `PARTIAL` | Mock-complete меняет UI; отдельной доски и живого потока ещё нет |
| C1 Realtime | `DONE` | STOMP backend и Vue-клиент проверены end-to-end |
| C2 Надёжный reset | `DONE` | `POST /api/demo/reset` атомарно восстанавливает seed |
| I1 UI + backend API | `NEXT` | Следующая задача |
| B3b Минимальная доска | `PLANNED` | После I1 показать три контрактных статуса из общего state |
| I2 Полный прогон | `PLANNED` | Три повторяемых цикла и проверка ошибок |
| P1/P2 Полировка | `PLANNED` | Только после стабильного I2 |

Backend подтверждён 40 тестами на Java 21 и PostgreSQL 17. Frontend проходит typecheck и production build.

## Выполненная основа

### Этап 0

Контракт 0.1 и владение 0.2 заморожены в [stages/00-contracts/README.md](stages/00-contracts/README.md). Изменение JSON, seed или формул требует совместного решения.

### Поток A — backend

- A1: миграции, seed, сущности и репозитории готовы.
- A2: завершение Квеста атомарно меняет Квест, XP и рейд; повтор не начисляет награду.
- A3: реализованы `GET /api/demo/state` и `POST /api/demo/quests/{questId}/complete`.

### Поток B — интерфейс

- Типы `DemoState` и `ProgressionResult` соответствуют nullable-полям backend.
- На `/` реализован светлый Dashboard с игроком, активным Квестом, drawer, XP и рейдом.
- Сейчас `useDashboardDemo` читает `dashboard.fixture.ts`, а Complete Quest применяет mock-результат.
- Отдельные маршруты Квестов, рейдов и достижений не реализованы; элементы sidebar пока декоративны.

### Поток C — доставка

- `POST /api/demo/reset` возвращает исходное состояние.
- Backend публикует применённый `ProgressionResponse` в `/topic/progression`.
- Vue устанавливает одну STOMP-сессию при старте и предоставляет `progressionRealtime.subscribe(...)`.
- Dashboard ещё не применяет realtime-события к своему состоянию.

## I1 — подключить UI к backend API (`NEXT`)

Это следующий шаг. Его цель — убрать mock-данные из рабочего пользовательского потока, сохранив fixture только как презентационные подписи и временные визуальные данные.

### Реализация

1. Добавить типизированные функции `getDemoState`, `completeQuest` и `resetDemo` поверх существующего Axios instance.
2. Создать один общий источник серверного состояния для Dashboard, layout и realtime. Небольшой Pinia store здесь оправдан; presentation-данные остаются отдельно.
3. При открытии Dashboard загружать `GET /api/demo/state`; показать короткие loading и retry/error состояния.
4. Сделать `completeQuest` асинхронным и отправлять `POST /api/demo/quests/{id}/complete` с уникальным `eventId` и `source=DEMO`.
5. Сразу применять REST-ответ к Квесту, игроку и рейду; frontend не рассчитывает XP, уровень, награду или урон.
6. На время запроса блокировать повторный клик. `applied=false` синхронизирует снимки без повторной реакции.
7. Подписать store на `progressionRealtime` и дедуплицировать визуальную реакцию по `eventId`, потому что собственный POST также публикует STOMP-событие.
8. После восстановления realtime или некорректного payload перечитывать `GET /api/demo/state`.
9. Reset не публикует STOMP: явное демо-действие должно применить возвращённый `DemoState`; автоматически сбрасывать состояние при загрузке нельзя.

### Файлы

- `frontend/src/api/demo.ts` — новые HTTP-функции.
- `frontend/src/stores/demo.ts` и регистрация Pinia — единое серверное состояние и действия.
- `frontend/src/composables/useDashboardDemo.ts` — presentation-состояние и временные реакции.
- `frontend/src/pages/IndexPage.vue` и drawer — только необходимые loading/error/disabled связи.
- `frontend/src/layouts/MainLayout.vue` — убрать чтение имени игрока из доменного fixture.
- `frontend/src/fixtures/dashboard.fixture.ts` — после I1 только presentation-данные; серверные снимки и mock-результаты удалить.

Backend DTO, схема и формулы в I1 не меняются. Если найдено реальное расхождение контракта, его фиксируют отдельно с HTTP-тестом.

### Критерии завершения I1

- После reset и перезагрузки Dashboard показывает 920 XP, Квест 101 `IN_PROGRESS` и 180 HP из backend.
- Complete Quest вызывает реальный POST и без перезагрузки показывает 1100 XP, уровень 5, `DONE` и 0 HP.
- Повтор не начисляет прогресс и не запускает повторную награду.
- Refresh сохраняет завершённое состояние.
- При выключенном backend UI показывает понятную ошибку и позволяет повторить загрузку.
- Typecheck, lint изменённых файлов и production build проходят.

## B3b — завершить минимальную доску (`PLANNED`)

После стабильного I1 показать Квесты из того же `DemoState` в колонках `TODO`, `IN_PROGRESS`, `DONE`. Допустимо встроить компактную доску в Dashboard вместо отдельного маршрута. Она не получает второй store и не меняет статусы локально в обход ответа backend.

## I2 — проверить вертикальный сценарий (`PLANNED`)

После I1 и B3b выполнить три цикла `reset -> load -> complete -> repeat -> refresh`. Проверить пропавший WebSocket, быстрый двойной клик, nullable `nextUnlock`, максимальный уровень и reduced motion. Исправлять только воспроизведённые дефекты.

## Полировка и дополнительные возможности

После I2 улучшить один демонстрационный момент и зафиксировать холодный запуск по [DEMO.md](DEMO.md). GitHub webhook, n8n, отдельные страницы и дополнительные игровые системы не начинать до готового I1/I2.

## Ближайшая передача

Вход следующей задачи: работающий backend, готовый Dashboard, согласованные TypeScript-типы, Axios instance и STOMP-клиент. Выход: Dashboard полностью работает с живым API, а fixture больше не управляет доменным состоянием.
