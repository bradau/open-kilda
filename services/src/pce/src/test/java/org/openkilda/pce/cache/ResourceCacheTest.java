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

package org.openkilda.pce.cache;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import edu.emory.mathcs.backport.java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openkilda.messaging.info.event.PathInfoData;
import org.openkilda.messaging.model.Flow;
import org.openkilda.messaging.model.ImmutablePair;
import org.openkilda.messaging.payload.flow.FlowState;
import org.openkilda.pce.NetworkTopologyConstants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ResourceCacheTest {
    private static final String SWITCH_ID = "switch-id";
    private static final String SWITCH_ID_2 = "switch-id-2";
    private final Flow forwardCreatedFlow = new Flow("created-flow", 0, false, 10L, "description",
            "timestamp", "sw3", "sw4", 21, 22, 100, 200, 4, 4, new PathInfoData(), FlowState.ALLOCATED);
    private final Flow reverseCreatedFlow = new Flow("created-flow", 0, false, 10L, "description",
            "timestamp", "sw4", "sw3", 22, 21, 200, 100, 5, 5, new PathInfoData(), FlowState.ALLOCATED);
    private ResourceCache resourceCache;

    @After
    public void tearDown() throws Exception {
        resourceCache.clear();
    }

    @Before
    public void setUp() throws Exception {
        resourceCache = new ResourceCache();
    }

    @Test
    public void allocateAll() throws Exception {
        // TODO
    }

    @Test
    public void cookiePool() throws Exception {
        resourceCache.allocateCookie(4);

        int first = resourceCache.allocateCookie();
        assertEquals(5, first);

        int second = resourceCache.allocateCookie();
        assertEquals(6, second);

        int third = resourceCache.allocateCookie();
        assertEquals(7, third);

        resourceCache.deallocateCookie(second);
        int fourth = resourceCache.allocateCookie();
        assertEquals(8, fourth);

        assertEquals(4, resourceCache.getAllCookies().size());

        int fifth = resourceCache.allocateCookie();
        assertEquals(9, fifth);
    }

    @Test
    public void vlanIdPool() throws Exception {
        resourceCache.allocateVlanId(5);

        int first = resourceCache.allocateVlanId();
        assertEquals(6, first);

        int second = resourceCache.allocateVlanId();
        assertEquals(7, second);

        int third = resourceCache.allocateVlanId();
        assertEquals(8, third);

        resourceCache.deallocateVlanId(second);
        int fourth = resourceCache.allocateVlanId();
        assertEquals(9, fourth);

        assertEquals(4, resourceCache.getAllVlanIds().size());

        int fifth = resourceCache.allocateVlanId();
        assertEquals(10, fifth);
    }

    @Test
    public void meterIdPool() throws Exception {
        resourceCache.allocateMeterId(SWITCH_ID, 4);
        int m1 = ResourceCache.MIN_METER_ID;

        int first = resourceCache.allocateMeterId(SWITCH_ID);
        assertEquals(m1, first);

        int second = resourceCache.allocateMeterId(SWITCH_ID);
        assertEquals(m1+1, second);

        int third = resourceCache.allocateMeterId(SWITCH_ID);
        assertEquals(m1+2, third);

        resourceCache.deallocateMeterId(SWITCH_ID, second);
        int fourth = resourceCache.allocateMeterId(SWITCH_ID);
        assertEquals(m1+3, fourth);

        assertEquals(4, resourceCache.getAllMeterIds(SWITCH_ID).size());

        int fifth = resourceCache.allocateMeterId(SWITCH_ID);
        assertEquals(m1+4, fifth);

        assertEquals(5, resourceCache.deallocateMeterId(SWITCH_ID).size());
        assertEquals(0, resourceCache.getAllMeterIds(SWITCH_ID).size());
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void vlanPoolFullTest() {
        resourceCache.allocateVlanId();
        int i = ResourceCache.MIN_VLAN_ID;
        while (i++ <= ResourceCache.MAX_VLAN_ID) {
            resourceCache.allocateVlanId();
        }
    }


// (crimi - 2018.04.06  ... Don't do this ... cookie pool is massive
//
//    @Test(expected = ArrayIndexOutOfBoundsException.class)
//    public void cookiePoolFullTest() {
//        resourceCache.allocateCookie();
//        int i = ResourceCache.MIN_COOKIE;
//        while (i++ <= ResourceCache.MAX_COOKIE) {
//            resourceCache.allocateCookie();
//        }
//    }
//
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void meterIdPoolFullTest() {
        resourceCache.allocateMeterId(SWITCH_ID);
        int i = ResourceCache.MIN_METER_ID;
        while (i++ <= ResourceCache.MAX_METER_ID) {
            resourceCache.allocateMeterId(SWITCH_ID);
        }
    }

    @Test
    public void allocateFlow() throws Exception {
        resourceCache.allocateFlow(new ImmutablePair<>(forwardCreatedFlow, reverseCreatedFlow));
        resourceCache.allocateFlow(new ImmutablePair<>(forwardCreatedFlow, reverseCreatedFlow));

        Set<Integer> allocatedCookies = resourceCache.getAllCookies();
        Set<Integer> allocatedVlanIds = resourceCache.getAllVlanIds();
        Set<Integer> allocatedMeterIds = new HashSet<>();

        allocatedMeterIds.addAll(resourceCache.getAllMeterIds(
                NetworkTopologyConstants.sw3.getSwitchId()));
        allocatedMeterIds.addAll(resourceCache.getAllMeterIds(
                NetworkTopologyConstants.sw4.getSwitchId()));

        Set<Integer> expectedCookies = new HashSet<>(Arrays.asList(
                (int) forwardCreatedFlow.getCookie(),
                (int) reverseCreatedFlow.getCookie()));

        Set<Integer> expectedVlanIds = new HashSet<>(Arrays.asList(
                forwardCreatedFlow.getTransitVlan(),
                reverseCreatedFlow.getTransitVlan()));

        Set<Integer> expectedMeterIds = new HashSet<>(Arrays.asList(
                forwardCreatedFlow.getMeterId(),
                reverseCreatedFlow.getMeterId()));

        assertEquals(expectedCookies, allocatedCookies);
        assertEquals(expectedVlanIds, allocatedVlanIds);
        assertEquals(expectedMeterIds, allocatedMeterIds);
    }

    @Test
    public void deallocateFlow() throws Exception {
        allocateFlow();
        resourceCache.deallocateFlow(new ImmutablePair<>(forwardCreatedFlow, reverseCreatedFlow));
        resourceCache.deallocateFlow(new ImmutablePair<>(forwardCreatedFlow, reverseCreatedFlow));

        Set<Integer> allocatedCookies = resourceCache.getAllCookies();
        Set<Integer> allocatedVlanIds = resourceCache.getAllVlanIds();
        Set<Integer> allocatedMeterIds = resourceCache.getAllMeterIds(
                NetworkTopologyConstants.sw3.getSwitchId());

        assertEquals(Collections.emptySet(), allocatedCookies);
        assertEquals(Collections.emptySet(), allocatedVlanIds);
        assertEquals(Collections.emptySet(), allocatedMeterIds);
    }

    @Test
    public void shouldSkipDellocateMeterPoolIfSwitchNotFound(){
        // given
        final String switchId = format("%s-%s", SWITCH_ID, UUID.randomUUID());

        // then
        assertNull(resourceCache.deallocateMeterId(switchId));
    }

    @Test
    public void shouldSkipDellocateMeterIdIfSwitchNotFound(){
        // given
        final String switchId = format("%s-%s", SWITCH_ID, UUID.randomUUID());

        // then
        assertNull(resourceCache.deallocateMeterId(switchId, forwardCreatedFlow.getMeterId()));
    }

    @Test
    public void getAllMeterIds(){
        int first = resourceCache.allocateMeterId(SWITCH_ID);
        assertEquals(ResourceCache.MIN_METER_ID, first);

        int second = resourceCache.allocateMeterId(SWITCH_ID);
        assertEquals(ResourceCache.MIN_METER_ID+1, second);

        int third = resourceCache.allocateMeterId(SWITCH_ID);
        assertEquals(ResourceCache.MIN_METER_ID+2, third);

        first = resourceCache.allocateMeterId(SWITCH_ID_2);
        assertEquals(ResourceCache.MIN_METER_ID, first);

        second = resourceCache.allocateMeterId(SWITCH_ID_2);
        assertEquals(ResourceCache.MIN_METER_ID+1, second);

        Map<String, Set<Integer>> allMeterIds = resourceCache.getAllMeterIds();
        assertEquals(2, allMeterIds.size());
        assertEquals(3, allMeterIds.get(SWITCH_ID).size());
        assertEquals(2, allMeterIds.get(SWITCH_ID_2).size());
    }


    @Test
    public void earlyMeterIds(){
        /*
         * Test that we can add a meter id less than the minimum, assuming the minimum is > 1.
         */
        int m1 = ResourceCache.MIN_METER_ID;
        int first = resourceCache.allocateMeterId(SWITCH_ID);
        assertEquals(m1, first);

        resourceCache.allocateMeterId(SWITCH_ID,m1-1);
        assertEquals(2, resourceCache.getAllMeterIds(SWITCH_ID).size());
        assertTrue(resourceCache.getAllMeterIds(SWITCH_ID).contains(m1-1));

        /*
         * verify that if we delete all, and then request a new vlan, it starts at min.
         */
        resourceCache.deallocateMeterId(SWITCH_ID,m1);
        resourceCache.deallocateMeterId(SWITCH_ID,m1-1);
        first = resourceCache.allocateMeterId(SWITCH_ID);
        assertEquals(m1+1, first);
    }


}
