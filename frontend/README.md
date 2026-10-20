# VibeSprint frontend

This Vue 3, TypeScript, and Quasar app is the interface for [VibeSprint](../README.md). The root README explains the project, shows the demo GIFs, and covers the full local setup.

With the backend running at `http://localhost:8080`, start the frontend with:

```bash
npm ci
npm run dev
```

Open `http://localhost:9000`. The dev server proxies `/api` to the backend. WebSocket/STOMP connects to `ws://localhost:8080/ws` by default; set `VITE_WS_URL` to use another endpoint.

For local checks, run `npm run typecheck` and `npm run build`.
