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
package io.siddhi.core.util.parser;

import io.siddhi.core.aggregation.AggregationRuntime;
import io.siddhi.core.config.SiddhiAppContext;
import io.siddhi.core.event.state.MetaStateEvent;
import io.siddhi.core.event.stream.MetaStreamEvent;
import io.siddhi.core.exception.OperationNotSupportedException;
import io.siddhi.core.exception.SiddhiAppCreationException;
import io.siddhi.core.executor.ConstantExpressionExecutor;
import io.siddhi.core.executor.ExpressionExecutor;
import io.siddhi.core.executor.VariableExpressionExecutor;
import io.siddhi.core.query.input.MultiProcessStreamReceiver;
import io.siddhi.core.query.input.ProcessStreamReceiver;
import io.siddhi.core.query.input.stream.StreamRuntime;
import io.siddhi.core.query.input.stream.join.JoinProcessor;
import io.siddhi.core.query.input.stream.join.JoinStreamRuntime;
import io.siddhi.core.query.input.stream.single.SingleStreamRuntime;
import io.siddhi.core.query.processor.Processor;
import io.siddhi.core.query.processor.stream.window.AggregateWindowProcessor;
import io.siddhi.core.query.processor.stream.window.FindableProcessor;
import io.siddhi.core.query.processor.stream.window.LengthBatchWindowProcessor;
import io.siddhi.core.query.processor.stream.window.TableWindowProcessor;
import io.siddhi.core.query.processor.stream.window.WindowProcessor;
import io.siddhi.core.query.processor.stream.window.WindowWindowProcessor;
import io.siddhi.core.table.Table;
import io.siddhi.core.util.ExceptionUtil;
import io.siddhi.core.util.SiddhiConstants;
import io.siddhi.core.util.collection.operator.CompiledCondition;
import io.siddhi.core.util.collection.operator.MatchingMetaInfoHolder;
import io.siddhi.core.util.config.ConfigReader;
import io.siddhi.core.util.statistics.LatencyTracker;
import io.siddhi.core.window.Window;
import io.siddhi.query.api.aggregation.Within;
import io.siddhi.query.api.definition.AbstractDefinition;
import io.siddhi.query.api.definition.Attribute;
import io.siddhi.query.api.execution.query.input.stream.InputStream;
import io.siddhi.query.api.execution.query.input.stream.JoinInputStream;
import io.siddhi.query.api.execution.query.input.stream.SingleInputStream;
import io.siddhi.query.api.expression.Expression;
import io.siddhi.query.compiler.exception.SiddhiParserException;

import java.util.List;
import java.util.Map;

import static io.siddhi.core.event.stream.MetaStreamEvent.EventType.AGGREGATE;
import static io.siddhi.core.event.stream.MetaStreamEvent.EventType.TABLE;
import static io.siddhi.core.event.stream.MetaStreamEvent.EventType.WINDOW;

public class JoinInputStreamParser {


    public static StreamRuntime parseInputStream(JoinInputStream joinInputStream,
                                                 SiddhiAppContext siddhiAppContext,
                                                 Map<String, AbstractDefinition> streamDefinitionMap,
                                                 Map<String, AbstractDefinition> tableDefinitionMap,
                                                 Map<String, AbstractDefinition> windowDefinitionMap,
                                                 Map<String, AbstractDefinition> aggregationDefinitionMap,
                                                 Map<String, Table> tableMap,
                                                 Map<String, Window> windowMap,
                                                 Map<String, AggregationRuntime> aggregationMap,
                                                 List<VariableExpressionExecutor> executors,
                                                 LatencyTracker latencyTracker,
                                                 boolean outputExpectsExpiredEvents,
                                                 String queryName) {
        try {
            ProcessStreamReceiver leftProcessStreamReceiver;
            ProcessStreamReceiver rightProcessStreamReceiver;

            MetaStreamEvent leftMetaStreamEvent = new MetaStreamEvent();
            MetaStreamEvent rightMetaStreamEvent = new MetaStreamEvent();

            String leftInputStreamId = ((SingleInputStream) joinInputStream.getLeftInputStream()).getStreamId();
            String rightInputStreamId = ((SingleInputStream) joinInputStream.getRightInputStream()).getStreamId();

            boolean leftOuterJoinProcessor = false;
            boolean rightOuterJoinProcessor = false;

            if (joinInputStream.getAllStreamIds().size() == 2) {

                setEventType(streamDefinitionMap, tableDefinitionMap, windowDefinitionMap, aggregationDefinitionMap,
                        leftMetaStreamEvent, leftInputStreamId);
                setEventType(streamDefinitionMap, tableDefinitionMap, windowDefinitionMap, aggregationDefinitionMap,
                        rightMetaStreamEvent, rightInputStreamId);
                leftProcessStreamReceiver = new ProcessStreamReceiver(leftInputStreamId, latencyTracker, queryName,
                        siddhiAppContext);
                leftProcessStreamReceiver.setBatchProcessingAllowed(
                        leftMetaStreamEvent.getEventType() == WINDOW);

                rightProcessStreamReceiver = new ProcessStreamReceiver(rightInputStreamId, latencyTracker, queryName,
                        siddhiAppContext);
                rightProcessStreamReceiver.setBatchProcessingAllowed(
                        rightMetaStreamEvent.getEventType() == WINDOW);

                if ((leftMetaStreamEvent.getEventType() == TABLE || leftMetaStreamEvent.getEventType() == AGGREGATE) &&
                        (rightMetaStreamEvent.getEventType() == TABLE ||
                                rightMetaStreamEvent.getEventType() == AGGREGATE)) {
                    throw new SiddhiAppCreationException("Both inputs of join " +
                            leftInputStreamId + " and " + rightInputStreamId + " are from static sources");
                }
                if (leftMetaStreamEvent.getEventType() != AGGREGATE &&
                        rightMetaStreamEvent.getEventType() != AGGREGATE) {
                    if (joinInputStream.getPer() != null) {
                        throw new SiddhiAppCreationException("When joining " + leftInputStreamId + " and " +
                                rightInputStreamId + " 'per' cannot be used as neither of them is an aggregation ");
                    } else if (joinInputStream.getWithin() != null) {
                        throw new SiddhiAppCreationException("When joining " + leftInputStreamId + " and " +
                                rightInputStreamId + " 'within' cannot be used as neither of them is an aggregation ");
                    }
                }
            } else {
                if (windowDefinitionMap.containsKey(joinInputStream.getAllStreamIds().get(0))) {
                    leftMetaStreamEvent.setEventType(WINDOW);
                    rightMetaStreamEvent.setEventType(WINDOW);
                    rightProcessStreamReceiver = new MultiProcessStreamReceiver(
                            joinInputStream.getAllStreamIds().get(0),
                            1, latencyTracker, queryName, siddhiAppContext);
                    rightProcessStreamReceiver.setBatchProcessingAllowed(true);
                    leftProcessStreamReceiver = rightProcessStreamReceiver;
                } else if (streamDefinitionMap.containsKey(joinInputStream.getAllStreamIds().get(0))) {
                    rightProcessStreamReceiver = new MultiProcessStreamReceiver(
                            joinInputStream.getAllStreamIds().get(0),
                            2, latencyTracker, queryName, siddhiAppContext);
                    leftProcessStreamReceiver = rightProcessStreamReceiver;
                } else {
                    throw new SiddhiAppCreationException("Input of join is from static source " + leftInputStreamId +
                            " and " + rightInputStreamId);
                }
            }

            SingleStreamRuntime leftStreamRuntime = SingleInputStreamParser.parseInputStream(
                    (SingleInputStream) joinInputStream.getLeftInputStream(), siddhiAppContext, executors,
                    streamDefinitionMap,
                    leftMetaStreamEvent.getEventType() != TABLE ? null : tableDefinitionMap,
                    leftMetaStreamEvent.getEventType() != WINDOW ? null : windowDefinitionMap,
                    leftMetaStreamEvent.getEventType() != AGGREGATE ? null : aggregationDefinitionMap,
                    tableMap, leftMetaStreamEvent, leftProcessStreamReceiver, true,
                    outputExpectsExpiredEvents, queryName);

            for (VariableExpressionExecutor variableExpressionExecutor : executors) {
                variableExpressionExecutor.getPosition()[SiddhiConstants.STREAM_EVENT_CHAIN_INDEX] = 0;
            }
            int size = executors.size();

            SingleStreamRuntime rightStreamRuntime = SingleInputStreamParser.parseInputStream(
                    (SingleInputStream) joinInputStream.getRightInputStream(), siddhiAppContext, executors,
                    streamDefinitionMap,
                    rightMetaStreamEvent.getEventType() != TABLE ? null : tableDefinitionMap,
                    rightMetaStreamEvent.getEventType() != WINDOW ? null : windowDefinitionMap,
                    rightMetaStreamEvent.getEventType() != AGGREGATE ? null : aggregationDefinitionMap,
                    tableMap, rightMetaStreamEvent, rightProcessStreamReceiver, true,
                    outputExpectsExpiredEvents, queryName);

            for (int i = size; i < executors.size(); i++) {
                VariableExpressionExecutor variableExpressionExecutor = executors.get(i);
                variableExpressionExecutor.getPosition()[SiddhiConstants.STREAM_EVENT_CHAIN_INDEX] = 1;
            }

            setStreamRuntimeProcessorChain(leftMetaStreamEvent, leftStreamRuntime, leftInputStreamId, tableMap,
                    windowMap, aggregationMap, executors, outputExpectsExpiredEvents, queryName,
                    joinInputStream.getWithin(), joinInputStream.getPer(), siddhiAppContext,
                    joinInputStream.getLeftInputStream());
            setStreamRuntimeProcessorChain(rightMetaStreamEvent, rightStreamRuntime, rightInputStreamId, tableMap,
                    windowMap, aggregationMap, executors, outputExpectsExpiredEvents, queryName,
                    joinInputStream.getWithin(), joinInputStream.getPer(), siddhiAppContext,
                    joinInputStream.getRightInputStream());

            MetaStateEvent metaStateEvent = new MetaStateEvent(2);
            metaStateEvent.addEvent(leftMetaStreamEvent);
            metaStateEvent.addEvent(rightMetaStreamEvent);

            switch (joinInputStream.getType()) {
                case FULL_OUTER_JOIN:
                    leftOuterJoinProcessor = true;
                    rightOuterJoinProcessor = true;
                    break;
                case RIGHT_OUTER_JOIN:
                    rightOuterJoinProcessor = true;
                    break;
                case LEFT_OUTER_JOIN:
                    leftOuterJoinProcessor = true;
                    break;
            }

            JoinProcessor leftPreJoinProcessor = new JoinProcessor(true, true, leftOuterJoinProcessor, 0);
            JoinProcessor leftPostJoinProcessor = new JoinProcessor(true, false, leftOuterJoinProcessor, 0);

            FindableProcessor leftFindableProcessor = insertJoinProcessorsAndGetFindable(leftPreJoinProcessor,
                    leftPostJoinProcessor, leftStreamRuntime, siddhiAppContext, outputExpectsExpiredEvents, queryName,
                    joinInputStream.getLeftInputStream());

            JoinProcessor rightPreJoinProcessor = new JoinProcessor(false, true, rightOuterJoinProcessor, 1);
            JoinProcessor rightPostJoinProcessor = new JoinProcessor(false, false, rightOuterJoinProcessor, 1);

            FindableProcessor rightFindableProcessor = insertJoinProcessorsAndGetFindable(rightPreJoinProcessor,
                    rightPostJoinProcessor, rightStreamRuntime, siddhiAppContext, outputExpectsExpiredEvents,
                    queryName, joinInputStream.getRightInputStream());

            leftPreJoinProcessor.setFindableProcessor(rightFindableProcessor);
            leftPostJoinProcessor.setFindableProcessor(rightFindableProcessor);

            rightPreJoinProcessor.setFindableProcessor(leftFindableProcessor);
            rightPostJoinProcessor.setFindableProcessor(leftFindableProcessor);

            Expression compareCondition = joinInputStream.getOnCompare();
            if (compareCondition == null) {
                compareCondition = Expression.value(true);
            }
            if (!(rightFindableProcessor instanceof TableWindowProcessor ||
                    rightFindableProcessor instanceof AggregateWindowProcessor) &&
                    (joinInputStream.getTrigger() != JoinInputStream.EventTrigger.LEFT)) {
                MatchingMetaInfoHolder leftMatchingMetaInfoHolder = MatcherParser.constructMatchingMetaStateHolder
                        (metaStateEvent, 1, leftMetaStreamEvent.getLastInputDefinition(),
                                SiddhiConstants.UNKNOWN_STATE);
                CompiledCondition rightCompiledCondition = leftFindableProcessor.compileCondition(compareCondition,
                        leftMatchingMetaInfoHolder, siddhiAppContext, executors, tableMap, queryName);
                populateJoinProcessors(rightMetaStreamEvent, rightInputStreamId, rightPreJoinProcessor,
                        rightPostJoinProcessor, rightCompiledCondition);
            }
            if (!(leftFindableProcessor instanceof TableWindowProcessor ||
                    leftFindableProcessor instanceof AggregateWindowProcessor) &&
                    (joinInputStream.getTrigger() != JoinInputStream.EventTrigger.RIGHT)) {
                MatchingMetaInfoHolder rightMatchingMetaInfoHolder = MatcherParser.constructMatchingMetaStateHolder
                        (metaStateEvent, 0, rightMetaStreamEvent.getLastInputDefinition(),
                                SiddhiConstants.UNKNOWN_STATE);
                CompiledCondition leftCompiledCondition = rightFindableProcessor.compileCondition(compareCondition,
                        rightMatchingMetaInfoHolder, siddhiAppContext, executors, tableMap, queryName);
                populateJoinProcessors(leftMetaStreamEvent, leftInputStreamId, leftPreJoinProcessor,
                        leftPostJoinProcessor, leftCompiledCondition);
            }
            JoinStreamRuntime joinStreamRuntime = new JoinStreamRuntime(siddhiAppContext, metaStateEvent);
            joinStreamRuntime.addRuntime(leftStreamRuntime);
            joinStreamRuntime.addRuntime(rightStreamRuntime);
            return joinStreamRuntime;
        } catch (Throwable t) {
            ExceptionUtil.populateQueryContext(t, joinInputStream, siddhiAppContext);
            throw t;
        }
    }

    private static void setEventType(Map<String, AbstractDefinition> streamDefinitionMap,
                                     Map<String, AbstractDefinition> tableDefinitionMap,
                                     Map<String, AbstractDefinition> windowDefinitionMap,
                                     Map<String, AbstractDefinition> aggregationDefinitionMap,
                                     MetaStreamEvent metaStreamEvent, String inputStreamId) {
        if (windowDefinitionMap.containsKey(inputStreamId)) {
            metaStreamEvent.setEventType(WINDOW);
        } else if (tableDefinitionMap.containsKey(inputStreamId)) {
            metaStreamEvent.setEventType(TABLE);
        } else if (aggregationDefinitionMap.containsKey(inputStreamId)) {
            metaStreamEvent.setEventType(AGGREGATE);
        } else if (!streamDefinitionMap.containsKey(inputStreamId)) {
            throw new SiddhiParserException("Definition of \"" + inputStreamId + "\" is not given");
        }
    }

    private static void populateJoinProcessors(MetaStreamEvent metaStreamEvent, String inputStreamId,
                                               JoinProcessor preJoinProcessor, JoinProcessor postJoinProcessor,
                                               CompiledCondition compiledCondition) {
        if (metaStreamEvent.getEventType() == TABLE && metaStreamEvent.getEventType() == AGGREGATE) {
            throw new SiddhiAppCreationException(inputStreamId + " of join query cannot trigger join " +
                    "because its a " + metaStreamEvent.getEventType() + ", only WINDOW and STEAM can " +
                    "trigger join");
        }
        preJoinProcessor.setTrigger(false);    // Pre JoinProcessor does not process the events
        preJoinProcessor.setCompiledCondition(compiledCondition);
        postJoinProcessor.setTrigger(true);
        postJoinProcessor.setCompiledCondition(compiledCondition);
    }

    private static void setStreamRuntimeProcessorChain(
            MetaStreamEvent metaStreamEvent, SingleStreamRuntime streamRuntime,
            String inputStreamId, Map<String, Table> tableMap, Map<String, Window> windowMap,
            Map<String, AggregationRuntime> aggregationMap,
            List<VariableExpressionExecutor> variableExpressionExecutors, boolean outputExpectsExpiredEvents,
            String queryName, Within within, Expression per, SiddhiAppContext siddhiAppContext,
            InputStream inputStream) {
        switch (metaStreamEvent.getEventType()) {

            case TABLE:
                TableWindowProcessor tableWindowProcessor = new TableWindowProcessor(tableMap.get(inputStreamId));
                tableWindowProcessor.initProcessor(metaStreamEvent,
                        new ExpressionExecutor[0], null, siddhiAppContext, outputExpectsExpiredEvents,
                        queryName, inputStream);
                streamRuntime.setProcessorChain(tableWindowProcessor);
                break;
            case WINDOW:
                WindowWindowProcessor windowWindowProcessor = new WindowWindowProcessor(
                        windowMap.get(inputStreamId));
                windowWindowProcessor.initProcessor(metaStreamEvent,
                        variableExpressionExecutors.toArray(new ExpressionExecutor[0]), null,
                        siddhiAppContext, outputExpectsExpiredEvents, queryName, inputStream);
                streamRuntime.setProcessorChain(windowWindowProcessor);
                break;
            case AGGREGATE:

                AggregationRuntime aggregationRuntime = aggregationMap.get(inputStreamId);
                AggregateWindowProcessor aggregateWindowProcessor = new AggregateWindowProcessor(
                        aggregationRuntime, within, per);
                aggregateWindowProcessor.initProcessor(metaStreamEvent,
                        variableExpressionExecutors.toArray(new ExpressionExecutor[0]), null,
                        siddhiAppContext, outputExpectsExpiredEvents, queryName, inputStream);
                streamRuntime.setProcessorChain(aggregateWindowProcessor);
                break;
            case DEFAULT:
                break;
        }
    }


    private static FindableProcessor insertJoinProcessorsAndGetFindable(JoinProcessor preJoinProcessor,
                                                                        JoinProcessor postJoinProcessor,
                                                                        SingleStreamRuntime streamRuntime,
                                                                        SiddhiAppContext siddhiAppContext,
                                                                        boolean outputExpectsExpiredEvents,
                                                                        String queryName,
                                                                        InputStream inputStream) {

        Processor lastProcessor = streamRuntime.getProcessorChain();
        Processor prevLastProcessor = null;
        if (lastProcessor != null) {
            while (lastProcessor.getNextProcessor() != null) {
                prevLastProcessor = lastProcessor;
                lastProcessor = lastProcessor.getNextProcessor();
            }
        }

        if (lastProcessor == null) {
            try {
                WindowProcessor windowProcessor = new LengthBatchWindowProcessor();
                ExpressionExecutor[] expressionExecutors = new ExpressionExecutor[1];
                expressionExecutors[0] = new ConstantExpressionExecutor(0, Attribute.Type.INT);
                ConfigReader configReader = siddhiAppContext.getSiddhiContext()
                        .getConfigManager().generateConfigReader("", "lengthBatch");
                windowProcessor.initProcessor(
                        ((MetaStreamEvent) streamRuntime.getMetaComplexEvent()),
                        expressionExecutors, configReader, siddhiAppContext, outputExpectsExpiredEvents, queryName,
                        inputStream);
                lastProcessor = windowProcessor;
            } catch (Throwable t) {
                throw new SiddhiAppCreationException(t);
            }
        }
        if (lastProcessor instanceof FindableProcessor) {
            if (prevLastProcessor != null) {
                prevLastProcessor.setNextProcessor(preJoinProcessor);
            } else {
                streamRuntime.setProcessorChain(preJoinProcessor);
            }
            preJoinProcessor.setNextProcessor(lastProcessor);
            lastProcessor.setNextProcessor(postJoinProcessor);
            return (FindableProcessor) lastProcessor;
        } else {
            throw new OperationNotSupportedException("Stream " + ((MetaStreamEvent) streamRuntime.getMetaComplexEvent
                    ()).getLastInputDefinition().getId() +
                    "'s last processor " + lastProcessor.getClass().getCanonicalName() + " is not an instance of " +
                    FindableProcessor.class.getCanonicalName() + " hence join cannot be proceed");
        }

    }
}
