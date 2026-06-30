-- 02. Общая статистика платформы

-- Общая статистика платформы
SELECT 
    COUNT(DISTINCT user_id) AS total_users,
    COUNT(*) AS total_orders,
    SUM(CASE WHEN status = 'PAID' THEN 1 ELSE 0 END) AS paid_orders,
    SUM(CASE WHEN status = 'PAID' THEN price ELSE 0 END) AS total_revenue_geocredits,
    AVG(CASE WHEN status = 'PAID' THEN price END) AS avg_paid_order_amount,
    SUM(CASE WHEN status = 'PAYMENT_FAILED' THEN 1 ELSE 0 END) AS failed_orders,
    SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected_orders,
    SUM(CASE WHEN status = 'PAYMENT_PENDING' THEN 1 ELSE 0 END) AS pending_orders,
    ROUND(AVG(CASE WHEN status = 'PAID' THEN price END), 2) AS avg_order_value
FROM orders;

-- Статистика по типам продуктов
SELECT 
    product_type,
    COUNT(*) AS order_count,
    SUM(price) AS total_revenue,
    AVG(price) AS avg_price,
    MIN(price) AS min_price,
    MAX(price) AS max_price,
    COUNT(DISTINCT user_id) AS unique_users,
    SUM(CASE WHEN status = 'PAID' THEN 1 ELSE 0 END) AS paid_count,
    SUM(CASE WHEN status = 'PAYMENT_FAILED' THEN 1 ELSE 0 END) AS failed_count,
    SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected_count
FROM orders
WHERE status IN ('PAID', 'PAYMENT_FAILED', 'REJECTED')
GROUP BY product_type
ORDER BY total_revenue DESC;

-- Распределение статусов заказов
SELECT 
    status,
    COUNT(*) AS order_count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM orders), 2) AS percentage,
    SUM(price) AS total_amount,
    AVG(price) AS avg_amount
FROM orders
GROUP BY status
ORDER BY order_count DESC;