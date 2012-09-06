/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/AbstractMessages.java,v 1.7 2009/01/29 16:56:28 aarong Exp $*/
package edu.cmu.cs.fluid.util;

import java.io.*;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.logging.*;

import com.surelogic.test.Testing;


public abstract class AbstractMessages {  
  /**
   * Converts the message to the format used for testing
   * 
   * @param key The key used to look up the message
   * @param raw The raw message
   * @return The key, followed by the number of arguments in the message 
   */
  private static String convertMessageFormat(String key, String raw) {
    int first = raw.indexOf("{");
    int last  = raw.indexOf("}");

    // Either no open or no close brace
    if (first < 0 || last < 0) {
      return key;
    }
    // first open brace comes after last close brace
    if (first >= last) {
      return key;
    }        
    if (raw.indexOf("{5}") >= 0) {
      return key+" {0} {1} {2} {3} {4} {5}";
    }
    if (raw.indexOf("{4}") >= 0) {
      return key+" {0} {1} {2} {3} {4}";
    }
    if (raw.indexOf("{3}") >= 0) {
      return key+" {0} {1} {2} {3}";
    }
    if (raw.indexOf("{2}") >= 0) {
      return key+" {0} {1} {2}";
    }
    if (raw.indexOf("{1}") >= 0) {
      return key+" {0} {1}";
    }
    if (raw.indexOf("{0}") >= 0) {
      return key+" {0}";
    }
    Testing.LOG.severe("Can't handle that many message arguments: "+raw);
    return key;
  }
  
  protected static String getString(ResourceBundle b, String key) {
    try {
      String raw = b.getString(key);
      if (Testing.testingIsOn) {
        return convertMessageFormat(key, raw);
      }
      return raw;
    } catch (MissingResourceException e) {
      return '!' + key + '!';
    }
  }
  
  /*******************************************************************************
   * Copyright (c) 2005 IBM Corporation and others.
   * All rights reserved. This program and the accompanying materials
   * are made available under the terms of the Eclipse Public License v1.0
   * which accompanies this distribution, and is available at
   * http://www.eclipse.org/legal/epl-v10.html
   * 
   * Contributors:
   *     IBM Corporation - initial API and implementation
   *     
   *******************************************************************************/
    
  /**
   * Load the given resource bundle using the specified class loader.
   */
  protected static void load(final String bundleName, Class<?> clazz) {
    final Field[] fieldArray = clazz.getDeclaredFields();
    ClassLoader loader = clazz.getClassLoader();

    boolean isAccessible = (clazz.getModifiers() & Modifier.PUBLIC) != 0;

    //build a map of field names to Field objects
    final int len = fieldArray.length;
    Map<String,Object> fields = new HashMap<String,Object>(len * 2);
    for (Field f : fieldArray) {
      fields.put(f.getName(), f);
    }
    
    final String name = bundleName.replace('.', File.separatorChar) + ".properties";

    // loader==null if we're launched off the Java boot classpath
    final InputStream input = loader==null ? ClassLoader.getSystemResourceAsStream(name) : loader.getResourceAsStream(name);
    if (input == null) {
      return;
    }
    
    // Load the properties directly into the fields of the clazz 
    try {
      final MessagesProperties properties = new MessagesProperties(fields, bundleName, isAccessible);
      properties.load(input);
    } catch (IOException e) {
      Testing.LOG.log(Level.SEVERE, "Error loading " + name, e); //$NON-NLS-1$
    } finally {
      if (input != null)
        try {
          input.close();
        } catch (IOException e) {
          // ignore
        }
    }
  }
  
  /**
   * This object is assigned to the value of a field map to indicate
   * that a translated message has already been assigned to that field.
   */
  private static final Object ASSIGNED = new Object();
  
  /**
   * Class which sub-classes java.util.Properties and uses the #put method
   * to set field values rather than storing the values in the table.
   * 
   * @since 3.1
   */
  private static class MessagesProperties extends Properties {
    private static final int MOD_EXPECTED = Modifier.PUBLIC | Modifier.STATIC;
    private static final int MOD_MASK = MOD_EXPECTED | Modifier.FINAL;
    private static final long serialVersionUID = 1L;

    private final String bundleName;
    private final Map<String,Object> fields;
    private final boolean isAccessible;

    public MessagesProperties(Map<String,Object> fieldMap, String bundleName, boolean isAccessible) {
      super();
      this.fields = fieldMap;
      this.bundleName = bundleName;
      this.isAccessible = isAccessible;
    }

    @Override
    public synchronized Object put(Object k, Object value) {
      final String key   = (String) k;
      Object fieldObject = fields.put(key, ASSIGNED);
      // if already assigned, there is nothing to do
      if (fieldObject == ASSIGNED)
        return null;
      if (fieldObject == null) {
        final String msg = "NLS unused message: " + key + " in: " + bundleName;//$NON-NLS-1$ //$NON-NLS-2$
        Testing.LOG.severe(msg);
        return null;
      }
      final Field field = (Field) fieldObject;
      //can only set value of public static non-final fields
      if ((field.getModifiers() & MOD_MASK) != MOD_EXPECTED)
        return null;
      try {
        // Check to see if we are allowed to modify the field. If we aren't (for instance 
        // if the class is not public) then change the accessible attribute of the field
        // before trying to set the value.
        if (!isAccessible)
          makeAccessible(field);
        
        // Set the value into the field. We should never get an exception here because
        // we know we have a public static non-final field. If we do get an exception, silently
        // log it and continue. This means that the field will (most likely) be un-initialized and
        // will fail later in the code and if so then we will see both the NPE and this error.
        
        if (Testing.testingIsOn) {
          field.set(null, convertMessageFormat(key, (String) value));
        } else {
          field.set(null, value);
        }
      } catch (Exception e) {
        Testing.LOG.log(Level.SEVERE, "Exception setting field value.", e); //$NON-NLS-1$
      }
      return null;
    }
  }

  /*
   * Change the accessibility of the specified field so we can set its value
   * to be the appropriate message string.
   */
  private static void makeAccessible(final Field field) {
    if (System.getSecurityManager() == null) {
      field.setAccessible(true);
    } else {
      AccessController.doPrivileged(new PrivilegedAction<Object>() {
        public Object run() {
          field.setAccessible(true);
          return null;
        }
      });
    }
  }
}
