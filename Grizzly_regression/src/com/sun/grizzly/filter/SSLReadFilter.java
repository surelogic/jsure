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

import com.sun.grizzly.Context;
import com.sun.grizzly.Controller;
import com.sun.grizzly.ProtocolFilter;
import com.sun.grizzly.util.SSLUtils;
import com.sun.grizzly.util.WorkerThreadImpl;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

/**
 * Simple ProtocolFilter implementation which execute an SSL handshake and
 * decrypt the bytes, the pass the control to the next filter.
 *
 * @author Jeanfrancois Arcand
 */
public class SSLReadFilter implements ProtocolFilter{
      
    public final static String HANDSHAKE = "handshake";
    public final static String INPUT_BB_REMAINDER = "inputBBRemainder";
    public final static String OUTPUT_BB_REMAINDER = "outputBBRemainder";
    
    
     
    /**
     * The <code>SSLContext</code> associated with the SSL implementation
     * we are running on.
     */
    protected SSLContext sslContext;
    
    
    /**
     * The list of cipher suite
     */
    private String[] enabledCipherSuites = null;
    
    
    /**
     * the list of protocols
     */
    private String[] enabledProtocols = null;
    
    
    /**
     * Client mode when handshaking.
     */
    private boolean clientMode = false;
    
    
    /**
     * Require client Authentication.
     */
    private boolean needClientAuth = false;
    
    
    /**
     * True when requesting authentication.
     */
    private boolean wantClientAuth = false;
    
    
    /**
     * Session keep-alive flag.
     */
    public final static String EXPIRE_TIME = "expireTime";
    
    
    /**
     * Has the enabled protocol configured.
     */
    private boolean isProtocolConfigured = false;
    
    
    /**
     * Has the enabled Cipher configured.
     */
    private boolean isCipherConfigured = false;
    
    
    /**
     * Encrypted ByteBuffer default size.
     */
    protected int inputBBSize = 5 * 4096;
    
    
    public SSLReadFilter() {
    }

    
    public boolean execute(Context ctx) throws IOException {
        boolean result = true;
        int count = 0;
        Throwable exception = null;
        SelectionKey key = ctx.getSelectionKey();
        WorkerThreadImpl workerThread;        
        try{
            workerThread = (WorkerThreadImpl)Thread.currentThread();   
        } catch (ClassCastException ex){
            throw new IllegalStateException(ex.getMessage());
        }

        SSLEngine sslEngine = newSSLEngine(key);
        workerThread.setSSLEngine(sslEngine);
        key.attach(sslEngine);

        boolean hasHandshake = Boolean.TRUE.equals(
                sslEngine.getSession().getValue(HANDSHAKE));

        restoreSecuredBufferRemainders(sslEngine);
        try {
            allocateBuffers();          
            if (hasHandshake) {
                count = doRead(key);
            } else if (doHandshake(key,SSLUtils.getReadTimeout())) {
                hasHandshake = true;
                sslEngine.getSession().putValue(HANDSHAKE, Boolean.TRUE);
            } else {
                count = -1;
            }
        } catch (IOException ex) {
            exception = ex;
            log("SSLReadFilter.execute",ex);
        } catch (Throwable ex) {
            exception = ex;    
            log("SSLReadFilter.execute",ex);
        } finally {            
            if (exception != null || count == -1){
                ctx.setAttribute(Context.THROWABLE,exception);
                ctx.setKeyRegistrationState(
                        Context.KeyRegistrationState.CANCEL);
                result = false;
            } 
        }     
        return result;
    }

    
    /**
     * If no bytes were available, close the connection by cancelling the
     * SelectionKey. If bytes were available, register the SelectionKey
     * for new bytes.
     *
     * @return <tt>true</tt> if the previous ProtocolFilter postExecute method
     *         needs to be invoked.
     */
    public boolean postExecute(Context ctx) throws IOException {
        if (ctx.getKeyRegistrationState()
                == Context.KeyRegistrationState.CANCEL){
            ctx.getController().getSelectorHandler(ctx.getProtocol()).
                getSelectionKeyHandler().cancel(ctx.getSelectionKey());
        } else if (ctx.getKeyRegistrationState()
                == Context.KeyRegistrationState.REGISTER){            
            SSLEngine sslEngine = (SSLEngine) ctx.getSelectionKey().attachment();
            sslEngine.getSession().putValue(EXPIRE_TIME,System.currentTimeMillis());   
            saveSecuredBufferRemainders(sslEngine);
            ctx.getController().registerKey(ctx.getSelectionKey(),
                    SelectionKey.OP_READ,ctx.getProtocol());
        }
        return true;
    }
    
    
    /**
     * Allocate themandatory <code>ByteBuffer</code>s. Since the ByteBuffer
     * are maintaned on the <code>WorkerThreadImpl</code> lazily, this method
     * makes sure the ByteBuffers are properly allocated and configured.
     */    
    protected void allocateBuffers(){
        final WorkerThreadImpl workerThread = 
                (WorkerThreadImpl)Thread.currentThread();
        ByteBuffer byteBuffer = workerThread.getByteBuffer();
        ByteBuffer outputBB = workerThread.getOutputBB();
        ByteBuffer inputBB = workerThread.getInputBB();
            
        int expectedSize = workerThread.getSSLEngine().getSession()
            .getPacketBufferSize();
        if (inputBBSize < expectedSize){
            inputBBSize = expectedSize;
        }

        if (inputBB != null && inputBB.capacity() < inputBBSize) {
            ByteBuffer newBB = ByteBuffer.allocate(inputBBSize);
            inputBB.flip();
            newBB.put(inputBB);
            inputBB = newBB;                                
        } else if (inputBB == null){
            inputBB = ByteBuffer.allocate(inputBBSize);
        }      
        
        if (outputBB == null) {
            outputBB = ByteBuffer.allocate(inputBBSize);
        } 
        
        if (byteBuffer == null){
            byteBuffer = ByteBuffer.allocate(inputBBSize * 2);
        } 

        expectedSize = workerThread.getSSLEngine().getSession()
            .getApplicationBufferSize();
        if ( expectedSize > byteBuffer.capacity() ) {
            ByteBuffer newBB = ByteBuffer.allocate(expectedSize);
            byteBuffer.flip();
            newBB.put(byteBuffer);
            byteBuffer = newBB;
        }   

        workerThread.setInputBB(inputBB);
        workerThread.setOutputBB(outputBB);  
        workerThread.setByteBuffer(byteBuffer);
   
        outputBB.position(0);
        outputBB.limit(0);
    }
    
    
    /**
     * Execute a non blocking SSL handshake.
     * @param key <code>SelectionKey</code>
     * @param timeout 
     * @return 
     * @throws java.io.IOException 
     */    
    protected boolean doHandshake(SelectionKey key,int timeout) throws IOException{
        final WorkerThreadImpl workerThread = 
                (WorkerThreadImpl)Thread.currentThread();
        ByteBuffer byteBuffer = workerThread.getByteBuffer();
        ByteBuffer outputBB = workerThread.getOutputBB();
        ByteBuffer inputBB = workerThread.getInputBB();
        SSLEngine sslEngine = workerThread.getSSLEngine();
        
        HandshakeStatus handshakeStatus = HandshakeStatus.NEED_UNWRAP;
        
        boolean OK = true;    
        try{ 
            byteBuffer = SSLUtils.doHandshake
                         ((SocketChannel) key.channel(), byteBuffer, inputBB,
                    outputBB, sslEngine, handshakeStatus, timeout);
            if (doRead(key) == -1){
                throw new EOFException();
            }
        } catch (EOFException ex) {
            Logger logger = Controller.logger();
            if ( logger.isLoggable(Level.FINE) ){
                logger.log(Level.FINE,"doHandshake",ex);
            }            
            OK = false;
        }
        return OK;
    }    
    
    
    private int doRead(SelectionKey key){ 
        final WorkerThreadImpl workerThread = 
                (WorkerThreadImpl)Thread.currentThread();
        ByteBuffer byteBuffer = workerThread.getByteBuffer();
        ByteBuffer outputBB = workerThread.getOutputBB();
        ByteBuffer inputBB = workerThread.getInputBB();
        SSLEngine sslEngine = workerThread.getSSLEngine();
        
        int count = -1;
        try{
            // Read first bytes to avoid continuing if the client
            // closed the connection.
            count = ((SocketChannel)key.channel()).read(inputBB);
            if (count != -1){
                // Decrypt the bytes we just read.
                byteBuffer =
                        SSLUtils.unwrapAll(byteBuffer,inputBB,sslEngine);
                workerThread.setInputBB(inputBB);
                workerThread.setOutputBB(outputBB);  
                workerThread.setByteBuffer(byteBuffer);
            }
            return count;
        } catch(IOException ex){
            return -1;
        } finally {
            if (count == -1){
                try{
                    sslEngine.closeInbound();
                } catch (SSLException ex){
                    ;
                }
            }
        }
    }
    
    
    /**
     * Get the peer certificate list by initiating a new handshake.
     * @param key <code>SelectionKey</code>
     * @param needClientAuth 
     * @return Object[] An array of X509Certificate.
     * @throws java.io.IOException 
     */
    public Object[] doPeerCertificateChain(SelectionKey key,
            boolean needClientAuth) throws IOException {
        
        Logger logger = Controller.logger();
        final WorkerThreadImpl workerThread = 
                (WorkerThreadImpl)Thread.currentThread();
        ByteBuffer byteBuffer = workerThread.getByteBuffer();
        ByteBuffer outputBB = workerThread.getOutputBB();
        SSLEngine sslEngine = workerThread.getSSLEngine();
    
        Certificate[] certs=null;
        try {
            certs = sslEngine.getSession().getPeerCertificates();
        } catch( Throwable t ) {
            if ( logger.isLoggable(Level.FINE))
                logger.log(Level.FINE,"Error getting client certs",t);
        }
 
        if (certs == null && needClientAuth){
            sslEngine.getSession().invalidate();
            sslEngine.setNeedClientAuth(true);
            sslEngine.beginHandshake();         
                      
            ByteBuffer origBB = byteBuffer;
            // In case the application hasn't read all the body bytes.
            if ( origBB.position() != origBB.limit() ){
                byteBuffer = ByteBuffer.allocate(origBB.capacity());
            } else {
                byteBuffer = origBB;
            }
            byteBuffer.clear();
            outputBB.position(0);
            outputBB.limit(0); 
            
            try{
                doHandshake(key,0);
            } catch (Throwable ex){
                if ( logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE,"Error during handshake",ex);   
                return null;
            } finally {
                byteBuffer = origBB;
                workerThread.setByteBuffer(byteBuffer);
                byteBuffer.clear();
            }            

            try {
                certs = sslEngine.getSession().getPeerCertificates();
            } catch( Throwable t ) {
                if ( logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE,"Error getting client certs",t);
            }
        }
        
        if( certs==null ) return null;
        
        X509Certificate[] x509Certs = new X509Certificate[certs.length];
        for(int i=0; i < certs.length; i++) {
            if( certs[i] instanceof X509Certificate ) {
                x509Certs[i] = (X509Certificate)certs[i];
            } else {
                try {
                    byte [] buffer = certs[i].getEncoded();
                    CertificateFactory cf =
                    CertificateFactory.getInstance("X.509");
                    ByteArrayInputStream stream = new ByteArrayInputStream(buffer);
                    x509Certs[i] = (X509Certificate)
                    cf.generateCertificate(stream);
                } catch(Exception ex) { 
                    logger.log(Level.INFO,"Error translating cert " + certs[i],
                                     ex);
                    return null;
                }
            }
            
            if(logger.isLoggable(Level.FINE))
                logger.log(Level.FINE,"Cert #" + i + " = " + x509Certs[i]);
        }
        
        if(x509Certs.length < 1)
            return null;
            
        return x509Certs;
    }
    
    
    /**
     * Return a new configured <code>SSLEngine</code>
     * @return a new configured <code>SSLEngine</code>
     */
    protected SSLEngine newSSLEngine(){
        SSLEngine sslEngine = sslContext.createSSLEngine();
        if (enabledCipherSuites != null){            
            if (!isCipherConfigured){
                enabledCipherSuites = configureEnabledCiphers(sslEngine,
                                                        enabledCipherSuites);
                isCipherConfigured = true;
            }
            sslEngine.setEnabledCipherSuites(enabledCipherSuites);
        }
        
        if (enabledProtocols != null){
            if (!isProtocolConfigured) {
                enabledProtocols = configureEnabledProtocols(sslEngine,
                                                    enabledProtocols);
                isProtocolConfigured = true;
            }
            sslEngine.setEnabledProtocols(enabledProtocols);
        }
        sslEngine.setUseClientMode(clientMode);
        return sslEngine;
    }
    
    
    /**
     * Configure and return an instance of SSLEngine
     * @param key  a <code>SelectionKey</code>
     * @return  a configured instance of <code>SSLEngine</code>
     */
    protected SSLEngine newSSLEngine(SelectionKey key){
        SSLEngine sslEngine = null;
        if (key.attachment() == null){
            sslEngine = newSSLEngine();
        } else if (key.attachment() instanceof SSLEngine){
            sslEngine = (SSLEngine)key.attachment();
        } else {
           sslEngine = newSSLEngine();
        }
        sslEngine.setWantClientAuth(wantClientAuth);
        sslEngine.getSession().removeValue(EXPIRE_TIME);
        sslEngine.setNeedClientAuth(needClientAuth);
        return sslEngine;
    }
           
    
    /**
     * Set the SSLContext required to support SSL over NIO.
     * @param sslContext <code>SSLContext</code>
     */
    public void setSSLContext(SSLContext sslContext){
        this.sslContext = sslContext;
    }
    
    
    /**
     * Return the SSLContext required to support SSL over NIO.
     * @return <code>SSLContext</code>
     */    
    public SSLContext getSSLContext(){
        return sslContext;
    }
    
    
    /**
     * Returns the list of cipher suites to be enabled when {@link SSLEngine}
     * is initialized.
     *
     * @return <tt>null</tt> means 'use {@link SSLEngine}'s default.'
     */
    public String[] getEnabledCipherSuites() {
        return enabledCipherSuites;
    }
    
    
    /**
     * Sets the list of cipher suites to be enabled when {@link SSLEngine}
     * is initialized.
     * @param enabledCipherSuites 
     */
    public void setEnabledCipherSuites(String[] enabledCipherSuites) {
        this.enabledCipherSuites = enabledCipherSuites;
    }
    
    
    /**
     * Returns the list of protocols to be enabled when {@link SSLEngine}
     * is initialized.
     *
     * @return <tt>null</tt> means 'use {@link SSLEngine}'s default.'
     */
    public String[] getEnabledProtocols() {
        return enabledProtocols;
    }
    
    
    /**
     * Sets the list of protocols to be enabled when {@link SSLEngine}
     * is initialized.
     *
     * @param enabledProtocols <tt>null</tt> means 'use {@link SSLEngine}'s default.'
     */
    public void setEnabledProtocols(String[] enabledProtocols) {
        this.enabledProtocols = enabledProtocols;
    }
    
    
    /**
     * Returns <tt>true</tt> if the SSlEngine is set to use client mode
     * when handshaking.
     * @return true / false
     */
    public boolean isClientMode() {
        return clientMode;
    }
    
    
    /**
     * Configures the engine to use client (or server) mode when handshaking.
     * @param clientMode 
     */    
    public void setClientMode(boolean clientMode) {
        this.clientMode = clientMode;
    }
    
    
    /**
     * Returns <tt>true</tt> if the SSLEngine will <em>require</em>
     * client authentication.
     * @return 
     */   
    public boolean isNeedClientAuth() {
        return needClientAuth;
    }
    
    
    /**
     * Configures the engine to <em>require</em> client authentication.
     * @param needClientAuth 
     */    
    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }
    
    
    /**
     * Returns <tt>true</tt> if the engine will <em>request</em> client
     * authentication.
     * @return 
     */   
    public boolean isWantClientAuth() {
        return wantClientAuth;
    }
    
    
    /**
     * Configures the engine to <em>request</em> client authentication.
     * @param wantClientAuth 
     */    
    public void setWantClientAuth(boolean wantClientAuth) {
        this.wantClientAuth = wantClientAuth;
    }
    
    
    /**
     * Return the list of allowed protocol.
     * @return String[] an array of supported protocols.
     */
    private final static String[] configureEnabledProtocols(
            SSLEngine sslEngine, String[] requestedProtocols){
        
        String[] supportedProtocols = sslEngine.getSupportedProtocols();
        String[] protocols = null;
        ArrayList<String> list = null;
        for(String supportedProtocol: supportedProtocols){        
            /*
             * Check to see if the requested protocol is among the
             * supported protocols, i.e., may be enabled
             */
            for(String protocol: requestedProtocols) {
                protocol = protocol.trim();
                if (supportedProtocol.equals(protocol)) {
                    if (list == null) {
                        list = new ArrayList<String>();
                    }
                    list.add(protocol);
                    break;
                }
            }
        } 

        if (list != null) {
            protocols = list.toArray(new String[list.size()]);                
        }
 
        return protocols;
    }
    
    
    /**
     * Determines the SSL cipher suites to be enabled.
     *
     * @return Array of SSL cipher suites to be enabled, or null if none of the
     * requested ciphers are supported
     */
    private final static String[] configureEnabledCiphers(SSLEngine sslEngine,
            String[] requestedCiphers) {

        String[] supportedCiphers = sslEngine.getSupportedCipherSuites();
        String[] ciphers = null;
        ArrayList<String> list = null;
        for(String supportedCipher: supportedCiphers){        
            /*
             * Check to see if the requested protocol is among the
             * supported protocols, i.e., may be enabled
             */
            for(String cipher: requestedCiphers) {
                cipher = cipher.trim();
                if (supportedCipher.equals(cipher)) {
                    if (list == null) {
                        list = new ArrayList<String>();
                    }
                    list.add(cipher);
                    break;
                }
            }
        } 

        if (list != null) {
            ciphers = list.toArray(new String[list.size()]);                
        }
 
        return ciphers;
    }
    
    /**
     * Restores (if required) secure buffers, associated with <code>SSLEngine</code>,
     * which were saved during previous SSLReadFilter execution.
     * It makes possible data, which wasn't processed on previous cycle to be processed now
     */
    private void restoreSecuredBufferRemainders(SSLEngine sslEngine) {
        WorkerThreadImpl workerThread = (WorkerThreadImpl) Thread.currentThread();   

        ByteBuffer inputBBRemainder = (ByteBuffer) sslEngine.getSession().
                getValue(INPUT_BB_REMAINDER);
        if (inputBBRemainder != null) {
            sslEngine.getSession().removeValue(INPUT_BB_REMAINDER);
            workerThread.setInputBB(inputBBRemainder);
        }
        
        ByteBuffer outputBBRemainder = (ByteBuffer) sslEngine.getSession().
                getValue(OUTPUT_BB_REMAINDER);
        if (outputBBRemainder != null) {
            sslEngine.getSession().removeValue(OUTPUT_BB_REMAINDER);
            workerThread.setOutputBB(outputBBRemainder);
        }
    }
    
    /**
     * Saves (if required) secure buffers, associated with <code>SSLEngine</code>
     * If secured data, associated with <code>SSLEngine</code>, is not completely
     * processed now (may be additional data is required) - then it could be processed
     * on next SSLReadFilter execution.
     */
    private void saveSecuredBufferRemainders(SSLEngine sslEngine) {
        WorkerThreadImpl workerThread = (WorkerThreadImpl) Thread.currentThread();   

        ByteBuffer inputBB = workerThread.getInputBB();
        if (inputBB.hasRemaining()) {
            sslEngine.getSession().putValue(INPUT_BB_REMAINDER, inputBB);
            workerThread.setInputBB(null);
        }

        ByteBuffer outputBB = workerThread.getOutputBB();
        if (outputBB.hasRemaining()) {
            sslEngine.getSession().putValue(OUTPUT_BB_REMAINDER, outputBB);
            workerThread.setOutputBB(null);
        }
    }
    
    /**
     * Log a message/exception.
     * @param msg <code>String</code>
     * @param t <code>Throwable</code>
     */
    protected void log(String msg,Throwable t){
        if (Controller.logger().isLoggable(Level.FINE)){
            Controller.logger().log(Level.FINE,"ReadFilter,execute()",t);
        }
    } 
}
