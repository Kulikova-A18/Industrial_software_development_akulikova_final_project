-- 06. Продвинутая аналитика

-- Анализ повторных покупок
WITH user_orders AS (
    SELECT 
        user_id,
        COUNT(*) AS order_count,
        COUNT(DISTINCT product_type) AS distinct_products
    FROM orders
    WHERE status = 'PAID'
    GROUP BY user_id
)
SELECT 
    CASE 
        WHEN order_count = 1 THEN 'One-time buyers'
        WHEN order_count BETWEEN 2 AND 3 THEN 'Occasional buyers'
        WHEN order_count BETWEEN 4 AND 6 THEN 'Regular buyers'
        ELSE 'Loyal buyers'
    END AS buyer_type,
    COUNT(*) AS user_count,
    AVG(order_count) AS avg_orders_per_user,
    AVG(distinct_products) AS avg_product_types
FROM user_orders
GROUP BY 
    CASE 
        WHEN order_count = 1 THEN 'One-time buyers'
        WHEN order_count BETWEEN 2 AND 3 THEN 'Occasional buyers'
        WHEN order_count BETWEEN 4 AND 6 THEN 'Regular buyers'
        ELSE 'Loyal buyers'
    END
ORDER BY MIN(order_count);

-- Кросс-продажи (какие продукты покупают вместе)
SELECT 
    a.product_type AS product1,
    b.product_type AS product2,
    COUNT(*) AS times_bought_together,
    COUNT(DISTINCT a.user_id) AS unique_users
FROM orders a
JOIN orders b ON a.user_id = b.user_id 
    AND a.product_type < b.product_type
    AND a.status = 'PAID'
    AND b.status = 'PAID'
GROUP BY a.product_type, b.product_type
ORDER BY times_bought_together DESC;

-- Когортный анализ (активность пользователей по месяцам)
WITH user_first_order AS (
    SELECT 
        user_id,
        DATE_TRUNC('month', MIN(created_at)) AS cohort_month
    FROM orders
    WHERE status = 'PAID'
    GROUP BY user_id
),
user_activity AS (
    SELECT 
        u.user_id,
        u.cohort_month,
        DATE_TRUNC('month', o.created_at) AS activity_month,
        COUNT(*) AS orders_in_month,
        SUM(o.price) AS revenue_in_month
    FROM user_first_order u
    JOIN orders o ON u.user_id = o.user_id
    WHERE o.status = 'PAID'
    GROUP BY u.user_id, u.cohort_month, DATE_TRUNC('month', o.created_at)
)
SELECT 
    TO_CHAR(cohort_month, 'YYYY-MM') AS cohort,
    EXTRACT(MONTH FROM activity_month) - EXTRACT(MONTH FROM cohort_month) + 
    (EXTRACT(YEAR FROM activity_month) - EXTRACT(YEAR FROM cohort_month)) * 12 AS month_number,
    COUNT(DISTINCT user_id) AS active_users,
    SUM(orders_in_month) AS total_orders,
    SUM(revenue_in_month) AS total_revenue
FROM user_activity
GROUP BY cohort_month, activity_month
ORDER BY cohort_month, month_number;