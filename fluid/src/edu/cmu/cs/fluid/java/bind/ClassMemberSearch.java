/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/ClassMemberSearch.java,v 1.3 2008/10/27 13:58:44 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.ThreadSafe;
import com.surelogic.analysis.JavaProjects;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;

@ThreadSafe
public class ClassMemberSearch {
  private static final Logger LOG = SLLogger.getLogger("FLUID.bind");
  
  private final IBinder binder;
  
  ClassMemberSearch(IBinder b) {
    binder = b; 
  }
  
  public <T> T findClassBodyMembers(final IRNode type, ISuperTypeSearchStrategy<T> tvs, boolean throwIfNotFound) {
    if (type == null) {
      return null;
    }    
    final boolean isFormal = TypeFormal.prototype.includes(type);
    if (isFormal) {
      findXinTypeFormal_up(tvs, JavaTypeFactory.getTypeFormal(type));
    } else {
      IJavaDeclaredType startT = (IJavaDeclaredType) binder.getTypeEnvironment().getMyThisType(type);
      findXinAncestors(tvs, startT);
    }
    
    final T o = tvs.getResult();  
    if (o == null && throwIfNotFound) {
      final String context = type+" -- "+DebugUnparser.toString(type)+"\nfor project "+
                             JavaProjects.getEnclosingProject(type).getName();      
      LOG.log(Level.INFO, "Couldn't find " + tvs.getLabel() + " in " + context
              //, new Throwable(">>> Just for Stack Trace <<<")
              );
      tvs.reset();
      if (isFormal) {
        findXinTypeFormal_up(tvs, JavaTypeFactory.getTypeFormal(type));
      } else {
        IJavaDeclaredType startT = (IJavaDeclaredType) binder.getTypeEnvironment().getMyThisType(type);
        findXinAncestors(tvs, startT);
      }
    }
    else if (LOG.isLoggable(Level.FINER)){
      LOG.finer("FOUND: "+tvs.getLabel()); 
    } 
    return o;
  }
  
  /*
   * Searches for X, starting at the given type and its ancestors
   */
  public <T> void findXinAncestors(ISuperTypeSearchStrategy<T> s, IJavaDeclaredType type) {
    if (type == null) {
      return;
    }
    Operator op = JJNode.tree.getOperator(type.getDeclaration());
    
    if (ClassDeclaration.prototype.includes(op)) {
      findXinClass_up(s, type);   
    } else if (InterfaceDeclaration.prototype.includes(op)) { 
      findXinInterface_up(s, type);
    } else if (AnonClassExpression.prototype.includes(op)) {
      findXinClass_up(s, type);   
    } else if (AnnotationDeclaration.prototype.includes(op)) {
      findXinAnnotation_up(s, type);
    } else if (EnumDeclaration.prototype.includes(op)) {
      findXinEnum_up(s, type);
    } else if (EnumConstantClassDeclaration.prototype.includes(op)) {
      findXinEnumConstantClassDecl_up(s, type);
    } else {
      LOG.severe("Calling findXinAncestors on unhandled op "+op.name());
      return;
    }
  }
  
  private <T> void findXinAnnotation_up(ISuperTypeSearchStrategy<T> s, IJavaDeclaredType type) {
    if (type == null) {
      return;
    }
    s.visitClass(type.getDeclaration());
    
    if (s.visitSuperclass()) {
      IJavaDeclaredType stype = (IJavaDeclaredType) 
      	binder.getTypeEnvironment().findJavaTypeByName("java.lang.annotation.Annotation");
      if (stype != null && !type.equals(stype)) {
        if (LOG.isLoggable(Level.FINER)) {
          LOG.finer("Looking for X from "+type+" to "+stype);
        }
        findXinClass_up(s, stype);
      }
    }
  }
  
  /*
   * Helper for findXinType
   * Only to be called by findXinType and its helpers
   */
  private <T> void findXinClass_up(ISuperTypeSearchStrategy<T> s, IJavaDeclaredType type) {
    if (type == null) {
      return;
    }
    // LOG.info("findXinClass_up @"+ClassDeclaration.getId(type));
    s.visitClass(type.getDeclaration());
    
    if (s.visitSuperclass()) {
      IJavaDeclaredType stype = type.getSuperclass(binder.getTypeEnvironment());
      if (stype != null && !type.equals(stype)) {
        if (LOG.isLoggable(Level.FINER)) {
          LOG.finer("Looking for X from "+type+" to "+stype);
        }
        findXinClass_up(s, stype);
      }
    }
    if (s.visitSuperifaces()) {
      boolean first = true;
      for(IJavaType stype : binder.getTypeEnvironment().getSuperTypes(type)) {
        if (first) {
          first = false;
          continue; // skip superclass
        }
        findXinInterface_up(s, (IJavaDeclaredType) stype);
      }
    } 
  }
  
  /*
   * Helper for findXinType
   * Only to be called by findXinType and its helpers
   */
  private <T> void findXinInterface_up(ISuperTypeSearchStrategy<T> s, IJavaDeclaredType type) { 
    if (type == null) {
      return;
    }
    if (LOG.isLoggable(Level.FINER)) {
      LOG.finer("findXinInterface_up @ "+type);
    }
    s.visitInterface(type.getDeclaration());
    
    if (s.visitSuperifaces()) {
      Iteratable<IJavaType> ifaces = binder.getTypeEnvironment().getSuperTypes(type);
      if (!ifaces.hasNext()) {
    	  // Due to inability to find a binding
    	  //Iteratable<IJavaType> temp = tEnv.getSuperTypes(type);
    	  //temp.next();
    	  return; 
      }
      IJavaType first              = ifaces.next();
      if (!ifaces.hasNext() && s.visitSuperclass()) {
        // Need to get 'superclass' of interface
        findXinClass_up(s, (IJavaDeclaredType) first);
      } else while (ifaces.hasNext()) {
        findXinInterface_up(s, (IJavaDeclaredType) ifaces.next());
      }     
    }
  }
  
  private <T> void findXinEnum_up(ISuperTypeSearchStrategy<T> s, IJavaDeclaredType type) {
    if (type == null) {
      return;
    }
    s.visitClass(type.getDeclaration());
    
    if (s.visitSuperclass()) {
      IJavaDeclaredType stype = (IJavaDeclaredType) binder.getTypeEnvironment().findJavaTypeByName("java.lang.Enum");
      if (stype != null && !type.equals(stype)) {
        if (LOG.isLoggable(Level.FINER)) {
          LOG.finer("Looking for X from "+type+" to "+stype);
        }
        findXinClass_up(s, stype);
      }
    }
    if (s.visitSuperifaces()) {
      boolean first = true;
      for(IJavaType stype : binder.getTypeEnvironment().getSuperTypes(type)) {
        if (first) {
          first = false;
          continue; // skip superclass
        }
        findXinInterface_up(s, (IJavaDeclaredType) stype);
      }
    } 
  }
  
  private <T> void findXinTypeFormal_up(ISuperTypeSearchStrategy<T> s, IJavaTypeFormal formal) {
    for(IJavaDeclaredType bound : getFormalBounds(formal)) {
      findXinAncestors(s, bound); 
    }    
  }
  
  // Will not return any type formals as part of the iterator
  private Iteratable<IJavaDeclaredType> getFormalBounds(final IJavaTypeFormal formal) {
    final IRNode bounds            = TypeFormal.getBounds(formal.getDeclaration());
    final Iteratable<IRNode> bIter = MoreBounds.getBoundIterator(bounds);
    if (!bIter.hasNext()) { // empty
      return new EmptyIterator<IJavaDeclaredType>();
    }
    final IJavaType temp = binder.getJavaType(bIter.next());
    final IJavaDeclaredType first;
    final Iterator<IJavaDeclaredType> nIter;
    if (temp instanceof IJavaTypeFormal) {
      first = null;
      nIter = getFormalBounds((IJavaTypeFormal) temp);
    } else { 
      first = (IJavaDeclaredType) temp;
      nIter = null;
    }
    
    return new SimpleRemovelessIterator<IJavaDeclaredType>() {
      IJavaDeclaredType next = first; 
      Iterator<IJavaDeclaredType> nestedIter = nIter;
      final Iterator<IRNode> bounds = bIter;
      
      @Override
      protected Object computeNext() {
        if (next != null) {
          IJavaDeclaredType rv = next;
          next = null;
          return rv;
        }
        // next is null
        if (nestedIter != null) {
          if (nestedIter.hasNext()) {
            return nestedIter.next();            
          } else {
            nestedIter = null; // get rid of it since it's empty
          }
        }        
        if (bounds.hasNext()) {
          IRNode bound = bounds.next();
          IJavaType t  = binder.getJavaType(bound);
          if (t instanceof IJavaTypeFormal) {
            nestedIter = getFormalBounds((IJavaTypeFormal) t);      
            return nestedIter.next();
          } else {
            return t;            
          }
        }
        return IteratorUtil.noElement;
      }
    };    
  }
  
  private <T> void findXinEnumConstantClassDecl_up(ISuperTypeSearchStrategy<T> s, IJavaDeclaredType type) {
    if (type == null) {
      return;
    }
    s.visitClass(type.getDeclaration());
    
    if (s.visitSuperclass()) {
      IJavaDeclaredType stype = type.getSuperclass(binder.getTypeEnvironment());
      if (stype != null && !type.equals(stype)) {
        if (LOG.isLoggable(Level.FINER)) {
          LOG.finer("Looking for X from "+type+" to "+stype);
        }
        findXinClass_up(s, stype);
      }
    }
  }
}
