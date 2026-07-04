# Архитектура программы SQL

```mermaid
graph TB
    subgraph "Источники данных"
        OrdersDB[(Orders Database<br/>PostgreSQL<br/>orders, users)]
        PaymentsDB[(Payments Database<br/>PostgreSQL<br/>accounts)]
    end

    subgraph "SQL Аналитика"
        subgraph "Базовые запросы"
            Basic[01_basic_analytics.sql<br/>Базовая аналитика пользователей]
            Platform[02_platform_stats.sql<br/>Общая статистика платформы]
            Ranking[03_user_ranking.sql<br/>Рейтинг пользователей]
        end

        subgraph "Временная аналитика"
            Time[04_time_analytics.sql<br/>Дневная/месячная статистика<br/>Время обработки заказов]
        end

        subgraph "Анализ ошибок"
            Failure[05_failure_analysis.sql<br/>Причины отказов<br/>Заказы с ошибками]
        end

        subgraph "Продвинутая аналитика"
            Advanced[06_advanced_analytics.sql<br/>Когортный анализ<br/>Кросс-продажи<br/>Типы покупателей]
        end

        subgraph "Качество данных"
            Quality[07_data_quality.sql<br/>Проверка целостности]
        end

        subgraph "Платежная аналитика"
            Payments[08_payments_analytics.sql<br/>Статистика по счетам<br/>Балансовые уровни]
        end

        subgraph "Объединенная аналитика"
            Combined[09_combined_analytics.sql<br/>Orders + Payments<br/>Текущий баланс + заказы]
        end

        subgraph "Презентация"
            Presentation[10_presentation_stats.sql<br/>Краткая статистика<br/>Ключевые метрики]
        end
    end

    subgraph "Запуск"
        Docker[Docker Compose<br/>orders-db / payments-db]
        PSQL[psql клиент<br/>Интерактивный/файловый]
    end

    %% Связи данных
    OrdersDB -->|Чтение| Basic
    OrdersDB -->|Чтение| Platform
    OrdersDB -->|Чтение| Ranking
    OrdersDB -->|Чтение| Time
    OrdersDB -->|Чтение| Failure
    OrdersDB -->|Чтение| Advanced
    OrdersDB -->|Чтение| Quality
    OrdersDB -->|Чтение| Presentation

    PaymentsDB -->|Чтение| Payments
    OrdersDB -->|Чтение| Combined
    PaymentsDB -->|Чтение| Combined

    %% Запуск
    Docker -->|exec| PSQL
    PSQL -->|Выполняет SQL| OrdersDB
    PSQL -->|Выполняет SQL| PaymentsDB

    %% Группировка
    subgraph "Результаты анализа"
        R1["Общая статистика<br/>Пользователи, заказы, выручка"]
        R2["Рейтинги<br/>Топ-пользователи по тратам"]
        R3["Тренды<br/>Временные паттерны"]
        R4["Ошибки<br/>Причины отказов"]
        R5["Когорты<br/>Удержание пользователей"]
        R6["Платежи<br/>Балансы аккаунтов"]
        R7["Презентация<br/>Ключевые метрики"]
    end

    Basic -.-> R1
    Platform -.-> R1
    Ranking -.-> R2
    Time -.-> R3
    Failure -.-> R4
    Advanced -.-> R5
    Payments -.-> R6
    Presentation -.-> R7


    class OrdersDB,PaymentsDB db
    class Basic,Platform,Ranking,Time,Failure,Advanced,Quality,Payments,Combined,Presentation sql
    class R1,R2,R3,R4,R5,R6,R7 result
    class Docker,PSQL run
```

Структура запросов

| Категория             | Файлы      | Описание                                      |
| --------------------- | ---------- | --------------------------------------------- |
| **Базовая аналитика** | 01, 02, 03 | Статистика пользователей, платформы, рейтинги |
| **Временная**         | 04         | Дневная/месячная динамика, время обработки    |
| **Ошибки**            | 05         | Причины отказов, анализ неуспешных заказов    |
| **Продвинутая**       | 06         | Когорты, кросс-продажи, типы покупателей      |
| **Качество**          | 07         | Проверка целостности данных                   |
| **Платежи**           | 08         | Статистика счетов, балансовые уровни          |
| **Объединенная**      | 09         | Orders + Payments вместе                      |
| **Презентация**       | 10         | Ключевые метрики для отчетов                  |

Метрики

| Метрика              | Источник | Описание                            |
| -------------------- | -------- | ----------------------------------- |
| Total Users          | orders   | Количество уникальных пользователей |
| Total Revenue        | orders   | Сумма оплаченных заказов            |
| Success Rate         | orders   | Доля успешных платежей              |
| Avg Order Value      | orders   | Средний чек                         |
| Top Users            | orders   | Рейтинг пользователей по тратам     |
| Balance Distribution | accounts | Распределение балансов              |
