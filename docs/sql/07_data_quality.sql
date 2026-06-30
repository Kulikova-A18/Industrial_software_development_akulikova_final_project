-- 07. Проверка целостности данных

-- Проверка целостности данных
SELECT 
    'orders_with_negative_price' AS check_name,
    COUNT(*) AS issue_count
FROM orders
WHERE price < 0
UNION ALL
SELECT 
    'orders_without_user_id',
    COUNT(*)
FROM orders
WHERE user_id IS NULL OR user_id = ''
UNION ALL
SELECT 
    'orders_with_empty_payload',
    COUNT(*)
FROM orders
WHERE payload IS NULL OR payload = ''
UNION ALL
SELECT 
    'orders_with_null_created_at',
    COUNT(*)
FROM orders
WHERE created_at IS NULL
UNION ALL
SELECT 
    'orders_with_duplicate_id',
    COUNT(*) - COUNT(DISTINCT id)
FROM orders;