# Архитектура программы Orders Service

## Содержание

1. [Архитектура системы](#архитектура-системы)
1. [Схема модели данных](#схема-модели-данных)
1. [Схема потока создания заказа](#схема-потока-создания-заказа)
1. [Схема статусов заказа](#схема-статусов-заказа)
1. [Поток команд между компонентами](#поток-команд-между-компонентами)

## Архитектура системы

```mermaid
flowchart TB
    subgraph External["Внешний мир"]
        Client["Клиентское приложение"]
        KafkaBroker["Kafka Broker\nTopic: order_payment_requests"]
        PaymentService["Payment Service\nВнешний сервис"]
    end

    subgraph Presentation["Презентационный слой"]
        Controller["OrderController"]
        DTO1["OrderRequest"]
        DTO2["OrderResponse"]
        DTO3["OrderPaymentRequested"]
        ErrorHandler["ErrorHandler"]
    end

    subgraph Business["Бизнес-слой"]
        Service["OrderService"]
        Validator["Payload Validator"]
        Mapper["Response Mapper"]
        ValidationRules["Validation Rules"]
    end

    subgraph Data["Слой данных"]
        OrderRepo["OrderRepository"]
        OutboxRepo["OutboxRepository"]
        OrderEntity["Order Entity"]
        OutboxEntity["OutboxEvent Entity"]
        Enums["Enums"]
        DB[("PostgreSQL")]
    end

    subgraph Integration["Интеграционный слой"]
        KafkaConfig["KafkaConfig"]
        OutboxWorker["OutboxWorker"]
        KafkaTemplateBean["KafkaTemplate"]
        ProducerConfig["Producer Config"]
        ConsumerConfig["Consumer Config"]
    end

    subgraph Testing["Тестирование"]
        UnitTests["Unit Tests"]
        IntegrationTests["Integration Tests"]
        TestTools["Test Tools"]
    end

    Client -->|"HTTP"| Controller
    Controller -->|"uses"| DTO1
    Controller -->|"uses"| DTO2
    Controller -->|"uses"| DTO3
    Controller -->|"uses"| ErrorHandler
    Controller -->|"calls"| Service

    Service -->|"uses"| Validator
    Service -->|"uses"| Mapper
    Service -->|"uses"| ValidationRules
    Service -->|"save"| OrderRepo
    Service -->|"save"| OutboxRepo

    OrderRepo -->|"works with"| OrderEntity
    OutboxRepo -->|"works with"| OutboxEntity
    OrderEntity -->|"uses"| Enums
    OutboxEntity -->|"uses"| Enums

    OrderRepo -->|"CRUD"| DB
    OutboxRepo -->|"CRUD"| DB

    OutboxRepo -->|"read PENDING"| OutboxWorker
    OutboxWorker -->|"uses"| KafkaTemplateBean
    KafkaTemplateBean -->|"configured by"| KafkaConfig
    KafkaConfig -->|"includes"| ProducerConfig
    KafkaConfig -->|"includes"| ConsumerConfig

    KafkaTemplateBean -->|"send"| KafkaBroker
    KafkaBroker -->|"consume"| PaymentService

    UnitTests -->|"test"| Service
    UnitTests -->|"test"| OutboxWorker
    IntegrationTests -->|"test"| Controller
    IntegrationTests -->|"use"| TestTools
```

## Схема модели данных

```mermaid
erDiagram
    Order {
        uuid id PK
        string user_id
        product_type product_type
        int price
        text payload
        order_status status
        string failure_reason
        timestamp created_at
        timestamp updated_at
    }

    OutboxEvent {
        uuid id PK
        uuid order_id FK
        string user_id
        int amount
        string event_type
        text event_data
        timestamp created_at
        outbox_status status
        timestamp processed_at
    }

    ProductType {
        string value
    }

    OrderStatus {
        string value
    }

    OutboxStatus {
        string value
    }

    Order ||--|| ProductType : "has"
    Order ||--|| OrderStatus : "has"
    Order ||--o{ OutboxEvent : "generates"
    OutboxEvent ||--|| OutboxStatus : "has"
```

### Таблица orders

Хранит информацию о заказах пользователей

| Поле           | Тип       | Описание                          | Пример                                                             |
| -------------- | --------- | --------------------------------- | ------------------------------------------------------------------ |
| id             | UUID      | Уникальный номер заказа           | `550e8400-e29b-41d4-a716-446655440000`                             |
| user_id        | String    | ID пользователя, кто создал заказ | `user-12345`                                                       |
| product_type   | Enum      | Тип продукта                      | `ARCHIVE`, `TASKING`, `MONITORING`                                 |
| price          | Integer   | Цена заказа                       | `1000`                                                             |
| payload        | TEXT      | Данные заказа (JSON)              | `{"aoi":"area1","capture_date":"2024-01-01"}`                      |
| status         | Enum      | Статус заказа                     | `CREATED`, `PAYMENT_PENDING`, `PAID`, `PAYMENT_FAILED`, `REJECTED` |
| failure_reason | String    | Причина ошибки (если есть)        | `Недостаточно средств`                                             |
| created_at     | Timestamp | Дата создания                     | `2024-01-01 10:00:00`                                              |
| updated_at     | Timestamp | Дата обновления                   | `2024-01-01 10:05:00`                                              |

### Таблица outbox_events

Хранит события, которые нужно отправить в Kafka

| Поле         | Тип       | Описание                     | Пример                                                   |
| ------------ | --------- | ---------------------------- | -------------------------------------------------------- |
| id           | UUID      | Уникальный номер события     | `550e8400-e29b-41d4-a716-446655440001`                   |
| order_id     | UUID      | ID заказа (ссылка на orders) | `550e8400-e29b-41d4-a716-446655440000`                   |
| user_id      | String    | ID пользователя              | `user-12345`                                             |
| amount       | Integer   | Сумма платежа                | `1000`                                                   |
| event_type   | String    | Тип события                  | `OrderPaymentRequested`                                  |
| event_data   | TEXT      | Данные события (JSON)        | `{"order_id":"...","amount":1000}`                       |
| created_at   | Timestamp | Дата создания                | `2024-01-01 10:00:00`                                    |
| status       | Enum      | Статус отправки              | `PENDING` (ждет), `SENT` (отправлено), `FAILED` (ошибка) |
| processed_at | Timestamp | Дата обработки               | `2024-01-01 10:00:05`                                    |

### ENUM

ProductType

1. ARCHIVE - Архивные данные (спутниковые снимки прошлых лет)
1. TASKING - Заказ на съемку (новые снимки)
1. MONITORING - Мониторинг (регулярные наблюдения)

OrderStatus

1. CREATED - Заказ создан
1. PAYMENT_PENDING - Ожидание оплаты
1. PAID - Оплачен
1. PAYMENT_FAILED - Оплата не прошла
1. REJECTED - Отклонен

OutboxStatus

1. PENDING - Ожидает отправки
1. SENT - Отправлено в Kafka
1. FAILED - Ошибка отправки

## Схема потока создания заказа

```mermaid
flowchart TD
    Start([POST /api/v1/orders]) --> ValidateHeaders{Validate\nX-User-Id}
    ValidateHeaders -->|Missing| Error1[Return 400\nMISSING_USER_ID]
    ValidateHeaders -->|Valid| ValidatePrice{Validate\nPrice > 0}
    ValidatePrice -->|Invalid| Error2[Return 400\nINVALID_PRICE]
    ValidatePrice -->|Valid| ValidateProductType{Validate\nProduct Type}
    ValidateProductType -->|Invalid| Error3[Return 400\nINVALID_PAYLOAD]
    ValidateProductType -->|Valid| ValidatePayload{Validate\nPayload fields}
    ValidatePayload -->|Invalid| Error4[Return 400\nINVALID_PAYLOAD]
    ValidatePayload -->|Valid| CreateOrder[Create Order\nStatus: CREATED]
    CreateOrder --> OutboxEvent[Create OutboxEvent\nStatus: PENDING]
    OutboxEvent --> UpdateStatus[Update Order\nStatus: PAYMENT_PENDING]
    UpdateStatus --> Response[Return OrderResponse]
    Response --> End([End])

    Error1 --> End
    Error2 --> End
    Error3 --> End
    Error4 --> End
```

### Описание проверки (с примером)

Клиент отправляет запрос

```
POST /api/v1/orders
Заголовок: X-User-Id: user-123
Тело запроса:
{
  "product_type": "ARCHIVE",
  "price": 1000,
  "payload": {
    "aoi": "Москва",
    "capture_date": "2024-01-01",
    "sensor_type": "optical"
  }
}
```

| №   | Проверка     | Условие                        | Результат | Код ошибки        | HTTP статус     |
| --- | ------------ | ------------------------------ | --------- | ----------------- | --------------- |
| 1   | X-User-Id    | Отсутствует                    | Ошибка    | `MISSING_USER_ID` | 400 Bad Request |
| 1   | X-User-Id    | Присутствует                   | Успех     | -                 | -               |
| 2   | Price        | ≤ 0                            | Ошибка    | `INVALID_PRICE`   | 400 Bad Request |
| 2   | Price        | > 0                            | Успех     | -                 | -               |
| 3   | Product Type | Неизвестный тип                | Ошибка    | `INVALID_PAYLOAD` | 400 Bad Request |
| 3   | Product Type | ARCHIVE / TASKING / MONITORING | Успех     | -                 | -               |
| 4   | Payload      | Не хватает полей               | Ошибка    | `INVALID_PAYLOAD` | 400 Bad Request |
| 4   | Payload      | Все поля присутствуют          | Успех     | -                 | -               |

Проверка payload по типу продукта

| Тип продукта | Обязательные поля                        | Пример payload                                                                 |
| ------------ | ---------------------------------------- | ------------------------------------------------------------------------------ |
| ARCHIVE      | `aoi`<br>`capture_date`<br>`sensor_type` | `{"aoi":"Москва","capture_date":"2024-01-01","sensor_type":"optical"}`         |
| TASKING      | `aoi`<br>`time_window`<br>`sensor_type`  | `{"aoi":"Москва","time_window":"2024-01-01/2024-01-07","sensor_type":"radar"}` |
| MONITORING   | `aoi`<br>`cadence`<br>`duration_days`    | `{"aoi":"Москва","cadence":"DAILY","duration_days":30}`                        |

Ошибки валидации

| Код ошибки        | Сообщение                                                             | Причина                         | Решение                                       |
| ----------------- | --------------------------------------------------------------------- | ------------------------------- | --------------------------------------------- |
| `MISSING_USER_ID` | "X-User-Id header is required"                                        | Отсутствует заголовок X-User-Id | Добавить заголовок X-User-Id в запрос         |
| `INVALID_PRICE`   | "Price must be greater than zero"                                     | Цена меньше или равна 0         | Указать цену > 0                              |
| `INVALID_PAYLOAD` | "product_type is required"                                            | Пустой product_type             | Указать тип продукта                          |
| `INVALID_PAYLOAD` | "payload is required"                                                 | Отсутствует payload             | Добавить payload с данными                    |
| `INVALID_PAYLOAD` | "UNKNOWN_PRODUCT_TYPE"                                                | Неизвестный тип продукта        | Использовать: ARCHIVE, TASKING или MONITORING |
| `INVALID_PAYLOAD` | "Missing required fields for ARCHIVE: aoi, capture_date, sensor_type" | Не хватает полей для ARCHIVE    | Добавить все обязательные поля                |
| `INVALID_PAYLOAD` | "Missing required fields for TASKING: aoi, time_window, sensor_type"  | Не хватает полей для TASKING    | Добавить все обязательные поля                |
| `INVALID_PAYLOAD` | "Missing required fields for MONITORING: aoi, cadence, duration_days" | Не хватает полей для MONITORING | Добавить все обязательные поля                |
| `INTERNAL_ERROR`  | "Failed to create order: ..."                                         | Внутренняя ошибка сервера       | Проверить логи, обратиться к администратору   |

Статусы заказа после валидации

| Статус          | Описание        | Когда присваивается                              |
| --------------- | --------------- | ------------------------------------------------ |
| CREATED         | Заказ создан    | После успешной валидации, перед сохранением в БД |
| PAYMENT_PENDING | Ожидание оплаты | После сохранения заказа и Outbox события         |

## Схема статусов заказа

```mermaid
stateDiagram-v2
    [*] --> CREATED: Create order
    CREATED --> PAYMENT_PENDING: After outbox event
    PAYMENT_PENDING --> PAID: Payment success
    PAYMENT_PENDING --> PAYMENT_FAILED: Payment failed
    PAYMENT_PENDING --> REJECTED: Validation/other error

    PAID --> [*]
    PAYMENT_FAILED --> [*]
    REJECTED --> [*]
```

| Состояние       | Описание        | Когда наступает                                   | Действия пользователя                         |
| --------------- | --------------- | ------------------------------------------------- | --------------------------------------------- |
| CREATED         | Заказ создан    | После успешной валидации и сохранения в БД        | Ожидание обработки                            |
| PAYMENT_PENDING | Ожидание оплаты | После создания Outbox события                     | Ожидание ответа от платежной системы          |
| PAID            | Оплачен         | После успешной оплаты                             | Заказ выполнен, можно получить услугу         |
| PAYMENT_FAILED  | Ошибка оплаты   | Если платеж не прошел                             | Требуется повторить оплату или отменить заказ |
| REJECTED        | Отклонен        | При ошибке валидации или другой внутренней ошибке | Заказ отклонен, требуется создание нового     |

## Поток команд между компонентами

```mermaid
sequenceDiagram
    participant C as Client
    participant OC as OrderController
    participant OS as OrderService
    participant OR as OrderRepository
    participant OER as OutboxRepository
    participant OW as OutboxWorker
    participant K as KafkaTemplate
    participant PS as PaymentService

    C->>OC: POST /orders (X-User-Id, payload)
    OC->>OC: Validate headers & payload
    OC->>OS: createOrder(userId, productType, price, payload)
    OS->>OS: Validate product type
    OS->>OS: Validate payload structure
    OS->>OS: Generate UUID for order
    OS->>OR: save(Order(status=CREATED))
    OR-->>OS: Order saved
    OS->>OS: Create OutboxEvent(PENDING)
    OS->>OER: save(OutboxEvent)
    OER-->>OS: OutboxEvent saved
    OS->>OR: updateOrder(status=PAYMENT_PENDING)
    OR-->>OS: Order updated
    OS-->>OC: OrderResponse
    OC-->>C: 200 OK

    Note over OW: Every 5 seconds
    OW->>OER: findPendingEvents()
    OER-->>OW: List<OutboxEvent>
    loop For each event
        OW->>K: send(order_payment_requests, eventData)
        alt Success
            K-->>OW: Success
            OW->>OER: updateStatus(SENT)
        else Failure
            K-->>OW: Error
            OW->>OER: updateStatus(FAILED)
        end
    end

    K->>PS: Consumer receives event
    PS->>PS: Process payment
    alt Payment Success
        PS->>K: send(order_payment_completed)
        Note over OS: @KafkaListener (not shown in code)
    else Payment Failed
        PS->>K: send(order_payment_failed)
        Note over OS: @KafkaListener (not shown in code)
    end
```

Краткая схема

```
Клиент -> Контроллер -> Сервис -> БД -> OutboxWorker -> Kafka -> PaymentService
```

Получение запроса

| Шаг | Кто        | Что делает                               |
| --- | ---------- | ---------------------------------------- |
| 1   | Клиент     | Отправляет POST запрос с данными заказа  |
| 2   | Контроллер | Проверяет заголовки и данные (валидация) |
| 3   | Контроллер | Передает запрос в Сервис                 |

Создание заказа

| Шаг | Кто    | Что делает                                          |
| --- | ------ | --------------------------------------------------- |
| 4   | Сервис | Проверяет тип продукта (ARCHIVE/TASKING/MONITORING) |
| 5   | Сервис | Проверяет структуру payload                         |
| 6   | Сервис | Создает уникальный ID для заказа                    |
| 7   | Сервис | Сохраняет заказ в БД (статус: CREATED)              |
| 8   | Сервис | Создает Outbox событие (статус: PENDING)            |
| 9   | Сервис | Сохраняет Outbox событие в БД                       |
| 10  | Сервис | Обновляет статус заказа (PAYMENT_PENDING)           |
| 11  | Сервис | Возвращает ответ клиенту                            |

Фоновая отправка

| Шаг | Кто          | Что делает                             |
| --- | ------------ | -------------------------------------- |
| 12  | OutboxWorker | Запускается каждые 5 секунд            |
| 13  | OutboxWorker | Ищет все события со статусом PENDING   |
| 14  | OutboxWorker | Для каждого события отправляет в Kafka |
| 15a | Kafka        | Успех -> статус меняется на SENT       |
| 15b | Kafka        | Ошибка -> статус меняется на FAILED    |

Обработка платежа

| Шаг | Кто            | Что делает                                    |
| --- | -------------- | --------------------------------------------- |
| 16  | PaymentService | Получает событие из Kafka                     |
| 17  | PaymentService | Обрабатывает платеж                           |
| 18a | PaymentService | Успех -> отправляет `order_payment_completed` |
| 18b | PaymentService | Ошибка -> отправляет `order_payment_failed`   |
