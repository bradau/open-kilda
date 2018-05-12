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

import lombok.Getter;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class KafkaProducerStub<K, V> {
    private int lastPartition = 0;
    private long offset = 0;

    @Getter
    private final ArrayList <ProducerRecord<K, V>> sendHistory = new ArrayList<>(10);

    Future send(ProducerRecord<K, V> payload) {
        return this.send(payload, null);
    }

    Future send(ProducerRecord<K, V> payload, Callback callback) {
        sendHistory.add(payload);

        Integer partition = payload.partition();
        if (partition == null) {
            partition = lastPartition++;
        }

        long offset = this.offset++;
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(payload.topic(), partition), offset, offset, System.currentTimeMillis(), 0, 0, 0);

        if (callback != null) {
            callback.onCompletion(metadata, null);
        }
        return new Future(metadata);
    }

    static class Future implements java.util.concurrent.Future<RecordMetadata> {
        private final RecordMetadata payload;

        public Future(RecordMetadata payload) {
            this.payload = payload;
        }

        @Override
        public boolean cancel(boolean b) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public RecordMetadata get() throws InterruptedException, ExecutionException {
            return payload;
        }

        @Override
        public RecordMetadata get(long l, @NotNull TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
            return payload;
        }
    }
}
