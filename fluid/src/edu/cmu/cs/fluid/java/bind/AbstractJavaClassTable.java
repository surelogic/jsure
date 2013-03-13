/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/AbstractJavaClassTable.java,v 1.13 2008/09/17 18:56:22 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import java.io.PrintStream;
import java.util.logging.Level;

import com.surelogic.ThreadSafe;
import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.debug.DebugUtil;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.CommonStrings;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.operator.NamedPackageDeclaration;


/**
 * An partial implementation of {@link IJavaClassTable}.
 * @author boyland
 */
@ThreadSafe
public abstract class AbstractJavaClassTable implements IJavaClassTable {
  // private static Logger LOG = Logger.getLogger("FLUID.java.bind");
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.project.IJavaClassTable#packageScope(edu.cmu.cs.fluid.ir.IRNode)
   */
  @Override
  public IJavaScope packageScope(IRNode pdecl) {
    return packageScope(NamedPackageDeclaration.getId(pdecl));
  }
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.project.IJavaClassTable#packageScope(java.lang.String)
   */
  @Override
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

	@Override
  public boolean canContainPackages() {
		return true;
	} 
    
    @Override
    public IBinding lookup(LookupContext context, Selector selector) {
      final String qname = prefix + context.name;
      IRNode node = getOuterClass(qname,context.useSite);
      if (LOG.isLoggable(Level.FINER)) {
        LOG.finer("getOuterClass(" + prefix + "+" + context.name + ") returns " + DebugUnparser.toString(node));
      }
      if (node != null && selector.select(node)) {
        return IBinding.Util.makeBinding(node); 
      } else {    	  
    	//getOuterClass(qname,useSite);
      }
      return null;
    }

    @Override
    public Iteratable<IBinding> lookupAll(LookupContext context,
        Selector selector) {
      IRNode node = getOuterClass(prefix + context.name,context.useSite);
      if (LOG.isLoggable(Level.FINER)) {
        LOG.finer("getOuterClass(" + prefix + "+" + context.name + ") returns " + DebugUnparser.toString(node));
      }
      if (node != null && selector.select(node)) {
        return new SingletonIterator<IBinding>(IBinding.Util.makeBinding(node)); 
      }
      return IJavaScope.EMPTY_BINDINGS_ITERATOR;
    }

    @Override
    public void printTrace(PrintStream out, int indent) {
      DebugUtil.println(out, indent, "[PackageScope:"+prefix+"]");
    }

  }
}
