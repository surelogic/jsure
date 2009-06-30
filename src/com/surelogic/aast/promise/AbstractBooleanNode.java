
package com.surelogic.aast.promise;

import java.util.*;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

public abstract class AbstractBooleanNode extends AASTRootNode 
{ 
  protected static abstract class Factory extends AbstractSingleNodeFactory {
    public Factory(String t) {
      super(t);
    }
    @Override
    public AASTNode create(String _token, int _start, int _stop,
        int _mods, String _id, int _dims, List<AASTNode> _kids) {
      return create(_start);
    }
    protected abstract AASTNode create(int offset);
  }

  // Constructors
  protected AbstractBooleanNode(int offset) {
    super(offset);
  }

  protected final String unparse(boolean debug, int indent, String token) {
    if (debug) {
      StringBuilder sb = new StringBuilder();
      if (debug) { indent(sb, indent); }
      sb.append(token);
      sb.append('\n');
      return sb.toString();
    } else {
      return token;
    }
  }
}

