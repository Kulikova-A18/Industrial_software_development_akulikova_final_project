
## SQL Аналитика

В проекте подготовлены SQL-запросы для аналитики по заказам и платежам. Все запросы находятся в директорииdocs/sql/`.

### Структура SQL файлов

| Файл | Описание |
|------|----------|
|01_basic_analytics.sql| Базовая аналитика по пользователям |
|02_platform_stats.sql| Общая статистика платформы |
|03_user_ranking.sql| Рейтинг пользователей |
|04_time_analytics.sql| Временная аналитика |
|05_failure_analysis.sql| Анализ ошибок и отказов |
|06_advanced_analytics.sql| Продвинутая аналитика (когорты, кросс-продажи) |
|07_data_quality.sql| Проверка целостности данных |
|08_payments_analytics.sql| Аналитика платежей (Payments DB) |
|09_combined_analytics.sql| Объединенная аналитика (Orders + Payments) |
|10_presentation_stats.sql| Статистика для презентации |

### Запуск SQL запросов в Docker

#### 1. Подключение к базе данных Orders

Подключиться к Orders DB

```bash
sudo docker exec -it orders-db psql -U ordersuser -d ordersdb
```

Выполнить конкретный SQL файл

```bash
sudo docker exec -i orders-db psql -U ordersuser -d ordersdb < docs/sql/01_basic_analytics.sql
```

Выполнить все SQL файлы

```bash
for f in docs/sql/*.sql; do
    echo "Executing: $f"
    sudo docker exec -i orders-db psql -U ordersuser -d ordersdb < "$f"
done
```

#### 2. Подключение к базе данных Payments

Подключиться к Payments DB

```bash
sudo docker exec -it payments-db psql -U paymentsuser -d paymentsdb
```

Выполнить SQL файл для Payments

```bash
sudo docker exec -i payments-db psql -U paymentsuser -d paymentsdb < docs/sql/08_payments_analytics.sql
```

#### 3. Выполнение конкретного запроса (пример)

Выполнить запрос 01_basic_analytics.sql напрямую

```bash 
sudo docker exec -i orders-db psql -U ordersuser -d ordersdb -c "
SELECT 
    user_id,
    COUNT(*) AS paid_orders_count,
    SUM(price) AS total_spent_geocredits
FROM orders
WHERE status = 'PAID'
GROUP BY user_id
ORDER BY total_spent_geocredits DESC;"
```

