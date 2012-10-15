// $Header: /var/cvs/fluid/code/fluid/java/bind/BindUtil.java,v 1.8 2002/09/11
// 21:03:28 chance Exp $
package edu.cmu.cs.fluid.java.util;

import java.util.*;
import java.util.logging.*;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Iteratable;

// This is for code that is used to lookup and bind names to declarations
public class BindUtil implements JavaGlobals {
  /**
	 * Logger for this class
	 */
  static final Logger LOG = SLLogger.getLogger("FLUID.java.util");

  /// localVarDefined
  public static boolean localVarDefined(IRNode contextPoint, String id) {
    return (findLV(contextPoint, id) != null);
  }

  /// findLV
  public static IRNode findLV(IRNode leaf, String id) {
    // System.out.println("Trying to find "+id);

    Iterator<IRNode> enm = VisitUtil.rootWalk(leaf);
    IRNode lv_decl = null, last = null;
    while (enm.hasNext()) {
      IRNode here = enm.next();
      Operator op = jtree.getOperator(here);
      // System.out.println(op+": "+DebugUnparser.toString(here));

      find_decl : if (op instanceof Expression) {
        break find_decl; // short-cut optimization
      } else if (op instanceof BlockStatement) {
        lv_decl = searchVarDeclsInStmts(here, last, id);
      } else if (op instanceof MethodDeclaration) {
        IRNode params = MethodDeclaration.getParams(here);
        lv_decl = searchParameters(params, id);
      }
      // FIX redundancy
      else if (op instanceof ConstructorDeclaration) {
        IRNode params = ConstructorDeclaration.getParams(here);
        lv_decl = searchParameters(params, id);
      } else if (op instanceof CatchClause) {
        IRNode param = CatchClause.getParam(here);
        lv_decl = searchVarDecl(param, id);
      } else if (op instanceof ForStatement) {
        IRNode forinit = ForStatement.getInit(here);
        Operator initop = jtree.getOperator(forinit);
        if (initop instanceof DeclStatement) {
          IRNode vars = DeclStatement.getVars(forinit);
          lv_decl = searchVarDecls(vars, id);
        }
      } else if (op instanceof SwitchStatements) {
        lv_decl = searchVarDeclsInStmts(here, last, id);
      } else if (op instanceof SwitchBlock) {
        // section 14.9 - unclear about how vars match outside an elt
        // FIX not type checking switches
        // FIX assuming normal scoping (even if label not executed)
        //
        // Similar to searchVarDeclsInStmts
        IRLocation loc = jtree.getLocation(last);

        // assuming there's at least one
        IRNode currElt = last;
        IRNode firstElt = jtree.getChild(here, 0);

        // check all SwitchElements before me
        while (!currElt.equals(firstElt)) {        	
          loc = jtree.prevChildLocation(here, loc);
          currElt = jtree.getChild(here, loc);
          lv_decl =
            searchVarDeclsInAllStmts(SwitchElement.getStmts(currElt), id);
          if (lv_decl != null) {
            return lv_decl;
            // OK since will check for definite assignment
            // throw new NotImplemented("Found "+id+" in a preceding sBlk");
          }
        }
      }
      // could be inside a DeclStatement or a FieldDeclaration
      // 
      // how do I handle refs to vars in the same statement? see JLS
      // FIX -- see below
      //
      else if (op instanceof VariableDeclarator) {
        lv_decl = searchVarDecl(here, id);

        if (lv_decl != null) {
          throw new FluidError(
            "using "
              + id
              + " which was just declared: "
              + DebugUnparser.toString(here));
          // FIX redundant with the code below?
        }
      } else if (op instanceof VariableDeclarators) {
        // last node = VariableDeclarator?
        IRNode decl = searchVarDecls(here, id);

        if (decl == null) {
          break find_decl;
        }
        IRLocation loc1 = jtree.getLocation(decl);
        IRLocation loc2 = jtree.getLocation(last);
        // check if the var declared before the use
        // ASSERT getParent(decl) == here
        // ASSERT getParent(last) == here
        if (jtree.compareChildLocations(here, loc1, loc2) < 0) {
          lv_decl = decl;
        }
        // otherwise keep searching
      }

      if (lv_decl != null) {
        // System.out.println("1 Found "+DebugUnparser.toString(lv_decl));
        // (new Throwable()).printStackTrace();
        return lv_decl;
      } else
        last = here;
    }
    // System.out.println("1 Couldn't find "+id+" at "+Version.getVersion());
    // (new Throwable()).printStackTrace();
    return null;
    // return ParameterDeclaration.createNode(0, IntType.prototype.jjtCreate(),
    // name.intern(), 0);
  }

  /// searchVarDeclsInAllStmts -- for SwitchBlock
  public static IRNode searchVarDeclsInAllStmts(IRNode here, String id) {
    Iterator<IRNode> enm = jtree.children(here);
    IRNode lv_decl = null;
    while ((lv_decl == null) && (enm.hasNext())) {
      IRNode currStmt = enm.next();
      lv_decl = searchVarDeclsInStmt(currStmt, id);
    }
    if (lv_decl == null) {
      // System.out.println("2 Couldn't find "+id);
    }
    return lv_decl;
  }

  /// searchVarDeclsInStmts -- for BlockStmts and SwitchStmts
  static IRNode searchVarDeclsInStmts(
    IRNode here,
    IRNode currStmt,
    String id) {
    // assuming there's at least one
    IRNode firstStmt = jtree.getChild(here, 0);
    IRLocation loc = jtree.getLocation(currStmt);

    // check all DeclStatements before me
    while (!currStmt.equals(firstStmt)) {
      loc = jtree.prevChildLocation(here, loc);
      currStmt = jtree.getChild(here, loc);
      IRNode d = searchVarDeclsInStmt(currStmt, id);
      if (d != null) {
        return d;
      }
    }
    // System.out.println("3 Couldn't find "+id);
    return null;
  }

  /// searchVarDeclsInStmt
  public static IRNode searchVarDeclsInStmt(IRNode stmt, String id) {
    Operator op = jtree.getOperator(stmt);
    IRNode decl = null;

    if (op instanceof DeclStatement) {
      IRNode vars = DeclStatement.getVars(stmt);
      decl = searchVarDecls(vars, id);
    }
    if (decl == null) {
      // System.out.println("4 Couldn't find "+id);
    }
    return decl;
  }

  /// searchVarDecls
  // returns null if not found
  public static IRNode searchVarDecls(IRNode decls, final String id) {
    Iterator<IRNode> enm = jtree.children(decls); // FIX is this right?
    while (enm.hasNext()) {
      // iterate over the vars and check
      IRNode rv = searchVarDecl(enm.next(), id);
      if (rv != null) {
        return rv;
      }
    }
    // System.out.println("5 Couldn't find "+id);
    return null;
  }
  
  public static IRNode searchParameters(IRNode decls, final String id) {
    IRNode rv = searchVarDecls(decls, id);    
    if (rv == null) {
      // check for canonical name: arg#
      if (id.startsWith("arg")) {
        try {
          int i   = Integer.parseInt(id.substring(3));
          int num = jtree.numChildren(decls);
          if (i < num) {
            return jtree.getChild(decls, i);
          }
          return null;
        }
        catch (NumberFormatException e) {
          return null;
        }
      }
    }
    return rv;
  }

  /// searchVarDecl
  // returns null if not found
  public static IRNode searchVarDecl(IRNode decl, String id) {
    String vName = VariableDeclaration.getId(decl);
    // System.out.println("Found decl: "+vName+" vs "+id);
    return (vName.equals(id)) ? decl : null;
  }

  /// findFieldInBody
  public static IRNode findFieldInBody(IRNode body, String name) {
    return findFieldInBodyExtl(body, name);
    // return scopeSI.findFieldInBody(body, name);
  }
  static IRNode findFieldInBodyExtl(IRNode body, String name) {
    Iterator<IRNode> enm = ClassBody.getDeclIterator(body);
    while (enm.hasNext()) {
      IRNode n = enm.next();
      Operator op = jtree.getOperator(n);

      if (op == FieldDeclaration.prototype) {
        IRNode vars = FieldDeclaration.getVars(n);
        IRNode f = searchVarDecls(vars, name);
        if (f != null) {
          return f;
        }
      }
      // FIX what about inner classes?
    }
    return null;
  }
  
//  /**
//   * Get the visibility of a NewRegionDeclaration, FieldDeclaration,
//   * MethodDeclaration, ConstructorDeclaration.
//   */
//  public static int getVisibility(final IRNode decl) {
//    final Operator op = JJNode.tree.getOperator(decl);
//    final int mods;
//    if (EnumConstantDeclaration.prototype.includes(op)) {
//      return JavaNode.PUBLIC;
//    } else if (VariableDeclarator.prototype.includes(op)) {
//      mods = VariableDeclarator.getMods(decl);
//    } else {
//      mods = JavaNode.getModifiers(decl);
//    }
//    return mods & (JavaNode.PRIVATE | JavaNode.PROTECTED | JavaNode.PUBLIC);
//  }
  
//  /**
//   * Given two region/field/method declarations determine 
//   * <code>decl1</code> is (at least) as visible as <code>decl2</code>;
//   * e.g., protected is at least as visible as (default).
//   */
//  public static boolean isAsVisibleAs( final IRNode decl1, final IRNode decl2 )
//  {
//    final int vis1 = getVisibility(decl1);
//    final int vis2 = getVisibility(decl2);    
//    return isMoreVisibleThan(vis1, vis2);
//  }
  
//  public static boolean isAsVisibleAs( final int vis1, final IRNode decl2 )
//  {
//    final int vis2 = getVisibility(decl2);    
//    return isMoreVisibleThan(vis1, vis2);
//  }
  
//  /**
//   * Given two visibilities return if the first one is as visible or more visible
//   * than the second one.
//   */
//  public static boolean isMoreVisibleThan(final int vis1, final int vis2) {
//    /* Exploiting the fact that
//     *   JavaNode.PRIVATE < JavaNode.PROTECTED < JavaNode.PUBLIC.
//     * But, vis == 0 implies DEFAULT visibility.
//     */
//    if( vis1 == JavaNode.PUBLIC ) {
//      return true;  // vis2 <= vis1
//    } else if( vis1 == JavaNode.PROTECTED ) {
//      return vis2 <= JavaNode.PROTECTED;  // vis2 <= vis1
//    } else if( vis1 == 0 ) {
//      return vis2 <= JavaNode.PRIVATE;
//    } else if( vis1 == JavaNode.PRIVATE ) {
//      return vis2 == JavaNode.PRIVATE;
//    } else {
//      return false;
//    }
//  }

  /**
   * Determine whether the declaration is accessible from the given viewpoint,
   * assuming it is visible, and assume its surrounding context is accessible.
   * @param decl declaration to access
   * @param from place to access from (assumed to be within a class)
   * @return whether accessible
   */
  public static final boolean isAccessible(ITypeEnvironment tEnv, IRNode decl, IRNode from) {
    IRNode enclosingType = VisitUtil.getEnclosingType(from);
    if (enclosingType == null) {
    	final IRNode cu = VisitUtil.getEnclosingCompilationUnit(from);
    	enclosingType = VisitUtil.getPrimaryType(cu);
    	if (enclosingType == null) {
    		// As a last resort, use the package decl
    		enclosingType = CompilationUnit.getPkg(cu);    				
    	}
    }
    boolean rv = isAccessible(tEnv, decl, from, enclosingType);
	if (!rv && enclosingType == null) {    		
		System.out.println("Unable to get enclosing type: "+DebugUnparser.toString(from));
		VisitUtil.getEnclosingType(from);
	}
    return rv;
  }
  
  /**
   * Determine whether the declaration is accessible from the given viewpoint,
   * assuming it is visible, and assume its surrounding context is accessible.
   * @param decl A declaration to access
   * @param enclosingType The type to access the decl from (somewhere within that type)
   * @return whether accessible
   */
  public static final boolean isAccessibleInsideType(ITypeEnvironment tEnv, IRNode decl, IRNode enclosingType) {
	if (decl == null) {
		return false;
	}
    return isAccessible(tEnv, decl, enclosingType, enclosingType);
  }

  /**
   * Determine whether a member declared in the given type with the given visibility
   * is accessible from the given viewpoint,
   * @param mods The accessibility of the type member in question
   * @param declaringType The type declaring the type member
   * @param enclosingType The type to access the decl from (somewhere within that type)
   * @return whether accessible
   */
  public static final boolean isAccessibleInsideType(ITypeEnvironment tEnv, 
                                                     Visibility visibility, IRNode declaringType, 
                                                     IRNode enclosingType) {
    return isAccessible(tEnv, visibility, declaringType, declaringType, enclosingType, enclosingType);
  }
  
  /**
   * @param enclosingType could be a package decl
   */
  private static final boolean isAccessible(ITypeEnvironment tEnv, IRNode decl, IRNode from, IRNode enclosingType) {
    // FIX?
    final Visibility viz = Visibility.getVisibilityOf(decl);
    IRNode declaringT = VisitUtil.getEnclosingType(decl); 
    if (declaringT == null && TypeDeclaration.prototype.includes(decl)) {
    	// Top-level type declares itself
    	declaringT = decl;
    }
    return isAccessible(tEnv, viz, decl, declaringT, enclosingType, enclosingType);
  }
  
  /**
   * @param tEnv
   * @param visibility
   * @param decl As close to where the decl should be
   * @param declaringType
   * @param from As close to where the access should be
   * @param enclosingType could be a package decl
   */
  private static final boolean isAccessible(ITypeEnvironment tEnv, 
                                            Visibility visibility, IRNode decl, IRNode declaringType,
                                            IRNode from, IRNode enclosingType) {
    if (visibility == Visibility.PUBLIC) return true;

    // FIX does this work right for inner classes?    
    IRNode parent = JJNode.tree.getParent(decl);
    for (IRNode here : JJNode.tree.rootWalk(from)) {
      if (here.equals(parent)) return true;
    }
    if (visibility == Visibility.PRIVATE) {
      // check if the use is in an outer class of the decl
      // (or a fellow inner class)
      IRNode fromT = enclosingType;
      while (fromT != null) {
        IRNode declT = declaringType;
        while (declT != null) {
          if (declT.equals(fromT)) {
            return true;
          }
          declT = VisitUtil.getEnclosingType(declT);
        }
        fromT = VisitUtil.getEnclosingType(fromT);
      }
      LOG.finest("was private: not accessible");
      return false;
    }

    IRNode declCU = VisitUtil.getEnclosingCompilationUnit(declaringType);
    IRNode fromCU = VisitUtil.getEnclosingCompilationUnit(enclosingType);
    if (declCU == null || fromCU == null) {
      // TODO is this right?
      if (NamedPackageDeclaration.prototype.includes(decl)) {
    	  return true;
      }
      LOG.warning("Not in a compilation unit: " + 
    		      DebugUnparser.toString(declCU == null ? decl : from));
      System.out.println("IRNode: "+decl);      
      return false;
    }
    IRNode declPkg = CompilationUnit.getPkg(declCU);
    IRNode fromPkg = CompilationUnit.getPkg(fromCU);
    if (JJNode.tree.getOperator(declPkg) instanceof UnnamedPackageDeclaration) {
      if (JJNode.tree.getOperator(fromPkg) instanceof UnnamedPackageDeclaration)
        return true;
    } else if (JJNode.tree.getOperator(fromPkg) instanceof NamedPackageDeclaration) {
      String declPkgN = NamedPackageDeclaration.getId(declPkg);
      String fromPkgN = NamedPackageDeclaration.getId(fromPkg);
      if (declPkgN.equals(fromPkgN)) return true;
      if (LOG.isLoggable(Level.FINEST)) {
        LOG.finest("Different packages: " + declPkgN + " != " + fromPkgN);
      }
    }
    if (visibility != Visibility.PROTECTED) return false;
    
    // Handle PROTECTED
    
    final IJavaType declT = tEnv.computeErasure(tEnv.getMyThisType(declaringType));
    IRNode here = VisitUtil.getClosestType(from);
    Stack<IJavaType> stack = new Stack<IJavaType>();
    
    // Check this type and its enclosing/outer types    
    while (here != null) {          
      IJavaType hereT = tEnv.computeErasure(tEnv.getMyThisType(here));
      
      /* Delayed to reverse the order, and prevent a DerivationException
       * 
      if (tEnv.isSubType(hereT, declT)) {
        return true;
      }
      */
      stack.push(hereT);
      here = VisitUtil.getEnclosingType(here);
    }
    while (!stack.isEmpty()) {
    	IJavaType hereT = stack.pop();
    	if (tEnv.isSubType(hereT, declT)) {
            return true;
    	}
    }
    return false;
  }
  
  /// other stuff 
  public static final int NORMAL_METHOD = 0;
  public static final int COMPILED_METHOD = 1;
  public static final int OMITTED_METHOD = 2;
  public static final int ABSTRACT_METHOD = 3;
  
  /**
   * 
   * @param method A method or constructor declaration
   * @return One of the constants above
   */
  public static int determineKindOfMethod(IRNode method) {
    IRNode body = VisitUtil.getMethodBody(method);
    Operator op = JJNode.tree.getOperator(body);
    if (MethodBody.prototype.includes(op)) {
      return NORMAL_METHOD;
    }
    else if (NoMethodBody.prototype.includes(op)) {
      return ABSTRACT_METHOD;
    }
    else if (CompiledMethodBody.prototype.includes(op)) {
      return COMPILED_METHOD;
    }
    else if (OmittedMethodBody.prototype.includes(op)) {
      return OMITTED_METHOD;
    }
    throw new FluidError("Getting a unknown kind of method body: "+op.name());
  }

  /**
   * @param use The potential NamedType in an extends or implements clause
   */
  public static boolean isPartOfSubtypeClause(IRNode n) {
    // n needs to be a NamedType
    final Operator op = jtree.getOperator(n);
    if (!(op instanceof NamedType)) {
      return false;
    }
    // p should be a TypeDeclaration
    final IRNode p = jtree.getParentOrNull(n);
    if (p == null) {
      return false;
    }
    final Operator pop = jtree.getOperator(p);
    if (TypeExpression.prototype.includes(pop)) {
      return false;
    }
    if (ClassDeclaration.prototype.includes(pop) ||
       AnonClassExpression.prototype.includes(pop)) {
      return true;
    } 
    else if (Implements.prototype.includes(pop) ||
             Extensions.prototype.includes(pop)) {
      return true;
    }
    return false;
  }


  public static IRNode findInnerType(IRNode root, String name) {
    Iteratable<IRNode> enm = VisitUtil.getInnerClasses(root);
    while (enm.hasNext()) {
      IRNode n = enm.next();
      String inner = JJNode.getInfo(n);
      if (name.equals(inner)) {
        return n;
      }
    }
    return null;
  }
  
  public static IRNode findAnonOrLocalClass(IRNode root, String name, IRNode enclosingMethod,
		                                    boolean isAnonymous, boolean isEnum) {
	if (!isAnonymous && name.endsWith("){}")) {
		isAnonymous = true;
	}
    //int leftParen = name.indexOf('(');
    //String prefix = name.substring(0, leftParen);
    IRNode bestMatch = null;
    
    for(IRNode n : JJNode.tree.topDown(root)) {
      if (!isAnonymous && ClassDeclaration.prototype.includes(n)) {
        if (name != null && name.equals(ClassDeclaration.getId(n))) {
          IRNode method = VisitUtil.getEnclosingMethod(n);
          if (enclosingMethod != null && enclosingMethod.equals(method)) {
            return n;
          }
          bestMatch = n;
        }        
      }
      else if (isEnum && isAnonymous && EnumConstantClassDeclaration.prototype.includes(n)) {
        bestMatch = n;
      }
      else if (isAnonymous && AnonClassExpression.prototype.includes(n)) {      
//        String msg = DebugUnparser.toString(n);
//        System.out.println("Considering ACE: "+msg);
        bestMatch = n;
      }
    }
    return bestMatch;
  }

  public static IRNode findAnonClass(IRNode root, String name) {
	  for(IRNode n : JJNode.tree.topDown(root)) {
		  if (AnonClassExpression.prototype.includes(n)) {
			  String id = JJNode.getInfoOrNull(n);
			  if (name.equals(id)) {
				  return n;
			  }
		  }
	  }
	  return null;
  }
}
