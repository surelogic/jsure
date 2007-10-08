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
import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * A ComplexSelectorHandler handles all java.nio.channels.Selector operations
 * similar way <code>SelectorHandler</code> does.
 * But can work with several <code>Protocol</code>s at the same time.
 *
 * @author Alexey Stashok
 */
public interface ComplexSelectorHandler extends SelectorHandler {
    
    /**
     * Checks if protocol is supported by RoundRobinSelectorHandler
     *
     * @param protocol Network protocol name
     * @return true if protocol is supported, false otherwise
     */
    public boolean supportsProtocol(Protocol protocol);
    
    
    /**
     * Handle OP_ACCEPT.
     * Additionaly to <code>SelectorHandler</code> implementation, protocol
     * information is passed
     *
     * @param key <code>SelectionKey</code>
     * @param protocolSelectorHandler underlying <code>SelectorHandler</code>
     * @param controllerCtx <code>Context</code>
     * @return true if and only if the ProtocolChain must be invoked after
     *              executing this method.
     * @throws java.io.IOException
     */
    public boolean onAcceptInterest(SelectionKey key, 
            Context controllerCtx, SelectorHandler protocolSelectorHandler)
            throws IOException;
    
}
