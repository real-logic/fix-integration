/*******************************************************************************
 * Copyright (c) quickfixengine.org  All rights reserved.
 *
 * This file is part of the QuickFIX FIX Engine
 *
 * This file may be distributed under the terms of the quickfixengine.org
 * license as defined by quickfixengine.org and appearing in the file
 * LICENSE included in the packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * See http://www.quickfixengine.org/LICENSE for licensing information.
 *
 * Contact ask@quickfixengine.org if any conditions of this licensing
 * are not clear to you.
 ******************************************************************************/

package uk.co.real_logic.fix_gateway.acceptance_tests;

import org.junit.Assert;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.mina.message.FIXProtocolCodecFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class TestConnection
{
    private static final HashMap<String, IoConnector> CONNECTORS = new HashMap<>();

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final HashMap<Integer, TestIoHandler> ioHandlers = new HashMap<Integer, TestIoHandler>();

    public void sendMessage(final int clientId, final String message) throws IOException
    {
        final TestIoHandler handler = getIoHandler(clientId);
        handler.getSession().write(message);
    }

    public TestIoHandler getIoHandler(final int clientId)
    {
        synchronized (ioHandlers)
        {
            return ioHandlers.get(Integer.valueOf(clientId));
        }
    }

    public void tearDown()
    {
        for (final TestIoHandler testIoHandler : ioHandlers.values())
        {
            closeConnection(testIoHandler);
        }
        ioHandlers.clear();
    }

    public void disconnect(final int clientId)
    {
        final TestIoHandler ioHandler = getIoHandler(clientId);
        closeConnection(ioHandler);
        ioHandler.assertNoMessages();
    }

    private void closeConnection(final TestIoHandler testIoHandler)
    {
        final CloseFuture closeFuture = testIoHandler.getSession().close(true);
        closeFuture.awaitUninterruptibly();
    }

    public void waitForClientDisconnect(final int clientId) throws IOException, InterruptedException
    {
        getIoHandler(clientId).waitForDisconnect();
    }

    public void connect(final int clientId, final int port)
        throws IOException
    {
        IoConnector connector = CONNECTORS.get(Integer.toString(clientId));
        if (connector != null)
        {
            log.info("Disposing connector for clientId " + clientId);
            connector.dispose();
        }

        connector = new NioSocketConnector();
        CONNECTORS.put(Integer.toString(clientId), connector);
        final SocketAddress address = new InetSocketAddress("localhost", port);

        final TestIoHandler testIoHandler = new TestIoHandler();
        synchronized (ioHandlers)
        {
            ioHandlers.put(clientId, testIoHandler);
            connector.setHandler(testIoHandler);
            final ConnectFuture future = connector.connect(address);
            future.awaitUninterruptibly(5000L);
            Assert.assertTrue("connection to server failed", future.isConnected());
        }
    }

    public class TestIoHandler extends IoHandlerAdapter
    {
        private IoSession session;
        private final BlockingQueue<Object> messages = new LinkedBlockingQueue<Object>();
        private final CountDownLatch sessionCreatedLatch = new CountDownLatch(1);
        private final CountDownLatch disconnectLatch = new CountDownLatch(1);

        public void sessionCreated(IoSession session) throws Exception
        {
            super.sessionCreated(session);
            this.session = session;
            session.getFilterChain().addLast("codec",
                new ProtocolCodecFilter(new FIXProtocolCodecFactory()));
            sessionCreatedLatch.countDown();
        }

        public void exceptionCaught(IoSession session, Throwable cause) throws Exception
        {
            super.exceptionCaught(session, cause);
            log.error(cause.getMessage(), cause);
        }

        public void sessionClosed(IoSession session) throws Exception
        {
            super.sessionClosed(session);
            disconnectLatch.countDown();
        }

        public void messageReceived(IoSession session, Object message) throws Exception
        {
            messages.add(message);
        }

        public IoSession getSession()
        {
            try
            {
                // 10 seconds more than retry time in ATServer.run()
                final boolean await = sessionCreatedLatch.await(70, TimeUnit.SECONDS);
                if (!await)
                {
                    log.error("sessionCreatedLatch timed out. Dumping threads...");

                    final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                    final long[] threadIds = bean.findDeadlockedThreads();
                    final List<String> deadlockedThreads = new ArrayList<String>();
                    if (threadIds != null)
                    {
                        for (long threadId : threadIds)
                        {
                            final ThreadInfo threadInfo = bean.getThreadInfo(threadId);
                            deadlockedThreads.add(threadInfo.getThreadId() + ": " + threadInfo.getThreadName() +
                                " state: " + threadInfo.getThreadState());
                        }
                    }
                    if (!deadlockedThreads.isEmpty())
                    {
                        log.error("Showing deadlocked threads:");
                        for (String deadlockedThread : deadlockedThreads)
                        {
                            log.error(deadlockedThread);
                        }
                    }
                }
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
            return session;
        }

        public String pollMessage()
        {
            return (String) messages.poll();
        }

        public void waitForDisconnect() throws InterruptedException
        {
            if (!disconnectLatch.await(500000L, TimeUnit.MILLISECONDS))
            {
                Assert.fail("client not disconnected");
            }

            assertNoMessages();
        }

        private void assertNoMessages()
        {
            assertTrue("Received messages before disconnect: " + messages, messages.isEmpty());
        }
    }
}
