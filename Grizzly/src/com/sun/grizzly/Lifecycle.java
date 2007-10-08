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

import java.io.IOException;

/**
 * Simple Life cycle interface used to manage Grizzly component.
 *
 * @author Jeanfrancois Arcand
 */
public interface Lifecycle {
    
    /**
     * Start the Lifecycle.  This is the interface where an object that
     * implements Lifecycle will start the object and begin its processing.
     * @throws java.io.IOException 
     */
    public void start() throws IOException;
    
    /**
     * Stops the Lifecycle.  This is the interface where an object that
     * implements Lifecycle will stop the object's processing and perform
     * any additional cleanup before it shutdown.
     * @throws java.io.IOException 
     */
    public void stop() throws IOException;
    
    /**
     * Pause this Lifecycle. This is the interface where an object that
     * implements Lifecycle will pause the object's processing.  Processing
     * may be resumed via the resume() interface or stopped via the stop()
     * interface after this interface has been called. Common uses for pause()
     * and resume() will be to support use cases such as reconfiguration.
     * @throws java.io.IOException 
     */
    public void pause() throws IOException;
    
    /**
     * Resume this Lifecycle.  This is the interface where an object that
     * implements Lifecycle will resume a paused object's processing. When
     * called processing will resume. Common uses for pause() and resume()
     * will be to support use cases such as reconfiguration.
     * @throws java.io.IOException 
     */
    public void resume() throws IOException;
    
}
