/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/AbstractJavaClassTable.java,v 1.13 2008/09/17 18:56:22 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import java.io.PrintStream;
import java.util.logging.Level;
import edu.cmu.cs.fluid.debug.DebugUtil;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.CommonStrings;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.operator.NamedPackageDeclaration;
import edu.cmu.cs.fluid.util.Iteratable;
import edu.cmu.cs.fluid.util.SingletonIterator;


/**
 * An partial implementation of {@link IJavaClassTable}.
 * @author boyland
 */
public abstract class AbstractJavaClassTable implements IJavaClassTable {
  // private static Logger LOG = Logger.getLogger("FLUID.java.bind");
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.project.IJavaClassTable#packageScope(edu.cmu.cs.fluid.ir.IRNode)
   */
  public IJavaScope packageScope(IRNode pdecl) {
    return packageScope(NamedPackageDeclaration.getId(pdecl));
  }
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.project.IJavaClassTable#packageScope(java.lang.String)
   */
  public IJavaPackageScope packageScope(String pName) {
    String prefix;
    if (pName.length() == 0) {
    	prefix = "";
    } else {
    	prefix = pName + ".";
    }
    prefix = CommonStrings.intern(prefix);
    return new PackageScope(prefix);
  }
  
  private class PackageScope implements IJavaPackageScope {

    private final String prefix;

    private PackageScope(String prefix) {
      super();
      this.prefix = prefix;
    }

	public boolean canContainPackages() {
		return true;
	} 
    
    public IBinding lookup(String name, IRNode useSite, Selector selector) {
      final String qname = prefix + name;
      IRNode node = getOuterClass(qname,useSite);
      if (LOG.isLoggable(Level.FINER)) {
        LOG.finer("getOuterClass(" + prefix + "+" + name + ") returns " + DebugUnparser.toString(node));
      }
      if (node != null && selector.select(node)) {
        return IBinding.Util.makeBinding(node); 
      } else {    	  
    	//getOuterClass(qname,useSite);
      }
      return null;
    }

    public Iteratable<IBinding> lookupAll(String name, IRNode useSite,
        Selector selector) {
      IRNode node = getOuterClass(prefix + name,useSite);
      if (LOG.isLoggable(Level.FINER)) {
        LOG.finer("getOuterClass(" + prefix + "+" + name + ") returns " + DebugUnparser.toString(node));
      }
      if (node != null && selector.select(node)) {
        return new SingletonIterator<IBinding>(IBinding.Util.makeBinding(node)); 
      }
      return IJavaScope.EMPTY_BINDINGS_ITERATOR;
    }

    public void printTrace(PrintStream out, int indent) {
      DebugUtil.println(out, indent, "[PackageScope:"+prefix+"]");
    }

  }
}
