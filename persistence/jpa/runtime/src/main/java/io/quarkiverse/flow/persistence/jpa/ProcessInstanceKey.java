package io.quarkiverse.flow.persistence.jpa;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;

public class ProcessInstanceKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "instance_id")
    private String instanceId;

    @Column(name = "application_id")
    private String applicationId;

    public ProcessInstanceKey() {
    }

    public ProcessInstanceKey(String instanceId, String applicationId) {
        super();
        this.instanceId = instanceId;
        this.applicationId = applicationId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId, instanceId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProcessInstanceKey other = (ProcessInstanceKey) obj;
        return Objects.equals(applicationId, other.applicationId) && Objects.equals(instanceId, other.instanceId);
    }
}
