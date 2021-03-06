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

/*
 * @(#)ObjStoreCObj.java	1.24 06/27/07
 */ 

package com.sun.messaging.jmq.admin.apps.console;

import javax.swing.ImageIcon;
import javax.swing.tree.MutableTreeNode;

import com.sun.messaging.jmq.admin.util.Globals;
import com.sun.messaging.jmq.admin.resources.AdminConsoleResources;
import com.sun.messaging.jmq.admin.objstore.ObjStore;

/** 
 * This class is used in the JMQ Administration console
 * to store information related to a particular object store.
 *
 * @see ConsoleObj
 * @see ObjStoreAdminCObj
 *
 */
public class ObjStoreCObj extends ObjStoreAdminCObj  {

    private transient ObjStore			os = null;
    private ObjStoreDestListCObj	objStoreDestList = null;
    private ObjStoreConFactoryListCObj	objStoreConFactoryList = null;
    private static AdminConsoleResources acr = Globals.getAdminConsoleResources();

    /**
     * Create/initialize the admin explorer GUI component.
     */
    public ObjStoreCObj(ObjStore os) {
	this.os = os;

	objStoreDestList = new ObjStoreDestListCObj(os);
	objStoreConFactoryList = new ObjStoreConFactoryListCObj(os);

	insert(objStoreDestList, 0);
	insert(objStoreConFactoryList, 1);
    } 

    public void setObjStore(ObjStore os)  {
	this.os = os;
    }

    public ObjStore getObjStore()  {
	return (os);
    }

    public String getExplorerLabel()  {
	if (os.getDescription() != null)  {
	    return (os.getDescription());
	} else  {
	    return (os.getID());
        }
    }

    public String getExplorerToolTip()  {
	return (null);
    }

    public ImageIcon getExplorerIcon()  {
	if (os.isOpen())  {
	    return (AGraphics.adminImages[AGraphics.OBJSTORE]);
	} else  {
	    return (AGraphics.adminImages[AGraphics.OBJSTORE_DISCONNECTED]);
	}
    }

    public ObjStoreDestListCObj getObjStoreDestListCObj() {
	return this.objStoreDestList;
    }

    public ObjStoreConFactoryListCObj getObjStoreConFactoryListCObj() {
	return this.objStoreConFactoryList;
    }

    public String getActionLabel(int actionFlag, boolean forMenu)  {
	if (forMenu)  {
	    switch (actionFlag)  {
	    case ActionManager.CONNECT:
	        return (acr.getString(acr.I_MENU_CONNECT_OBJSTORE));

	    case ActionManager.DISCONNECT:
	        return (acr.getString(acr.I_MENU_DISCONNECT_OBJSTORE));

	    case ActionManager.DELETE:
	        return (acr.getString(acr.I_MENU_DELETE));

	    case ActionManager.PROPERTIES:
	        return (acr.getString(acr.I_MENU_PROPERTIES));
	    }
	} else  {
	    switch (actionFlag)  {
	    case ActionManager.CONNECT:
	        return (acr.getString(acr.I_CONNECT_OBJSTORE));

	    case ActionManager.DISCONNECT:
	        return (acr.getString(acr.I_DISCONNECT_OBJSTORE));

	    case ActionManager.DELETE:
	        return (acr.getString(acr.I_DELETE));

	    case ActionManager.PROPERTIES:
	        return (acr.getString(acr.I_PROPERTIES));
	    }
	}

	return (null);
    }

    public ImageIcon getActionIcon(int actionFlag)  {
	switch (actionFlag)  {
	case ActionManager.CONNECT:
	    return (AGraphics.adminImages[AGraphics.CONNECT_TO_OBJSTORE]);
	case ActionManager.DISCONNECT:
	    return (AGraphics.adminImages[AGraphics.DISCONNECT_FROM_OBJSTORE]);
	}

	return (null);
    }


    public void insert(MutableTreeNode node, int newIndex)  {
	if ((node instanceof ObjStoreDestListCObj) ||
	    (node instanceof ObjStoreConFactoryListCObj))  {
	    super.insert(node, newIndex);
	} else {
	    /*
	     * No special behaviour yet
	     */
	    super.insert(node, newIndex);
	}
    }


    public int getExplorerPopupMenuItemMask()  {
	return (ActionManager.DELETE | ActionManager.PROPERTIES
		| ActionManager.DISCONNECT | ActionManager.CONNECT);
    }

    public int getActiveActions()  {
	int	mask;

	if (os.isOpen())  {
	    mask =  ActionManager.DELETE  | ActionManager.PROPERTIES
		| ActionManager.DISCONNECT | ActionManager.REFRESH;
	} else  {
	    mask =  ActionManager.DELETE | ActionManager.PROPERTIES
		| ActionManager.CONNECT;
	}
	
	return (mask);
    }



    public String getInspectorPanelClassName()  {
	return (ConsoleUtils.getPackageName(this) + ".ObjStoreInspector");
    }

    public String getInspectorPanelId()  {
	return ("Object Store");
    }

    public String getInspectorPanelHeader()  {
	return (getInspectorPanelId());
    }
}
