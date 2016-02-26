
package com.surelogic.aast.promise;


import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeModifier;
import com.surelogic.aast.INodeVisitor;

public class TrackPartiallyInitializedNode extends AbstractBooleanNode 
{ 
	public static final String VERIFY_PARENT = "verifyParent";
	private final boolean verifyParent;
	
  // Constructors
  public TrackPartiallyInitializedNode(int offset, boolean verify) {
    super(offset);
    verifyParent = verify;
  }
  
  @Override
  public String unparse(boolean debug, int indent) {
	if (!debug) {
		return unparseForPromise();
	}		
    return unparse(debug, indent, "TrackPartiallyInitialized");
  }

  @Override
  public String unparseForPromise() {
	return "TrackPartiallyInitialized"+(verifyParent ? "" : "(verifyParent=false)");
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new TrackPartiallyInitializedNode(offset,verifyParent);
  }

  public boolean verifyParent() {
	  return verifyParent;
  }
}

