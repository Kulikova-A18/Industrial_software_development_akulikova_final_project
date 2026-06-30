-- 03. Рейтинг пользователей

-- Топ-10 пользователей по тратам
SELECT 
    user_id,
    COUNT(*) AS orders_count,
    SUM(price) AS total_spent,
    AVG(price) AS avg_order,
    MAX(created_at) AS last_order_date,
    COUNT(DISTINCT product_type) AS product_types_used
FROM orders
WHERE status = 'PAID'
GROUP BY user_id
ORDER BY total_spent DESC
LIMIT 10;

-- Пользователи с наибольшим количеством заказов
SELECT 
    user_id,
    COUNT(*) AS total_orders,
    SUM(price) AS total_spent,
    AVG(price) AS avg_order_value,
    COUNT(CASE WHEN status = 'PAID' THEN 1 END) AS paid_orders,
    COUNT(CASE WHEN status = 'PAYMENT_FAILED' THEN 1 END) AS failed_orders,
    COUNT(CASE WHEN status = 'REJECTED' THEN 1 END) AS rejected_orders,
    MAX(created_at) AS last_activity
FROM orders
GROUP BY user_id
HAVING COUNT(*) > 0
ORDER BY total_orders DESC
LIMIT 10;