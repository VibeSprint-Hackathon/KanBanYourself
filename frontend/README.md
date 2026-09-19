# VibeSprint — фронтенд

Vue 3 + TypeScript + Quasar Dashboard. Текущее состояние и следующий шаг описаны в [FRONTEND.md](../docs/FRONTEND.md) и [IMPLEMENTATION_PLAN.md](../docs/IMPLEMENTATION_PLAN.md).

## Требования

- Node.js `>=22.12` согласно `package.json`.
- Запущенный backend на `http://localhost:8080` для API и WebSocket.

## Установка и запуск

```bash
npm install
npm run dev
```

Dev-сервер проксирует `/api` на backend. STOMP в разработке подключается к `ws://localhost:8080/ws`; другой адрес можно задать через `VITE_WS_URL`.

## Проверки

```bash
npm run typecheck
npm run lint:check
npm run build
```

## Текущая граница

Dashboard, Quests и Raids используют типизированный API, общий Pinia store и realtime. На доске доступны создание, редактирование, удаление и перемещение Квестов между пятью серверными статусами. Локально хранятся только настройки вида доски. Не переносить расчёты XP, уровней, наград или урона во frontend.
