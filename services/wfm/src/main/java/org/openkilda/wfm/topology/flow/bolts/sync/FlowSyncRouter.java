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

import org.openkilda.messaging.Utils;
import org.openkilda.messaging.info.InfoMessage;
import org.openkilda.messaging.info.flow.FlowInfoData;
import org.openkilda.wfm.AbstractBolt;
import org.openkilda.wfm.error.AbstractException;
import org.openkilda.wfm.error.JsonDecodeException;
import org.openkilda.wfm.error.PipelineException;
import org.openkilda.wfm.topology.flow.ComponentType;
import org.openkilda.wfm.topology.flow.bolts.CrudBolt;

import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.io.IOException;

public class FlowSyncRouter extends AbstractBolt {
    public static final String BOLT_ID = ComponentType.FLOW_SYNC_ROUTER.toString();

    public static final String FIELD_ID_FLOW_ID = Utils.FLOW_ID;
    public static final String FIELD_ID_FLOW = "flow";

    private static final Fields STREAM_FIELDS = new Fields(FIELD_ID_FLOW_ID, FIELD_ID_FLOW);

    @Override
    protected void handleInput(Tuple input) throws AbstractException {
        FlowInfoData payload = decode(input);
        getOutput().emit(input, new Values(payload.getFlowId(), payload));
    }

    private FlowInfoData decode(Tuple input) throws AbstractException {
        String json = input.getStringByField(CrudBolt.FIELD_ID_MESSAGE);
        FlowInfoData payload;
        try {
            InfoMessage message = Utils.MAPPER.readValue(json, InfoMessage.class);
            payload = (FlowInfoData) message.getData();
        } catch (ClassCastException e) {
            throw new PipelineException(this, input, CrudBolt.FIELD_ID_MESSAGE, e.toString());
        } catch (IOException e) {
            throw new JsonDecodeException(InfoMessage.class, json, e);
        }
        return payload;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputManager) {
        outputManager.declare(STREAM_FIELDS);
    }
}
