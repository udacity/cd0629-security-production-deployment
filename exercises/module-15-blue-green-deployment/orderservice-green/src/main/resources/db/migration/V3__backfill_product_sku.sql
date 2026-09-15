-- Expand step 2 of 4: backfill existing rows. Runs AFTER V2 (column
-- exists) and BEFORE green starts taking traffic — any row written by
-- blue before this point only has "sku" populated; this catches those up
-- so nothing reading "product_sku" later finds unexpected NULLs for
-- pre-migration orders.
UPDATE placed_orders SET product_sku = sku WHERE product_sku IS NULL;
