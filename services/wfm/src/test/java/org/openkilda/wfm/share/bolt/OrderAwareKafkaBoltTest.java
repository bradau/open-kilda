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

package org.openkilda.wfm.share.bolt;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.storm.kafka.bolt.selector.DefaultTopicSelector;
import org.apache.storm.kafka.bolt.selector.KafkaTopicSelector;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.TupleImpl;
import org.apache.storm.tuple.Values;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.neo4j.helpers.TimeUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class OrderAwareKafkaBoltTest {
    private OrderAwareKafkaBolt<String, String> subject;

    private final int sourceTaskId = 16;
    private final String sourceTaskName = "source";
    private final String sourceStreamId = "stream";
    private final Fields streamFields = new Fields("group", "key", "message");

    @Mock
    private TopologyContext topologyContext;
    @Mock
    private OutputCollector outputCollector;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        IOrderKeyExtractor orderKeyExtractor = new TupleToOrderKeyMapper("group");
        KafkaTopicSelector topicSelector = new DefaultTopicSelector("topic");
        subject = new OrderAwareKafkaBoltStub<>(new Properties(), orderKeyExtractor, topicSelector);
        subject.prepare(new HashMap(), topologyContext, outputCollector);

        Mockito.when(topologyContext.getComponentId(sourceTaskId)).thenReturn(sourceTaskName);
        Mockito.when(topologyContext.getComponentOutputFields(sourceTaskName, sourceStreamId)).thenReturn(streamFields);
    }

    @Test
    public void testOrdering() throws Exception {
        KafkaProducerStub<String, String> producerStub = new KafkaProducerStub<>();
        KafkaProducer<String, String> producerMock = subject.getProducer();
        Mockito.when(producerMock.send(Mockito.any())).thenAnswer(
                invocation -> producerStub.send(invocation.getArgument(0)));
        Mockito.when(producerMock.send(Mockito.any(), Mockito.any())).thenAnswer(
                invocation -> producerStub.send(invocation.getArgument(0), invocation.getArgument(1)));

        subject.execute(makeTuple(new Values("groupAAAA", "AAA", "message 0")));
        subject.execute(makeTuple(new Values("groupBBBB", "BBB", "message 0")));
        subject.execute(makeTuple(new Values("groupCCCC", "CCC", "message 0")));
        subject.execute(makeTuple(new Values("groupBBBB", "BBB", "message 0")));
        subject.execute(makeTuple(new Values("groupAAAA", "AAA", "message 0")));

        HashMap<String, Integer> keyToPartition = new HashMap<>();
        keyToPartition.put("AAA", 0);
        keyToPartition.put("BBB", 1);
        HashSet<String> seenGroups = new HashSet<>();
        for (ProducerRecord<String, String> record : producerStub.getSendHistory()) {
            Integer expect = null;
            if (!seenGroups.add(record.key())) {
                expect = keyToPartition.get(record.key());
            }

            Assert.assertEquals(
                    String.format("Send order is not preserved for group %s", record.key()),
                    expect, record.partition());
        }
    }

    private Tuple makeTuple(Values payload) {
        return new TupleImpl(topologyContext, payload, sourceTaskId, sourceStreamId);
    }
}
