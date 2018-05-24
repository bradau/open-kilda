/*
 * Copyright 2018 Telstra Open Source
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
import org.openkilda.messaging.model.BidirectionalFlow;
import org.openkilda.wfm.AbstractBolt;
import org.openkilda.wfm.error.AbstractException;
import org.openkilda.wfm.topology.flow.ComponentType;
import org.openkilda.wfm.topology.flow.bolts.CrudBolt;

import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;

public class FlowSyncRouter extends AbstractBolt {
    public static final String BOLT_ID = ComponentType.FLOW_SYNC_ROUTER.toString();

    public static final String FIELD_ID_FLOW_ID = Utils.FLOW_ID;
    public static final String FIELD_ID_FLOW = "flow";

    private static final Fields STREAM_FIELDS = new Fields(FIELD_ID_FLOW_ID, FIELD_ID_FLOW);

    @Override
    protected void handleInput(Tuple input) throws AbstractException {

    }

    private BidirectionalFlow decode(Tuple input) {
        String json = input.getStringByField(CrudBolt.FIELD_ID_MESSAGE);
        Message message
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputManager) {
        outputManager.declare(STREAM_FIELDS);
    }
}
