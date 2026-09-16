-- Module 31: the "expand" half of Module 30's expand-and-contract
-- pattern. Green's new code will read this column; Blue's old code
-- has no idea it exists and keeps running completely unaffected —
-- that's the whole point. Nullable, no default forced onto existing
-- rows, nothing here breaks Blue. The "contract" step (actually
-- dropping an old column, once every version reads only the new one)
-- deliberately isn't built here — it only makes sense after Green has
-- been live for a while, not on day one of a migration.
ALTER TABLE account ADD COLUMN region VARCHAR(50);
