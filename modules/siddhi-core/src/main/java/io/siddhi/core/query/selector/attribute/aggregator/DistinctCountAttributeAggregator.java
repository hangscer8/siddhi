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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link AttributeAggregator} to calculate distinct count based on an event attribute.
 */
@Extension(
        name = "distinctCount",
        namespace = "",
        description = "This returns the count of distinct occurrences for a given arg.",
        parameters = {
                @Parameter(name = "arg",
                        description = "The object for which the number of distinct occurences needs to be counted.",
                        type = {DataType.INT, DataType.LONG, DataType.DOUBLE, DataType.FLOAT, DataType.STRING})
        },
        returnAttributes = @ReturnAttribute(
                description = "Returns the count of distinct occurrences for a given arg.",
                type = {DataType.LONG}),
        examples = @Example(
                syntax = "from fooStream\n" +
                        "select distinctcount(pageID) as count\n" +
                        "insert into barStream;",
                description = "distinctcount(pageID) for the following output returns '3' when the available values" +
                        " are as follows.\n" +
                        " \"WEB_PAGE_1\"\n" +
                        " \"WEB_PAGE_1\"\n" +
                        " \"WEB_PAGE_2\"\n" +
                        " \"WEB_PAGE_3\"\n" +
                        " \"WEB_PAGE_1\"\n" +
                        " \"WEB_PAGE_2\"\n" +
                        " The three distinct occurences identified are 'WEB_PAGE_1', 'WEB_PAGE_2', and 'WEB_PAGE_3'."
        )
)
public class DistinctCountAttributeAggregator extends AttributeAggregator {
    private Map<Object, Long> distinctValues = new HashMap<Object, Long>();

    /**
     * The initialization method for FunctionExecutor
     *
     * @param attributeExpressionExecutors are the executors of each attributes in the function
     * @param processingMode               query processing mode
     * @param outputExpectsExpiredEvents   is expired events sent as output
     * @param configReader                 this hold the {@link DistinctCountAttributeAggregator} configuration reader.
     * @param siddhiAppContext             Siddhi app runtime context
     */
    @Override
    protected void init(ExpressionExecutor[] attributeExpressionExecutors, ProcessingMode processingMode,
                        boolean outputExpectsExpiredEvents, ConfigReader configReader,
                        SiddhiAppContext siddhiAppContext) {
        if (attributeExpressionExecutors.length != 1) {
            throw new OperationNotSupportedException("Distinct count aggregator has to have exactly 1 parameter, " +
                    "currently " + attributeExpressionExecutors.length +
                    " parameters provided");
        }
    }

    public Attribute.Type getReturnType() {
        return Attribute.Type.LONG;
    }

    @Override
    public Object processAdd(Object data) {
        Long preVal = distinctValues.get(data);
        if (preVal != null) {
            distinctValues.put(data, ++preVal);
        } else {
            distinctValues.put(data, 1L);
        }
        return getDistinctCount();
    }

    @Override
    public Object processAdd(Object[] data) {
        return new IllegalStateException(
                "Distinct count aggregator cannot process data array, but found " + Arrays.deepToString(data));
    }

    @Override
    public Object processRemove(Object data) {
        Long preVal = distinctValues.get(data);
        preVal--;
        if (preVal > 0) {
            distinctValues.put(data, preVal);
        } else {
            distinctValues.remove(data);
        }
        return getDistinctCount();
    }

    @Override
    public Object processRemove(Object[] data) {
        return new IllegalStateException(
                "Distinct count aggregator cannot process data array, but found " + Arrays.deepToString(data));
    }

    @Override
    public Object reset() {
        distinctValues.clear();
        return getDistinctCount();
    }

    @Override
    public boolean canDestroy() {
        return distinctValues.size() == 0;
    }

    @Override
    public Map<String, Object> currentState() {
        Map<String, Object> state = new HashMap<>();
        state.put("DistinctValues", distinctValues);
        return state;
    }

    @Override
    public void restoreState(Map<String, Object> state) {
        distinctValues = (Map<Object, Long>) state.get("DistinctValues");
    }

    private long getDistinctCount() {
        return distinctValues.size();
    }
}
