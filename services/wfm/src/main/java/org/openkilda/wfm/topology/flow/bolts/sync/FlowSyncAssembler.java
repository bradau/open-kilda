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

import org.openkilda.messaging.info.event.FlowSync;
import org.openkilda.messaging.info.event.FlowSyncCommand;
import org.openkilda.messaging.info.flow.FlowInfoData;
import org.openkilda.messaging.info.flow.FlowOperation;
import org.openkilda.messaging.model.BidirectionalFlow;
import org.openkilda.messaging.payload.flow.FlowState;
import org.openkilda.wfm.AbstractBolt;
import org.openkilda.wfm.error.AbstractException;
import org.openkilda.wfm.error.PipelineException;
import org.openkilda.wfm.topology.flow.ComponentType;

import lombok.extern.log4j.Log4j2;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@Log4j2
public class FlowSyncAssembler extends AbstractBolt {
    public static final String BOLT_ID = ComponentType.FLOW_SYNC_ASSEMBLER.toString();

    public static final String FIELD_ID_PAYLOAD = "payload";

    private static final Fields STREAM_FIELDS = new Fields(FIELD_ID_PAYLOAD);

    private HashMap<String, FlowOperation> pendingFlows;

    @Override
    protected void handleInput(Tuple input) throws AbstractException {
        FlowInfoData command = decodeCrudCommand(input);
        BidirectionalFlow flow = new BidirectionalFlow(command.getPayload());

        log.debug("Flow sync trigger (operation: {}, flowId: {})", command.getOperation(), flow.getFlowId());

        if (FlowOperation.STATE == command.getOperation()) {
            handleStateUpdate(input, flow);
        } else if (shouldWaitStatus(command.getOperation())) {
            makePendingRecord(flow, command.getOperation());
        } else {
            emit(input, makeSyncRecord(command.getOperation(), flow));
        }
    }

    private FlowInfoData decodeCrudCommand(Tuple input) throws PipelineException {
        FlowInfoData command;
        try {
            command = (FlowInfoData) input.getValueByField(FlowSyncRouter.FIELD_ID_FLOW);
        } catch (ClassCastException e) {
            throw new PipelineException(this, input, FlowSyncRouter.FIELD_ID_FLOW, e.toString());
        }
        return command;
    }

    private void handleStateUpdate(Tuple input, BidirectionalFlow flow) {
        FlowOperation operation = pendingFlows.get(flow.getFlowId());
        if (operation == null) {
            log.error("Got status update for flow {} without pending record", flow.getFlowId());
            return;
        }

        if (isPermanentStatus(flow.getState())) {
            pendingFlows.remove(flow.getFlowId());
            emit(input, makeSyncRecord(operation, flow));
        } else {
            log.debug("Got flow's {} transient status state transition - {}", flow.getFlowId(), flow.getState());
        }
    }

    private void makePendingRecord(BidirectionalFlow flow, FlowOperation operation) {
        FlowOperation current = pendingFlows.put(flow.getFlowId(), operation);
        if (current != null) {
            log.error("Interrupted flow {} operation {}", flow.getFlowId(), current);
        }
    }

    private FlowSync makeSyncRecord(FlowOperation operation, BidirectionalFlow flow) {
        FlowSync sync;
        switch (operation) {
            case PUSH:
            case CREATE:
            case PUSH_PROPAGATE:
                sync = new FlowSync(FlowSyncCommand.CREATE, flow);
                break;

            case UPDATE:
            case CACHE:
                sync = new FlowSync(FlowSyncCommand.UPDATE, flow);
                break;

            case UNPUSH:
            case DELETE:
            case UNPUSH_PROPAGATE:
                sync = new FlowSync(FlowSyncCommand.DELETE, flow);
                break;

            default:
                throw new IllegalArgumentException(
                        String.format("Can\'t translate FlowOperation.{} into flow state record", operation));
        }

        return sync;
    }

    private void emit(Tuple input, FlowSync sync) {
        log.debug(
                "Emit flow sync record (command: {}, flowId: {}, state: {})",
                sync.getCommand(), sync.getFlow().getFlowId(), sync.getFlow().getState());
        getOutput().emit(input, new Values(sync));
    }

    private boolean shouldWaitStatus(FlowOperation operation) {
        boolean result;
        switch (operation) {
            case PUSH:
            case UNPUSH:
                result = false;
                break;
            default:
                result = true;
        }

        return result;
    }

    private static boolean isPermanentStatus(FlowState state) {
        boolean result;
        switch (state) {
            case UP:
            case CACHED:
            case DOWN:
                result = true;
                break;
            default:
                result = false;
        }

        return result;
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        super.prepare(stormConf, context, collector);

        pendingFlows = new HashMap<>();
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputManager) {
        outputManager.declare(STREAM_FIELDS);
    }
}
