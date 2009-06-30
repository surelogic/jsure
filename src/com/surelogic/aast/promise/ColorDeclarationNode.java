
package com.surelogic.aast.promise;


import java.util.*;



import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

public class ColorDeclarationNode extends ColorNameListNode { 
  // Fields

  public static final AbstractSingleNodeFactory factory =
    new AbstractSingleNodeFactory("ColorDeclaration") {
      @Override
      @SuppressWarnings("unchecked")      
      public AASTNode create(String _token, int _start, int _stop,
          int _mods, String _id, int _dims, List<AASTNode> _kids) {
        List<ColorNameNode> color = ((List) _kids);
        return new ColorDeclarationNode (_start, color);
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ColorDeclarationNode(int offset,
                              List<ColorNameNode> color) {
    super(offset, color, "ColorDeclaration");
  }


  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	public IAASTNode cloneTree() {
		return new ColorDeclarationNode(getOffset(), cloneColorList());
	}
}

