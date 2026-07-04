# Архитектура программы Payments Service

```mermaid
graph TB
    subgraph "Внешние системы"
        Client[Клиент/Пользователь]
        Kafka[Apache Kafka<br/>Брокер сообщений]
    end

    subgraph "Микросервис платежей (Payments Service)"
        subgraph "Web Layer (REST API)"
            Controller[AccountController<br/>/api/v1/payments/*]
        end

        subgraph "Service Layer"
            AccountService[AccountService<br/>Управление балансом]
            PaymentProcessor[PaymentProcessor<br/>Обработка платежей]
        end

        subgraph "Repository Layer"
            AccountRepo[AccountRepository<br/>JPA + Pessimistic Lock]
            InboxRepo[InboxRepository<br/>Идемпотентность]
        end

        subgraph "Data Layer"
            DB[(PostgreSQL<br/>accounts, inbox_events)]
        end

        subgraph "Kafka Layer"
            KafkaProducer[KafkaTemplate<br/>Отправка событий]
            KafkaConsumer[KafkaListener<br/>Прием запросов]
            KafkaConfig[KafkaConfig<br/>Настройка сериализации]
        end

        subgraph "DTO Layer"
            Request[PaymentRequest<br/>OrderPaymentRequested]
            Response[AccountResponse<br/>BalanceResponse]
            Events[OrderPaymentCompleted<br/>OrderPaymentFailed]
            Error[ErrorResponse]
        end

        subgraph "Domain Model"
            Account[Account<br/>userId, balance, version]
            InboxEvent[InboxEvent<br/>eventId, orderId, status]
        end
    end

    %% Взаимодействия REST
    Client -->|HTTP POST GET<br/>X-User-Id| Controller
    Controller -->|createAccount topUp getBalance| AccountService
    Controller -->|Ошибки| Error

    %% Взаимодействия Kafka
    Kafka -->|order_payment_requests| KafkaConsumer
    KafkaConsumer -->|Прием сообщений| PaymentProcessor

    PaymentProcessor -->|Проверка дубликатов| InboxRepo
    PaymentProcessor -->|Дебит баланса| AccountService

    AccountService -->|CRUD операции| AccountRepo
    AccountService -->|CRUD операции| InboxRepo

    AccountRepo -->|SQL запросы| DB
    InboxRepo -->|SQL запросы| DB

    AccountService -->|Обновление| Account

    PaymentProcessor -->|payment_completed_events| KafkaProducer
    PaymentProcessor -->|payment_failed_events| KafkaProducer
    KafkaProducer -->|Отправка событий| Kafka

    %% Взаимодействия между компонентами
    PaymentProcessor -->|Сохранение| InboxEvent
    AccountService -->|Сохранение| Account


    class Controller api
    class AccountService,PaymentProcessor service
    class AccountRepo,InboxRepo repository
    class DB data
    class KafkaConsumer,KafkaProducer,KafkaConfig kafka
    class Request,Response,Events,Error dto
    class Account,InboxEvent model
```

Основные компоненты:

1. Web Layer (REST API) - `AccountController`
   - 3 эндпоинта: создание аккаунта, пополнение, проверка баланса
   - Аутентификация через заголовок `X-User-Id`
   - Централизованная обработка ошибок

2. Service Layer
   - `AccountService`: бизнес-логика управления балансом с оптимистичной/пессимистичной блокировкой
   - `PaymentProcessor`: обработка асинхронных платежей из Kafka с идемпотентностью

3. Repository Layer
   - `AccountRepository`: JPA + `@Lock(LockModeType.PESSIMISTIC_WRITE)` для конкурентного доступа
   - `InboxRepository`: проверка дубликатов для идемпотентной обработки

4. Kafka Integration
   - Consumer: `order_payment_requests` → дебит баланса
   - Producer: `payment_completed_events` / `payment_failed_events`

5. Схема БД
   - `accounts`: баланс + `version` для optimistic lock
   - `inbox_events`: идемпотентность (status: PENDING/PROCESSED/FAILED)

Паттерны:

- Idempotent Consumer через Inbox таблицу
- Transactional Outbox для атомарности
- Pessimistic Locking для конкурентного дебита
