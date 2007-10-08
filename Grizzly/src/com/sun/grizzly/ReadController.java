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

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;

import com.surelogic.Aggregate;
import com.surelogic.InRegion;
import com.surelogic.Promise;
import com.surelogic.Promises;
import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.Regions;
import com.surelogic.Unique;

/**
 * ReadController class represents <code>Controller</code>,
 * which is not itself independent.
 *
 * Should be used for handling OP_READ operations
 * Supports TCP derived protocols
 *
 * @author Alexey Stashok
 */
@Region("ChannelsRegion")
@RegionLock("Lock is channels protects ChannelsRegion")
class ReadController extends Controller {
    /**
     * List of <code>Channel<code> to process.
     */
	@InRegion("ChannelsRegion")
	@Unique
	@Aggregate("Instance into ChannelsRegion")
    final List<RegisterChannelRecord> channels = new ArrayList<RegisterChannelRecord>();
	
	public ReadController(){
		
	}
	
	/*
	 * XXX
	 * @see {@link com.sun.grizzly.Controller#Controller}
	 */
	protected ReadController(Controller copyFrom){
		super(copyFrom);
	}
    
    /**
     * Add a <code>Channel</code>
     * to be processed by <code>ReadController</code>'s
     * <code>SelectorHandler</code>
     *
     * @param channel new channel to be managed by ReadController
     * @param protocol name of the protocol channel corresponds to
     */
    public void addChannel(SelectableChannel channel, Protocol protocol) {
        synchronized(channels) {
            channels.add(new RegisterChannelRecord(channel, protocol));
        }
        
        getSelectorHandler(protocol).getSelector().wakeup();
    }
    
    /**
     * Register all <code>Channel</code> with an OP_READ opeation.
     * @throws java.io.IOException 
     */
    private void registerNewChannels() throws IOException {
        synchronized(channels) {
            int size = channels.size();
            for (int i = 0; i < size; i++) {
                RegisterChannelRecord record = channels.get(i);
                SelectorHandler selectorHandler = 
                        getSelectorHandler(record.protocol);
                Selector auxSelector = selectorHandler.getSelector();
                SelectableChannel channel = record.channel;
                SelectionKey readKey =
                        channel.register(auxSelector, SelectionKey.OP_READ);
                readKey.attach(System.currentTimeMillis());
            }
            
            channels.clear();
        }
    }
    
    /**
     * Start the ReadController.
     * Some <code>Controller</code> properties should not be initialized
     */
    @Override
    public void start() throws IOException {
        state = State.STARTED;
        synchronized(shutdownLock){
            while(state == State.STARTED){
                registerNewChannels();
                for(SelectorHandler selectorHandler: selectorHandlers) {
                    // State changed inside the loop 
                    if (state != State.STARTED){
                        break;
                    }
                    doSelect(selectorHandler);
                }
            }

            for (SelectorHandler selectorHandler: selectorHandlers){
                SelectionKeyHandler selectionKeyHandler = 
                        selectorHandler.getSelectionKeyHandler();
                
                for (SelectionKey selectionKey : selectorHandler.keys()) {
                    selectionKeyHandler.close(selectionKey);
                }
                selectorHandler.shutdown();
            }
        }
    }
    
    static final class RegisterChannelRecord {
        public SelectableChannel channel;
        public Protocol protocol;
        
        public RegisterChannelRecord(SelectableChannel channel, Protocol protocol) {
            this.channel = channel;
            this.protocol = protocol;
        }
    }
}
