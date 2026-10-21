# Occupancy forecasting

## What is measured

- **Operational beds:** beds whose state is not `OUT_OF_SERVICE`.
- **Occupied beds:** beds in `OCCUPIED`.
- **Available beds:** beds in `AVAILABLE`.
- **Cleaning beds:** beds in `CLEANING`. They are operational but not available for a new patient.
- **Occupancy percentage:** `occupied / operational * 100`. If there are no operational beds, the percentage is 0.

Ward rows use the same definitions inside each ward.

## Prediction method

The baseline is a **14-day rolling average of net daily flow**.

1. For each of the last 14 calendar days, count admissions and discharges (missing days count as zero).
2. Daily net = admissions − discharges.
3. Average net = arithmetic mean of those 14 daily nets.
4. Next 24 hours: for hour `h` from 1 to 24, `predictedOccupied = round(currentOccupied + averageNet * h / 24)`, then clamp to operational capacity.
5. Next 7 days: for day `d` from 1 to 7, `predictedOccupied = round(currentOccupied + averageNet * d)`, then clamp.

If the window contains no admission or discharge events, average net is 0, the curve stays flat, and the payload sets `lowConfidence` to true.

## Alerts

- Warning when occupancy is at least 85%.
- Critical when occupancy is at least 90%.

## Limitations

The line assumes the recent average net change continues. It does not model hour-of-day patterns, day of week, elective schedules, length of stay, or sudden incidents. Predictions are not observations. The dashboard labels forecast series as predicted. Do not use the figures for clinical decisions without staff review.

## Capacity boundary

Predicted occupied beds are never below 0 and never above the current operational bed count. A real ward can also change capacity (beds taken out of service) after the forecast is drawn; the next refresh recalculates from current capacity.
