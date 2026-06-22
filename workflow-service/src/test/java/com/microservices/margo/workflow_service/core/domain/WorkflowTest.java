package com.microservices.margo.workflow_service.core.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowTest {

    private Workflow base() {
        return Workflow.builder()
                .id(UUID.randomUUID())
                .type(WorkflowType.CREATE_ORDER.toString())
                .state(WorkflowState.STARTED)
                .payload("test")
                .build();
    }

    @Test
    void transitionTo_changesStateAndSetsUpdatedAt() {
        Workflow w = base().transitionTo(WorkflowState.ORDER_CREATED);
        assertThat(w.state()).isEqualTo(WorkflowState.ORDER_CREATED);
        assertThat(w.updatedAt()).isNotNull();
    }

    @Test
    void transitionTo_doesNotMutateOriginal() {
        Workflow original = base();
        Workflow updated = original.transitionTo(WorkflowState.ORDER_CREATED);
        assertThat(original.state()).isEqualTo(WorkflowState.STARTED);
        assertThat(updated.state()).isEqualTo(WorkflowState.ORDER_CREATED);
    }

    @Test
    void fail_setsFAILEDAndError() {
        Workflow w = base().fail("something went wrong");
        assertThat(w.state()).isEqualTo(WorkflowState.FAILED);
        assertThat(w.lastError()).isEqualTo("something went wrong");
        assertThat(w.updatedAt()).isNotNull();
    }

    @Test
    void compensating_setsCOMPENSATINGAndError() {
        Workflow w = base().compensating("compensation needed");
        assertThat(w.state()).isEqualTo(WorkflowState.COMPENSATING);
        assertThat(w.lastError()).isEqualTo("compensation needed");
        assertThat(w.updatedAt()).isNotNull();
    }
}