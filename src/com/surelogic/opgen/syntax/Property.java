package com.surelogic.opgen.syntax;

import java.util.*;

public interface Property {
  String getName();
  String getMessage();

  /**
   * Used to keep track of known and unknown properties
   */
  @SuppressWarnings("serial")
  static final Map<String,Property> props = new HashMap<String,Property>() {
    @Override
    public Property get(Object o) {
      String name = (String) o;
      Property p = KnownProperty.get(name); 
      if (p != null) {
        return p;
      }
      
      p = super.get(name);
      if (p != null) {
        return p;
      }
      p = new UnknownProperty(name);
      super.put(name, p);
      return p;
    }
  };
}
