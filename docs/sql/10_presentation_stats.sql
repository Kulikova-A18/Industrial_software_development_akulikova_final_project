-- 10. Статистика для презентации

-- Вывод для презентации (краткая статистика)
SELECT 
    'Total Users' AS metric,
    COUNT(DISTINCT user_id)::TEXT AS value
FROM orders
UNION ALL
SELECT 
    'Total Orders',
    COUNT(*)::TEXT
FROM orders
UNION ALL
SELECT 
    'Total Revenue (geocredits)',
    COALESCE(SUM(CASE WHEN status = 'PAID' THEN price END)::TEXT, '0')
FROM orders
UNION ALL
SELECT 
    'Successful Payments',
    COUNT(CASE WHEN status = 'PAID' THEN 1 END)::TEXT || '/' || COUNT(*)::TEXT || ' (' || 
    ROUND(COUNT(CASE WHEN status = 'PAID' THEN 1 END) * 100.0 / COUNT(*), 1)::TEXT || '%)'
FROM orders
UNION ALL
SELECT 
    'Average Order Value',
    ROUND(COALESCE(AVG(CASE WHEN status = 'PAID' THEN price END), 0)::NUMERIC, 2)::TEXT
FROM orders
UNION ALL
SELECT 
    'Top Product Type',
    (SELECT product_type FROM orders WHERE status = 'PAID' GROUP BY product_type ORDER BY COUNT(*) DESC LIMIT 1)::TEXT;