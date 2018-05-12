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
import org.apache.storm.kafka.bolt.selector.KafkaTopicSelector;
import org.mockito.Mockito;

import java.util.Properties;

public class OrderAwareKafkaBoltStub<K, V> extends OrderAwareKafkaBolt<K, V> {
    public OrderAwareKafkaBoltStub(
            Properties producerProperties, IOrderKeyExtractor orderKeyExtractor, KafkaTopicSelector topicSelector) {
        super(producerProperties, orderKeyExtractor, topicSelector);
    }

    @Override
    @SuppressWarnings("unchecked")
    KafkaProducer<K, V> makeProducer(Properties properties) {
        return (KafkaProducer<K, V>) Mockito.mock(KafkaProducer.class);
    }
}
