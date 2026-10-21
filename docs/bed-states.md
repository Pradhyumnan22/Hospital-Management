# Bed states and recommendations

## States

| State | Meaning | Next states |
| --- | --- | --- |
| AVAILABLE | Can be offered and allocated | OCCUPIED via confirmed admission; OUT_OF_SERVICE manually |
| OCCUPIED | Patient currently admitted to this bed | CLEANING via discharge only |
| CLEANING | Discharge complete, not ready for a patient | AVAILABLE manually when housekeeping finishes |
| OUT_OF_SERVICE | Closed for maintenance or staffing | AVAILABLE manually |

`OCCUPIED` and `CLEANING` cannot be set through the generic state endpoint. That keeps allocation and discharge as the only writers of those transitions.

## Recommendation rules

Hard filters (a failure removes the bed, regardless of score):

1. State is `AVAILABLE`.
2. If a required ward type is set, the bed's ward type matches.
3. If isolation is required, the bed is isolation-capable.
4. If a ventilator, cardiac monitor, or oxygen is required, the bed has that equipment.

Score for beds that pass, starting at 100:

- Preferred ward type matches: +20
- Isolation-capable when isolation is not required: −15 (keep those rooms for patients who need them)
- Ward occupancy at or below 70%: +15
- Ward occupancy above 70% and at or below 85%: +8
- Otherwise: +0 for occupancy

Ties break toward the lower ward occupancy, then bed code. The API returns the points that were applied and a separate list of excluded beds with reasons.

Confirmation runs inside a database transaction: lock the bed, reject it if it is no longer `AVAILABLE`, reject a second active admission for the patient, and run the hard filters again.
