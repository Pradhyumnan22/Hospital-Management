package com.hospital.bedmgmt.domain;

/**
 * Manual transitions are housekeeping and maintenance only.
 * Occupied and cleaning are entered by admission and discharge services.
 */
public final class BedStateMachine {

    private BedStateMachine() {
    }

    public static boolean isManualTransition(BedState from, BedState to) {
        if (from == null || to == null) {
            return false;
        }
        return (from == BedState.AVAILABLE && to == BedState.OUT_OF_SERVICE)
                || (from == BedState.OUT_OF_SERVICE && to == BedState.AVAILABLE)
                || (from == BedState.CLEANING && to == BedState.AVAILABLE);
    }
}
