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

package com.sun.grizzly.util;

/**
 * Class Cloner creates a clone of given object, 
 * which should implement interface <code>Copyable</code>
 * 
 * @author Alexey Stashok
 */
public class Cloner {
    /**
     * Method creates a clone of given object pattern
     * Pattern parameter should implement <class>Copyable</class> interface
     * 
     * @param pattern represents object, which will be cloned. Should implement <code>Copyable</code>
     * @return clone
     */
    public static <T extends Copyable> T clone(T pattern) {
        try {
            T copy = (T) pattern.getClass().newInstance();
            pattern.copyTo(copy);
            return copy;
        } catch (Exception e) {
            throw new RuntimeException("Error copying objects! " + e.getClass().getName() + ": " + e.getMessage());
        }
    }
}
