package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.*;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class NamedTypePatternNode extends NamedTypeNode { 
  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("NamedTypePattern") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        String type = _id;
        return new NamedTypePatternNode (_start,
          type        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public NamedTypePatternNode(int offset,
                       String type) {
    super(offset, type);
  }

  @Override
  public boolean typeExists() {
    if (isFullWildcard()) {
      return true;
    }
    return true;
    //TODO return AASTBinder.getInstance().isResolvableToType(this);
  }

  /**
   * Gets the binding corresponding to the type of the NamedType
   */
  @Override
  public ISourceRefType resolveType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {   
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new NamedTypePatternNode(getOffset(), getType());
  }

  public boolean isFullWildcard() {
	  return getType().equals("*") || getType().equals("**");
  }
}