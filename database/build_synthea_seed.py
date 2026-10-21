"""Build a Flyway migration from the public Synthea 100-patient CSV sample.

Source: https://github.com/synthetichealth/synthea-sample-data
The CSV is synthetic (MITRE Synthea). Social security numbers and other
identifiers are not copied. Dates are shifted so the newest encounter lands
on the migration clock's recent past, which keeps the forecast window populated.
"""

import csv
import datetime as dt
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CSV_DIR = Path.home() / "AppData/Local/Temp/synthea/csv"
OUT = ROOT / "backend/src/main/resources/db/migration/V3__synthea_sample.sql"

WARDS = [
    ("EMER", "Emergency Department", "GENERAL", 1, 10),
    ("GEN", "General Medicine", "GENERAL", 2, 16),
    ("ICU", "Intensive Care", "ICU", 3, 8),
    ("PED", "Pediatrics", "PEDIATRIC", 4, 8),
    ("MAT", "Maternity", "MATERNITY", 5, 6),
]


def sql(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def parse_time(value: str) -> dt.datetime | None:
    if not value:
        return None
    return dt.datetime.fromisoformat(value.replace("Z", "+00:00"))


def ward_for(encounter_class: str, description: str, age: int) -> str:
    text = f"{description} {encounter_class}".lower()
    if age < 18:
        return "PED"
    if any(word in text for word in ("pregnan", "obstetric", "birth", "labor")):
        return "MAT"
    if encounter_class == "inpatient" and any(word in text for word in ("sepsis", "failure", "pneumonia", "cardiac", "stroke")):
        return "ICU"
    if encounter_class == "emergency":
        return "EMER"
    if encounter_class == "inpatient":
        return "ICU" if "icu" in text else "GEN"
    return "EMER"


def main() -> None:
    patients: dict[str, dict[str, str]] = {}
    with (CSV_DIR / "patients.csv").open(newline="", encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            if row["DEATHDATE"]:
                continue
            patients[row["Id"]] = row

    encounters = []
    with (CSV_DIR / "encounters.csv").open(newline="", encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            if row["ENCOUNTERCLASS"] not in {"inpatient", "emergency", "urgentcare"}:
                continue
            if row["PATIENT"] not in patients:
                continue
            start = parse_time(row["START"])
            if start is None:
                continue
            encounters.append({
                "patient": row["PATIENT"],
                "start": start,
                "stop": parse_time(row["STOP"]),
                "kind": row["ENCOUNTERCLASS"],
                "description": (row["DESCRIPTION"] or row["REASONDESCRIPTION"] or "Hospital encounter")[:240],
            })

    # Keep the newest encounter per patient, then place those visits across the
    # last three weeks. Original Synthea clocks span decades, so an unshifted
    # extract would leave the dashboard empty.
    newest_per_patient: dict[str, dict] = {}
    for item in sorted(encounters, key=lambda row: row["start"]):
        newest_per_patient[item["patient"]] = item
    cohort = list(newest_per_patient.values())[-70:]
    now = dt.datetime.now(dt.timezone.utc).replace(microsecond=0)
    for index, item in enumerate(cohort):
        day_offset = 20 - (index % 21)
        item["start"] = now - dt.timedelta(days=day_offset, hours=index % 10)
        item["stop"] = item["start"] + dt.timedelta(hours=6 + (index % 30))
        item["active"] = index >= len(cohort) - 34
        if item["active"]:
            item["start"] = now - dt.timedelta(hours=4 + (index % 48))
            item["stop"] = None

    beds = []
    for code, _name, _type, _floor, count in WARDS:
        for index in range(1, count + 1):
            beds.append({
                "ward": code,
                "code": f"{code}-{index:02d}",
                "state": "AVAILABLE",
                "isolation": code in {"ICU", "EMER"} and index <= 2,
                "ventilator": code == "ICU",
                "monitor": code in {"ICU", "EMER", "PED"},
                "oxygen": code != "MAT" or index <= 3,
            })
    beds_by_ward: dict[str, list[dict]] = {}
    for bed in beds:
        beds_by_ward.setdefault(bed["ward"], []).append(bed)

    def take_bed(ward: str, active: bool) -> dict | None:
        pool = beds_by_ward[ward]
        reserved = {"GEN-15", "GEN-16", "ICU-08"}
        if active:
            free = [bed for bed in pool if bed["state"] == "AVAILABLE" and bed["code"] not in reserved]
            if not free:
                return None
            chosen = free[0]
            chosen["state"] = "OCCUPIED"
            return chosen
        return pool[len(pool) // 2]

    admission_rows = []
    for item in cohort:
        patient_id = item["patient"]
        patient = patients[patient_id]
        birth = dt.date.fromisoformat(patient["BIRTHDATE"])
        age = now.year - birth.year
        ward = ward_for(item["kind"], item["description"], age)
        is_active = bool(item["active"])
        bed = take_bed(ward, is_active)
        if bed is None:
            continue
        admission_rows.append((patient_id, bed, item, is_active, ward))

    beds_by_code = {bed["code"]: bed for bed in beds}
    for code in ("EMER-09", "PED-08"):
        if beds_by_code[code]["state"] == "AVAILABLE":
            beds_by_code[code]["state"] = "CLEANING"
    if beds_by_code["MAT-06"]["state"] == "AVAILABLE":
        beds_by_code["MAT-06"]["state"] = "OUT_OF_SERVICE"

    patient_ids = []
    for patient_id, *_rest in admission_rows:
        if patient_id not in patient_ids:
            patient_ids.append(patient_id)

    lines = [
        "-- Public Synthea synthetic sample (MITRE). Not real patients.",
        "-- https://github.com/synthetichealth/synthea-sample-data",
        "DELETE FROM notification;",
        "DELETE FROM admission;",
        "DELETE FROM bed;",
        "DELETE FROM patient;",
        "DELETE FROM ward;",
        "",
    ]
    for code, name, ward_type, floor, _count in WARDS:
        lines.append(
            "INSERT INTO ward (code, name, ward_type, floor_number) VALUES "
            f"({sql(code)}, {sql(name)}, {sql(ward_type)}, {floor});"
        )
    lines.append("")
    for bed in beds:
        lines.append(
            "INSERT INTO bed (ward_id, bed_code, state, isolation_capable, has_ventilator, has_monitor, has_oxygen) "
            f"SELECT id, {sql(bed['code'])}, {sql(bed['state'])}, {str(bed['isolation']).upper()}, "
            f"{str(bed['ventilator']).upper()}, {str(bed['monitor']).upper()}, {str(bed['oxygen']).upper()} "
            f"FROM ward WHERE code = {sql(bed['ward'])};"
        )
    lines.append("")
    mrn = {}
    for index, patient_id in enumerate(patient_ids, start=1):
        patient = patients[patient_id]
        gender = {"M": "MALE", "F": "FEMALE"}.get(patient["GENDER"], "UNKNOWN")
        number = f"SYN-{index:04d}"
        mrn[patient_id] = number
        lines.append(
            "INSERT INTO patient (medical_record_number, first_name, last_name, date_of_birth, sex, phone) VALUES "
            f"({sql(number)}, {sql(patient['FIRST'])}, {sql(patient['LAST'])}, DATE {sql(patient['BIRTHDATE'])}, {sql(gender)}, NULL);"
        )
    lines.append("")
    for patient_id, bed, item, is_active, ward in admission_rows:
        start = item["start"].strftime("%Y-%m-%d %H:%M:%S")
        if is_active:
            lines.append(
                "INSERT INTO admission (patient_id, bed_id, status, diagnosis, isolation_required, required_ward_type, admitted_at, discharged_at, admitted_by, active_patient_key) "
                f"SELECT p.id, b.id, 'ACTIVE', {sql(item['description'])}, FALSE, {sql(ward_type_name(ward))}, TIMESTAMP {sql(start)}, NULL, 'synthea-import', p.id "
                f"FROM patient p, bed b WHERE p.medical_record_number = {sql(mrn[patient_id])} AND b.bed_code = {sql(bed['code'])};"
            )
        else:
            stop = item["stop"] or (item["start"] + dt.timedelta(hours=8))
            if stop <= item["start"]:
                stop = item["start"] + dt.timedelta(hours=4)
            lines.append(
                "INSERT INTO admission (patient_id, bed_id, status, diagnosis, isolation_required, required_ward_type, admitted_at, discharged_at, admitted_by, discharged_by, active_patient_key) "
                f"SELECT p.id, b.id, 'DISCHARGED', {sql(item['description'])}, FALSE, {sql(ward_type_name(ward))}, TIMESTAMP {sql(start)}, TIMESTAMP {sql(stop.strftime('%Y-%m-%d %H:%M:%S'))}, 'synthea-import', 'synthea-import', NULL "
                f"FROM patient p, bed b WHERE p.medical_record_number = {sql(mrn[patient_id])} AND b.bed_code = {sql(bed['code'])};"
            )
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"patients={len(patient_ids)} admissions={len(admission_rows)} beds={len(beds)} -> {OUT}")


def ward_type_name(code: str) -> str:
    return {"EMER": "GENERAL", "GEN": "GENERAL", "ICU": "ICU", "PED": "PEDIATRIC", "MAT": "MATERNITY"}[code]


if __name__ == "__main__":
    main()
