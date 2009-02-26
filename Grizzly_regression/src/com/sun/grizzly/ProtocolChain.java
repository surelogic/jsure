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

/**
 * <p>
 * This class implement the "Chain of Responsibility" pattern (for more info, 
 * take a look at the classic "Gang of Four" design patterns book). Towards 
 * that end, the Chain API models a computation as a series of "protocol filter"
 * that can be combined into a "protocol chain". 
 * </p><p>
 * The API for ProtocolFilter consists of a two methods (execute() and 
 * postExecute) which is passed a "protocol context" parameter containing the 
 * dynamic state of the computation, and whose return value is a boolean 
 * that determines whether or not processing for the current chain has been 
 * completed (false), or whether processing should be delegated to the next 
 * ProtocolFilter in the chain (true). The owning ProtocolChain  must call the
 * postExectute() method of each ProtocolFilter in a ProtocolChain in reverse 
 * order of the invocation of their execute() methods.
 * </p><p>
 * The following picture describe how it ProtocolFilter(s) 
 * </p><p><pre><code>
 * -----------------------------------------------------------------------------
 * - ProtocolFilter1.execute() --> ProtocolFilter2.execute() -------|          -
 * -                                                                |          -
 * -                                                                |          -
 * -                                                                |          -
 * - ProtocolFilter1.postExecute() <-- ProtocolFilter2.postExecute()|          -    
 * -----------------------------------------------------------------------------
 * </code></pre></p><p>
 * The "context" abstraction is designed to isolate ProtocolFilter
 * implementations from the environment in which they are run 
 * (such as a ProtocolFilter that can be used in either IIOP or HTTP parsing, 
 * without being tied directly to the API contracts of either of these 
 * environments). For ProtocolFilter that need to allocate resources prior to 
 * delegation, and then release them upon return (even if a delegated-to 
 * ProtocolFilter throws an exception), the "postExecute" method can be used 
 * for cleanup. 
 * </p>
 * @author Jeanfrancois Arcand
 */
public interface ProtocolChain{

    /**
     * Add a <code>ProtocolFilter</code> to the list. <code>ProtocolFilter</code>
     * will be invoked in the order they have been added.
     * @param protocolFilter <code>ProtocolFilter</code>
     * @return <code>ProtocolFilter</code> added successfully (yes/no) ?
     */
    public boolean addFilter(ProtocolFilter protocolFilter);
    
    
    /**
     * Remove the <code>ProtocolFilter</code> from this chain.
     * @param theFilter <code>ProtocolFilter</code> 
     * @return <code>ProtocolFilter</code> removed successfully (yes/no) ?
     */
    public boolean removeFilter(ProtocolFilter theFilter);
    
     
    /**
     * Insert a <code>ProtocolFilter</code> to the list at position 'pos'.
     * @param pos The insertion position 
     * @param protocolFilter <code>ProtocolFilter</code>
     */
    public void addFilter(int pos, ProtocolFilter protocolFilter);

    
    /**
     * Execute using the <code>Context</code> instance.
     * @param context <code>Context<code>
     * @throws java.lang.Exception 
     */
    public void execute(Context context) throws Exception;
    
  
}
