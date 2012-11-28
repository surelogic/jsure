package com.surelogic.javac.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import com.surelogic.common.SortedProperties;
import com.surelogic.Starts;

public abstract class ScanProperty<T> {
	public static final String SCAN_PROPERTIES = "scan.properties";
	
	final String key;

    ScanProperty(String k) {
      key = k;
    }

    boolean isValid(String value) {
      return value != null;
    }

    Object computeValue(T s) {
    	return null;
    }
    
    /**
     * @return non-null
     */
    Iterable<Map.Entry<String,Object>> computeValues(T s) {
    	final Object value = computeValue(s);
    	if (value == null) {
    		return Collections.emptySet();
    	}
    	final Map.Entry<String,Object> e = new Map.Entry<String,Object>() {
			@Starts("nothing")
			public String getKey() {
				return key;
			}
			@Starts("nothing")
			public Object getValue() {
				return value;
			}
			public Object setValue(Object value) {
				return null;
			}
    	};
    	return Collections.singleton(e);
    }
    
    /**
     * Returns all the expected properties
     */
    static <T> Properties getScanProperties(File scanDir, T info, List<ScanProperty<T>> requiredProps) {
      final Properties props = new SortedProperties();
      final File precomputed = new File(scanDir, SCAN_PROPERTIES);
      final boolean alreadyPrecomputed = precomputed.exists();
      if (alreadyPrecomputed) {
        InputStream in = null;
        try {
          in = new FileInputStream(precomputed);
          props.load(in);
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          if (in != null) {
            try {
              in.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      }
      // Check if I have all the info that I need
      boolean changed = false;
      for (ScanProperty<T> p : requiredProps) {
    	  if (!p.isValid(props.getProperty(p.key))) {
    		  for(Map.Entry<String,Object> pair : p.computeValues(info)) {
    			  final Object value = pair.getValue();
    			  if (value != null) {
    				  props.setProperty(pair.getKey(), value.toString());
    				  changed = true;
    			  }
    		  }
    	  }
      }

      final File completed = JSureScan.findResultsXML(scanDir);
      if (completed.exists()) {
        if (alreadyPrecomputed) {
          final long pMod = precomputed.lastModified();
          final long cMod = completed.lastModified();
          changed |= pMod <= cMod;
          /*
          if (changed) {
            System.out.println("Changed");
          }
          */
        }
      } else {
        // Don't write it out if it's not done yet
        changed = false;
      }
      if (changed) {
        // Rewrite the properties file
        OutputStream out = null;
        try {
          out = new FileOutputStream(precomputed);
          props.store(out, "Precomputed info for JSureScan");
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          if (out != null) {
            try {
              out.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      }
      return props;
    }
}
