package org.bitbucket.openkilda.messaging.info.flow;

import static com.google.common.base.MoreObjects.toStringHelper;

import org.bitbucket.openkilda.messaging.info.InfoData;
import org.bitbucket.openkilda.messaging.payload.response.FlowsStatusResponsePayload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;

/**
 * Represents flows status northbound response.
 */
@JsonSerialize
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "command",
        "destination",
        "payload"})
public class FlowsStatusResponse extends InfoData {
    /**
     * Serialization version number constant.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The response payload.
     */
    @JsonProperty("payload")
    protected FlowsStatusResponsePayload payload;

    /**
     * Default constructor.
     */
    public FlowsStatusResponse() {
    }

    /**
     * Constructs instance.
     *
     * @param   payload  response payload
     *
     * @throws  IllegalArgumentException if payload is null
     */
    @JsonCreator
    public FlowsStatusResponse(@JsonProperty("payload") final FlowsStatusResponsePayload payload) {
        setPayload(payload);
    }

    /**
     * Returns response payload.
     *
     * @return  response payload
     */
    public FlowsStatusResponsePayload getPayload() {
        return payload;
    }

    /**
     * Sets response payload.
     *
     * @param   payload  response payload
     */
    public void setPayload(final FlowsStatusResponsePayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("need to set payload");
        }
        this.payload = payload;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toStringHelper(this)
                .add("payload", payload)
                .toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(payload);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        FlowsStatusResponse that = (FlowsStatusResponse) object;
        return Objects.equals(getPayload(), that.getPayload());
    }
}