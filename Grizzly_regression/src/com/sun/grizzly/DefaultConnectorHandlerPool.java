/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html or
 * glassfish/bootstrap/legal/CDDLv1.0.txt.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at glassfish/bootstrap/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */

package com.sun.grizzly;

import com.sun.grizzly.Controller.Protocol;

/**
 * Simple <code>ConnectorHandlerPool</code> implementation
 *
 * @author Alexey Stashok
 */
public class DefaultConnectorHandlerPool
        implements ConnectorHandlerPool<ConnectorHandler> {
    private Controller controller;
    
    public DefaultConnectorHandlerPool(Controller controller) {
        this.controller = controller;
    }
    
    public ConnectorHandler acquireConnectorHandler(Protocol protocol){
        ConnectorHandler ch = null;
        SelectorHandler selectorHandler = controller.getSelectorHandler(protocol);
        if (selectorHandler != null) {
            ch = selectorHandler.acquireConnectorHandler();
            ch.setController(controller);
        }
        
        return ch;
    }
    
    
    public void releaseConnectorHandler(ConnectorHandler connectorHandler){
        SelectorHandler selectorHandler = controller.getSelectorHandler(connectorHandler.protocol());
        if (selectorHandler != null) {
            selectorHandler.releaseConnectorHandler(connectorHandler);
        }
    }
}
