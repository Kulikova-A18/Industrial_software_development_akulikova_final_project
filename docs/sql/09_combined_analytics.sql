-- 09. Объединенная аналитика (Orders + Payments)

-- Объединенная аналитика
SELECT 
    o.user_id,
    o.id AS order_id,
    o.price AS order_price,
    o.status AS order_status,
    o.failure_reason,
    o.created_at AS order_created_at,
    a.balance AS current_balance,
    a.created_at AS account_created_at,
    (SELECT COALESCE(SUM(price), 0) FROM orders o2 WHERE o2.user_id = o.user_id AND o2.status = 'PAID') AS total_paid_by_user
FROM orders o
LEFT JOIN accounts a ON o.user_id = a.user_id
WHERE o.created_at >= CURRENT_DATE - INTERVAL '7 days'
ORDER BY o.created_at DESC
LIMIT 20;