/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.AbstractAASTNodeFactory;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeModifier;
import com.surelogic.aast.INodeVisitor;

public class ModuleScopeNode extends ModuleAnnotationNode {
	  // Fields
	  private final String id;

	  public static final AbstractAASTNodeFactory factory =
	    new AbstractAASTNodeFactory("ModuleScope") {
	      @Override
	      public AASTNode create(String _token, int _start, int _stop,
	                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
	        String id = _id;
	        return new ModuleScopeNode (_start, id);
	      }
	    };

	  // Constructors
	  /**
	   * Lists passed in as arguments must be @unique
	   */
	  public ModuleScopeNode(int offset,
	                    String id) {
	    super(offset);
	    if (id == null) { throw new IllegalArgumentException("id is null"); }
	    this.id = id;
	  }

	  @Override
	  public String unparse(boolean debug, int indent) {
	    StringBuilder sb = new StringBuilder();
	    if (debug) { indent(sb, indent); }
	    sb.append("ModuleScope\n");
	    indent(sb, indent+2);
	    sb.append("id=").append(id);
	    sb.append("\n");
	    return sb.toString();
	  }

	  /**
	   * @return A non-null String
	   */
	  public String getModuleName() {
	    return id;
	  }
	  
	  
	  public ModuleScopeNode getAnno() {
	    return this;
	  }

//	  public List<String> getWrappedModuleName() {
//	    return Collections.emptyList();
//	  }

	  @Override
	  public <T> T accept(INodeVisitor<T> visitor) {	   
	    return visitor.visit(this);
	  }
	  
	  @Override
	  protected IAASTNode internalClone(final INodeModifier mod) {
	    return new ModuleScopeNode(getOffset(), id);
	  }
}
