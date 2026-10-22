UPDATE patient
SET first_name = TRIM(REGEXP_REPLACE(first_name, '[0-9]+', '')),
    last_name = TRIM(REGEXP_REPLACE(last_name, '[0-9]+', ''));
