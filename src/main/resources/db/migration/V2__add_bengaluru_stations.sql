-- Expand VOLTRA EV Charging Grid across key Bengaluru neighborhoods
-- Uses existing host/admin user safely without altering V1 schema

INSERT INTO users (
    full_name, email, password_hash, role, phone, payout_upi, base_address, wallet_balance, created_at
)
SELECT
    'Bengaluru Grid Host',
    'blr.host@voltra.in',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5L8z0k8mX6x0k8mX6x0k8mX6x0k8m',
    'HOST',
    '+91 98450 11223',
    'voltra.blr@okicici',
    'Indiranagar, Bengaluru, Karnataka',
    0.00,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'blr.host@voltra.in');

INSERT INTO charger_stations (
    name, host_name, host_phone, address, city, latitude, longitude,
    category, connector_type, power_kw, price_per_hour, price_per_kwh,
    total_bays, online, relay_active, approval_status,
    average_rating, review_count, booked_slots_preview, host_id
)
SELECT
    'Indiranagar 100ft Road Villa Wallbox',
    u.full_name,
    '+91 98450 11223',
    '742, 100 Feet Road, HAL 2nd Stage, Indiranagar',
    'Bengaluru',
    12.9784,
    77.6408,
    'P2P_HOME',
    'Type 2 AC',
    7.4,
    140.00,
    16.50,
    2,
    TRUE,
    FALSE,
    'APPROVED',
    4.9,
    18,
    'Available Now',
    u.id
FROM users u
WHERE u.id = COALESCE((SELECT id FROM users WHERE email = 'blr.host@voltra.in' LIMIT 1), (SELECT MIN(id) FROM users))
  AND NOT EXISTS (SELECT 1 FROM charger_stations WHERE name = 'Indiranagar 100ft Road Villa Wallbox');

INSERT INTO charger_stations (
    name, host_name, host_phone, address, city, latitude, longitude,
    category, connector_type, power_kw, price_per_hour, price_per_kwh,
    total_bays, online, relay_active, approval_status,
    average_rating, review_count, booked_slots_preview, host_id
)
SELECT
    'Koramangala 4th Block EV SuperHub',
    u.full_name,
    '+91 98450 22334',
    '80 Feet Road, Near Sony World Signal, Koramangala 4th Block',
    'Bengaluru',
    12.9352,
    77.6245,
    'PRIVATE_COMMERCIAL',
    'CCS2 DC Fast',
    60.0,
    420.00,
    21.00,
    4,
    TRUE,
    FALSE,
    'APPROVED',
    4.8,
    34,
    'Available Now',
    u.id
FROM users u
WHERE u.id = COALESCE((SELECT id FROM users WHERE email = 'blr.host@voltra.in' LIMIT 1), (SELECT MIN(id) FROM users))
  AND NOT EXISTS (SELECT 1 FROM charger_stations WHERE name = 'Koramangala 4th Block EV SuperHub');

INSERT INTO charger_stations (
    name, host_name, host_phone, address, city, latitude, longitude,
    category, connector_type, power_kw, price_per_hour, price_per_kwh,
    total_bays, online, relay_active, approval_status,
    average_rating, review_count, booked_slots_preview, host_id
)
SELECT
    'Whitefield ITPL Main Road Fast Plaza',
    u.full_name,
    '+91 98450 33445',
    'ITPL Main Road, Pattandur Agrahara, Whitefield',
    'Bengaluru',
    12.9855,
    77.7369,
    'PRIVATE_COMMERCIAL',
    'CCS2 DC Fast',
    50.0,
    380.00,
    19.50,
    3,
    TRUE,
    FALSE,
    'APPROVED',
    4.7,
    27,
    'Available Now',
    u.id
FROM users u
WHERE u.id = COALESCE((SELECT id FROM users WHERE email = 'blr.host@voltra.in' LIMIT 1), (SELECT MIN(id) FROM users))
  AND NOT EXISTS (SELECT 1 FROM charger_stations WHERE name = 'Whitefield ITPL Main Road Fast Plaza');

INSERT INTO charger_stations (
    name, host_name, host_phone, address, city, latitude, longitude,
    category, connector_type, power_kw, price_per_hour, price_per_kwh,
    total_bays, online, relay_active, approval_status,
    average_rating, review_count, booked_slots_preview, host_id
)
SELECT
    'Electronic City Phase 1 BESCOM Plaza',
    'BESCOM Public Grid',
    '+91 80 2287 3333',
    'Hosur Road, Near Infosys Gate 1, Electronic City Phase 1',
    'Bengaluru',
    12.8452,
    77.6602,
    'GOVT_PUBLIC',
    'CCS2 + Type 2',
    30.0,
    240.00,
    15.00,
    4,
    TRUE,
    FALSE,
    'APPROVED',
    4.6,
    41,
    'Available Now',
    u.id
FROM users u
WHERE u.id = COALESCE((SELECT id FROM users WHERE email = 'blr.host@voltra.in' LIMIT 1), (SELECT MIN(id) FROM users))
  AND NOT EXISTS (SELECT 1 FROM charger_stations WHERE name = 'Electronic City Phase 1 BESCOM Plaza');

INSERT INTO charger_stations (
    name, host_name, host_phone, address, city, latitude, longitude,
    category, connector_type, power_kw, price_per_hour, price_per_kwh,
    total_bays, online, relay_active, approval_status,
    average_rating, review_count, booked_slots_preview, host_id
)
SELECT
    'HSR Layout Sector 2 Green Home Charger',
    u.full_name,
    '+91 98450 55667',
    '27th Main Road, Sector 2, HSR Layout',
    'Bengaluru',
    12.9121,
    77.6446,
    'P2P_HOME',
    'Type 2 AC',
    11.0,
    160.00,
    17.00,
    2,
    TRUE,
    FALSE,
    'APPROVED',
    4.9,
    22,
    'Available Now',
    u.id
FROM users u
WHERE u.id = COALESCE((SELECT id FROM users WHERE email = 'blr.host@voltra.in' LIMIT 1), (SELECT MIN(id) FROM users))
  AND NOT EXISTS (SELECT 1 FROM charger_stations WHERE name = 'HSR Layout Sector 2 Green Home Charger');

INSERT INTO charger_stations (
    name, host_name, host_phone, address, city, latitude, longitude,
    category, connector_type, power_kw, price_per_hour, price_per_kwh,
    total_bays, online, relay_active, approval_status,
    average_rating, review_count, booked_slots_preview, host_id
)
SELECT
    'MG Road Metro Station Public EV Hub',
    'BMRCL & BESCOM Grid',
    '+91 80 2558 1111',
    'Mahatma Gandhi Road, Near Trinity Circle Metro',
    'Bengaluru',
    12.9756,
    77.6066,
    'GOVT_PUBLIC',
    'CCS2 DC Fast',
    50.0,
    350.00,
    16.00,
    3,
    TRUE,
    FALSE,
    'APPROVED',
    4.8,
    29,
    'Available Now',
    u.id
FROM users u
WHERE u.id = COALESCE((SELECT id FROM users WHERE email = 'blr.host@voltra.in' LIMIT 1), (SELECT MIN(id) FROM users))
  AND NOT EXISTS (SELECT 1 FROM charger_stations WHERE name = 'MG Road Metro Station Public EV Hub');
