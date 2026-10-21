package com.hospital.bedmgmt.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedStateMachineTest {

    @Test
    void allowsHousekeepingAndMaintenanceOnly() {
        assertTrue(BedStateMachine.isManualTransition(BedState.CLEANING, BedState.AVAILABLE));
        assertTrue(BedStateMachine.isManualTransition(BedState.AVAILABLE, BedState.OUT_OF_SERVICE));
        assertTrue(BedStateMachine.isManualTransition(BedState.OUT_OF_SERVICE, BedState.AVAILABLE));
        assertFalse(BedStateMachine.isManualTransition(BedState.AVAILABLE, BedState.OCCUPIED));
        assertFalse(BedStateMachine.isManualTransition(BedState.OCCUPIED, BedState.CLEANING));
        assertFalse(BedStateMachine.isManualTransition(BedState.OCCUPIED, BedState.AVAILABLE));
    }
}
