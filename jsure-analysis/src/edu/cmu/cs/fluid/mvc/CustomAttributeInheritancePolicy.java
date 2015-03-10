// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/CustomAttributeInheritancePolicy.java,v 1.7 2007/07/05 18:15:16 aarong Exp $
package edu.cmu.cs.fluid.mvc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An implementation of attribute inheritance policy that can be customized 
 * with various attribute InheritancePredicates.  
 *
 * These include {@link #filterCompAttr} and {@link #filterNodeAttr}.
 * It defaults to not inheriting the attributes, if no predicate matches.
 */
public final class CustomAttributeInheritancePolicy
  extends AbstractAttributeInheritancePolicy
{
  /** The list of predicates used when filtering */
  private final List<AttributeInheritancePredicate> compPreds = new ArrayList<AttributeInheritancePredicate>();

  /** The list of predicates used when filtering */
  private final List<AttributeInheritancePredicate> nodePreds = new ArrayList<AttributeInheritancePredicate>();

  // Javadoc inherited
  @Override
  protected HowToInherit filterCompAttr( Model from, String attr ) {
    final Iterator it = compPreds.iterator();
    while (it.hasNext()) {
      AttributeInheritancePredicate p = 
	(AttributeInheritancePredicate) it.next();

      Object mode = p.howToInherit(from, attr);

      if (mode == AttributeInheritancePredicate.DONT_INHERIT) {
	return DONT_INHERIT;
      } else if (mode == AttributeInheritancePredicate.SKIP_ME) {
	continue;
      } else {
	return new HowToInherit(attr, p.inheritAs(attr), 
				mode, from.getCompAttrKind(attr));
      }
    }
    return DONT_INHERIT;
  }

  // Javadoc inherited
  @Override
  protected HowToInherit filterNodeAttr( Model from, String attr ) {
    final Iterator it = nodePreds.iterator();
    while (it.hasNext()) {
      AttributeInheritancePredicate p = 
	(AttributeInheritancePredicate) it.next();

      Object mode = p.howToInherit(from, attr);

      if (mode == AttributeInheritancePredicate.DONT_INHERIT) {
	return DONT_INHERIT;
      } else if (mode == AttributeInheritancePredicate.SKIP_ME) {
	continue;
      } else {
	return new HowToInherit(attr, p.inheritAs(attr), 
				mode, from.getNodeAttrKind(attr));
      }
    }
    return DONT_INHERIT;
  }

  /** Adds an AttributeInheritancePredicate to the end of list of
   * component-level predicates 
   */ 
  public void addCompPredicate(AttributeInheritancePredicate p) {
    compPreds.add(p);
  }

  /** Adds an AttributeInheritancePredicate to the end of list of
   * node-level predicates 
   */ 
  public void addNodePredicate(AttributeInheritancePredicate p) {
    nodePreds.add(p);
  }
}
