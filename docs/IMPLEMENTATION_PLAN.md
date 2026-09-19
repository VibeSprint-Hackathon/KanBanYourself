# План реализации

План ведёт к одному надёжному вертикальному сценарию. Источники истины: границы MVP — [MVP.md](MVP.md), HTTP API — [backend/API.md](backend/API.md), состояние текущего этапа — [stages/01-mvp/README.md](stages/01-mvp/README.md).

Распределение потоков: A — Тимофей, B — Андрей, C — Никита. Тимофей временно закрыл backend-часть C. Статусы: `DONE`, `PARTIAL`, `NEXT`, `PLANNED`, `OPTIONAL`.

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
| B3 Реакция прогресса | `DONE` | Dashboard, Quests и Raids используют один state и realtime |
| C1 Realtime | `DONE` | STOMP backend и Vue-клиент проверены end-to-end |
| C2 Надёжный reset | `DONE` | `POST /api/demo/reset` атомарно восстанавливает seed |
| I1 UI + backend API | `DONE` | Dashboard использует REST, Pinia и realtime |
| B3b UI вкладок | `DONE` | `/quests` и `/raids` реализованы визуально на fixture |
| I1Q Quests + backend | `DONE` | Доска использует общий `DemoState` и complete API |
| I1R Raids + backend | `DONE` | Активный рейд и последний урон приходят с backend |
| I1P Профили и исполнители | `DONE` | 3 игрока, mock switch, assignee CRUD и персональный Dashboard |
| I1W Запись и история | `OPTIONAL` | CRUD Квестов и история рейдов после MVP |
| I2 Полный прогон | `NEXT` | Три повторяемых цикла и проверка ошибок |
| P1/P2 Полировка | `PLANNED` | Только после стабильного I2 |

Backend подтверждён полным набором из 70 тестов на Java 21 и PostgreSQL 17. Frontend проходит typecheck и production build.

## Выполненная основа

### Этап 0

Контракт 0.1 и владение 0.2 заморожены в [stages/00-contracts/README.md](stages/00-contracts/README.md). Изменение JSON, seed или формул требует совместного решения.

### Поток A — backend

- A1: миграции, seed, сущности и репозитории готовы.
- A2: завершение Квеста атомарно меняет Квест, XP и рейд; повтор не начисляет награду.
- A3: реализованы `GET /api/demo/state` и `POST /api/demo/quests/{questId}/complete`.
- I1P: `GET /api/players`, GitHub identity mapping и начисление XP исполнителю покрыты тестами.

### Поток B — интерфейс

- Типы `DemoState` и `ProgressionResult` соответствуют nullable-полям backend.
- На `/` реализован светлый Dashboard с игроком, активным Квестом, drawer, XP и рейдом.
- Dashboard читает серверное состояние через Pinia; fixture содержит только presentation-данные.
- `/quests` использует серверные Квесты, общий loading/error/realtime и complete API.
- `/raids` использует общий серверный рейд и показывает последний фактический урон.
- История, награда и суммарный вклад не подменяются fixture: текущий API их не предоставляет.

### Поток C — доставка

- `POST /api/demo/reset` возвращает исходное состояние.
- Backend публикует применённый `ProgressionResponse` в `/topic/progression`.
- Vue устанавливает одну STOMP-сессию при старте и предоставляет `progressionRealtime.subscribe(...)`.
- Dashboard применяет realtime-события к общему состоянию и не повторяет визуальную реакцию одного `eventId`.

## I1 — подключить UI к backend API (`DONE`)

Mock-данные удалены из рабочего пользовательского потока; fixture сохраняет только презентационные подписи и временные визуальные данные.

### Реализовано

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

## B3b — подготовить UI вкладок (`DONE`)

Добавлены отдельные вкладки `/quests` и `/raids`, карточки, drawer, состояния экрана и навигация. Это визуальная основа: данные Квестов, активного рейда и истории рейдов пока не являются серверными.

## I1Q — подключить Quests к backend (`DONE`)

На `/quests` показаны те же Квесты, которые вернул `GET /api/demo/state`, без второго доменного store.

1. Заменить `boardQuests` на вычисляемое представление `demoStore.state.quests`.
2. Для MVP показывать только серверные статусы `TODO`, `IN_PROGRESS`, `DONE`; `BACKLOG` и `TESTING` не выдавать за сохранённые статусы.
3. Описания и проценты оставить presentation-данными по `quest.id`; для неизвестного id использовать нейтральный fallback.
4. Использовать общие loading/error/retry и realtime из `useDemoStore`.
5. Завершение доступного Квеста проводить через существующий `completeQuest`; не рассчитывать XP и статус локально.
6. До появления write API скрыть или явно отключить Create/Edit/Delete/drag. Настройки вида колонок могут оставаться локальными.

Готово, когда reset показывает три seed-Квеста в правильных колонках, complete одновременно обновляет Quests, Dashboard и Raid, а refresh не возвращает fixture.

## I1R — подключить Raids к backend (`DONE`)

Цель — сделать `/raids` вторым представлением того же серверного рейда.

1. Брать `id`, `name`, HP и `status` из `demoStore.state.raid`.
2. Показывать последний урон из `ProgressionResult`, а итоговое состояние — из серверного снимка.
3. Использовать общие loading/error/retry и статус realtime; удалить переключатель fixture-состояний из рабочего потока.
4. Не показывать выдуманные `rewardXp`, суммарный вклад игрока и завершённые рейды как серверные данные.
5. После complete вкладки Dashboard и Raids должны без перезагрузки показать одинаковые HP и `DEFEATED`.

Текущий API содержит один рейд и не содержит историю. Поэтому интерфейс показывает честное информационное состояние вместо вымышленных записей.

Проверено в браузере: открытая `/raids` через STOMP без перезагрузки изменилась с 180 HP `ACTIVE` на 0 HP `DEFEATED` и показала урон 180 из `ProgressionResult`.

## I1W — серверные записи и история (`OPTIONAL`)

Только после I2 отдельно согласовать DTO и добавить backend API для Create/Edit/Delete/смены статуса Квеста и списка завершённых рейдов. Если понадобятся новые поля (`description`, порядок, progress, `completedAt`, reward, вклад игрока), сначала Liquibase и HTTP-тесты. Этот этап не блокирует демонстрацию и не должен размывать атомарный поток завершения Квеста.

## I2 — проверить вертикальный сценарий (`NEXT`)

После I1Q и I1R выполнить три цикла `reset -> load всех вкладок -> complete -> repeat -> refresh`. Проверить одинаковое состояние на Dashboard, Quests и Raids, пропавший WebSocket, быстрый двойной клик, nullable `nextUnlock`, максимальный уровень и reduced motion.

## Полировка и дополнительные возможности

После I2 улучшить один демонстрационный момент и зафиксировать холодный запуск по [DEMO.md](DEMO.md). GitHub webhook, n8n, отдельные страницы и дополнительные игровые системы не начинать до готового I2.

## Ближайшая передача

Вход следующей задачи: Dashboard, Quests и Raids работают с одним серверным state. Выход I2: три воспроизводимых полных цикла, проверенные повторы, refresh и отказ backend/realtime.
