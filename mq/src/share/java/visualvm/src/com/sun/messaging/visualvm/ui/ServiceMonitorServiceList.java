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

package com.sun.messaging.visualvm.ui;

import com.sun.messaging.jms.management.server.MQObjectName;
import com.sun.messaging.jms.management.server.ServiceAttributes;
import com.sun.messaging.jms.management.server.ServiceOperations;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;

@SuppressWarnings("serial")
public class ServiceMonitorServiceList extends MultipleMBeanResourceList {

    public ServiceMonitorServiceList(DataViewComponent dvc) {
		super(dvc);
	}

	private static String initialDisplayedAttrsList[] = {
    	ServiceAttributes.NAME, // Primary attribute
    	ServiceAttributes.PORT,
    	ServiceAttributes.STATE_LABEL
    };
    
    @Override
	public String[] getinitialDisplayedAttrsList() {
        return initialDisplayedAttrsList;  
    }
    
    // copied from com.sun.messaging.jmq.jmsserver.management.mbeans.ServiceMonitor
    private String completeAttrsList[] = {
    	    ServiceAttributes.MSG_BYTES_IN,
    	    ServiceAttributes.MSG_BYTES_OUT,
    	    ServiceAttributes.NAME,
    	    ServiceAttributes.NUM_ACTIVE_THREADS,
    	    ServiceAttributes.NUM_CONNECTIONS,
    	    ServiceAttributes.NUM_CONNECTIONS_OPENED,
    	    ServiceAttributes.NUM_CONNECTIONS_REJECTED,
    	    ServiceAttributes.NUM_CONSUMERS,
    	    ServiceAttributes.NUM_MSGS_IN,
    	    ServiceAttributes.NUM_MSGS_OUT,
    	    ServiceAttributes.NUM_PKTS_IN,
    	    ServiceAttributes.NUM_PKTS_OUT,
    	    ServiceAttributes.NUM_PRODUCERS,
    	    ServiceAttributes.PORT,
    	    ServiceAttributes.PKT_BYTES_IN,
    	    ServiceAttributes.PKT_BYTES_OUT,
    	    ServiceAttributes.STATE,
    	    ServiceAttributes.STATE_LABEL
   };        
    
    @Override
	public String[] getCompleteAttrsList() {
        return completeAttrsList;
    }      
    
	public String getPrimaryAttribute() {
		return ServiceAttributes.NAME;
	} 
   
	@Override
	protected String getManagerMBeanName() {
		return MQObjectName.SERVICE_MANAGER_MONITOR_MBEAN_NAME;
	}

	@Override
	protected String getGetSubMbeanOperationName(){
		return ServiceOperations.GET_SERVICES;
	}    

    @Override
    public void handleItemQuery(Object obj) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

	@Override
	public int getCorner() {
		return DataViewComponent.BOTTOM_LEFT;
	}
}