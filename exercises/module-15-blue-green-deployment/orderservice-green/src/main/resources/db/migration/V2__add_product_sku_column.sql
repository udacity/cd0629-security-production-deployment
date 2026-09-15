-- Expand step 1 of 4 (expand-contract): add the new column. Nullable, and
-- nothing reads or writes it yet — blue keeps running against this schema
-- completely unaffected, since a new nullable column with no NOT NULL
-- constraint is invisible to code that doesn't know it exists.
ALTER TABLE placed_orders ADD COLUMN product_sku VARCHAR(255);
