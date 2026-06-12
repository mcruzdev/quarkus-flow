package io.quarkiverse.flow.persistence.jpa;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import io.serverlessworkflow.impl.WorkflowModel;

@Entity
@DiscriminatorValue("1")
public class CompletedTaskEntity extends TaskInfoEntity {

    @Column
    private Instant instant;
    private WorkflowModel model;
    @Column
    private WorkflowModel context;
    private boolean isEndNode;
    private String nextPosition;

    public CompletedTaskEntity() {
    }

    public CompletedTaskEntity(TaskInfoKey key, Instant instant, WorkflowModel model, WorkflowModel context,
            boolean isEndNode,
            String nextPosition) {
        super(key);
        this.instant = instant;
        this.model = model;
        this.context = context;
        this.isEndNode = isEndNode;
        this.nextPosition = nextPosition;
    }

    public Instant getInstant() {
        return instant;
    }

    public WorkflowModel getModel() {
        return model;
    }

    public WorkflowModel getContext() {
        return context;
    }

    public boolean isEndNode() {
        return isEndNode;
    }

    public String getNextPosition() {
        return nextPosition;
    }

}
