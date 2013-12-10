/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2000-2012 Oracle and/or its affiliates. All rights reserved.
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
 * @(#)RemoteAcknowledgeException.java	1.3 06/27/07
 */ 

package com.sun.messaging.jmq.jmsclient;

import java.util.Hashtable;

import com.sun.messaging.jms.JMSException;

/**
 * 
 * This exception is thrown when a remote broker is killed and one of the following 
 * activities occurred:
 * 
 * 1. Auto-ack/dups-ok ack a message originated from the killed remote broker.
 * 
 * 2. Client-ack message(s) and the messages are originated from the killed remote broker.
 * 
 * 3. Client runtime sending a PREPARE or COMMIT protocol packet to broker and the
 * messages to be prepared/committed are originated from the killed remote broker.
 * 
 */
public class RemoteAcknowledgeException extends JMSException {
	
	/**
	 * property name in the props entry.  The property vale is a space
     * separated consumer UID String.
	 */
	public static final String JMQRemoteConsumerIDs = "JMQRemoteConsumerIDs";

	private Hashtable props = null;
	
	/** Constructs a <CODE>JMSException</CODE> with the specified reason and
	   *  error code.
	   *
	   *  @param  reason        a description of the exception
	   *  @param  errorCode     a string specifying the vendor-specific
	   *                        error code
	   **/
	  public
	  RemoteAcknowledgeException (String reason, String errorCode) {
	    super(reason, errorCode);
	  }

	  /** Constructs a <CODE>JMSException</CODE> with the specified reason and with
	   *  the error code defaulting to null.
	   *
	   *  @param  reason        a description of the exception
	   **/
	  public
	  RemoteAcknowledgeException (String reason) {
	    super(reason);
	  }

	  /** Constructs a <CODE>JMSException</CODE> with the specified reason,
	   *  error code, and a specified cause.
	   *
	   *  @param  reason        a description of the exception
	   *  @param  errorCode     a string specifying the vendor-specific
	   *                        error code
	   *  @param  cause         the cause. A <tt>null</tt> value is permitted,
	   *                        and indicates that the cause is non-existent
	   *                        or unknown.
	   **/
	  public
	  RemoteAcknowledgeException (String reason, String errorCode, Throwable cause) {
	    super(reason, errorCode, cause);
	  }
	  
	  /**
	   * Get the property object associate with this remote exception.
	   * 
	   * @return the property object associate with this remote exception.
	   */
	  public Hashtable getProperties() {
		  
		  if (this.props == null) {
			  synchronized (this) {
				  if (this.props == null) {
					  props = new Hashtable();
				  }
			  }
		  }
		  
		  return this.props;
	  }
	  
	  /**
	   * Set properties associate with this remote exception.
	   * @param p the property object associate with the remote exception.
	   */
	  public void setProperties(Hashtable p) {
		  synchronized (this) {
			  this.props = p;
		  }
	  }
	  
}
