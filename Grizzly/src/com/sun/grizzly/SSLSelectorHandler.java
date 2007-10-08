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

import java.util.concurrent.Callable;

/**
 * A SelectorHandler handles all java.nio.channels.Selector operations.
 * One or more instance of a Selector are handled by SelectorHandler.
 * The logic for processing of SelectionKey interest (OP_ACCEPT,OP_READ, etc.)
 * is usually defined using an instance of SelectorHandler.
 *
 * This class represents a SSL (secured) implementation of a SelectorHandler.
 * This class first bind a ServerSocketChannel to a TCP port and then start
 * waiting for NIO events.
 *
 * @author Alexey Stashok
 */
public class SSLSelectorHandler extends TCPSelectorHandler {
    public SSLSelectorHandler() {
        super();
    }
    
    /**
     * SSLSelectorHandler constructor
     * @param isClient true, if SSLSelectorHandler will work only in client mode (will
     * not listen for incoming client connections).
     */
    public SSLSelectorHandler(boolean isClient) {
        super(isClient);
    }
    
    @Override
    public Controller.Protocol protocol(){
        return Controller.Protocol.TLS;
    }

    @Override
    protected Callable<ConnectorHandler> getConnectorInstanceHandlerDelegate() {
        return new Callable<ConnectorHandler>() {
            public ConnectorHandler call() throws Exception {
                return new SSLConnectorHandler();
            }
        };
    }
}
