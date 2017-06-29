/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.siddhi.core.aggregation;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.stream.input.InputHandler;

public class AggregationTestCase {

    static final Logger log = Logger.getLogger(AggregationTestCase.class);
    private volatile int count;
    private volatile boolean eventArrived;

    @Before
    public void init() {
        count = 0;
        eventArrived = false;
    }

    @Test
    public void externalTimeTest() throws InterruptedException {
        log.info("Incremental Processing: externalTimeTest");
        SiddhiManager siddhiManager = new SiddhiManager();

        String cseEventStream = "define stream cseEventStream (symbol string, price1 float, price2 float, volume long , quantity int, timestamp long);";
        String query = " define aggregation test " + "from cseEventStream "
                + "select symbol, avg(price1) as avgPrice, sum(price1) as totprice1, (quantity * volume) as mult  "
                + "group by symbol " + "aggregate by timestamp every sec...year ;";

        ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(cseEventStream + query);

        InputHandler inputHandler = executionPlanRuntime.getInputHandler("cseEventStream");
        executionPlanRuntime.start();
        // TODO: 6/29/17 Check with null later

        // Thursday, June 1, 2017 4:05:50 AM
        inputHandler.send(new Object[] { "WSO2", 50f, 60f, 90L, 6, 1496289950000L });
        inputHandler.send(new Object[] { "WSO2", 70f, null, 40L, 10, 1496289950000L });

        // Thursday, June 1, 2017 4:05:52 AM
        inputHandler.send(new Object[] { "WSO2", 60f, 44f, 200L, 56, 1496289952000L });
        inputHandler.send(new Object[] { "WSO2", 100f, null, 200L, 16, 1496289952000L });

        // Thursday, June 1, 2017 4:05:54 AM
        inputHandler.send(new Object[] { "IBM", 100f, null, 200L, 26, 1496289954000L });
        inputHandler.send(new Object[] { "IBM", 100f, null, 200L, 96, 1496289954000L });

        // Thursday, June 1, 2017 4:05:56 AM
        inputHandler.send(new Object[] { "IBM", 900f, null, 200L, 60, 1496289956000L });
        inputHandler.send(new Object[] { "IBM", 500f, null, 200L, 7, 1496289956000L });

        // Thursday, June 1, 2017 4:06:56 AM
        inputHandler.send(new Object[] { "IBM", 400f, null, 200L, 9, 1496290016000L });

        // Thursday, June 1, 2017 4:07:56 AM
        inputHandler.send(new Object[] { "IBM", 600f, null, 200L, 6, 1496290076000L });

        // Thursday, June 1, 2017 5:07:56 AM
        inputHandler.send(new Object[] { "CISCO", 700f, null, 200L, 20, 1496293676000L });

        // Thursday, June 1, 2017 6:07:56 AM
        inputHandler.send(new Object[] { "WSO2", 60f, 44f, 200L, 56, 1496297276000L });

        // Friday, June 2, 2017 6:07:56 AM
        inputHandler.send(new Object[] { "CISCO", 800f, null, 100L, 10, 1496383676000L });

        // Saturday, June 3, 2017 6:07:56 AM
        inputHandler.send(new Object[] { "CISCO", 900f, null, 100L, 15, 1496470076000L });

        // Monday, July 3, 2017 6:07:56 AM
        inputHandler.send(new Object[] { "IBM", 100f, null, 200L, 96, 1499062076000L });

        // Thursday, August 3, 2017 6:07:56 AM
        inputHandler.send(new Object[] { "IBM", 400f, null, 200L, 9, 1501740476000L });

        // Friday, August 3, 2018 6:07:56 AM
        inputHandler.send(new Object[] { "WSO2", 60f, 44f, 200L, 6, 1533276476000L });

        // Saturday, August 3, 2019 6:07:56 AM
        inputHandler.send(new Object[] { "WSO2", 260f, 44f, 200L, 16, 1564812476000L });

        // Monday, August 3, 2020 6:07:56 AM
        inputHandler.send(new Object[] { "CISCO", 260f, 44f, 200L, 16, 1596434876000L });

        Thread.sleep(2000);
        executionPlanRuntime.shutdown();
    }

    @Test
    public void eventTimeTest() throws InterruptedException {
        log.info("Incremental Processing: eventTimeTest");
        SiddhiManager siddhiManager = new SiddhiManager();

        String cseEventStream = "define stream cseEventStream (symbol string, price1 float, price2 float, volume long , quantity int, timestamp long);";
        String query = " define aggregation test " + "from cseEventStream "
                + "select symbol, avg(price1) as avgPrice, sum(price1) as totprice1, (quantity * volume) as mult  "
                + "group by symbol " + "aggregate every sec...hour ;";

        ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(cseEventStream + query);

        InputHandler inputHandler = executionPlanRuntime.getInputHandler("cseEventStream");
        executionPlanRuntime.start();

        inputHandler.send(new Object[] { "WSO2", 50f, 60f, 90L, 6, 1496289950000L });
        inputHandler.send(new Object[] { "WSO2", 70f, null, 40L, 10, 1496289950000L });
        Thread.sleep(2000);

        inputHandler.send(new Object[] { "WSO2", 60f, 44f, 200L, 56, 1496289952000L });
        inputHandler.send(new Object[] { "WSO2", 100f, null, 200L, 16, 1496289952000L });
        Thread.sleep(2000);

        inputHandler.send(new Object[] { "IBM", 100f, null, 200L, 26, 1496289954000L });
        inputHandler.send(new Object[] { "IBM", 100f, null, 200L, 96, 1496289954000L });
        Thread.sleep(2000);

        inputHandler.send(new Object[] { "IBM", 900f, null, 200L, 60, 1496289956000L });
        inputHandler.send(new Object[] { "IBM", 500f, null, 200L, 7, 1496289956000L });
        Thread.sleep(60000);


        inputHandler.send(new Object[] { "IBM", 400f, null, 200L, 9, 1496290016000L });
        Thread.sleep(60000);
        executionPlanRuntime.shutdown();
    }
}
