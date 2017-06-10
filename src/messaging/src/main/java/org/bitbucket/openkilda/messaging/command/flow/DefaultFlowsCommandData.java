package org.bitbucket.openkilda.messaging.command.flow;

import org.bitbucket.openkilda.messaging.command.CommandData;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Defines the payload payload of a Message representing an command for default flows installation.
 */
@JsonSerialize
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "command",
        "switch_id"})
public class DefaultFlowsCommandData extends CommandData {
    /**
     * Serialization version number constant.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Switch id for default flows installation.
     */
    @JsonProperty("switch_id")
    protected String switchId;

    /**
     * Default constructor.
     */
    public DefaultFlowsCommandData() {
    }

    /**
     * Instance constructor.
     *
     * @param switchId switch id to install default flows on
     */
    @JsonCreator
    public DefaultFlowsCommandData(@JsonProperty("switch_id") final String switchId) {
        this.switchId = switchId;
    }

    /**
     * Returns switch id.
     *
     * @return switch id
     */
    public String getSwitchId() {
        return switchId;
    }

    /**
     * Sets switch id.
     *
     * @param switchId switch id to set
     */
    public void setSwitchId(final String switchId) {
        this.switchId = switchId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return switchId;
    }
}
