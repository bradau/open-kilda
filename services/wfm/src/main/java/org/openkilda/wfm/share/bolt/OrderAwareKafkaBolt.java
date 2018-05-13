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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.storm.kafka.bolt.mapper.FieldNameBasedTupleToKafkaMapper;
import org.apache.storm.kafka.bolt.mapper.TupleToKafkaMapper;
import org.apache.storm.kafka.bolt.selector.KafkaTopicSelector;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Tuple;
import org.openkilda.wfm.AbstractBolt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Log4j2
@Accessors(chain = true)
public class OrderAwareKafkaBolt<K, V> extends AbstractBolt {
    @Setter
    private long timeWindow = TimeUnit.MINUTES.toMillis(2);

    private final Properties producerProperties;
    private final IOrderKeyExtractor orderKeyExtractor;
    private final KafkaTopicSelector topicSelector;
    private final TupleToKafkaMapper<K, V> payloadExtractor;

    private final HashMap<Object, OrderKey> partitionsMapping = new HashMap<>();

    @Getter(AccessLevel.PACKAGE)
    private KafkaProducer<K, V> producer;

    public OrderAwareKafkaBolt(
            Properties producerProperties, IOrderKeyExtractor orderKeyExtractor, KafkaTopicSelector topicSelector) {
        this(producerProperties, orderKeyExtractor, topicSelector, new FieldNameBasedTupleToKafkaMapper<>());
    }

    public OrderAwareKafkaBolt(
            Properties producerProperties, IOrderKeyExtractor orderKeyExtractor,
            KafkaTopicSelector topicSelector, TupleToKafkaMapper<K, V> payloadExtractor) {
        this.producerProperties = producerProperties;
        this.orderKeyExtractor = orderKeyExtractor;
        this.topicSelector = topicSelector;
        this.payloadExtractor = payloadExtractor;
    }

    @Override
    protected void handleInput(Tuple input) {
        String topic = topicSelector.getTopic(input);
        if (topic == null) {
            log.warn("Skip input record ({}) - topic selector return null", makeInputId(input));
            return;
        }

        OrderKey orderKey = partitionsMapping.get(orderKeyExtractor.extract(input));
        if (orderKey == null) {
            send(input, topic);
        } else {
            sendAsync(input, topic, orderKey);
        }

        dropObsoleteOrderKeys();
    }

    private void send(Tuple input, String topic) {
        K key = payloadExtractor.getKeyFromTuple(input);
        V message = payloadExtractor.getMessageFromTuple(input);
        ProducerRecord<K, V> record = new ProducerRecord<>(topic, key, message);

        log.debug("Going to send record {}", makeInputId(input));
        Future<RecordMetadata> future = producer.send(record);
        try {
            RecordMetadata metadata = future.get();
            reportSuccess(input);
            makePartitionMapping(input, metadata.partition());
        } catch (ExecutionException | InterruptedException e) {
            reportError(input, e);
        }
    }

    private void sendAsync(Tuple input, String topic, OrderKey orderKey) {
        orderKey.touch();
        K key = payloadExtractor.getKeyFromTuple(input);
        V message = payloadExtractor.getMessageFromTuple(input);
        ProducerRecord<K, V> record = new ProducerRecord<>(topic, orderKey.partition, key, message);
        AsyncResultProxy<K, V> callback = new AsyncResultProxy<>(this, input);

        log.debug("Going to send(async) record {}", makeInputId(input));
        producer.send(record, callback);
    }

    private void dropObsoleteOrderKeys() {
        final long cutOffLine = System.currentTimeMillis() - timeWindow;
        ArrayList<Object> obsolete = new ArrayList<>(512);
        partitionsMapping.forEach((key, payload) -> {
            if (payload.isUsedBeforeThan(cutOffLine)) {
                obsolete.add(key);
            }
        });
        for (Object item : obsolete) {
            partitionsMapping.remove(item);
        }
    }

    private void asyncResult(Tuple input, Exception e) {
        if (e == null) {
            reportSuccess(input);
        } else {
            reportError(input, e);
        }
    }

    private void makePartitionMapping(Tuple input, int partition) {
        Object order = orderKeyExtractor.extract(input);
        OrderKey orderKey = new OrderKey(partition);
        log.debug("Route future record with key {} to partition {}", order, orderKey.partition);
        synchronized (partitionsMapping) {
            partitionsMapping.put(order, orderKey);
        }
    }

    private void reportSuccess(Tuple input) {
        log.debug("Record ({}) sent", makeInputId(input));
    }

    private void reportError(Tuple input, Exception e) {
        log.error("Can't send record ({}) - {}", makeInputId(input), e);
    }

    private String makeInputId(Tuple input) {
        return String.format(
                "id: %s, source: %s, stream: %s, kafkaKey: %s",
                input.getMessageId(), input.getSourceComponent(), input.getSourceStreamId(),
                payloadExtractor.getKeyFromTuple(input));
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        super.prepare(stormConf, context, collector);
        producer = makeProducer(producerProperties);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {}

    KafkaProducer<K, V> makeProducer(Properties properties) {
        return new KafkaProducer<>(properties);
    }

    static class OrderKey {
        final int partition;
        long lastUsedTime;

        OrderKey(int partition) {
            this.partition = partition;
            this.lastUsedTime = System.currentTimeMillis();
        }

        void touch() {
            lastUsedTime = System.currentTimeMillis();
        }

        boolean isUsedBeforeThan(long time) {
            return lastUsedTime < time;
        }
    }

    static class AsyncResultProxy<K, V> implements Callback {
        private final OrderAwareKafkaBolt<K, V> owner;
        private final Tuple input;

        AsyncResultProxy(OrderAwareKafkaBolt<K, V> owner, Tuple input) {
            this.owner = owner;
            this.input = input;
        }

        @Override
        public void onCompletion(RecordMetadata metadata, Exception e) {
            owner.asyncResult(input, e);
        }
    }
}
