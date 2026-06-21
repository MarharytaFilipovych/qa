ALTER TABLE orders ADD COLUMN owner_user_id UUID NOT NULL;
ALTER TABLE orders DROP COLUMN customer_name;