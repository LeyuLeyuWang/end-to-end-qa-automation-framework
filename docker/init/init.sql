-- Independent local fixture. Not SauceDemo's or Restful Booker's backend.
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    status TEXT NOT NULL CHECK (status IN ('active', 'inactive'))
);
CREATE TABLE orders (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    status TEXT NOT NULL CHECK (status IN ('pending', 'paid', 'cancelled')),
    total_amount NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE order_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_name TEXT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0)
);
INSERT INTO users(email, status) VALUES ('seed@example.test', 'active');
INSERT INTO orders(user_id, status, total_amount)
SELECT id, 'paid', 39.98 FROM users WHERE email = 'seed@example.test';
INSERT INTO order_items(order_id, product_name, quantity, unit_price)
SELECT id, 'Demo Backpack', 1, 29.99 FROM orders;
INSERT INTO order_items(order_id, product_name, quantity, unit_price)
SELECT id, 'Demo Light', 1, 9.99 FROM orders;

