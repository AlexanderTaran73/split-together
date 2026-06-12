INSERT INTO platform_roles (code, name) VALUES
    ('USER',  'Пользователь'),
    ('ADMIN', 'Администратор платформы');

INSERT INTO group_roles (code, name) VALUES
    ('OWNER',  'Создатель'),
    ('ADMIN',  'Администратор'),
    ('MEMBER', 'Участник');

INSERT INTO group_statuses (code, name) VALUES
    ('ACTIVE',   'Активна'),
    ('ARCHIVED', 'Архивирована');

INSERT INTO membership_statuses (code, name) VALUES
    ('ACTIVE',   'Активен'),
    ('LEFT',     'Покинул группу'),
    ('REMOVED',  'Удалён из группы');

INSERT INTO invitation_types (code, name) VALUES
    ('DIRECT', 'Прямое приглашение'),
    ('LINK',   'Приглашение по ссылке');

INSERT INTO invitation_statuses (code, name) VALUES
    ('PENDING',  'Ожидает принятия'),
    ('ACCEPTED', 'Принято'),
    ('DECLINED', 'Отклонено'),
    ('REVOKED',  'Отозвано'),
    ('EXPIRED',  'Истекло');

INSERT INTO email_verification_purposes (code, name) VALUES
    ('REGISTRATION', 'Подтверждение регистрации'),
    ('EMAIL_CHANGE', 'Смена email');

INSERT INTO settlement_statuses (code, name) VALUES
    ('PENDING',   'Ожидает подтверждения'),
    ('CONFIRMED', 'Подтверждено'),
    ('REJECTED',  'Отклонено');

INSERT INTO split_methods (code, name) VALUES
    ('EQUAL',  'Поровну'),
    ('SHARES', 'По долям'),
    ('EXACT',  'По суммам');

INSERT INTO expense_categories (code, name) VALUES
    ('FOOD',          'Еда и напитки'),
    ('TRANSPORT',     'Транспорт'),
    ('ACCOMMODATION', 'Проживание'),
    ('ENTERTAINMENT', 'Развлечения'),
    ('SHOPPING',      'Покупки'),
    ('UTILITIES',     'Коммунальные услуги'),
    ('OTHER',         'Прочее');

INSERT INTO currencies (code, name) VALUES
    ('RUB', 'Российский рубль'),
    ('USD', 'Доллар США'),
    ('EUR', 'Евро');

INSERT INTO expense_confirmation_statuses (code, name) VALUES
    ('PENDING',   'Ожидает подтверждения'),
    ('CONFIRMED', 'Подтверждено'),
    ('DISPUTED',  'Оспорено');
