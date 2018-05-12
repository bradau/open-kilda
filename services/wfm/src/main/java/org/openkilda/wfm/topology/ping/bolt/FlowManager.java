/*
 * Copyright 2018 Telstra Open Source
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openkilda.wfm.topology.ping.bolt;

import org.openkilda.messaging.model.BiFlow;
import org.openkilda.pce.provider.Auth;
import org.openkilda.wfm.AbstractBolt;
import org.openkilda.wfm.share.utils.PathComputerFlowFetcher;

import lombok.extern.log4j.Log4j2;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public class FlowManager extends AbstractBolt {
    public static final String BOLT_ID = ComponentId.FLOW_MANAGER.toString();

    public static final String FIELD_ID_FLOW_ID = "flow_id";

    public static final String STREAM_PING_ID = "ping";
    public static final Fields STREAM_PING_FIELDS = new Fields(FIELD_ID_FLOW_ID);

    private final HashMap<String, BiFlow> flows = new HashMap<>();

    @Override
    protected void handleInput(Tuple input) {
        String source = input.getSourceComponent();

        if (PingTick.BOLT_ID.equals(source)) {
        } else {
            unhandledInput(input);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputManager) {
        outputManager.declareStream(STREAM_PING_ID, STREAM_PING_FIELDS);
    }
}
