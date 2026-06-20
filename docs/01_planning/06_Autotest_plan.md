# План автотестов

Автотесты будут вынесены в отдельный публичный репозиторий.

Пример названия репозитория: `orbitamarket-api-tests`

| Эндпоинт                         | Тест                                                |
| :------------------------------- | :-------------------------------------------------- |
| `POST /payments/accounts`        | Успешное создание счета                             |
| `POST /payments/accounts`        | Повторное создание счета                            |
| `POST /payments/accounts/top-up` | Успешное пополнение                                 |
| `POST /payments/accounts/top-up` | Пополнение с amount = 0                             |
| `GET /payments/accounts/balance` | Получение баланса                                   |
| `POST /orders/orders`            | Успешное создание заказа                            |
| `POST /orders/orders`            | Ошибка INVALID_PRICE                                |
| `POST /orders/orders`            | Ошибка INVALID_PAYLOAD                              |
| `POST /orders/orders`            | Ошибка UNKNOWN_PRODUCT_TYPE                         |
| `GET /orders/orders`             | Получение списка заказов                            |
| `GET /orders/orders/{order_id}`  | Получение заказа по ID                              |
| `GET /orders/orders/{order_id}`  | Ошибка ORDER_NOT_FOUND                              |
| Полный сценарий                  | 1. Счет <br> 2. пополнение <br>3. заказ <br>4. PAID |
| Полный сценарий                  | 1. Недостаточно средств<br>2. PAYMENT_FAILED        |
| Полный сценарий                  | Два заказа по 400 при балансе 1000                  |
