INSERT INTO ward (code, name, ward_type, floor_number) VALUES
    ('GEN', 'General Medicine', 'GENERAL', 1),
    ('ICU', 'Intensive Care', 'ICU', 2),
    ('ISO', 'Isolation Unit', 'ISOLATION', 2),
    ('PED', 'Pediatrics', 'PEDIATRIC', 3);

INSERT INTO bed (ward_id, bed_code, state, isolation_capable, has_ventilator, has_monitor, has_oxygen)
SELECT w.id, b.bed_code, b.state, b.isolation_capable, b.has_ventilator, b.has_monitor, b.has_oxygen
FROM ward w
JOIN (
    SELECT 'GEN' AS ward_code, 'G-01' AS bed_code, 'OCCUPIED' AS state, FALSE AS isolation_capable, FALSE AS has_ventilator, TRUE AS has_monitor, TRUE AS has_oxygen UNION ALL
    SELECT 'GEN', 'G-02', 'OCCUPIED', FALSE, FALSE, FALSE, TRUE UNION ALL
    SELECT 'GEN', 'G-03', 'AVAILABLE', FALSE, FALSE, FALSE, TRUE UNION ALL
    SELECT 'GEN', 'G-04', 'AVAILABLE', FALSE, FALSE, TRUE, TRUE UNION ALL
    SELECT 'ICU', 'U-01', 'OCCUPIED', FALSE, TRUE, TRUE, TRUE UNION ALL
    SELECT 'ICU', 'U-02', 'AVAILABLE', FALSE, TRUE, TRUE, TRUE UNION ALL
    SELECT 'ICU', 'U-03', 'AVAILABLE', FALSE, FALSE, TRUE, TRUE UNION ALL
    SELECT 'ICU', 'U-04', 'OUT_OF_SERVICE', FALSE, TRUE, TRUE, TRUE UNION ALL
    SELECT 'ISO', 'I-01', 'OCCUPIED', TRUE, FALSE, TRUE, TRUE UNION ALL
    SELECT 'ISO', 'I-02', 'AVAILABLE', TRUE, FALSE, TRUE, TRUE UNION ALL
    SELECT 'ISO', 'I-03', 'AVAILABLE', TRUE, FALSE, FALSE, FALSE UNION ALL
    SELECT 'ISO', 'I-04', 'CLEANING', TRUE, FALSE, TRUE, TRUE UNION ALL
    SELECT 'PED', 'P-01', 'OCCUPIED', FALSE, FALSE, TRUE, TRUE UNION ALL
    SELECT 'PED', 'P-02', 'AVAILABLE', FALSE, FALSE, FALSE, TRUE UNION ALL
    SELECT 'PED', 'P-03', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE UNION ALL
    SELECT 'PED', 'P-04', 'AVAILABLE', FALSE, FALSE, TRUE, TRUE
) b ON b.ward_code = w.code;

INSERT INTO patient (medical_record_number, first_name, last_name, date_of_birth, sex, phone) VALUES
    ('MRN-1001', 'Alex', 'Demo', DATE '1988-04-12', 'FEMALE', '555-0101'),
    ('MRN-1002', 'Blake', 'Sample', DATE '1975-11-02', 'MALE', '555-0102'),
    ('MRN-1003', 'Casey', 'Example', DATE '1992-07-19', 'OTHER', '555-0103'),
    ('MRN-1004', 'Devon', 'Synthetic', DATE '1968-01-30', 'MALE', '555-0104'),
    ('MRN-1005', 'Eden', 'Fictional', DATE '2016-09-08', 'FEMALE', '555-0105'),
    ('MRN-1006', 'Finley', 'Placeholder', DATE '1983-03-22', 'FEMALE', '555-0106'),
    ('MRN-1007', 'Gray', 'Notreal', DATE '1999-12-01', 'MALE', '555-0107'),
    ('MRN-1008', 'Harper', 'Imaginary', DATE '2001-06-14', 'FEMALE', '555-0108');

INSERT INTO admission (patient_id, bed_id, status, diagnosis, isolation_required, required_ward_type, admitted_at, discharged_at, admitted_by, active_patient_key)
SELECT p.id, b.id, 'ACTIVE', 'Synthetic community pneumonia', FALSE, 'GENERAL', CURRENT_TIMESTAMP - INTERVAL '2' DAY, NULL, 'seed', p.id
FROM patient p, bed b WHERE p.medical_record_number = 'MRN-1001' AND b.bed_code = 'G-01';

INSERT INTO admission (patient_id, bed_id, status, diagnosis, isolation_required, required_ward_type, admitted_at, discharged_at, admitted_by, active_patient_key)
SELECT p.id, b.id, 'ACTIVE', 'Synthetic observation', FALSE, 'GENERAL', CURRENT_TIMESTAMP - INTERVAL '1' DAY, NULL, 'seed', p.id
FROM patient p, bed b WHERE p.medical_record_number = 'MRN-1002' AND b.bed_code = 'G-02';

INSERT INTO admission (patient_id, bed_id, status, diagnosis, isolation_required, required_ward_type, admitted_at, discharged_at, admitted_by, active_patient_key)
SELECT p.id, b.id, 'ACTIVE', 'Synthetic respiratory support', FALSE, 'ICU', CURRENT_TIMESTAMP - INTERVAL '3' DAY, NULL, 'seed', p.id
FROM patient p, bed b WHERE p.medical_record_number = 'MRN-1003' AND b.bed_code = 'U-01';

INSERT INTO admission (patient_id, bed_id, status, diagnosis, isolation_required, required_ward_type, admitted_at, discharged_at, admitted_by, active_patient_key)
SELECT p.id, b.id, 'ACTIVE', 'Synthetic isolation precaution', TRUE, 'ISOLATION', CURRENT_TIMESTAMP - INTERVAL '1' DAY, NULL, 'seed', p.id
FROM patient p, bed b WHERE p.medical_record_number = 'MRN-1004' AND b.bed_code = 'I-01';

INSERT INTO admission (patient_id, bed_id, status, diagnosis, isolation_required, required_ward_type, admitted_at, discharged_at, admitted_by, active_patient_key)
SELECT p.id, b.id, 'ACTIVE', 'Synthetic pediatric asthma', FALSE, 'PEDIATRIC', CURRENT_TIMESTAMP - INTERVAL '4' DAY, NULL, 'seed', p.id
FROM patient p, bed b WHERE p.medical_record_number = 'MRN-1005' AND b.bed_code = 'P-01';

INSERT INTO admission (patient_id, bed_id, status, diagnosis, isolation_required, required_ward_type, admitted_at, discharged_at, admitted_by, discharged_by, active_patient_key)
SELECT p.id, b.id, 'DISCHARGED', 'Synthetic completed stay', FALSE, 'GENERAL', CURRENT_TIMESTAMP - INTERVAL '10' DAY, CURRENT_TIMESTAMP - INTERVAL '8' DAY, 'seed', 'seed', NULL
FROM patient p, bed b WHERE p.medical_record_number = 'MRN-1006' AND b.bed_code = 'G-03';

INSERT INTO admission (patient_id, bed_id, status, diagnosis, isolation_required, required_ward_type, admitted_at, discharged_at, admitted_by, discharged_by, active_patient_key)
SELECT p.id, b.id, 'DISCHARGED', 'Synthetic short stay', FALSE, 'GENERAL', CURRENT_TIMESTAMP - INTERVAL '6' DAY, CURRENT_TIMESTAMP - INTERVAL '5' DAY, 'seed', 'seed', NULL
FROM patient p, bed b WHERE p.medical_record_number = 'MRN-1007' AND b.bed_code = 'P-03';

INSERT INTO admission (patient_id, bed_id, status, diagnosis, isolation_required, required_ward_type, admitted_at, discharged_at, admitted_by, discharged_by, active_patient_key)
SELECT p.id, b.id, 'DISCHARGED', 'Synthetic day case', FALSE, 'ICU', CURRENT_TIMESTAMP - INTERVAL '3' DAY, CURRENT_TIMESTAMP - INTERVAL '2' DAY, 'seed', 'seed', NULL
FROM patient p, bed b WHERE p.medical_record_number = 'MRN-1008' AND b.bed_code = 'U-03';

INSERT INTO admission (patient_id, bed_id, status, diagnosis, isolation_required, required_ward_type, admitted_at, discharged_at, admitted_by, discharged_by, active_patient_key)
SELECT p.id, b.id, 'DISCHARGED', 'Synthetic earlier stay', FALSE, 'GENERAL', CURRENT_TIMESTAMP - INTERVAL '12' DAY, CURRENT_TIMESTAMP - INTERVAL '11' DAY, 'seed', 'seed', NULL
FROM patient p, bed b WHERE p.medical_record_number = 'MRN-1006' AND b.bed_code = 'G-04';
