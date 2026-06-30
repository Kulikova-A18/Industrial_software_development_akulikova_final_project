-- 08. Аналитика платежей (Payments DB)

-- Статистика по счетам
SELECT 
    COUNT(*) AS total_accounts,
    SUM(balance) AS total_balance_geocredits,
    AVG(balance) AS avg_balance,
    MAX(balance) AS max_balance,
    MIN(balance) AS min_balance,
    COUNT(CASE WHEN balance = 0 THEN 1 END) AS zero_balance_accounts,
    COUNT(CASE WHEN balance > 0 THEN 1 END) AS active_accounts
FROM accounts;

-- Пользователи с балансом
SELECT 
    user_id,
    balance,
    created_at,
    updated_at,
    CASE 
        WHEN balance = 0 THEN 'Empty'
        WHEN balance < 100 THEN 'Low'
        WHEN balance < 500 THEN 'Medium'
        WHEN balance < 1000 THEN 'High'
        ELSE 'Premium'
    END AS balance_level
FROM accounts
ORDER BY balance DESC
LIMIT 20;