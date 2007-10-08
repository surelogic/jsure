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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * SSL configuration
 *
 * @author Alexey Stashok
 */
public class SSLConfig {
    /**
     * Default Logger.
     */
    private static Logger logger = Logger.getLogger("grizzly");
    
    /**
     * Default SSL configuration
     */
    public static SSLConfig DEFAULT_CONFIG = new SSLConfig();
    
    private String trustStoreType = "JKS";
    private String keyStoreType = "JKS";
    
    private char[] trustStorePass = "changeit".toCharArray();
    private char[] keyStorePass = "changeit".toCharArray();
    
    private String trustStoreFile = System.getProperty("javax.net.ssl.trustStore");
    private String keyStoreFile = System.getProperty("javax.net.ssl.keyStore");
    
    private String trustStoreAlgorithm = "SunX509";
    private String keyStoreAlgorithm = "SunX509";
    
    private String securityProtocol = "TLS";
    
    private boolean needClientAuth = false;
    
    private boolean wantClientAuth = false;
    
    public String getTrustStoreType() {
        return trustStoreType;
    }
    
    public void setTrustStoreType(String trustStoreType) {
        this.trustStoreType = trustStoreType;
    }
    
    public String getKeyStoreType() {
        return keyStoreType;
    }
    
    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }
    
    public String getTrustStorePass() {
        return new String(trustStorePass);
    }
    
    public void setTrustStorePass(String trustStorePass) {
        this.trustStorePass = trustStorePass.toCharArray();
    }
    
    public String getKeyStorePass() {
        return new String(keyStorePass);
    }
    
    public void setKeyStorePass(String keyStorePass) {
        this.keyStorePass = keyStorePass.toCharArray();
    }
    
    public String getTrustStoreFile() {
        return trustStoreFile;
    }
    
    public void setTrustStoreFile(String trustStoreFile) {
        this.trustStoreFile = trustStoreFile;
    }
    
    public String getKeyStoreFile() {
        return keyStoreFile;
    }
    
    public void setKeyStoreFile(String keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
    }
    
    public String getTrustStoreAlgorithm() {
        return trustStoreAlgorithm;
    }
    
    public void setTrustStoreAlgorithm(String trustStoreAlgorithm) {
        this.trustStoreAlgorithm = trustStoreAlgorithm;
    }
    
    public String getKeyStoreAlgorithm() {
        return keyStoreAlgorithm;
    }
    
    public void setKeyStoreAlgorithm(String keyStoreAlgorithm) {
        this.keyStoreAlgorithm = keyStoreAlgorithm;
    }
    
    public String getSecurityProtocol() {
        return securityProtocol;
    }
    
    public void setSecurityProtocol(String securityProtocol) {
        this.securityProtocol = securityProtocol;
    }
    
    public boolean isNeedClientAuth() {
        return needClientAuth;
    }
    
    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }
    
    public boolean isWantClientAuth() {
        return wantClientAuth;
    }
    
    public void setWantClientAuth(boolean wantClientAuth) {
        this.wantClientAuth = wantClientAuth;
    }
    
    public SSLContext createSSLContext() {
        SSLContext sslContext = null;
        
        try {
            TrustManagerFactory trustManagerFactory = null;
            KeyManagerFactory keyManagerFactory = null;
            
            if (trustStoreFile != null) {
                try {
                    KeyStore trustStore = KeyStore.getInstance(keyStoreType);
                    trustStore.load(new FileInputStream(trustStoreFile),
                            trustStorePass);
                    
                    trustManagerFactory =
                            TrustManagerFactory.getInstance(trustStoreAlgorithm);
                    trustManagerFactory.init(trustStore);
                } catch (KeyStoreException e) {
                    logger.log(Level.FINE, "Error initializing trust store", e);
                } catch (CertificateException e) {
                    logger.log(Level.FINE, "Trust store certificate exception.", e);
                } catch (FileNotFoundException e) {
                    logger.log(Level.FINE, "Can't find trust store file: " + trustStoreFile, e);
                } catch (IOException e) {
                    logger.log(Level.FINE, "Error loading trust store from file: " + trustStoreFile, e);
                }
            }
            
            if (keyStoreFile != null) {
                try {
                    KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                    keyStore.load(new FileInputStream(keyStoreFile),
                            keyStorePass);
                    
                    keyManagerFactory =
                            KeyManagerFactory.getInstance(keyStoreAlgorithm);
                    keyManagerFactory.init(keyStore, keyStorePass);
                } catch (KeyStoreException e) {
                    logger.log(Level.FINE, "Error initializing key store", e);
                } catch (CertificateException e) {
                    logger.log(Level.FINE, "Key store certificate exception.", e);
                } catch (UnrecoverableKeyException e) {
                    logger.log(Level.FINE, "Key store unrecoverable exception.", e);
                } catch (FileNotFoundException e) {
                    logger.log(Level.FINE, "Can't find key store file: " + keyStoreFile, e);
                } catch (IOException e) {
                    logger.log(Level.FINE, "Error loading key store from file: " + keyStoreFile, e);
                }
            }
            
            sslContext = SSLContext.getInstance(securityProtocol);
            sslContext.init(keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null,
                    trustManagerFactory != null ? trustManagerFactory.getTrustManagers() : null,
                    null);
        } catch (KeyManagementException e) {
            logger.log(Level.FINE, "Key management error.", e);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.FINE, "Error initializing algorithm.", e);
        }
        
        return sslContext;
    }
}
