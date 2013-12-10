/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2000-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

/*
 */ 

package com.sun.messaging.jmq.jmsserver.service;

import java.util.List;
import java.util.Vector;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import com.sun.messaging.jmq.util.DestType;
import com.sun.messaging.jmq.jmsserver.Globals;
import com.sun.messaging.jmq.util.MQThread;
import com.sun.messaging.jmq.jmsserver.core.Consumer;
import com.sun.messaging.jmq.jmsserver.core.Destination;
import com.sun.messaging.jmq.jmsserver.core.DestinationList;
import com.sun.messaging.jmq.jmsserver.core.DestinationUID;
import com.sun.messaging.jmq.jmsserver.core.BrokerAddress;
import com.sun.messaging.jmq.jmsserver.service.imq.IMQConnection;
import com.sun.messaging.jmq.jmsserver.resources.BrokerResources;
import com.sun.messaging.jmq.jmsserver.common.handlers.InfoRequestHandler;
import com.sun.messaging.jmq.util.log.Logger;

/**
 */

public class ConsumerInfoNotifyManager implements Runnable 
{
    /**
     * consumer info types (protocol)
     */
    private static final int CONSUMER_NOT_READY = 0;
    private static final int CONSUMER_READY = 1;
    private static final int CONSUMER_ALL_EVENT = -1;

    private static boolean DEBUG = false;
    private Logger logger = Globals.getLogger();

    private MQThread notifyThread = null;
    private Vector eventQueue = new Vector();
    private ConnectionManager cm = null;
    private boolean shutdown = false;
    private boolean requested = false; 
    
    public ConsumerInfoNotifyManager(ConnectionManager cm) {
        this.cm = cm;
        if (Globals.getLogger().getLevel() <= Logger.DEBUG) DEBUG = true;
    }

    private synchronized void wakeup() {
        createNotifyThread();
        notifyAll();
    }

    private void createNotifyThread() {
        if (notifyThread == null) {
            notifyThread = new MQThread(this, "ConsumerInfoNotifyManager");
            notifyThread.setDaemon(true);
            notifyThread.start();
        }
    }

    protected synchronized void shutdown() {
        shutdown = true;
        eventQueue.clear();
        notifyAll();
    }

    public void run() {
        ArrayList pendingEvents = new ArrayList();
        DestinationList DL = Globals.getDestinationList();

        while (!shutdown) {

            boolean dowait =true;
            List list = null;
            synchronized(eventQueue) {
                list = new ArrayList(eventQueue);
            }
            if (list.size() > 0 && pendingEvents.size() > 0) {
                Iterator itr = list.iterator();
                Object e = null;
                while(itr.hasNext()) {
                    e = itr.next();
                    if (!pendingEvents.contains(e)) {
                        dowait = false;
                        break;
                    }
                }
            } else if (pendingEvents.size() == 0) {
                dowait = eventQueue.isEmpty();
            }

            synchronized (this) {
                if (dowait || eventQueue.isEmpty()) {
                    try {
                    wait();
                    } catch (InterruptedException inte) {}
                }
            }

            if (shutdown) {
                return;
            }

            HashMap notifications = new  HashMap();
            Object[] events = eventQueue.toArray();
            Object o = null;
            for (int i = 0; i < events.length && !shutdown; i++) {
                o = events[i];  
                if (DEBUG) {
                    logger.log(logger.INFO, "Processing "+o);
                }
                if (o instanceof ConsumerAddedEvent) {
                    ConsumerAddedEvent e =  (ConsumerAddedEvent)o;
                    IMQConnection conn =  (IMQConnection)cm.getConnection(e.connid);
                    if (e.dest.getAllActiveConsumerCount() > 0) {
                        if (conn == null || conn.isConnectionStarted()) {
                            notifications.put(e.dest.getDestinationUID(),
                                              new ConsumerInfoNotification(
                                              e.dest.getDestinationUID(),
                                              e.dest.getType(), CONSUMER_READY));
                        } else {
                            pendingEvents.add(o);
                            continue;
                        }
                    } else {
                        notifications.put(e.dest.getDestinationUID(),
                                          new ConsumerInfoNotification(
                                          e.dest.getDestinationUID(),
                                          e.dest.getType(), CONSUMER_NOT_READY));
                    }
                    eventQueue.remove(o);
                    pendingEvents.remove(o);
                    continue;
                }
                if (o instanceof RemoteConsumerAddedEvent) {
                    RemoteConsumerAddedEvent e =  (RemoteConsumerAddedEvent)o;
                    if (e.dest.getAllActiveConsumerCount() > 0) {
                        notifications.put(e.dest.getDestinationUID(),
                                          new ConsumerInfoNotification(
                                          e.dest.getDestinationUID(),
                                          e.dest.getType(), CONSUMER_READY));
                    } else {
                        notifications.put(e.dest.getDestinationUID(),
                                          new ConsumerInfoNotification(
                                          e.dest.getDestinationUID(),
                                          e.dest.getType(), CONSUMER_NOT_READY));
                    }
                    eventQueue.remove(o);
                    continue;

                }
                if (o instanceof ConsumerRemovedEvent) {
                    ConsumerRemovedEvent e =  (ConsumerRemovedEvent)o;
                    if (e.dest.getAllActiveConsumerCount() == 0) { 
                        notifications.put(e.dest.getDestinationUID(),
                                          new ConsumerInfoNotification(
                                          e.dest.getDestinationUID(),
                                          e.dest.getType(), CONSUMER_NOT_READY));
                    }
                    eventQueue.remove(o);
                    continue;
                }
                if (o instanceof ConnectionStartedEvent) {
                    ConnectionStartedEvent e =  (ConnectionStartedEvent)o;
                    for (int j = 0; j < events.length && !shutdown; j++) {
                        Object oo = events[j];
                        if (oo instanceof ConsumerAddedEvent) {
                            ConsumerAddedEvent ee =  (ConsumerAddedEvent)oo;
                            IMQConnection conn =  (IMQConnection)cm.getConnection(ee.connid);
                            if (conn != null && conn == e.conn &&
                                ee.dest.getAllActiveConsumerCount() > 0) {
                                notifications.put(ee.dest.getDestinationUID(),
                                                  new ConsumerInfoNotification(
                                                  ee.dest.getDestinationUID(),
                                                  ee.dest.getType(), CONSUMER_READY));
                                pendingEvents.remove(ee);
                            }
                        }   
                    }
                    eventQueue.remove(e);
                    continue;
                }
                if (o instanceof ConsumerInfoRequestEvent) {
                    boolean foundmatch = false;
                    boolean hasconsumer = false;
                    boolean notifyadded = false;
                    ConsumerInfoRequestEvent e =  (ConsumerInfoRequestEvent)o;
                    Iterator[] itrs = DL.getAllDestinations(null);
                    Iterator itr = itrs[0]; //PART
                    while (itr.hasNext()) {
                        Destination d = (Destination)itr.next();
                        if (d.isInternal()) {
                            continue;
                        }
                        if ((!e.duid.isWildcard() && d.getDestinationUID().equals(e.duid))) {
                            foundmatch = true;
                            if (d.getAllActiveConsumerCount() == 0) {
                                notifications.put(d.getDestinationUID(),
                                                  new ConsumerInfoNotification(
                                                  d.getDestinationUID(),
                                                  d.getType(), CONSUMER_NOT_READY,
                                                  ((ConsumerInfoRequestEvent)o).infoType, true));
                                notifyadded = true;
                                break;
                            }
                            hasconsumer = true;
                            Iterator itrr = d.getAllActiveConsumers().iterator();
                            while (itrr.hasNext()) {
                                Consumer c = (Consumer)itrr.next();
                                IMQConnection conn = (IMQConnection)cm.getConnection(c.getConnectionUID());
                                BrokerAddress ba = c.getConsumerUID().getBrokerAddress();
                                if ((conn != null && conn.isConnectionStarted()) ||
                                     (ba != null && ba != Globals.getMyAddress())) {
                                    notifications.put(d.getDestinationUID(),
                                                      new ConsumerInfoNotification(
                                                      d.getDestinationUID(),
                                                      d.getType(), CONSUMER_READY,
                                                      ((ConsumerInfoRequestEvent)o).infoType, true));
                                    notifyadded = true;
                                    break; 
                                }
                            }
                            break;
                        }
                        if (e.duid.isWildcard() && DestinationUID.match(d.getDestinationUID(), e.duid)) {
                            foundmatch = true;
                            if (d.getAllActiveConsumerCount() == 0) {
                                continue;
                            }
                            hasconsumer = true;
                            Iterator itrr = d.getAllActiveConsumers().iterator();
                            while (itrr.hasNext()) {
                                Consumer c = (Consumer)itrr.next();
                                IMQConnection conn = (IMQConnection)cm.getConnection(c.getConnectionUID());
                                BrokerAddress ba = c.getConsumerUID().getBrokerAddress();
                                if ((conn != null && conn.isConnectionStarted()) ||
                                     (ba != null && ba != Globals.getMyAddress())) {
                                    notifications.put(d.getDestinationUID(),
                                                  new ConsumerInfoNotification(
                                                  d.getDestinationUID(),
                                                  e.destType, CONSUMER_READY,
                                                  ((ConsumerInfoRequestEvent)o).infoType, true));
                                    notifyadded = true;
                                    break;
                                }
                            }
                            if (notifyadded) {
                                break;
                            }
                        }
                    }
                    if (!foundmatch || (!hasconsumer && !notifyadded)) {
                        notifications.put(e.duid,
                                          new ConsumerInfoNotification(
                                          e.duid,
                                          e.destType, CONSUMER_NOT_READY,
                                          ((ConsumerInfoRequestEvent)o).infoType, true));
                    }
                    eventQueue.remove(o);
                }
            }
            Iterator itr = notifications.values().iterator();
            ConsumerInfoNotification cin = null;
            while (itr.hasNext()) {
                cin = (ConsumerInfoNotification)itr.next();  
                if (DEBUG) {
                    logger.log(logger.INFO, "Sending "+cin);
                }
                if (cin.shouldNotify()) {
                    cm.sendConsumerInfo(InfoRequestHandler.REQUEST_CONSUMER_INFO,
                        cin.duid, cin.destType, cin.infoType, cin.sendToWildcard);
                }
            }
            notifications.clear();
        }
    }

    public void remoteConsumerAdded(Destination dest) {
        if (!requested) return;
        eventQueue.add(new RemoteConsumerAddedEvent(dest));
        wakeup(); 
    }

    public void consumerAdded(Destination dest, Connection conn) {
        if (!requested) return;
        eventQueue.add(new ConsumerAddedEvent(dest, 
                           (conn == null ? null: conn.getConnectionUID())));
        wakeup(); 
    }

    public void consumerRemoved(Destination dest) {
        if (!requested) return;
        eventQueue.add(new ConsumerRemovedEvent(dest));
        wakeup(); 
    }


    public void connectionStarted(Connection conn) {
        if (!requested) return;
        eventQueue.add(new ConnectionStartedEvent(conn));
        wakeup();
    }

    public void consumerInfoRequested(Connection conn, DestinationUID duid, int destType) {
        consumerInfoRequested(conn, duid, destType, CONSUMER_ALL_EVENT);
    }

    public void consumerInfoRequested(Connection conn, DestinationUID duid,
                                      int destType, int infoType) {
        requested = true;
        eventQueue.add(new ConsumerInfoRequestEvent(conn, duid, destType, infoType));
        wakeup();
    }

    protected static String toString(int infoType) {
        switch (infoType) {
            case CONSUMER_NOT_READY: return "CONSUMER_NOT_READY"; 
            case CONSUMER_READY:     return "CONSUMER_READY";
            case CONSUMER_ALL_EVENT: return "CONSUMER_ALL_EVENT";
            default:                 return "UNKNOWN";
        }
    }

    static class RemoteConsumerAddedEvent {
        Destination dest = null;

        public RemoteConsumerAddedEvent(Destination dest) {
            this.dest = dest;
        }
        public String toString() {
            return "RemoteConsumerAddedEvent: dest="+dest;
        }
    }

    static class ConsumerAddedEvent {
        Destination dest = null;
        ConnectionUID connid = null;

        public ConsumerAddedEvent(Destination dest, ConnectionUID connid) {
            this.dest = dest;
            this.connid = connid;
        }
        public String toString() {
            return "ConsumerAddedEvent: dest="+dest+", conn="+connid;
        }
    }

    static class ConsumerRemovedEvent {
        Destination dest = null;

        public ConsumerRemovedEvent(Destination dest) {
            this.dest = dest;
        }
        public String toString() {
            return "ConsumerRemovedEvent: dest="+dest;
        }
    }

    static class ConnectionStartedEvent {
        Connection conn = null;

        public ConnectionStartedEvent(Connection conn) {
            this.conn = conn;
        }
        public String toString() {
            return "ConnectionStartedEvent: conn="+conn;
        }
    }

    static class ConsumerInfoRequestEvent {
        Connection conn = null;
        DestinationUID duid = null;
        int destType;
        int infoType;

        public ConsumerInfoRequestEvent(Connection conn,
                                        DestinationUID duid,
                                        int destType, int infoType) {
            this.conn = conn;
            this.duid = duid;
            this.destType = destType;
            this.infoType = infoType;
        }
        public String toString() {
            return "ConsumerInfoRequestEvent: conn="+conn+", duid="+duid+
                   ", destType="+DestType.toString(destType)+
                   ", infoType="+ConsumerInfoNotifyManager.toString(infoType);
        }
    }

    static class ConsumerInfoNotification {
        DestinationUID duid = null;
        int destType;
        int infoType;
        int requestInfoType = CONSUMER_ALL_EVENT;
        boolean sendToWildcard = false;

        public ConsumerInfoNotification(DestinationUID duid, 
                                        int destType, int infoType) {
            this.duid = duid;
            this.destType = destType;
            this.infoType = infoType;
            if (infoType != CONSUMER_NOT_READY) {
                sendToWildcard = true;
            }
        }

        public ConsumerInfoNotification(DestinationUID duid, 
                                        int destType, int infoType,
                                        int requestInfoType,
                                        boolean sendToWildcard) {
            this.duid = duid;
            this.destType = destType;
            this.infoType = infoType;
            this.requestInfoType = requestInfoType;
            this.sendToWildcard = sendToWildcard;
        }

        public boolean shouldNotify() {
            if (requestInfoType == CONSUMER_ALL_EVENT) {
                return true;
            }
            return (requestInfoType == infoType);
        }

        public String toString() {
            return "ConsumerInfoNotification: duid="+duid+", destType="+
                    DestType.toString(destType)+", infoType="+ConsumerInfoNotifyManager.toString(infoType);
        }

    }
}
