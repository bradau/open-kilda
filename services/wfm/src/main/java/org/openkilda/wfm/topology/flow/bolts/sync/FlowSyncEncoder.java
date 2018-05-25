/* Copyright 2018 Telstra Open Source
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.openkilda.wfm.topology.flow.bolts.sync;

import static org.openkilda.messaging.Utils.MAPPER;

import org.openkilda.messaging.Utils;
import org.openkilda.messaging.info.event.FlowSync;
import org.openkilda.wfm.AbstractBolt;
import org.openkilda.wfm.error.AbstractException;
import org.openkilda.wfm.error.JsonEncodeException;
import org.openkilda.wfm.error.PipelineException;
import org.openkilda.wfm.topology.flow.ComponentType;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.storm.kafka.bolt.mapper.FieldNameBasedTupleToKafkaMapper;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

public class FlowSyncEncoder extends AbstractBolt {
    public static final String BOLT_ID = ComponentType.FLOW_SYNC_ENCODER.toString();

    public static final String FIELD_ID_FLOW_ID = Utils.FLOW_ID;
    public static final String FIELD_ID_KEY = FieldNameBasedTupleToKafkaMapper.BOLT_KEY;
    public static final String FIELD_ID_PAYLOAD = FieldNameBasedTupleToKafkaMapper.BOLT_MESSAGE;

    public static final Fields STREAM_FIELDS = new Fields(FIELD_ID_FLOW_ID, FIELD_ID_PAYLOAD, FIELD_ID_KEY);

    @Override
    protected void handleInput(Tuple input) throws AbstractException {
        FlowSync syncRecord = decode(input);
        emit(input, syncRecord.getFlow().getFlowId(), encode(syncRecord));
    }

    private FlowSync decode(Tuple input) throws PipelineException {
        FlowSync record;
        try {
            record = (FlowSync) input.getValueByField(FlowSyncAssembler.FIELD_ID_PAYLOAD);
        } catch (ClassCastException e) {
            throw new PipelineException(this, input, FlowSyncAssembler.FIELD_ID_PAYLOAD, e.toString());
        }
        return record;
    }

    private String encode(FlowSync event) throws JsonEncodeException {
        String json;
        try {
            json = MAPPER.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new JsonEncodeException(event, e);
        }
        return json;
    }

    private void emit(Tuple input, String flowId, String encodedEvent) {
        Values values = new Values(flowId, encodedEvent, null);
        getOutput().emit(input, values);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputManager) {
        outputManager.declare(STREAM_FIELDS);
    }
}
