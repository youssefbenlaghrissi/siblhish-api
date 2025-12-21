-- Requête SQL simple : Agrégation des revenus et dépenses par mois
-- Pour un utilisateur spécifique et une année donnée

SELECT 
    month,
    COALESCE(SUM(total_income), 0) as total_income,
    COALESCE(SUM(total_expenses), 0) as total_expenses,
    COALESCE(SUM(total_income), 0) - COALESCE(SUM(total_expenses), 0) as balance
FROM (
    -- Revenus par mois
    SELECT 
        TO_CHAR(creation_date, 'YYYY-MM') as month,
        amount as total_income,
        0 as total_expenses
    FROM incomes
    WHERE user_id = :userId 
      AND EXTRACT(YEAR FROM creation_date) = :year
    
    UNION ALL
    
    -- Dépenses par mois
    SELECT 
        TO_CHAR(creation_date, 'YYYY-MM') as month,
        0 as total_income,
        amount as total_expenses
    FROM expenses
    WHERE user_id = :userId 
      AND EXTRACT(YEAR FROM creation_date) = :year
) combined
GROUP BY month
ORDER BY month;

-- Version encore plus simple (sans UNION) :
-- Utilise des sous-requêtes corrélées (peut être moins performant)

SELECT 
    TO_CHAR(creation_date, 'YYYY-MM') as month,
    COALESCE((
        SELECT SUM(amount) 
        FROM incomes i 
        WHERE i.user_id = :userId 
          AND TO_CHAR(i.creation_date, 'YYYY-MM') = TO_CHAR(expenses.creation_date, 'YYYY-MM')
    ), 0) as total_income,
    SUM(amount) as total_expenses,
    COALESCE((
        SELECT SUM(amount) 
        FROM incomes i 
        WHERE i.user_id = :userId 
          AND TO_CHAR(i.creation_date, 'YYYY-MM') = TO_CHAR(expenses.creation_date, 'YYYY-MM')
    ), 0) - SUM(amount) as balance
FROM expenses
WHERE user_id = :userId 
  AND EXTRACT(YEAR FROM creation_date) = :year
GROUP BY TO_CHAR(creation_date, 'YYYY-MM')
ORDER BY month;

-- Version optimale avec FULL OUTER JOIN (PostgreSQL)
-- Plus performante et gère les mois sans revenus ni dépenses

SELECT 
    COALESCE(income_month, expense_month) as month,
    COALESCE(total_income, 0) as total_income,
    COALESCE(total_expenses, 0) as total_expenses,
    COALESCE(total_income, 0) - COALESCE(total_expenses, 0) as balance
FROM (
    SELECT 
        TO_CHAR(creation_date, 'YYYY-MM') as income_month,
        SUM(amount) as total_income
    FROM incomes
    WHERE user_id = :userId 
      AND EXTRACT(YEAR FROM creation_date) = :year
    GROUP BY TO_CHAR(creation_date, 'YYYY-MM')
) income_data
FULL OUTER JOIN (
    SELECT 
        TO_CHAR(creation_date, 'YYYY-MM') as expense_month,
        SUM(amount) as total_expenses
    FROM expenses
    WHERE user_id = :userId 
      AND EXTRACT(YEAR FROM creation_date) = :year
    GROUP BY TO_CHAR(creation_date, 'YYYY-MM')
) expense_data ON income_month = expense_month
ORDER BY month;


