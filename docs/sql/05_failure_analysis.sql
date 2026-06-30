-- 05. Анализ ошибок и отказов

-- Причины отказа в оплате
SELECT 
    failure_reason,
    COUNT(*) AS count,
    COUNT(DISTINCT user_id) AS affected_users,
    SUM(price) AS total_amount,
    AVG(price) AS avg_amount,
    MIN(created_at) AS first_occurrence,
    MAX(created_at) AS last_occurrence,
    COUNT(CASE WHEN status = 'PAYMENT_FAILED' THEN 1 END) AS payment_failed_count,
    COUNT(CASE WHEN status = 'REJECTED' THEN 1 END) AS rejected_count
FROM orders
WHERE failure_reason IS NOT NULL
GROUP BY failure_reason
ORDER BY count DESC;

-- Заказы с ошибками
SELECT 
    id,
    user_id,
    product_type,
    status,
    failure_reason,
    price,
    created_at,
    updated_at
FROM orders
WHERE status IN ('PAYMENT_FAILED', 'REJECTED')
ORDER BY created_at DESC
LIMIT 20;