# REST-контракт демонстрации

## GET `/api/demo/state`

Возвращает все данные, необходимые первому Dashboard и доске Квестов.

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

Seed может содержать дополнительные Квесты для трёх колонок, но форма каждого элемента остаётся такой же.

## POST `/api/demo/reset`

Атомарно возвращает подготовленное состояние и отвечает тем же объектом, что `GET /api/demo/state`.

Тело запроса отсутствует. Endpoint предназначен только для локального демо и не является продуктовой функцией.

## POST `/api/demo/quests/{questId}/complete`

Имитирует внешнее завершение, но вызывает тот же сервис, который позже использует GitHub-адаптер.

```json
{
  "eventId": "demo-payment-validation-1",
  "source": "DEMO"
}
```

`eventId` обязателен и непуст. В MVP от повторной награды прежде всего защищает статус Квеста; `eventId` готовит границу к внешним событиям.

## Успешный результат

```json
{
  "eventId": "demo-payment-validation-1",
  "applied": true,
  "reason": null,
  "xpGained": 180,
  "raidDamage": 180,
  "levelUp": true,
  "unlockedCosmetic": {
    "key": "rare-hoodie",
    "displayName": "Rare Hoodie"
  },
  "reaction": "level-up",
  "bossDefeated": true,
  "quest": {
    "id": 101,
    "title": "Fix payment validation",
    "status": "DONE",
    "xpReward": 180,
    "assigneeId": 1,
    "externalReference": null
  },
  "player": {
    "id": 1,
    "name": "Andrei",
    "totalXp": 1100,
    "level": 5,
    "nextLevelXp": 1500,
    "title": "Code Raider",
    "cosmeticKey": "rare-hoodie",
    "characterState": "idle"
  },
  "raid": {
    "id": 201,
    "name": "Merge Conflict Hydra",
    "maxHp": 1000,
    "currentHp": 0,
    "status": "DEFEATED"
  }
}
```

## Повторное завершение

Для Квеста `DONE` возвращается HTTP 200 и полные текущие объекты без новых наград:

```json
{
  "eventId": "demo-payment-validation-1",
  "applied": false,
  "reason": "ALREADY_COMPLETED",
  "xpGained": 0,
  "raidDamage": 0,
  "levelUp": false,
  "unlockedCosmetic": null,
  "reaction": null,
  "bossDefeated": true,
  "quest": {},
  "player": {},
  "raid": {}
}
```

Пустые объекты в примере обозначают полные текущие объекты соответствующей формы, а не сокращённый реальный ответ.

## Формат ошибки

```json
{
  "code": "QUEST_NOT_FOUND",
  "message": "Quest 999 was not found"
}
```

Список кодов и правила прогресса находятся в [README.md](README.md).

