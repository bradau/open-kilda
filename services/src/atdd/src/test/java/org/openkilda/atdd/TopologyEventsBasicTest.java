/* Copyright 2017 Telstra Open Source
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

package org.openkilda.atdd;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.openkilda.LinksUtils;
import org.openkilda.SwitchesUtils;
import org.openkilda.messaging.info.event.IslChangeType;
import org.openkilda.messaging.info.event.IslInfoData;
import org.openkilda.messaging.info.event.PathNode;
import org.openkilda.messaging.info.event.SwitchInfoData;
import org.openkilda.messaging.info.event.SwitchState;
import org.openkilda.topo.builders.TestTopologyBuilder;

import cucumber.api.PendingException;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.junit.Before;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Created by carmine on 5/1/17.
 */
public class TopologyEventsBasicTest {

    private final RetryPolicy retryPolicy = new RetryPolicy()
            .withDelay(2, TimeUnit.SECONDS)
            .withMaxRetries(10);

    private String manipulatedLinkId;

    @Before
    public void setUp() {
        manipulatedLinkId = null;
    }

    @When("^multiple links exist between all switches$")
    public void multipleLinksExistBetweenAllSwitches() throws Exception {
        List<String> switchIds = IntStream.range(1, 6)
                .mapToObj(TestTopologyBuilder::intToSwitchId)
                .collect(Collectors.toList());
        assertTrue("Switches should have multiple links",
                getSwitchesWithoutMultipleLinks(switchIds).isEmpty());
    }

    private List<String> getSwitchesWithoutMultipleLinks(List<String> switches) throws Exception {
        List<IslInfoData> links = LinksUtils.dumpLinks();

        return switches.stream()
                .filter(sw -> isSwitchHasLessThanTwoLinks(sw, links))
                .collect(Collectors.toList());
    }

    @When("^a link is dropped in the middle$")
    public void linkIsDroppedInTheMiddle() throws Exception {
        List<IslInfoData> links = LinksUtils.dumpLinks();
        IslInfoData middleLink = getMiddleLink(links);

        PathNode node = middleLink.getPath().get(0);
        assertTrue(LinksUtils.islFail(getSwitchName(node.getSwitchId()), String.valueOf(node.getPortNo())));

        manipulatedLinkId = middleLink.getId();
    }

    @Then("^the link disappears from the topology engine in (\\d+) seconds\\.$")
    public void theLinkDisappearsFromTheTopologyEngine(int timeout) throws Exception {
        List<String> cutLinks = Failsafe.with(retryPolicy
                .retryIf(links -> links instanceof List && ((List) links).isEmpty()))
                .get(() -> LinksUtils.dumpLinks().stream()
                        .filter(isl -> isl.getState() != IslChangeType.DISCOVERED)
                        .map(IslInfoData::getId)
                        .collect(Collectors.toList())
                );

        assertFalse("Link should be cut", cutLinks.isEmpty());
        assertThat("Only one link should be cut", cutLinks, hasItems(manipulatedLinkId));
    }

    @When("^a link is added in the middle$")
    public void linkIsAddedInTheMiddle() throws Exception {
        List<IslInfoData> links = LinksUtils.dumpLinks();
        IslInfoData middleLink = getMiddleLink(links);

        String srcSwitch = getSwitchName(middleLink.getPath().get(0).getSwitchId());
        String dstSwitch = getSwitchName(middleLink.getPath().get(1).getSwitchId());
        assertTrue("Link is not added", LinksUtils.addLink(srcSwitch, dstSwitch));

        manipulatedLinkId = middleLink.getId();
    }

    @Then("^the link appears in the topology engine in (\\d+) seconds\\.$")
    public void theLinkAppearsInTheTopologyEngine(int timeout) throws Exception {
        Optional<IslInfoData> theLink = Failsafe.with(retryPolicy
                .retryIf(link -> ((Optional) link).isPresent()))
                .get(() -> LinksUtils.dumpLinks().stream()
                        .filter(isl -> isl.getId().equals(manipulatedLinkId))
                        .findAny()
                );

        assertTrue("Link should be present", theLink.isPresent());
        assertThat("Link should have health check", theLink.get().getState(), equalTo(IslChangeType.DISCOVERED));
    }

    @When("^a switch is dropped in the middle$")
    public void switchIsDroppedInTheMiddle() throws Exception {
        List<SwitchInfoData> switches = SwitchesUtils.dumpSwitches();
        SwitchInfoData middleSwitch = getMiddleSwitch(switches);
        assertTrue("Should successfully knockout switch",
                SwitchesUtils.knockoutSwitch(getSwitchName(middleSwitch.getSwitchId())));

        TimeUnit.SECONDS.sleep(1);
        List<SwitchInfoData> updatedSwitches = SwitchesUtils.dumpSwitches();
        SwitchInfoData deactivatedSwitch = updatedSwitches.stream()
                .filter(sw -> sw.getSwitchId().equalsIgnoreCase(middleSwitch.getSwitchId()))
                .findFirst().orElseThrow(() -> new IllegalStateException("Switch should exist"));
        assertThat(deactivatedSwitch.getState(), is(SwitchState.DEACTIVATED));
    }

    @Then("^all links through the dropped switch will have no health checks$")
    public void allLinksThroughTheDroppedSwitchWillHaveNoHealthChecks() throws Exception {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @Then("^the links disappear from the topology engine\\.$")
    public void theLinksDisappearFromTheTopologyEngine() throws Exception {
        //todo check whether we need to wait until links will disappear or
        // we might delete them instantly when switch goes down
        TimeUnit.SECONDS.sleep(15);
        final SwitchInfoData middleSwitch = getMiddleSwitch(SwitchesUtils.dumpSwitches());
        final List<IslInfoData> links = LinksUtils.dumpLinks();

        List<IslInfoData> switchLinks = links.stream()
                .filter(isl -> isLinkBelongToSwitch(middleSwitch.getSwitchId(), isl))
                .filter(isl -> isl.getState() == IslChangeType.DISCOVERED)
                .collect(Collectors.toList());
        assertTrue("Switch shouldn't have any active links", switchLinks.isEmpty());
    }

    @Then("^the switch disappears from the topology engine\\.$")
    public void theSwitchDisappearsFromTheTopologyEngine() throws Exception {
        List<SwitchInfoData> switches = SwitchesUtils.dumpSwitches();
        SwitchInfoData middleSwitch = getMiddleSwitch(switches);

        //right now switch doesn't disappear in neo4j - we just update status
        assertThat(middleSwitch.getState(), is(SwitchState.DEACTIVATED));
    }

    @When("^a switch is added at the edge$")
    public void switchIsAddedAtTheEdge() throws Exception {
        assertTrue("Should add switch to mininet topology",
                SwitchesUtils.addSwitch("01010001", "DEADBEEF01010001"));
        TimeUnit.SECONDS.sleep(1);
    }

    @When("^links are added between the new switch and its neighbor$")
    public void linksAreAddedBetweenTheNewSwitchAndItsNeighbor() throws Exception {
        List<IslInfoData> links = LinksUtils.dumpLinks();
        List<SwitchInfoData> switches = SwitchesUtils.dumpSwitches();

        SwitchInfoData switchWithoutLinks = switches.stream()
                .filter(sw -> links.stream()
                        .anyMatch(isl -> isLinkBelongToSwitch(sw.getSwitchId(), isl)))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("At least one switch should exist"));

        SwitchInfoData latestConnectedSwitch = switches.stream()
                .sorted(Comparator.comparing(SwitchInfoData::getSwitchId).reversed())
                .findFirst().get();

        assertTrue(LinksUtils.addLink(getSwitchName(switchWithoutLinks.getSwitchId()),
                getSwitchName(latestConnectedSwitch.getSwitchId())));
        assertTrue(LinksUtils.addLink(getSwitchName(switchWithoutLinks.getSwitchId()),
                getSwitchName(latestConnectedSwitch.getSwitchId())));
        TimeUnit.SECONDS.sleep(1);
    }

    @Then("^all links through the added switch will have health checks$")
    public void allLinksThroughTheAddedSwitchWillHaveHealthChecks() throws Exception {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @Then("^now amount of switches is (\\d+)\\.$")
    public void theSwitchAppearsInTheTopologyEngine(int switches) throws Exception {
        List<SwitchInfoData> switchList = SwitchesUtils.dumpSwitches();
        List<SwitchInfoData> activeSwitches = switchList.stream()
                .filter(sw -> sw.getState() == SwitchState.ACTIVATED)
                .collect(Collectors.toList());

        assertThat("Switch should disappear from neo4j", activeSwitches.size(), is(switches));
    }

    private boolean isSwitchHasLessThanTwoLinks(String switchId, List<IslInfoData> links) {
        int inputsAmount = 0;
        int outputsAmount = 0;
        for (IslInfoData isl : links) {
            for (PathNode node : isl.getPath()) {
                if (switchId.equalsIgnoreCase(node.getSwitchId())) {
                    if (node.getSeqId() == 0) {
                        outputsAmount++;
                    } else if (node.getSeqId() == 1) {
                        inputsAmount++;
                    }
                }
            }
        }

        //check whether switch has more than one link in both direction (sequence id 0 and 1)
        return inputsAmount <= NumberUtils.INTEGER_ONE && outputsAmount <= NumberUtils.INTEGER_ONE;
    }

    private IslInfoData getMiddleLink(List<IslInfoData> links) {
        return links.stream()
                .sorted(Comparator.comparing((isl) -> isl.getPath().get(0).getSwitchId()))
                .collect(Collectors.toList())
                .get((links.size() / 2) + 1);
    }

    private SwitchInfoData getMiddleSwitch(List<SwitchInfoData> switches) {
        return switches.stream()
                .sorted(Comparator.comparing(SwitchInfoData::getSwitchId))
                .collect(Collectors.toList())
                .get(switches.size() / 2);
    }

    private String getSwitchName(String switchId) {
        return switchId.replaceAll("[^\\d]", StringUtils.EMPTY);
    }

    private boolean isLinkBelongToSwitch(String switchId, IslInfoData isl) {
        return switchId.equalsIgnoreCase(isl.getPath().get(0).getSwitchId())
                || switchId.equalsIgnoreCase(isl.getPath().get(1).getSwitchId());
    }
}
