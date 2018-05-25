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

import org.openkilda.messaging.info.flow.FlowInfoData;
import org.openkilda.wfm.AbstractBolt;
import org.openkilda.wfm.error.AbstractException;
import org.openkilda.wfm.error.PipelineException;
import org.openkilda.wfm.topology.flow.ComponentType;

import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Tuple;

public class FlowSyncAssembler extends AbstractBolt {
    public static final String BOLT_ID = ComponentType.FLOW_SYNC_ASSEMBLER.toString();

    @Override
    protected void handleInput(Tuple input) throws AbstractException {
        FlowInfoData command = decodeCrudCommand(input);

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

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {

    }
}
