/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2000-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package com.sun.messaging.bridge.service.jms;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService; 
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;
import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;
import javax.jms.Connection;
import javax.jms.XAConnection;
import javax.jms.JMSException;
import com.sun.messaging.bridge.service.jms.xml.JMSBridgeXMLConstant;
import com.sun.messaging.bridge.service.jms.resources.JMSBridgeResources;


/**
 *
 * @author amyk
 *
 */

public class SharedConnectionFactory implements Runnable {
    
    private Logger _logger = null;
    private Object _cf = null; 
    private int _idleTimeout = 0;  //in secs
    private int _maxRetries = 0;
    private int _retryInterval = 0;

    private final int _closeWaittime = 15; //secs

    private ScheduledExecutorService _scheduler = null;

    private final ReentrantLock _lock = new ReentrantLock();
    private final Condition _refcnt0 = _lock.newCondition();
    private SharedConnection _conn = null;
    private int _refcnt = 0;
    private ScheduledFuture _future = null;
    private boolean _closed = false;

    private String _username = null;
    private String _password = null;

    private EventNotifier _notifier = new EventNotifier();
    private static JMSBridgeResources _jbr = JMSBridge.getJMSBridgeResources();

    public SharedConnectionFactory(Object cf, Properties attrs, Logger logger) throws Exception {
        _cf = cf;
        _logger = logger;

        String val = attrs.getProperty(JMSBridgeXMLConstant.CF.USERNAME); 
        if (val != null) {
            _username = val.trim();
            _password = attrs.getProperty(JMSBridgeXMLConstant.CF.PASSWORD);
        } 
        _idleTimeout = Integer.parseInt(attrs.getProperty(
                                       JMSBridgeXMLConstant.CF.IDLETIMEOUT, 
                                       JMSBridgeXMLConstant.CF.IDLETIMEOUT_DEFAULT));
        if (_idleTimeout < 0) _idleTimeout = 0;
        _maxRetries = Integer.parseInt(attrs.getProperty(
                                      JMSBridgeXMLConstant.CF.CONNECTATTEMPTS,
                                      JMSBridgeXMLConstant.CF.CONNECTATTEMPTS_DEFAULT));
        _retryInterval = Integer.parseInt(attrs.getProperty(
                                         JMSBridgeXMLConstant.CF.CONNECTATTEMPTINTERVAL,
                                         JMSBridgeXMLConstant.CF.CONNECTATTEMPTINTERVAL_DEFAULT));
        if (_retryInterval < 0) _retryInterval = 0;

        _scheduler = Executors.newSingleThreadScheduledExecutor();
     
    }

    /** 
     */ 
    public Connection obtainConnection(Connection c, 
                                       String logstr,
                                       Object caller, boolean doReconnect) 
                                       throws Exception {
        _lock.lockInterruptibly();
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Obtaining shared connection from shared connection factory "+this);
        }
        if (_closed)  {
            if (c == null) {
            throw new JMSException(_jbr.getString(_jbr.X_SHARED_CF_CLOSED, this.toString())); 
            }
            try {
            c.close();
            } catch (Exception e) {
            _logger.log(Level.WARNING, "Unable to close conneciton from shared connection factory "+this);
            }
            throw new JMSException(_jbr.getString(_jbr.X_SHARED_CF_CLOSED, this.toString()));
        }
        if (c != null) {
            if (c instanceof XAConnection) {
                 _conn = new SharedXAConnectionImpl((XAConnection)c);
            } else {
                 _conn = new SharedConnectionImpl(c);
            }
        }
        if (_conn != null && !_conn.isValid()) {
            try {
                _logger.log(Level.INFO, _jbr.getString(_jbr.I_CLOSE_INVALID_CONN_IN_SHAREDCF, c.toString(), this.toString()));
                ((Connection)_conn).close();
            } catch (Exception e) {
                _logger.log(Level.WARNING, 
                "Unable to close invalid connection "+_conn+ " from shared connection factory "+this);
            }
            _conn = null;
        }
        try {
            if (_conn == null) {
                Connection cn = null;
                EventListener l = new EventListener(this);
                try {
                _notifier.addEventListener(EventListener.EventType.CONN_CLOSE, l);
                cn = JMSBridge.openConnection(_cf, _maxRetries, _retryInterval, _username, _password,
                                              logstr, caller, l, _logger, doReconnect);
                } finally {
                _notifier.removeEventListener(l);
                }
                if (_cf instanceof XAConnectionFactory) {
                    _conn = new SharedXAConnectionImpl((XAConnection)cn);
                } else {
                    _conn = new SharedConnectionImpl(cn);
                }
            }
            if (_closed) {
                try {
                ((Connection)_conn).close();
                } catch (Exception e) {
                _logger.log(Level.FINE, "Exception on closing connection "+_conn+": "+e.getMessage());
                }
                _conn = null;
                throw new JMSException(_jbr.getString(_jbr.X_SHARED_CF_CLOSED, this.toString()));
            }
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Increment refcnt in shared connection factory "+this);
            }
            _refcnt++;
            if (_future != null) {
                _future.cancel(true);
                _future = null;
            }
        } finally {
            _lock.unlock();
        }
        return (Connection)_conn;
    }

    /** 
     */ 
    public void returnConnection(Connection conn) throws Exception {
        _lock.lock();
        try {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Decrement refcnt in shared connection factory "+this);
            }
            _refcnt--;
            assert _refcnt >= 0;
            if (_refcnt == 0 && _idleTimeout > 0) {
                if (_future != null) _future.cancel(true);
                _logger.log(Level.INFO, _jbr.getString(_jbr.I_SCHEDULE_TIMEOUT_FOR_SHAREDCF, _idleTimeout, this.toString()));
                _future = _scheduler.schedule(this, _idleTimeout,
                                              TimeUnit.SECONDS); 
            }
            if (_refcnt == 0) _refcnt0.signalAll();
        } finally {
            _lock.unlock();
        }
    }

    /**
	 */
    public void run() {
        if (_logger.isLoggable(Level.FINE)) {
        _logger.log(Level.FINE, "Check idle timeout in shared connection factory "+this);
        }
        try {
            _lock.lockInterruptibly();
        } catch (Throwable t) {
            _logger.log(Level.WARNING, 
            "Unable to get lock for idle connection check in shared connection factory "+this+": "+t.getMessage());
            return;
        }
        try {
            if (_refcnt > 0) return; 
            _logger.log(Level.INFO, _jbr.getString(_jbr.I_CLOSE_TIMEOUT_CONN_IN_SHAREDCF, _conn.toString(), this.toString()));
            try {
                ((Connection)_conn).close();
            } catch (Throwable t) {
                _logger.log(Level.WARNING, 
                "Exception in closing idle timed out shared connection: "+t.getMessage()+" in shared connection factory "+this);
            } finally { 
                _conn = null;
            }
        } finally {
            _lock.unlock();
        }
    }

    public void close() throws Exception {
        _closed = true;

        _logger.log(Level.INFO, _jbr.getString(_jbr.I_CLOSE_SHAREDCF, this.toString()));

        _notifier.notifyEvent(EventListener.EventType.CONN_CLOSE, this);
        _scheduler.shutdownNow();

        boolean done = false;
        try {
            _lock.lockInterruptibly();
            try {

                if (_conn == null) return;

                if (_refcnt > 0) {
                    /**
                     * should not happen, all links/dmqs have been closed by now
                     * we want to close as soon as possible, no need to wait 
                     */
                    _logger.log(Level.WARNING, 
                    "Force close shared connection factory "+this+ " with outstanding reference count "+_refcnt);
                }
                ((Connection)_conn).close();
                done = true; 

            } catch (Exception e) {
                _logger.log(Level.WARNING, 
                "Exception in closing shared connection "+e.getMessage());

            } finally {
                _lock.unlock(); 
            }

        } finally {
            Connection c = (Connection)_conn;
            try {
            if (c != null && !done) c.close();
            } catch (Exception e) {
            _logger.log(Level.FINE, 
            "Exception in closing shared connection "+c+": "+e.getMessage());
            }
        }
    }

    public Object getCF() {
        return _cf;
    }

    public int getIdleTimeout() {
        return _idleTimeout;
    }

    public int getMaxRetries() {
        return _maxRetries;
    }

    public int getRetryInterval() {
        return _retryInterval;
    }

    public int getRefCount() {
        return _refcnt;
    }


    public String toString() {
        return _cf+"["+_refcnt+"]";
    }

}

