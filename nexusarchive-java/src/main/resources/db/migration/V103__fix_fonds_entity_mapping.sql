-- Fix missing entity_id in bas_fonds table
-- This restores the relationship between fonds and legal entities for the Group Structure View

-- 1. BR Group
UPDATE bas_fonds 
SET entity_id = 'ORG_BR_GROUP' 
WHERE fonds_code = 'BR-GROUP';

-- 2. Sales Company
UPDATE bas_fonds 
SET entity_id = 'ORG_BR_SALES' 
WHERE fonds_code = 'BR-SALES';

-- 3. Trade Company
UPDATE bas_fonds 
SET entity_id = 'ORG_BR_TRADE' 
WHERE fonds_code = 'BR-TRADE';

-- 4. Manufacturing Company
UPDATE bas_fonds 
SET entity_id = 'ORG_BR_MFG' 
WHERE fonds_code = 'BR-MFG';

-- 5. Demo Fonds (Map to Group)
UPDATE bas_fonds 
SET entity_id = 'ORG_BR_GROUP' 
WHERE fonds_code = 'DEMO';
