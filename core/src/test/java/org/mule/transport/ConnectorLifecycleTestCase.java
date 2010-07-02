/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;


import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.service.Service;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestConnector;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import junit.framework.Assert;

/**
 * Tests that lifecycle methods on a connector are not processed more than once. (@see MULE-3062)
 * Also test lifecycle of a connector dispatchers, receivers, workManagers and scheduler.
 */
public class ConnectorLifecycleTestCase extends AbstractMuleTestCase 
{
    private TestConnector connector;

    @Override
    public void doSetUp() throws Exception
    {
        connector = new TestConnector(muleContext);
        connector.initialise();
    }

    @Override
    public void doTearDown() throws Exception
    {
        connector = null;
    }

    /**
     * This test ensures that the connector is only initialised once even on a
     * direct initialisation (not through Mule).
     */
    public void testDoubleInitialiseConnector() throws Exception
    {
        // Note: the connector was already initialized once during doSetUp()

        // Initialising the connector should leave it disconnected.
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(0, connector.getConnectCount());
        assertEquals(0, connector.getStartCount());
        assertEquals(0, connector.getStopCount());
        assertEquals(0, connector.getDisconnectCount());
        assertEquals(0, connector.getDisposeCount());

        // Initialising the connector again should not throw an exception.
        try 
        {
            connector.initialise();
            Assert.fail("Expected IllegalStateException not thrown.");
        } 
        catch (IllegalStateException ex)
        {
            // ignore since expected
        }
    }

    /**
     * This test ensures that the connector is only started once even on a
     * direct restart (not through Mule).
     */
    public void testDoubleStartConnector() throws Exception
    {
        // Starting the connector should leave it uninitialised,
        // but connected and started.
        connector.start();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        assertEquals(0, connector.getStopCount());
        assertEquals(0, connector.getDisconnectCount());
        assertEquals(0, connector.getDisposeCount());

        // Starting the connector again
        try
        {
            connector.start();
            fail("cannot start the connector twice");
        }
        catch (IllegalStateException e)
        {
            //expected
        }
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        assertEquals(0, connector.getStopCount());
        assertEquals(0, connector.getDisconnectCount());
        assertEquals(0, connector.getDisposeCount());
    }

    /**
     * This test ensures that the connector is only stopped once even on a
     * direct restop (not through Mule).
     */
    public void testDoubleStopConnector() throws Exception
    {
        // Starting the connector should leave it uninitialised,
        // but connected and started.
        connector.start();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        assertEquals(0, connector.getStopCount());
        assertEquals(0, connector.getDisconnectCount());
        assertEquals(0, connector.getDisposeCount());

        assertTrue(connector.isStarted());

        // Stopping the connector should stop and disconnect it.
        connector.stop();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        assertEquals(1, connector.getStopCount());
        assertEquals(1, connector.getDisconnectCount());
        assertEquals(0, connector.getDisposeCount());


        try
        {
            connector.stop();
            fail("cannot stop the connector twice");
        }
        catch (IllegalStateException e)
        {
            //expected
        }
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        assertEquals(1, connector.getStopCount());
        assertEquals(1, connector.getDisconnectCount());
        assertEquals(0, connector.getDisposeCount());
    }

    /**
     * This test ensures that the connector is only disposed once even on a
     * direct disposal (not through Mule).
     */
    public void testDoubleDisposeConnectorStartStop() throws Exception
    {
        connector.start();
        assertTrue(connector.isStarted());
        
        connector.stop();
        assertFalse(connector.isStarted());
        
        // Disposing the connector should leave it uninitialised.
        connector.dispose();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        assertEquals(1, connector.getStopCount());
        assertEquals(1, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());

        try
        {
            connector.dispose();
            fail("cannot dispose the connector twice");
        }
        catch (IllegalStateException e)
        {
            //expected
        }
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        assertEquals(1, connector.getStopCount());
        assertEquals(1, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());
    }

    /**
     * This test ensures that the connector is only disposed once even on a
     * direct disposal (not through Mule).
     */
    public void testDoubleDisposeConnectorStartOnly() throws Exception 
    {
        connector.start();
        assertTrue(connector.isStarted());
        
        // Disposing the connector should leave it uninitialised.
        connector.dispose();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        // dispose() implicitly calls stop()
        assertEquals(1, connector.getStopCount());
        assertEquals(1, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());

        try
        {
            connector.dispose();
            fail("cannot dispose the connector twice");
        }
        catch (IllegalStateException e)
        {
            //expected
        }
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(1, connector.getConnectCount());
        assertEquals(1, connector.getStartCount());
        // dispose() implicitly calls stop()
        assertEquals(1, connector.getStopCount());
        assertEquals(1, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());   
    }

    /**
     * This test ensures that the connector is only disposed once even on a
     * direct disposal (not through Mule).
     */
    public void testDoubleDisposeConnector() throws Exception 
    {
        // Disposing the connector should leave it uninitialised.
        connector.dispose();
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(0, connector.getConnectCount());
        assertEquals(0, connector.getStartCount());
        assertEquals(0, connector.getStopCount());
        assertEquals(0, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());

      try
        {
            connector.dispose();
            fail("cannot dispose the connector twice");
        }
        catch (IllegalStateException e)
        {
            //expected
        }
        assertEquals(1, connector.getInitialiseCount());
        assertEquals(0, connector.getConnectCount());
        assertEquals(0, connector.getStartCount());
        assertEquals(0, connector.getStopCount());
        assertEquals(0, connector.getDisconnectCount());
        assertEquals(1, connector.getDisposeCount());
    }

    public void testReceiversLifecycle() throws Exception
    {
        connector.registerListener(getTestInboundEndpoint("in", "test://in"), getNullMessageProcessor(), getTestService());

        assertEquals(1, connector.receivers.size());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in")).isConnected());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());

        connector.start();
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in")).isConnected());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());

        connector.registerListener(getTestInboundEndpoint("in2", "test://in2"), getNullMessageProcessor(), getTestService());

        assertEquals(2, connector.receivers.size());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in")).isConnected());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());

        // TODO MULE-4554 Receivers that are created (when new listener is registered) while connector is started are not started or connected
        // assertTrue(((AbstractMessageReceiver)connector.receivers.get("in2")).isConnected());
        // assertTrue(((AbstractMessageReceiver)connector.receivers.get("in2")).isStarted());

        connector.stop();
        assertEquals(2, connector.receivers.size());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in")).isConnected());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in2")).isConnected());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in2")).isStarted());

        connector.start();
        assertEquals(2, connector.receivers.size());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in")).isConnected());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in2")).isConnected());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in2")).isStarted());

        connector.dispose();
        assertEquals(0, connector.receivers.size());

    }
    
    public void testReceiversServiceLifecycle() throws Exception
    {
        Service service = getTestService();
        InboundEndpoint endpoint = getTestInboundEndpoint("in", "test://in");
        service.getInboundRouter().addEndpoint(endpoint);
        connector = (TestConnector) endpoint.getConnector();
        
        assertEquals(0, connector.receivers.size());

        connector.start();
        assertEquals(0, connector.receivers.size());
        
        service.start();
        assertEquals(1, connector.receivers.size());
        assertTrue(((AbstractMessageReceiver) connector.receivers.get("in")).isConnected());
        assertTrue(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());

        connector.stop();
        assertEquals(1, connector.receivers.size());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in")).isConnected());
        assertFalse(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());

        connector.start();
        assertEquals(1, connector.receivers.size());
        assertTrue(((AbstractMessageReceiver) connector.receivers.get("in")).isConnected());
        assertTrue(((AbstractMessageReceiver) connector.receivers.get("in")).isStarted());
        
        service.stop();
        assertEquals(0, connector.receivers.size());

        connector.stop();
        assertEquals(0, connector.receivers.size());

    }

    public void testDispatchersLifecycle() throws Exception
    {
        OutboundEndpoint out = getTestOutboundEndpoint("out", "test://out", null, null, null, connector);

        // attempts to send/dispatch/request are made on a stopped/stopping connector
        // This should fail because the connector is not started!
        try
        {
            out.process(getTestEvent("data"));
            fail("cannot sent on a connector that is not started");
        }
        catch (LifecycleException e)
        {
            //Expected
            //assertTrue(e.getCause() instanceof LifecycleException);
        }

        assertEquals(0, connector.dispatchers.getNumIdle());

        // Dispatcher is not started or connected
        assertDispatcherStartedConntected(out, false, false);

        connector.start();
        assertDispatcherStartedConntected(out, true, true);

        OutboundEndpoint out2 = getTestOutboundEndpoint("out2", "test://out2", null, null, null, connector);
        out2.process(getTestEvent("data"));


        assertEquals(1, connector.dispatchers.getNumIdle());
        assertDispatcherStartedConntected(out, true, true);
        assertDispatcherStartedConntected(out2, true, true);

        connector.stop();
        System.out.println("ACTIVE = "+connector.dispatchers.getNumActive());
        System.out.println("IDEL = "+connector.dispatchers.getNumIdle());
        
        // Pool is cleared because of implementation of workaround for MULE-4553
        assertEquals(0, connector.dispatchers.getNumActive() + connector.dispatchers.getNumIdle());
         assertDispatcherStartedConntected(out, false, false);
         assertDispatcherStartedConntected(out2, false, false);

        connector.start();
        //TODO 
        assertEquals(2, connector.dispatchers.getNumActive() + connector.dispatchers.getNumIdle());
         assertDispatcherStartedConntected(out, true, true);
         assertDispatcherStartedConntected(out2, true, true);

        out.process(getTestEvent("data"));
        assertEquals(2, connector.dispatchers.getNumIdle());
        assertDispatcherStartedConntected(out, true, true);

        connector.dispose();
        assertEquals(0, connector.dispatchers.getNumActive() + connector.dispatchers.getNumIdle());

    }

    public void testWorkManagerLifecycle() throws MuleException, WorkException
    {
        //ConnectorLifecycleTestCase These are now created in the "initialize" phase
        //   assertNull(connector.getReceiverWorkManager());
        //   assertNull(connector.getDispatcherWorkManager());
        //   assertNull(connector.getRequesterWorkManager());

        connector.start();
        assertNotNull(connector.getReceiverWorkManager());
        assertNotNull(connector.getDispatcherWorkManager());
        assertNotNull(connector.getRequesterWorkManager());
        connector.getReceiverWorkManager().doWork(createSomeWork());
        connector.getDispatcherWorkManager().doWork(createSomeWork());
        connector.getRequesterWorkManager().doWork(createSomeWork());

        connector.stop();
        assertNull(connector.getReceiverWorkManager());
        assertNull(connector.getDispatcherWorkManager());
        assertNull(connector.getRequesterWorkManager());

        connector.start();
        assertNotNull(connector.getReceiverWorkManager());
        assertNotNull(connector.getDispatcherWorkManager());
        assertNotNull(connector.getRequesterWorkManager());
        connector.getReceiverWorkManager().doWork(createSomeWork());
        connector.getDispatcherWorkManager().doWork(createSomeWork());
        connector.getRequesterWorkManager().doWork(createSomeWork());

        connector.dispose();
        assertNull(connector.getReceiverWorkManager());
        assertNull(connector.getDispatcherWorkManager());
        assertNull(connector.getRequesterWorkManager());
    }

    public void testSchedulerLifecycle() throws MuleException, WorkException
    {
        assertNull(connector.getScheduler());

        connector.start();
        assertFalse(connector.getScheduler().isShutdown());
        assertFalse(connector.getScheduler().isTerminated());

        connector.stop();
        assertNull(connector.getScheduler());

        connector.start();
        assertFalse(connector.getScheduler().isShutdown());
        assertFalse(connector.getScheduler().isTerminated());

        connector.dispose();
        assertNull(connector.getScheduler());
    }

    protected Work createSomeWork()
    {
        return new Work()
        {
            public void run()
            {
                System.out.println("I'm doing some work");
            }

            public void release()
            {
                // nothing to do
            }
        };
    }

    private void assertDispatcherStartedConntected(OutboundEndpoint out, boolean started, boolean connected)
        throws Exception
    {
        AbstractMessageDispatcher dispatcher = (AbstractMessageDispatcher) connector.dispatchers.borrowObject(out);
        assertEquals("Dispatcher started", started, dispatcher.isStarted());
        assertEquals("Dispatcher connected", connected, dispatcher.isConnected());
        connector.dispatchers.returnObject(out, dispatcher);
    }
}
