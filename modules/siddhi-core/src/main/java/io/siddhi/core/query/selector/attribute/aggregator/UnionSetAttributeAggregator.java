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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link AttributeAggregator} to return a union of an aggregation of sets.
 */
@Extension(
        name = "unionSet",
        namespace = "",
        description = "Union multiple sets. \n This attribute aggregator maintains a union of sets. " +
                "The given input set is put into the union set and the union set is returned.",
        parameters =
        @Parameter(name = "set",
                description = "The java.util.Set object that needs to be added into the union set.",
                type = {DataType.OBJECT})
        ,
        returnAttributes = @ReturnAttribute(
                description = "Returns a java.util.Set object which is the union of aggregated sets",
                type = {DataType.OBJECT}),
        examples = @Example(
                syntax = "from stockStream \n" +
                        "select createSet(symbol) as initialSet \n" +
                        "insert into initStream \n\n" +
                        "" +
                        "from initStream#window.timeBatch(10 sec) \n" +
                        "select unionSet(initialSet) as distinctSymbols \n" +
                        "insert into distinctStockStream;",
                description = "distinctStockStream will return the set object which contains the distinct set of " +
                        "stock symbols received during a sliding window of 10 seconds."
        )
)
public class UnionSetAttributeAggregator extends AttributeAggregator {

    /**
     * This map aggregates the count per each distinct element
     */
    private Map<Object, Integer> counter = null;
    private Set set = new HashSet();

    /**
     * The initialization method for FunctionExecutor
     *
     * @param attributeExpressionExecutors are the executors of each attributes in the function
     * @param processingMode               query processing mode
     * @param outputExpectsExpiredEvents   is expired events sent as output
     * @param configReader                 this hold the {@link CountAttributeAggregator} configuration reader.
     * @param siddhiAppContext             Siddhi app runtime context
     */
    @Override
    protected void init(ExpressionExecutor[] attributeExpressionExecutors, ProcessingMode processingMode,
                        boolean outputExpectsExpiredEvents, ConfigReader configReader,
                        SiddhiAppContext siddhiAppContext) {
        if (attributeExpressionExecutors.length != 1) {
            throw new OperationNotSupportedException("unionSet aggregator has to have exactly 1 parameter, currently " +
                    attributeExpressionExecutors.length + " parameters provided");
        }
        if (attributeExpressionExecutors[0].getReturnType() != Attribute.Type.OBJECT) {
            throw new OperationNotSupportedException("Parameter passed to unionSet aggregator should be of type" +
                    " object but found: " + attributeExpressionExecutors[0].getReturnType());
        }
        if (processingMode == ProcessingMode.SLIDE || outputExpectsExpiredEvents) {
            counter = new HashMap<>();
        }
    }

    public Attribute.Type getReturnType() {
        return Attribute.Type.OBJECT;
    }

    @Override
    public Object processAdd(Object data) {
        Set inputSet = (Set) data;
        for (Object o : inputSet) {
            set.add(o);
            if (counter != null) {
                Integer currentCount = counter.get(o);
                if (currentCount == null) {
                    counter.put(o, 1);
                } else {
                    counter.put(o, currentCount + 1);
                }
            }
        }
        // Creating a new set object as the returned set reference is kept until the aggregated values are
        // inserted into the store
        Set returnSet = new HashSet();
        returnSet.addAll(set);
        return returnSet;
    }

    @Override
    public Object processAdd(Object[] data) {
        //UnionSet can have only one input parameter, hence this will not be invoked.
        return null;
    }

    @Override
    public Object processRemove(Object data) {
        Set newSet = (Set) data;
        for (Object o : newSet) {
            if (counter != null) {
                Integer currentCount = counter.get(o);
                if (currentCount == null) {
                    //means o does not exist in the counter map or in the set hence doing nothing
                } else if (currentCount == 0) {
                    throw new IllegalStateException("Error occurred when removing element from " +
                            "union-set for element: " + o.toString());
                } else if (currentCount == 1) {
                    set.remove(o);
                } else {
                    counter.put(o, currentCount - 1);
                }
            } else {
                set.remove(o);
            }
        }
        Set returnSet = new HashSet();
        returnSet.addAll(set);
        return returnSet;
    }

    @Override
    public Object processRemove(Object[] data) {
        //UnionSet can have only one input parameter, hence this will not be invoked.
        return null;
    }

    @Override
    public Object reset() {
        set.clear();
        counter.clear();
        Set returnSet = new HashSet();
        return returnSet;   // returning an empty set.
    }

    @Override
    public boolean canDestroy() {
        return set.size() == 0;
    }

    @Override
    public Map<String, Object> currentState() {
        Map<String, Object> state = new HashMap<>();
        state.put("Set", set);
        state.put("Counter", counter);
        return state;
    }

    @Override
    public void restoreState(Map<String, Object> state) {
        set = (Set) state.get("Set");
        counter = (Map) state.get("Counter");
    }
}
