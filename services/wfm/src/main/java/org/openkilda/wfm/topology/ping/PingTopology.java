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

package org.openkilda.wfm.topology.ping;

import org.openkilda.pce.provider.Auth;
import org.openkilda.wfm.ConfigurationException;
import org.openkilda.wfm.LaunchEnvironment;
import org.openkilda.wfm.NameCollisionException;
import org.openkilda.wfm.topology.AbstractTopology;
import org.openkilda.wfm.topology.Topology;
import org.openkilda.wfm.topology.ping.bolt.FloodlightDecoder;
import org.openkilda.wfm.topology.ping.bolt.FloodlightEncoder;
import org.openkilda.wfm.topology.ping.bolt.FlowKeeper;
import org.openkilda.wfm.topology.ping.bolt.MonotonicTick;
import org.openkilda.wfm.topology.ping.bolt.PingManager;
import org.openkilda.wfm.topology.ping.bolt.PingTick;
import org.openkilda.wfm.topology.ping.bolt.RequestProducer;
import org.openkilda.wfm.topology.ping.bolt.ResponseConsumer;

import org.apache.storm.generated.StormTopology;
import org.apache.storm.topology.TopologyBuilder;

public class PingTopology extends AbstractTopology {
    public static final String TOPOLOGY_ID = "flowping";

    public static final String SPOUT_FLOODLIGHT_IN_ID = "floodlight.kafka.in";
    public static final String BOLT_FLOODLIGHT_OUT_ID = "floodlight.kafka.out";

    protected PingTopology(LaunchEnvironment env) throws ConfigurationException {
        super(env);
    }

    @Override
    public StormTopology createTopology() throws NameCollisionException {
        TopologyBuilder topology = new TopologyBuilder();

        topology.setBolt(FloodlightDecoder.BOLT_ID, new FloodlightDecoder());
        attachPingTick(topology);
        attachMonotonicTick(topology);
        attachFlowKeeper(topology);
        topology.setBolt(PingManager.BOLT_ID, new PingManager());
        topology.setBolt(RequestProducer.BOLT_ID, new RequestProducer());
        topology.setBolt(ResponseConsumer.BOLT_ID, new ResponseConsumer());
        topology.setBolt(FloodlightEncoder.BOLT_ID, new FloodlightEncoder());

        return topology.createTopology();
    }

    private void attachPingTick(TopologyBuilder topology) {
        topology.setBolt(PingTick.BOLT_ID, new PingTick(getConfig().getFlowPingInterval()));
    }

    private void attachMonotonicTick(TopologyBuilder topology) {
        topology.setBolt(MonotonicTick.BOLT_ID, new MonotonicTick());
    }

    private void attachFlowKeeper(TopologyBuilder topology) {
        Auth pceAuth = config.getPathComputerAuth();
        topology.setBolt(FlowKeeper.BOLT_ID, new FlowKeeper(pceAuth))
                .allGrouping(PingTick.BOLT_ID);
    }

    @Override
    public String makeTopologyName() {
        return TOPOLOGY_ID;
    }
}
