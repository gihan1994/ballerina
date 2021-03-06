/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.nativeimpl.functions.ws;

import org.ballerinalang.testutils.EnvironmentInitializer;
import org.ballerinalang.testutils.MessageUtils;
import org.ballerinalang.testutils.Services;
import org.ballerinalang.testutils.ws.MockWebSocketSession;
import org.ballerinalang.util.codegen.ProgramFile;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Test the connection groups.
 */
public class ConnectionGroupTest {

    private ProgramFile wsApp;
    private final Map<String, String> oddGroup = new HashMap<>();
    private final Map<String, String> evenGroup = new HashMap<>();

    // Client are identified here by Sessions
    private MockWebSocketSession session1 = new MockWebSocketSession("session1");
    private MockWebSocketSession session2 = new MockWebSocketSession("session2");
    private MockWebSocketSession session3 = new MockWebSocketSession("session3");
    private MockWebSocketSession session4 = new MockWebSocketSession("session4");
    private MockWebSocketSession session5 = new MockWebSocketSession("session5");
    private MockWebSocketSession session6 = new MockWebSocketSession("session6");

    // paths
    private final String wsEndpointPath = "/chat-group/ws";

    @BeforeClass
    public void  setup() {
        wsApp = EnvironmentInitializer.setupProgramFile(
                "samples/websocket/connection_group_sample/connectionGroupTest.bal");
        oddGroup.put("group", "odd");
        evenGroup.put("group", "even");
    }

    @Test(priority = 0)
    public void testGrouping() {
        String sentText = "test message";
        String textExpectedForEven = "evenGroup: " + sentText;
        String textExpectedForOdd = "oddGroup: " + sentText;

        Services.invoke(MessageUtils.generateWebSocketOnOpenMessage(session1, wsEndpointPath, oddGroup));
        Services.invoke(MessageUtils.generateWebSocketOnOpenMessage(session2, wsEndpointPath, evenGroup));
        Services.invoke(MessageUtils.generateWebSocketOnOpenMessage(session3, wsEndpointPath, oddGroup));
        Services.invoke(MessageUtils.generateWebSocketOnOpenMessage(session4, wsEndpointPath, evenGroup));
        Services.invoke(MessageUtils.generateWebSocketTextMessage(sentText, session1, wsEndpointPath));

        Assert.assertEquals(session1.getTextReceived(), textExpectedForOdd);
        Assert.assertEquals(session2.getTextReceived(), textExpectedForEven);
        Assert.assertEquals(session3.getTextReceived(), textExpectedForOdd);
        Assert.assertEquals(session4.getTextReceived(), textExpectedForEven);
    }

    @Test(priority = 1)
    public void testRemoveConnectionFromGroup() {
        String sentTextToRemove = "removeOddConnection";
        Services.invoke(MessageUtils.generateWebSocketTextMessage(sentTextToRemove, session1, wsEndpointPath));

        String sentText = "hello again";
        String textExpectedForEven = "evenGroup: " + sentText;
        String textExpectedForOdd = "oddGroup: " + sentText;
        Services.invoke(MessageUtils.generateWebSocketTextMessage(sentText, session2, wsEndpointPath));

        Assert.assertEquals(session1.getTextReceived(), null);
        Assert.assertEquals(session2.getTextReceived(), textExpectedForEven);
        Assert.assertEquals(session3.getTextReceived(), textExpectedForOdd);
        Assert.assertEquals(session4.getTextReceived(), textExpectedForEven);
    }

    @Test(priority = 2)
    public void testRemoveConnectionGroup() {
        String sentTextToRemove = "removeEvenGroup";
        Services.invoke(MessageUtils.generateWebSocketTextMessage(sentTextToRemove, session3, wsEndpointPath));

        String sentText = "hello only odd";
        Services.invoke(MessageUtils.generateWebSocketTextMessage(sentText, session3, wsEndpointPath));

        Assert.assertEquals(session1.getTextReceived(), null);
        Assert.assertEquals(session2.getTextReceived(), null);
        Assert.assertEquals(session3.getTextReceived(), "oddGroup: " + sentText);
        Assert.assertEquals(session4.getTextReceived(), null);
    }

    @Test(priority = 3)
    public void testCloseConnectionGroup() {
        // Check pre conditions
        Services.invoke(MessageUtils.generateWebSocketOnOpenMessage(session5, wsEndpointPath, oddGroup));
        Services.invoke(MessageUtils.generateWebSocketOnOpenMessage(session6, wsEndpointPath, evenGroup));
        Assert.assertTrue(session3.isOpen());
        Assert.assertTrue(session4.isOpen());
        Assert.assertTrue(session5.isOpen());
        Assert.assertTrue(session6.isOpen());

        String sentText = "closeOddGroup";
        Services.invoke(MessageUtils.generateWebSocketTextMessage(sentText, session3, wsEndpointPath));

        // Check post conditions
        Assert.assertFalse(session3.isOpen());
        Assert.assertTrue(session4.isOpen());
        Assert.assertFalse(session5.isOpen());
        Assert.assertTrue(session6.isOpen());
    }

    @AfterClass
    public void cleanUp() {
        EnvironmentInitializer.cleanup(wsApp);
    }
}
