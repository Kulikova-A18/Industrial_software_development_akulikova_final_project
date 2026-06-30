-- 01. Базовая аналитика по пользователям

-- Кто и сколько купил
SELECT 
    user_id,
    COUNT(*) AS paid_orders_count,
    SUM(price) AS total_spent_geocredits,
    AVG(price) AS avg_order_amount,
    MIN(price) AS min_order_amount,
    MAX(price) AS max_order_amount
FROM orders
WHERE status = 'PAID'
GROUP BY user_id
ORDER BY total_spent_geocredits DESC;

-- Детальная статистика по пользователю
SELECT 
    user_id,
    COUNT(*) AS total_orders,
    SUM(CASE WHEN status = 'PAID' THEN price ELSE 0 END) AS total_paid,
    SUM(CASE WHEN status = 'PAYMENT_FAILED' THEN price ELSE 0 END) AS total_failed,
    SUM(CASE WHEN status = 'REJECTED' THEN price ELSE 0 END) AS total_rejected,
    SUM(CASE WHEN status = 'PAYMENT_PENDING' THEN price ELSE 0 END) AS total_pending,
    COUNT(CASE WHEN status = 'PAID' THEN 1 END) AS paid_count,
    COUNT(CASE WHEN status = 'PAYMENT_FAILED' THEN 1 END) AS failed_count,
    COUNT(CASE WHEN status = 'REJECTED' THEN 1 END) AS rejected_count,
    COUNT(CASE WHEN status = 'PAYMENT_PENDING' THEN 1 END) AS pending_count,
    MIN(created_at) AS first_order_date,
    MAX(created_at) AS last_order_date
FROM orders
GROUP BY user_id
ORDER BY total_paid DESC;
