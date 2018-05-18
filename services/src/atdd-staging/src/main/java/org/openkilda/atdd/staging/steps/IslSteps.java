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

package org.openkilda.atdd.staging.steps;

import cucumber.api.java.After;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.openkilda.atdd.staging.model.topology.TopologyDefinition;
import org.openkilda.atdd.staging.service.aswitch.ASwitchService;
import org.openkilda.atdd.staging.service.aswitch.model.ASwitchFlow;
import org.openkilda.atdd.staging.service.northbound.NorthboundService;
import org.openkilda.atdd.staging.steps.helpers.TopologyUnderTest;
import org.openkilda.messaging.info.event.IslChangeType;
import org.openkilda.northbound.dto.PathDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class IslSteps {

    @Autowired
    private NorthboundService northboundService;

    @Autowired
    private ASwitchService aSwitchService;

    @Autowired
    @Qualifier("topologyUnderTest")
    private TopologyUnderTest topologyUnderTest;

    List<TopologyDefinition.Isl> changedIsls = new ArrayList<>();

    @Autowired
    @Qualifier("topologyEngineRetryPolicy")
    private RetryPolicy retryPolicy;

    @When("ISL between switches goes down")
    public void transitIslDown() {
        topologyUnderTest.getFlowIsls().forEach((flow, isls) -> {
            TopologyDefinition.Isl islToRemove = isls.stream().filter(isl -> isl.getASwitch() != null).findFirst().get();
            TopologyDefinition.ASwitch aSwitch = islToRemove.getASwitch();
            ASwitchFlow aSwFlowForward = new ASwitchFlow(aSwitch.getInPort(), aSwitch.getOutPort());
            ASwitchFlow aSwFlowReverese = new ASwitchFlow(aSwitch.getOutPort(), aSwitch.getInPort());
            aSwitchService.removeFlow(aSwFlowForward);
            aSwitchService.removeFlow(aSwFlowReverese);
            changedIsls.add(islToRemove);
        });
    }

    @When("Changed ISLs? go(?:es)? up")
    public void transitIslUp() {
        changedIsls.forEach(isl -> {
            TopologyDefinition.ASwitch aSwitch = isl.getASwitch();
            ASwitchFlow aSwFlowForward = new ASwitchFlow(aSwitch.getInPort(), aSwitch.getOutPort());
            ASwitchFlow aSwFlowReverese = new ASwitchFlow(aSwitch.getOutPort(), aSwitch.getInPort());
            aSwitchService.addFlow(aSwFlowForward);
            aSwitchService.addFlow(aSwFlowReverese);
        });
    }

    @Then("ISLs? status changes? to (.*)")
    public void waitForIslStatus(String islStatus) {
        IslChangeType expectedIslState = IslChangeType.valueOf(islStatus);
        changedIsls.forEach(isl -> {
            IslChangeType actualIslState = Failsafe.with(retryPolicy
                    .retryIf(state -> state != expectedIslState))
                    .get(() -> northboundService.getAllLinks().stream().filter(link -> {
                        PathDto src = link.getPath().get(0);
                        PathDto dst = link.getPath().get(1);
                        return src.getPortNo() == isl.getSrcPort() && dst.getPortNo() == isl.getDstPort();
                    }).findFirst().get().getState());
            assertEquals(expectedIslState, actualIslState);
        });
    }

    @After( {"@requires_cleanup", "@cuts_out_isls"})
    public void bringIslBack() {
        transitIslUp();
    }
}
