-- 04. Временная аналитика

-- Ежедневная статистика (последние 30 дней)
SELECT 
    DATE(created_at) AS order_date,
    COUNT(*) AS total_orders,
    SUM(price) AS total_amount,
    COUNT(DISTINCT user_id) AS unique_users,
    SUM(CASE WHEN status = 'PAID' THEN 1 ELSE 0 END) AS paid_count,
    SUM(CASE WHEN status = 'PAID' THEN price ELSE 0 END) AS revenue,
    SUM(CASE WHEN status = 'PAYMENT_FAILED' THEN 1 ELSE 0 END) AS failed_count,
    SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected_count,
    AVG(price) AS avg_order_amount
FROM orders
WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE(created_at)
ORDER BY order_date DESC;

-- Ежемесячная статистика
SELECT 
    TO_CHAR(DATE_TRUNC('month', created_at), 'YYYY-MM') AS month,
    COUNT(*) AS total_orders,
    SUM(price) AS total_amount,
    COUNT(DISTINCT user_id) AS unique_users,
    SUM(CASE WHEN status = 'PAID' THEN 1 ELSE 0 END) AS paid_count,
    SUM(CASE WHEN status = 'PAID' THEN price ELSE 0 END) AS revenue,
    SUM(CASE WHEN status = 'PAYMENT_FAILED' THEN 1 ELSE 0 END) AS failed_count,
    AVG(price) AS avg_order_amount
FROM orders
GROUP BY DATE_TRUNC('month', created_at)
ORDER BY month DESC;

-- Время обработки заказов (от CREATED до PAID)
SELECT 
    COUNT(*) AS processed_orders,
    AVG(EXTRACT(EPOCH FROM (updated_at - created_at))) AS avg_seconds,
    MIN(EXTRACT(EPOCH FROM (updated_at - created_at))) AS min_seconds,
    MAX(EXTRACT(EPOCH FROM (updated_at - created_at))) AS max_seconds,
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (updated_at - created_at))) AS median_seconds,
    AVG(price) AS avg_order_price,
    SUM(price) AS total_value
FROM orders
WHERE status = 'PAID' 
    AND created_at IS NOT NULL 
    AND updated_at IS NOT NULL
    AND EXTRACT(EPOCH FROM (updated_at - created_at)) > 0;