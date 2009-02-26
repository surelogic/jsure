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
import com.sun.grizzly.util.Copyable;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Set;

/**
 * A SelectorHandler handles all java.nio.channels.Selector operations.
 * One or more instance of a Selector are handled by SelectorHandler.
 * The logic for processing of SelectionKey interest (OP_ACCEPT,OP_READ, etc.)
 * is usually defined using an instance of SelectorHandler.
 *
 * This class represents a TCP implementation of a SelectorHandler,
 * which handles "accept" events by registering newly accepted connections
 * to auxiliary <code>Controller<code>s in a round robin fashion.
 *
 * @author Alexey Stashok
 */
public class RoundRobinSelectorHandler extends TCPSelectorHandler
        implements ComplexSelectorHandler {
    private ReadController[] rrControllers;
    private int roundRobinCounter;
    private Set<Protocol> customProtocols;
    
    public RoundRobinSelectorHandler() {}
    
    public RoundRobinSelectorHandler(ReadController[] rrControllers) {
        this.rrControllers = rrControllers;
    }
    
    @Override
    public void copyTo(Copyable copy) {
        super.copyTo(copy);
        RoundRobinSelectorHandler copyHandler = (RoundRobinSelectorHandler) copy;
        copyHandler.roundRobinCounter = roundRobinCounter;
        copyHandler.rrControllers = rrControllers;
    }
    
    @Override
    public boolean onAcceptInterest(SelectionKey key, Context context) throws IOException {
        return onAcceptInterest(key, context, context.getController().getSelectorHandler(protocol()));
    }
    
    public boolean onAcceptInterest(SelectionKey key, Context context,
            SelectorHandler protocolSelectorHandler) throws IOException {
        
        ReadController auxController = nextController();
        
        SelectableChannel channel = protocolSelectorHandler.acceptWithoutRegistration(key);
        
        if (channel != null) {
            protocolSelectorHandler.configureChannel(channel);
            auxController.addChannel(channel, protocolSelectorHandler.protocol());
        }
        return false;
    }
    
    /**
     * Add custom protocol support
     * @param customProtocol custom <code>Protocol</code>
     */
    public void addProtocolSupport(Protocol customProtocol) {
        customProtocols.add(customProtocol);
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean supportsProtocol(Protocol protocol) {
        return protocol == Protocol.TCP || protocol == Protocol.TLS ||
                customProtocols.contains(protocol);
    }
    
    /**
     * Return next aux. ReadController to process an accepted connection
     * @return <code>ReadController</ccode>
     */
    private ReadController nextController() {
        return rrControllers[roundRobinCounter++ % rrControllers.length];
    }
}
