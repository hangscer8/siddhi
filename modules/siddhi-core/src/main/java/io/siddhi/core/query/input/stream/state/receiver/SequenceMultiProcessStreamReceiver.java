/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.siddhi.core.query.input.stream.state.receiver;

import io.siddhi.core.config.SiddhiAppContext;
import io.siddhi.core.event.ComplexEvent;
import io.siddhi.core.event.Event;
import io.siddhi.core.query.input.StateMultiProcessStreamReceiver;
import io.siddhi.core.query.input.stream.state.StateStreamRuntime;
import io.siddhi.core.util.statistics.LatencyTracker;

import java.util.List;

/**
 * {StreamJunction.Receiver} implementation to receive events into sequence queries
 * with multiple streams.
 */
public class SequenceMultiProcessStreamReceiver extends StateMultiProcessStreamReceiver {

    private StateStreamRuntime stateStreamRuntime;

    public SequenceMultiProcessStreamReceiver(String streamId, int processCount, StateStreamRuntime
            stateStreamRuntime, LatencyTracker latencyTracker, String queryName, SiddhiAppContext siddhiAppContext) {
        super(streamId, processCount, latencyTracker, queryName, siddhiAppContext);
        this.stateStreamRuntime = stateStreamRuntime;
        eventSequence = new int[processCount];
        int count = 0;
        for (int i = eventSequence.length - 1; i >= 0; i--) {
            eventSequence[count] = i;
            count++;
        }
    }

    public SequenceMultiProcessStreamReceiver clone(String key) {
        return new SequenceMultiProcessStreamReceiver(streamId + key, processCount, null,
                latencyTracker, queryName, siddhiAppContext);
    }

    public void setStateStreamRuntime(StateStreamRuntime stateStreamRuntime) {
        this.stateStreamRuntime = stateStreamRuntime;
    }

    protected void stabilizeStates() {
        stateStreamRuntime.resetAndUpdate();
    }

    @Override
    public void receive(ComplexEvent complexEvent) {
        super.receive(complexEvent);
    }

    @Override
    public void receive(Event event) {
        super.receive(event);
    }

    @Override
    public void receive(Event[] events) {
        super.receive(events);
    }

    @Override
    public void receive(List<Event> events) {
        super.receive(events);
    }

    @Override
    public void receive(long timestamp, Object[] data) {
        super.receive(timestamp, data);
    }
}
