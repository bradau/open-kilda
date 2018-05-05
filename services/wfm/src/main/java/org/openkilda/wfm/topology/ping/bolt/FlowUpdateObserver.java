/*
 * Copyright 2017 Telstra Open Source
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

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.openkilda.messaging.model.BiFlow;
import org.openkilda.pce.provider.Auth;
import org.openkilda.wfm.AbstractBolt;
import org.openkilda.wfm.share.utils.PathComputerFlowFetcher;

import java.util.LinkedList;
import java.util.Map;

public class FlowUpdateObserver extends AbstractBolt {
    public static final String BOLT_ID = "flow_update_observer";

    public static final String FIELD_ID_FLOW_ID = "flow_id";
    public static final String FIELD_ID_ACTION = "action";
    public static final String FIELD_ID_PAYLOAD = "payload";
    public static final Fields STREAM_FIELDS = new Fields(FIELD_ID_FLOW_ID, FIELD_ID_ACTION, FIELD_ID_PAYLOAD);

    public static enum Action {
        ADD,
        REMOVE,
        PING_SUCCESS,
        PING_ERROR
    }

    private final LinkedList<BiFlow> pending = new LinkedList<>();
    private final Auth pceAuth;

    public FlowUpdateObserver(Auth pceAuth) {
        this.pceAuth = pceAuth;
    }

    @Override
    protected void handleInput(Tuple input) {
        String source = input.getSourceComponent();

        if (MonotonicTick.BOLT_ID.equals(source)) {
            doMonotonicTick(input);
        } else {
            unhandledInput(input);
        }
    }

    private void doMonotonicTick(Tuple input) {
        sendPending(input);
    }

    private void sendPending(Tuple input) {
        for (BiFlow biFlow : pending) {
            getOutput().emit(input, new Values(biFlow.getFlowId(), Action.ADD, biFlow));
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputManager) {
        outputManager.declare(STREAM_FIELDS);
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        super.prepare(stormConf, context, collector);
        initialFetch();
    }

    private void initialFetch() {
        PathComputerFlowFetcher flowFetcher = new PathComputerFlowFetcher(pceAuth.connect());
        pending.addAll(flowFetcher.getFlows());
    }
}
