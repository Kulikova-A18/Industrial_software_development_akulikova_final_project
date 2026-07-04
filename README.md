# Итоговый проект по программе

Работу выполнила: `Куликова Алёна Владимировна`.

## Содержание

1. [Документация](#документация)
1. [Задание](#задание)
1. [Общая архитектура системы orbitamarket](#общая-архитектура-системы-orbitamarket)
1. [SQL Аналитика](#sql-аналитика)
1. [Отчет по результатам Allure-тестирования](#отчет-по-результатам-allure-тестирования)
1. [Запуск проекта](#запуск-проекта)

## Документация

> Вся документация располагается по следующему пути [/docs](/docs/).

Назначение [документации о планировании проекта](/docs/01_planning/)

| Документ                                             | Назначение                                                                                                        |
| :--------------------------------------------------- | :---------------------------------------------------------------------------------------------------------------- |
| `01_Planning_OrbitaMarket_project_tasks.md`          | Поэтапный план реализации проекта OrbitaMarket с описанием задач, сроков и ожидаемых результатов каждого этапа    |
| `02_Planned_REST_requests_and_expected_responses.md` | Спецификация всех REST-эндпоинтов с примерами запросов и ожидаемых ответов, включая успешные и ошибочные сценарии |
| `03_Broker_Expected_Events.md`                       | Описание событий брокера сообщений, их структуры и маршрутов публикации и потребления между микросервисами        |
| `04_Planned_order_status.md`                         | Описание жизненного цикла заказа и всех возможных статусов с указанием переходов между ними                       |
| `05_Script_checklist.md`                             | Чек-лист сценариев для ручной и автоматизированной проверки ключевых бизнес-кейсов системы                        |
| `06_Autotest_plan.md`                                | План покрытия автотестами всех эндпоинтов и сквозных сценариев, структура тестового репозитория                   |
| `07_GUI_implementation_plan.md`                      | План дальнейшей разработки графического интерфейса для проекта                                                    |
| `08_Minimum_demonstration_plan_defense.md`           | Минимальный план демонстрации проекта на защите с командами и ожидаемыми результатами                             |

Назначение [документации о архитектуре решения](/docs/02_schemes/)

| №   | Документ                                  | Описание                         | Основные компоненты                                               | Основные технологии                               |
| --- | ----------------------------------------- | -------------------------------- | ----------------------------------------------------------------- | ------------------------------------------------- |
| 1   | `OrbitMarket_Schemes_API_Gateway.md`      | Архитектура API Gateway          | Spring Cloud Gateway, WebHandler, RouteLocator, Filters, Actuator | Spring Boot 3.2.0, Spring WebFlux, Reactor, Maven |
| 2   | `OrbitMarket_Schemes_API_Tests.md`        | Архитектура тестового фреймворка | BaseTest, PaymentsTest, OrdersTest, ScenariosTest                 | JUnit 5, RestAssured, Allure, Awaitility, Jackson |
| 3   | `OrbitMarket_Schemes_Orders_Service.md`   | Архитектура сервиса заказов      | OrderController, OrderService, OutboxWorker, Kafka                | Spring Boot, JPA, PostgreSQL, Kafka               |
| 4   | `OrbitMarket_Schemes_Payments_Service.md` | Архитектура сервиса платежей     | AccountController, AccountService, PaymentProcessor, Inbox        | Spring Boot, JPA, Pessimistic Lock, Kafka         |
| 5   | `OrbitMarket_Schemes_SQL.md`              | Архитектура SQL аналитики        | 10 SQL файлов с аналитическими запросами                          | PostgreSQL, psql, Docker                          |

## Задание

Задание для итогового проекта по программе располагается в [lms.bmstu.ru](https://lms.bmstu.ru/mod/assign/view.php?id=45972).

## Общая архитектура системы orbitamarket

```mermaid
graph TB
    Client[Внешние клиенты<br/>Web/Мобильные приложения]

    Gateway[API Gateway<br/>Port: 8080<br/>Spring Cloud Gateway]

    Orders[Orders Service<br/>Port: 8082<br/>Spring Boot 3.2.0]
    Payments[Payments Service<br/>Port: 8081<br/>Spring Boot 3.2.0]

    Kafka[Apache Kafka<br/>Port: 9092]

    OrdersDB[(Orders DB<br/>PostgreSQL<br/>orders, outbox_events)]
    PaymentsDB[(Payments DB<br/>PostgreSQL<br/>accounts, inbox_events)]

    Client -->|HTTP Requests| Gateway
    Gateway -->|/api/v1/orders/**| Orders
    Gateway -->|/api/v1/payments/**| Payments

    Orders -->|CRUD| OrdersDB
    Payments -->|CRUD| PaymentsDB

    Orders -.->|Producer: order_payment_requests| Kafka
    Kafka -.->|Consumer: order_payment_requests| Payments

    Payments -.->|Producer: payment_completed_events| Kafka
    Payments -.->|Producer: payment_failed_events| Kafka

    class Gateway gateway
    class Orders,Payments service
    class OrdersDB,PaymentsDB db
    class Kafka kafka
    class Client client
```

Поток создания заказа

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant Orders as Orders Service
    participant OrdersDB as Orders DB
    participant Kafka as Apache Kafka
    participant Payments as Payments Service
    participant PaymentsDB as Payments DB

    Client->>Gateway: POST /api/v1/orders<br/>Headers: X-User-Id<br/>Body: {product_type, price, payload}
    Gateway->>Orders: Прокси с X-User-Id


      Note over Orders,OrdersDB: СИНХРОННАЯ ЧАСТЬ
      Orders->>Orders: Валидация X-User-Id, product_type, payload
      Orders->>OrdersDB: Сохранение Order (CREATED)
      Orders->>OrdersDB: Сохранение OutboxEvent (PENDING)
      Orders->>OrdersDB: Обновление Order -> PAYMENT_PENDING
      Orders-->>Gateway: OrderResponse
      Gateway-->>Client: 200 OK



      Note over Orders,Kafka: АСИНХРОННАЯ ЧАСТЬ (Outbox Worker)
      loop Каждые 5 секунд
          Orders->>OrdersDB: SELECT PENDING outbox_events
          Orders->>Kafka: send order_payment_requests
          Orders->>OrdersDB: UPDATE -> SENT/FAILED
      end


      Note over Payments,PaymentsDB: ОБРАБОТКА ПЛАТЕЖА
      Kafka->>Payments: consume order_payment_requests

      alt Проверка идемпотентности
          Payments->>PaymentsDB: existsByOrderId?
          Payments->>PaymentsDB: Save InboxEvent (PENDING)
      end

      Payments->>PaymentsDB: SELECT accounts FOR UPDATE (Pessimistic Lock)

      alt Баланс достаточен
          Payments->>PaymentsDB: UPDATE balance
          Payments->>PaymentsDB: UPDATE InboxEvent -> PROCESSED
          Payments->>Kafka: send payment_completed_events
      else Баланс недостаточен
          Payments->>PaymentsDB: UPDATE InboxEvent -> FAILED
          Payments->>Kafka: send payment_failed_events
      end

```

Все подробности можно прочитать в следующих документах:

- [Архитектура программы API Gateway](/docs/02_schemes/OrbitMarket_Schemes_API_Gateway.md)
- [Архитектура программы Orders Service](/docs/02_schemes/OrbitMarket_Schemes_Orders_Service.md)
- [Архитектура программы Payments Service](/docs/02_schemes/OrbitMarket_Schemes_Payments_Service.md)

## SQL Аналитика

Отчет по выполнению SQL аналитики OrbitaMarket располагается по следующему пути [/docs/03_reports/OrbitaMarket_SQL_Analytics_Report.md](/docs/03_reports/OrbitaMarket_SQL_Analytics_Report.md).

## Отчет по результатам Allure-тестирования

Отчет по результатам Allure-тестирования располагается по следующему пути [/docs/03_reports/OrbitaMarket_Allure_Report.md](/docs/03_reports/OrbitaMarket_Allure_Report.md).

## Запуск проекта

### Быстрый старт

```
sudo ./run.sh
```

### Ручное управление контейнерами

```
# Остановить все контейнеры и удалить тома
sudo docker-compose down -v

# Запустить все сервисы в фоновом режиме
sudo docker-compose up -d

# Просмотр статуса контейнеров
sudo docker-compose ps

# Просмотр логов всех сервисов
sudo docker-compose logs --tail=50

# Просмотр логов конкретного сервиса
sudo docker-compose logs zookeeper
sudo docker-compose logs kafka
sudo docker-compose logs orders
sudo docker-compose logs payments
sudo docker-compose logs gateway
```

### Тестирование API

Установка утилит (если не установлены)

```
sudo apt-get install jq -y
```

Проверка работоспособности

```
# Проверка API Gateway
curl -s http://localhost:8080/actuator/health | jq .

# Создание аккаунта
curl -X POST http://localhost:8080/api/v1/payments/accounts \
  -H "X-User-Id: test-user" \
  -H "Content-Type: application/json" | jq .

# Пополнение баланса
curl -X POST http://localhost:8080/api/v1/payments/accounts/top-up \
  -H "X-User-Id: test-user" \
  -H "Content-Type: application/json" \
  -d '{"amount": 1000}' | jq .

# Проверка баланса
curl -s http://localhost:8080/api/v1/payments/accounts/balance \
  -H "X-User-Id: test-user" | jq .

# Создание заказа
curl -X POST http://localhost:8080/api/v1/orders \
  -H "X-User-Id: test-user" \
  -H "Content-Type: application/json" \
  -d '{
    "product_type": "ARCHIVE",
    "price": 100,
    "payload": {
      "aoi": "test-area",
      "capture_date": "2024-01-01",
      "sensor_type": "optical"
    }
  }' | jq .

# Просмотр заказов
curl -s http://localhost:8080/api/v1/orders \
  -H "X-User-Id: test-user" | jq .
```
