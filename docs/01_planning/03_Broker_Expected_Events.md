# Ожидаемые события брокера

## OrderPaymentRequested

Событие отправляется из Orders Service в Payments Service.

```
{
"event_id": "a1f4d85d-0001-4000-9000-111111111111",
"order_id": "550e8400-e29b-41d4-a716-446655440000",
"user_id": "user-42",
"amount": 120,
"occurred_at": "2026-06-10T12:00:01Z"
}
```

(Ожидаемое поведение) Payments Service должен:

- проверить, не обрабатывался ли уже order_id;
- найти счет пользователя;
- проверить баланс;
- списать сумму, если средств достаточно;
- сохранить запись об операции;
- отправить результат оплаты.

## OrderPaymentCompleted

Событие отправляется из Payments Service в Orders Service.

```
{
  "event_id": "b2f4d85d-0002-4000-9000-222222222222",
  "order_id": "550e8400-e29b-41d4-a716-446655440000",
  "user_id": "user-42",
  "amount": 120,
  "new_balance": 880,
  "occurred_at": "2026-06-10T12:00:02Z"
}
```

(Ожидаемое поведение) Orders Service должен:

- проверить дубликат события;
- найти заказ;
- проверить принадлежность заказа пользователю;
- перевести заказ в статус PAID;
- сохранить время обновления.

## OrderPaymentFailed

Событие отправляется из Payments Service в Orders Service.

```
{
"event_id": "c3f4d85d-0003-4000-9000-333333333333",
"order_id": "550e8400-e29b-41d4-a716-446655440000",
"user_id": "user-42",
"reason": "INSUFFICIENT_BALANCE",
"occurred_at": "2026-06-10T12:00:02Z"
}
```

(Ожидаемое поведение) Orders Service должен:

- проверить дубликат события;
- найти заказ;
- перевести заказ в статус PAYMENT_FAILED;
- сохранить failure_reason.
