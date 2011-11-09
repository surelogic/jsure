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

package com.sun.grizzly.filter;

import com.sun.grizzly.*;
import com.sun.grizzly.util.SSLOutputWriter;
import com.sun.grizzly.util.WorkerThread;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Simple echo filter
 *
 * @author Alexey Stashok
 */
public class SSLEchoFilter implements ProtocolFilter {
    public boolean execute(Context ctx) throws IOException {
        final WorkerThread workerThread = ((WorkerThread)Thread.currentThread());
        ByteBuffer buffer = workerThread.getByteBuffer();
        buffer.flip();
        if (buffer.hasRemaining()) {
            // Store incoming data in byte[]
            byte[] data = new byte[buffer.remaining()];
            int position = buffer.position();
            buffer.get(data);
            buffer.position(position);
            SocketChannel channel = (SocketChannel) ctx.getSelectionKey().channel();
            try {
                SSLOutputWriter.flushChannel(channel, buffer);
            } catch (IOException ex) {
                throw ex;
            }
        }
        
        buffer.clear();
        return false;
    }
    
    public boolean postExecute(Context ctx) throws IOException {
        return true;
    }
}
