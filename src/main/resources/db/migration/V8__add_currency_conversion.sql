ALTER TABLE groups RENAME COLUMN currency_id TO base_currency_id;

CREATE TABLE exchange_rates (
    id          BIGSERIAL      PRIMARY KEY,
    currency_id INTEGER        NOT NULL REFERENCES currencies(id),
    rate_date   DATE           NOT NULL,
    rate        NUMERIC(19, 6) NOT NULL CHECK (rate > 0),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (currency_id, rate_date)
);

ALTER TABLE expense_participants ADD COLUMN base_share NUMERIC(19, 2);
UPDATE expense_participants SET base_share = share;
ALTER TABLE expense_participants ALTER COLUMN base_share SET NOT NULL;
ALTER TABLE expense_participants ADD CONSTRAINT chk_expense_participants_base_share CHECK (base_share >= 0);

ALTER TABLE settlements ADD COLUMN settlement_date DATE;
UPDATE settlements SET settlement_date = (created_at AT TIME ZONE 'UTC')::date;
ALTER TABLE settlements ALTER COLUMN settlement_date SET NOT NULL;

ALTER TABLE settlements ADD COLUMN base_amount NUMERIC(19, 2);
UPDATE settlements s SET base_amount = s.amount
WHERE s.status_id = (SELECT id FROM settlement_statuses WHERE code = 'CONFIRMED');

INSERT INTO currencies (code, name) VALUES
    ('AUD', 'Австралийский доллар'),
    ('AZN', 'Азербайджанский манат'),
    ('GBP', 'Фунт стерлингов'),
    ('AMD', 'Армянский драм'),
    ('BYN', 'Белорусский рубль'),
    ('BGN', 'Болгарский лев'),
    ('BRL', 'Бразильский реал'),
    ('HUF', 'Венгерский форинт'),
    ('VND', 'Вьетнамский донг'),
    ('HKD', 'Гонконгский доллар'),
    ('GEL', 'Грузинский лари'),
    ('DKK', 'Датская крона'),
    ('AED', 'Дирхам ОАЭ'),
    ('EGP', 'Египетский фунт'),
    ('INR', 'Индийская рупия'),
    ('IDR', 'Индонезийская рупия'),
    ('KZT', 'Казахстанский тенге'),
    ('CAD', 'Канадский доллар'),
    ('QAR', 'Катарский риал'),
    ('KGS', 'Киргизский сом'),
    ('CNY', 'Китайский юань'),
    ('MDL', 'Молдавский лей'),
    ('NZD', 'Новозеландский доллар'),
    ('NOK', 'Норвежская крона'),
    ('PLN', 'Польский злотый'),
    ('RON', 'Румынский лей'),
    ('XDR', 'СДР (специальные права заимствования)'),
    ('SGD', 'Сингапурский доллар'),
    ('TJS', 'Таджикский сомони'),
    ('THB', 'Таиландский бат'),
    ('TRY', 'Турецкая лира'),
    ('TMT', 'Новый туркменский манат'),
    ('UZS', 'Узбекский сум'),
    ('UAH', 'Украинская гривна'),
    ('CZK', 'Чешская крона'),
    ('SEK', 'Шведская крона'),
    ('CHF', 'Швейцарский франк'),
    ('RSD', 'Сербский динар'),
    ('ZAR', 'Южноафриканский рэнд'),
    ('KRW', 'Вон Республики Корея'),
    ('JPY', 'Японская иена')
ON CONFLICT (code) DO NOTHING;
