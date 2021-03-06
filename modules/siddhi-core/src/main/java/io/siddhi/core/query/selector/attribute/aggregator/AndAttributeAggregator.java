/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package io.siddhi.core.query.selector.attribute.aggregator;

import io.siddhi.annotation.Example;
import io.siddhi.annotation.Extension;
import io.siddhi.annotation.Parameter;
import io.siddhi.annotation.ReturnAttribute;
import io.siddhi.annotation.util.DataType;
import io.siddhi.core.config.SiddhiAppContext;
import io.siddhi.core.exception.OperationNotSupportedException;
import io.siddhi.core.executor.ExpressionExecutor;
import io.siddhi.core.query.processor.ProcessingMode;
import io.siddhi.core.util.config.ConfigReader;
import io.siddhi.query.api.definition.Attribute;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link AttributeAggregator} to calculate sum based on an event attribute.
 */
@Extension(
        name = "and",
        namespace = "",
        description = "Returns the results of AND operation for all the events.",
        parameters = {
                @Parameter(name = "arg",
                        description = "The value that needs to be AND operation.",
                        type = {DataType.BOOL})
        },
        returnAttributes = @ReturnAttribute(
                description = "Returns true only if all of its operands are true, else false.",
                type = {DataType.BOOL}),
        examples = {
                @Example(
                        syntax = "from cscStream#window.lengthBatch(10)\n" +
                                "select and(isFraud) as isFraudTransaction\n" +
                                "insert into alertStream;",
                        description = "This will returns the result for AND operation of isFraud values as a " +
                                "boolean value for event chunk expiry by window length batch."
                )
        }
)
public class AndAttributeAggregator extends AttributeAggregator {

    private static Attribute.Type type = Attribute.Type.BOOL;
    private int trueEventsCount = 0;
    private int falseEventsCount = 0;

    /**
     * The initialization method for FunctionExecutor
     *
     * @param attributeExpressionExecutors are the executors of each attributes in the function
     * @param processingMode               query processing mode
     * @param outputExpectsExpiredEvents   is expired events sent as output
     * @param configReader                 this hold the {@link AndAttributeAggregator} configuration reader.
     * @param siddhiAppContext             Siddhi app runtime context
     */
    @Override
    protected void init(ExpressionExecutor[] attributeExpressionExecutors, ProcessingMode processingMode,
                        boolean outputExpectsExpiredEvents, ConfigReader configReader,
                        SiddhiAppContext siddhiAppContext) {
        if (attributeExpressionExecutors.length != 1) {
            throw new OperationNotSupportedException("And aggregator has to have exactly 1 parameter, currently " +
                    attributeExpressionExecutors.length
                    + " parameters provided");
        }
    }

    public Attribute.Type getReturnType() {
        return type;
    }

    @Override
    public Object processAdd(Object data) {
        if ((boolean) data) {
            trueEventsCount++;
        } else {
            falseEventsCount++;
        }
        return computeLogicalOperation();
    }

    @Override
    public Object processAdd(Object[] data) {
        for (Object object : data) {
            if ((boolean) object) {
                trueEventsCount++;
            } else {
                falseEventsCount++;
            }
        }
        return computeLogicalOperation();
    }

    @Override
    public Object processRemove(Object data) {
        if ((boolean) data) {
            trueEventsCount--;
        } else {
            falseEventsCount--;
        }
        return computeLogicalOperation();
    }

    @Override
    public Object processRemove(Object[] data) {
        for (Object object : data) {
            if ((boolean) object) {
                trueEventsCount--;
            } else {
                falseEventsCount--;
            }
        }
        return computeLogicalOperation();
    }

    private boolean computeLogicalOperation() {
        return trueEventsCount > 0 && falseEventsCount == 0;
    }

    @Override
    public Object reset() {
        trueEventsCount = 0;
        falseEventsCount = 0;
        return false;
    }

    @Override
    public boolean canDestroy() {
        return trueEventsCount == 0 && falseEventsCount == 0;
    }

    @Override
    public Map<String, Object> currentState() {
        Map<String, Object> state = new HashMap<>();
        state.put("trueEventsCount", trueEventsCount);
        state.put("falseEventsCount", falseEventsCount);
        return state;
    }

    @Override
    public void restoreState(Map<String, Object> state) {
        trueEventsCount = (int) state.get("trueEventsCount");
        falseEventsCount = (int) state.get("falseEventsCount");
    }
}
