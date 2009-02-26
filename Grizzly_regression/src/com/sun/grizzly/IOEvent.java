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
 * Copyright 2007 Sun Microsystems, Inc. All rights reserved.
 */

package com.sun.grizzly;

/**
 * When a non blocking operation occurs, the SelectorHandler implementation 
 * will invoke an instance of <code>CallbackHandler</code> with an instance of 
 * this class. By Default, the class will be instanciated using 
 * IOEvent<SelectionKey>. 
 *
 * @param E 
 * @author Jeanfrancois Arcand
 */
public interface IOEvent<E> {
    
    /**
     * Attach an E and return the previous value.
     * @param e  object to attache
     * @return  previous attached value
     */
    public E attach(E e);

    
    /** 
     * Return the current attachment.
     * @return  the attachment
     */
    public E attachment();

    /**
     * Simple IOEvent implementation
     */
    public class DefaultIOEvent<E> implements IOEvent<E> {
        private E attachment;

        public DefaultIOEvent(E attachment) {
            this.attachment = attachment;
        }

        public E attach(E attachment) {
            this.attachment = attachment;
            return attachment;
        }

        public E attachment() {
            return attachment;
        }
    }
}
