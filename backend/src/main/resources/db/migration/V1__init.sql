CREATE TABLE mortgage (
    id              BIGSERIAL PRIMARY KEY,
    property_price  NUMERIC(14, 2) NOT NULL,
    deposit         NUMERIC(14, 2) NOT NULL,
    mortgage_amount NUMERIC(14, 2) NOT NULL,
    term_years      INTEGER        NOT NULL,
    start_date      DATE           NOT NULL,
    payment_day     INTEGER        NOT NULL,
    repayment_type  VARCHAR(20)    NOT NULL
);

CREATE TABLE interest_rate (
    id             BIGSERIAL PRIMARY KEY,
    mortgage_id    BIGINT        NOT NULL REFERENCES mortgage (id) ON DELETE CASCADE,
    rate           NUMERIC(6, 3) NOT NULL,
    effective_from DATE          NOT NULL
);

CREATE INDEX idx_interest_rate_mortgage ON interest_rate (mortgage_id, effective_from);

CREATE TABLE entry (
    id            BIGSERIAL PRIMARY KEY,
    mortgage_id   BIGINT         NOT NULL REFERENCES mortgage (id) ON DELETE CASCADE,
    entry_date    DATE           NOT NULL,
    total_payment NUMERIC(14, 2) NOT NULL,
    rent          NUMERIC(14, 2) NOT NULL,
    equity        NUMERIC(14, 2) NOT NULL,
    type          VARCHAR(20)    NOT NULL
);

CREATE INDEX idx_entry_mortgage_date ON entry (mortgage_id, entry_date);
