
package com.surelogic.aast.promise;


import com.surelogic.aast.bind.AASTBinder;
import com.surelogic.aast.bind.IRegionBinding;
import com.surelogic.ast.Resolvable;

public abstract class RegionSpecificationNode extends FieldRegionSpecificationNode 
implements Resolvable { 
  // Fields
  private final String id;

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public RegionSpecificationNode(int offset,
                                 String id) {
    super(offset);
    this.id = id;
  }

  @Override
  public boolean bindingExists() {
    return AASTBinder.getInstance().isResolvable(this);
  }

  public IRegionBinding resolveBinding() {
    return AASTBinder.getInstance().resolve(this);
  }

  /**
   * @return A possibly-null String
   */
  public String getId() {
    return id;
  }
  
//  @Override
//  public RegionSpecificationNode cloneTree() {
//    throw new UnsupportedOperationException();
//  }
}

