CREATE EXTENSION IF NOT EXISTS "unaccent";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Roles
CREATE TABLE roles (
    id   SMALLINT PRIMARY KEY,
    name VARCHAR(20) UNIQUE NOT NULL
);

-- 2. Users
CREATE TABLE users (
    id                       BIGSERIAL PRIMARY KEY,
    email                    VARCHAR(255) UNIQUE NOT NULL,
    password_hash            VARCHAR(255) NOT NULL,
    full_name                VARCHAR(255) NOT NULL,
    address                  TEXT,
    birth_date               DATE,
    
    role_id                  SMALLINT NOT NULL DEFAULT 3 REFERENCES roles(id), -- 3 = bidder
    
    -- Quyền bán tạm thời 7 ngày
    seller_expires_at        TIMESTAMPTZ,
    seller_upgraded_by       BIGINT REFERENCES users(id),
    
    positive_rating          INTEGER NOT NULL DEFAULT 0,
    negative_rating          INTEGER NOT NULL DEFAULT 0,
    
    email_verified           BOOLEAN NOT NULL DEFAULT false,
    
    is_active                BOOLEAN NOT NULL DEFAULT true,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email          ON users(email);
CREATE INDEX idx_users_role           ON users(role_id);
CREATE INDEX idx_users_seller_active  ON users(seller_expires_at) WHERE seller_expires_at > NOW(); -- partial index để kiểm tra user có đăng bán được không
CREATE INDEX idx_users_rating         ON users(positive_rating DESC, negative_rating);

-- 2.1. Email OTPs (for registration & password reset)
CREATE TABLE email_otps (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    otp_code    VARCHAR(6) NOT NULL,
    purpose     VARCHAR(20) NOT NULL CHECK (purpose IN ('registration', 'password_reset')),
    is_used     BOOLEAN NOT NULL DEFAULT false,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_email_otps_lookup ON email_otps(email, otp_code, purpose, is_used) WHERE NOT is_used AND expires_at > NOW();

-- 2.2. Refresh Tokens (for JWT authentication)
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(500) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at  TIMESTAMPTZ,
    replaced_by VARCHAR(500)
);
CREATE INDEX idx_refresh_tokens_user   ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token  ON refresh_tokens(token) WHERE revoked_at IS NULL;

-- 3. Categories (2-level only)
CREATE TABLE categories (
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    slug       VARCHAR(500) UNIQUE NOT NULL,
    parent_id  INTEGER REFERENCES categories(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(name, parent_id)
);

CREATE INDEX idx_categories_parent ON categories(parent_id);
CREATE INDEX idx_categories_slug ON categories(slug);

-- 4. Products
CREATE TABLE products (
    id                    BIGSERIAL PRIMARY KEY,
    seller_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id           INTEGER NOT NULL REFERENCES categories(id),
	
    title                 VARCHAR(255) NOT NULL,
    slug                  VARCHAR(500) UNIQUE NOT NULL,
    description           TEXT NOT NULL,
    
    starting_price        DECIMAL(15,2) NOT NULL CHECK (starting_price > 0),
    current_price         DECIMAL(15,2) NOT NULL,
    buy_now_price         DECIMAL(15,2),                    -- NULL = no buy-now
    price_step            DECIMAL(15,2) NOT NULL CHECK (price_step > 0),
    
    auto_extend           BOOLEAN NOT NULL DEFAULT true,
    allow_unrated_bidders BOOLEAN NOT NULL DEFAULT false,
    
    start_time            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    end_time              TIMESTAMPTZ NOT NULL,
    
    winner_id             BIGINT REFERENCES users(id),
    highest_bidder_id     BIGINT REFERENCES users(id),              -- current highest (for quick access)
    
    bid_count             INTEGER NOT NULL DEFAULT 0,
    view_count            INTEGER NOT NULL DEFAULT 0,
    
    is_ended              BOOLEAN NOT NULL DEFAULT false,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- MODERN PostgreSQL: generated column → no trigger!
    search_vector         TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('vietnamese', unaccent(coalesce(title, ''))), 'A') ||
        setweight(to_tsvector('vietnamese', unaccent(coalesce(description, ''))), 'B')
    ) STORED
);

-- Full-text search indexes
CREATE INDEX idx_products_fts          ON products USING GIN(search_vector);
CREATE INDEX idx_products_slug         ON products(slug);
CREATE INDEX idx_products_end_time     ON products(end_time ASC);
CREATE INDEX idx_products_created      ON products(created_at DESC);
CREATE INDEX idx_products_price        ON products(current_price);
CREATE INDEX idx_products_category     ON products(category_id);
CREATE INDEX idx_products_active       ON products(is_ended, end_time);

-- 5. Product Images (min 3 + 1 primary)
CREATE TABLE product_images (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    url         TEXT NOT NULL,
    is_primary  BOOLEAN NOT NULL DEFAULT false,
    sort_order  INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_product_images_primary ON product_images(product_id, is_primary DESC);

-- 6. Favorites / Watchlist
CREATE TABLE favorites (
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id  BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, product_id)
);
CREATE INDEX idx_favorites_user_created ON favorites(user_id, created_at DESC);

-- 7. AUTO-BID
CREATE TABLE auto_bids (
    product_id   BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    bidder_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    max_amount   DECIMAL(15,2) NOT NULL CHECK (max_amount > 0),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_active    BOOLEAN NOT NULL DEFAULT true,
    PRIMARY KEY (product_id, bidder_id)
);
CREATE INDEX idx_auto_bids_product_amount ON auto_bids(product_id, max_amount DESC);

-- 8. Lịch sử bid hiển thị (để hiện trên giao diện)
CREATE TABLE bid_history (
    id            BIGSERIAL PRIMARY KEY,
    product_id    BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    bidder_id     BIGINT NOT NULL REFERENCES users(id),
    bid_amount    DECIMAL(15,2) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_bid_history_product_time ON bid_history(product_id, created_at DESC);

-- 9. Blocked bidders (seller refuses a bidder)
CREATE TABLE blocked_bidders (
    product_id  BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    bidder_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (product_id, bidder_id)
);

-- 10. Description append logs (seller bổ sung mô tả)
CREATE TABLE description_logs (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 11. Q&A
CREATE TABLE auction_questions (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    asker_id    BIGINT NOT NULL REFERENCES users(id),
    question    TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    answered_at TIMESTAMPTZ
);

CREATE TABLE auction_answers (
    id           BIGSERIAL PRIMARY KEY,
    question_id  BIGINT NOT NULL REFERENCES auction_questions(id) ON DELETE CASCADE,
    answerer_id  BIGINT NOT NULL REFERENCES users(id),
    answer       TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 12. Reviews (after auction ends)
CREATE TABLE reviews (
    id           BIGSERIAL PRIMARY KEY,
    product_id   BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    reviewer_id  BIGINT NOT NULL REFERENCES users(id),
    reviewee_id  BIGINT NOT NULL REFERENCES users(id),
    rating       SMALLINT NOT NULL CHECK (rating IN (-1, 1)),
    comment      TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(product_id, reviewer_id)
);

-- 13. Upgrade request bidder → seller
CREATE TABLE upgrade_requests (
    id          BIGSERIAL PRIMARY KEY,
    bidder_id   BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    reason      TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'pending'
                CHECK (status IN ('pending', 'approved', 'rejected')),
    admin_id    BIGINT REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 14. Post-auction order completion flow
CREATE TABLE order_completions (
    product_id        BIGINT PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    winner_id         BIGINT NOT NULL REFERENCES users(id),
    payment_status    VARCHAR(30) NOT NULL DEFAULT 'pending' CHECK (payment_status IN ('pending', 'paid', 'cancelled')),
    shipping_address  TEXT,
    tracking_number   VARCHAR(100),
    cancelled_by      BIGINT REFERENCES users(id),         -- seller who cancelled
    cancel_reason     TEXT,
    buyer_confirmed   BOOLEAN DEFAULT false,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 15. Chat between seller & winner after auction ends
CREATE TABLE order_chat_messages (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sender_id   BIGINT NOT NULL REFERENCES users(id),
    message     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 16. System configuration (for admin-configurable settings)
CREATE TABLE system_configs (
    key         VARCHAR(100) PRIMARY KEY,
    value       TEXT NOT NULL,
    description TEXT,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);