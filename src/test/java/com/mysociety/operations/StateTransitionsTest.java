package com.mysociety.operations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StateTransitionsTest {
    @Test
    void complaintStatesAreExplicit() {
        assertThat(ComplaintStatus.valueOf("OPEN")).isEqualTo(ComplaintStatus.OPEN);
        assertThat(ComplaintStatus.values()).contains(ComplaintStatus.RESOLVED, ComplaintStatus.CLOSED);
    }

    @Test
    void workOrderStatesAreExplicit() {
        assertThat(WorkOrderStatus.values()).contains(WorkOrderStatus.CREATED, WorkOrderStatus.VERIFIED);
    }
}
