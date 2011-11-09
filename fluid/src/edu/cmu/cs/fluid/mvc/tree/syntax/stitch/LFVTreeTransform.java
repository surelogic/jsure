/*
 * Created on Sep 14, 2004
 *
 */
package edu.cmu.cs.fluid.mvc.tree.syntax.stitch;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.MarkedIRNode;
import static edu.cmu.cs.fluid.java.JavaGlobals.noNodes;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.mvc.tree.ForestModelCore;
import edu.cmu.cs.fluid.mvc.tree.ForestUtil;
import edu.cmu.cs.fluid.mvc.tree.ForestUtil.RootMutator;
import edu.cmu.cs.fluid.mvc.tree.stitch.IStitchTreeTransform;
import edu.cmu.cs.fluid.mvc.tree.syntax.SyntaxForestModel;
import edu.cmu.cs.fluid.mvc.tree.syntax.SyntaxForestModelCore;
import edu.cmu.cs.fluid.parse.Ellipsis;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;


/**
 * @author Edwin
 * @deprecated
 */
@SuppressWarnings("all")
@Deprecated
public class LFVTreeTransform implements IStitchTreeTransform {
  final SyntaxForestModelCore synModelCore;
  final ForestModelCore model;
  final SyntaxTreeInterface tree = JJNode.tree;
  final IBinder binder = null;
  
  LFVTreeTransform(ForestModelCore model, SyntaxForestModelCore smodel) {
    this.model = model;
    synModelCore = smodel;
  }
  
  /**
   * Logger for this class
   */
  protected static final Logger LOG = SLLogger
      .getLogger("FLUID.ir.lfv");
  
  public void init(AttributeHandler am) {
  }
  
  void addRoot(IRNode n) {
    if (!model.isRoot(n)) {
      model.addRoot(n);
    }
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.mvc.tree.stitch.IStitchTreeTransform#rewriteTree(edu.cmu.cs.fluid.ir.IRNode)
   */
  public void rewriteTree(IRNode n) {
    Operator op = tree.getOperator(n);
    if (op instanceof Ellipsis) {
      model.addRoot(n);
      return; // Nothing to do
    }
    if (CompilationUnit.prototype.includes(op)) {
      Iterator enm = getTypes(n);
      while (enm.hasNext()) {
        rewriteType((IRNode) enm.next());
      }
    } 
    addRoot(n);
    RootMutator rm = new RootMutator() {
      public void addRoot(IRNode root) {
        if (model.isRoot(root)) {
          return;
        }
        model.addRoot(root);        
      }
      public Operator getOperator(IRNode n) {
        return ((SyntaxForestModel)model).getOperator(n);
      }    
    };
    ForestUtil.addPromisesOnEnum(rm, model.bottomUp(n));
  }
  
  Iterator getTypes(IRNode cu) {
    IRNode decls = CompilationUnit.getDecls(tree, cu);
    return TypeDeclarations.getTypesIterator(tree, decls);
  }
  
  IRNode newNode(Operator op, IRNode[] children) {
    IRNode n = new MarkedIRNode("LFV");
    try {

    for(int i=0; i<children.length; i++) {
      model.setParent(children[i], 0, null);
//        model.clearParent(children[i]);
//      }
        // System.out.println("Got child: "+DebugUnparser.toString(children[i]));
    }    
    synModelCore.initNode(n, op, children);
    } catch(Throwable t) {
      t.printStackTrace();
    }
    return n;
  }
  
  IRNode getClassBody(IRNode type) {
    Operator op = tree.getOperator(type);
    if (op instanceof Ellipsis) {
      return null;
    }
    return VisitUtil.getClassBody(type); // XXX
  }
  
  Iterator getClassBodyMembers(IRNode body) {
    return VisitUtil.getClassBodyMembers(body); // XXX
  }
  
  IRNode getEnclosingType(IRNode n) {
    return VisitUtil.getEnclosingType(n); // XXX
  }
  
  void rewriteType(IRNode type) {
    ISuperTypeSearchStrategy s = new RewriteTypeStrategy();
    binder.findClassBodyMembers(type, s, false);

    List l           = (List) s.getResult();
    IRNode[] members = (IRNode[]) l.toArray(noNodes);
    IRNode body      = newNode(ClassBody.prototype, members);
    model.replaceSubtree(getClassBody(type), body);
  }

  /**
   * Should be based on qualified name
   * @param type
   * @return
   */
  String makeTypeLabel(IRNode type) {
    return TypeDeclaration.getId(type);
  }

  class RewriteTypeStrategy extends AbstractSuperTypeSearchStrategy {
    final List members = new ArrayList();
    
    public RewriteTypeStrategy() {
      super(null, "type", "");
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.eclipse.bind.ISuperTypeSearchStrategy#visitClass_internal(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public void visitClass_internal(IRNode type) {  
      rewriteClassBodyMembers(members, type);
      searchAfterLastType = true;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.eclipse.bind.ITypeSearchStrategy#getResult()
     */
    public Object getResult() {
      return members;
    }
  }
  
  void addMember(List members, IRNode member) {
    if (members.contains(member)) {
      (new Throwable("Duplicate member")).printStackTrace();
      return;
    }
    members.add(member);
  }
  
  void rewriteClassBodyMembers(final List members, IRNode type) {
    if (type == null) {
      return; // Nothing to do
    }
    final List<IRNode> statics = new ArrayList();
    final List inits   = new ArrayList();
    
    Iterator enm   = getClassBodyMembers(type);
    while (enm.hasNext()) {
      IRNode member = (IRNode) enm.next();
      Operator op   = tree.getOperator(member);
      if (MethodDeclaration.prototype.includes(op) ||
          TypeDeclaration.prototype.includes(op)) {
        addMember(members, member);
        // ignoring TypeDeclarations for now
      }
      else if (ConstructorDeclaration.prototype.includes(op)) {
        addMember(members, convertToMethod(member));
      }
      else if (FieldDeclaration.prototype.includes(op)) {
        List l = (JavaNode.getModifier(member, JavaNode.STATIC)) ? statics : inits;       
        splitVariableDeclarators(members, l, member);
      }
      else if (ClassInitializer.prototype.includes(op)) {
        if (JavaNode.getModifier(member, JavaNode.STATIC)) {
          statics.add(ClassInitializer.getBlock(member));
        } else {
          inits.add(ClassInitializer.getBlock(member));
        }
      }
    }
    // Make static/class initializers into separate methods
    String name = makeTypeLabel(type); 
    addMember(members, makeInitializerMethod("classInit_"+name, statics));
    addMember(members, makeInitializerMethod("init_"+name, inits));
  }
  
  IRNode makeInitializerMethod(String name, List inits) {
    int mods       = JavaNode.PRIVATE;
    IRNode typeFormals = newNode(TypeFormals.prototype,noNodes);
    IRNode type    = newNode(VoidType.prototype, noNodes);     // void return type
    IRNode params  = newNode(Parameters.prototype, noNodes);
    IRNode throwC  = newNode(Throws.prototype, noNodes);
    IRNode[] stmts = (IRNode[]) inits.toArray(noNodes);
    IRNode block   = newNode(BlockStatement.prototype, stmts);
    IRNode body    = newNode(MethodBody.prototype, new IRNode[] { block });
    return newMethod(mods, typeFormals,type, name, params, throwC, body);
  }
  
  IRNode newMethod(int mods, IRNode typeFormals, IRNode type, String name, IRNode params, IRNode throwC, IRNode body) {
    IRNode method = newNode(MethodDeclaration.prototype, 
                            new IRNode[] { typeFormals, type, params, throwC, body});
    JavaNode.setModifiers(method, mods);
    JJNode.setInfo(method, name);
    return method;
  }
  
  //convert a constructor to a method
  IRNode convertToMethod(IRNode c) {
    int mods      = ConstructorDeclaration.getModifiers(c);
    IRNode typeFormals = ConstructorDeclaration.getTypes(tree,c);
    IRNode type   = newNode(VoidType.prototype, noNodes);     // void return type
    String name   = ConstructorDeclaration.getId(c);
    IRNode params = ConstructorDeclaration.getParams(tree, c);
    IRNode throwC = ConstructorDeclaration.getExceptions(tree, c);
    IRNode body   = ConstructorDeclaration.getBody(tree, c);

    if (body == null) {
      LOG.severe("Ignoring "+DebugUnparser.toString(c));
      return c;
    }
    
    // modify body to eliminate any constructor call (if any)
    Operator bop = tree.getOperator(body);    
    if (MethodBody.prototype.includes(bop)) {
      IRNode block = MethodBody.getBlock(tree, body);
      if (tree.hasChildren(block)) {
        IRNode stmt = BlockStatement.getStmt(tree, block, 0);
        Operator op = tree.getOperator(stmt);  
        if (op instanceof ExprStatement) {
          IRNode call = ExprStatement.getExpr(tree,stmt);
          op = tree.getOperator(call);
          if (op instanceof OuterObjectSpecifier) {
            LOG.warning("don't know how to convert a nested super call to a method call");
            // TODO: Fix for nested classes.
            //  Probably the best thing is to insert the specifier as a new first argument.
            // proceed blindly:
            call = OuterObjectSpecifier.getCall(tree,call);
            op = tree.getOperator(call);
          }
        if (ConstructorCall.prototype.includes(op)) {
          // redirect to method to be created
          IRNode methodCall = convertToMethodCall(call);
          model.replaceSubtree(call, methodCall);
        }
        }
      }
    } 
    return newMethod(mods, typeFormals,type, name, params, throwC, body);
  }
  
  /**
   * 
   * @return
   */
  IRNode convertToMethodCall(IRNode call) {
    Operator op = tree.getOperator(call);
    IRNode base;
    IRNode args;
    IRNode types;
    // short cut methods don't take STI
    if (op instanceof NonPolymorphicConstructorCall) {
      base = NonPolymorphicConstructorCall.getObject(tree,call);
      args = NonPolymorphicConstructorCall.getArgs(tree,call);
      types = null;
    } else {
      base = PolymorphicConstructorCall.getObject(tree,call);
      args = PolymorphicConstructorCall.getArgs(tree,call);
      types = PolymorphicConstructorCall.getTypeArgs(tree,call);
    }
    
    Operator bop      = tree.getOperator(base);
    String methodname = "<unknown>";
    
    // previously this code had rules to handle the OuterObjectSpecifier
    // (Called a Qualifier) but this code was buggy: it converted the specifier
    // into a qualifier for QualifiedThisExpression or QualifiedSuperExpression
    // but this makes no sense since the OuterObjectSpecifier is an expression
    // but a qualifier for this or super is a type (previous a TypeExpression).
    // The OuterObjectSpecifier is a separate hidden argument to the
    // constructor.
    
    // This code assumes no qualifiers (warning in caller)
    IRNode type = getEnclosingType(call);

    // Figure out which class name to use for the new method
    if (SuperExpression.prototype.includes(bop)) {
      IRNode ext = ClassDeclaration.getExtension(tree,type);
      IJavaDeclaredType pt = (IJavaDeclaredType) JavaTypeFactory
          .convertNodeTypeToIJavaType(ext,binder);
      methodname = makeTypeLabel(pt.getDeclaration());
    } else if (ThisExpression.prototype.includes(bop)) {
      methodname = makeTypeLabel(type);
    } else {
      LOG.severe("Unexpected base object for constructor call: " + bop.name());
      return null;
    }
    
    IRNode mcall;
    if (types == null) {
      mcall = newNode(NonPolymorphicMethodCall.prototype, new IRNode[] {base, args});
    } else {
      mcall = newNode(PolymorphicMethodCall.prototype, new IRNode[] {base, types,args});
    }
    JJNode.setInfo(mcall, methodname); // What if node used in multiple trees?
    return mcall;
  }
  
  // go through declarators and separate out the initializers (if any)
  void splitVariableDeclarators(List members, List inits, IRNode field) {
    IRNode vs        = FieldDeclaration.getVars(tree, field);
    Iterator vars = VariableDeclarators.getVarIterator(tree, vs);
    while (vars.hasNext()) {
      IRNode var  = (IRNode) vars.next();          
      IRNode init = VariableDeclarator.getInit(tree, var);
      Operator op = tree.getOperator(init);
      if (Initialization.prototype.includes(op)) {
        // remove the init (for use below)
        model.replaceSubtree(init, newNode(NoInitialization.prototype, noNodes));
        
        // create assignment for initializer
        IRNode base   = newNode(ThisExpression.prototype, noNodes); // XXX FIX for static fields
        
        IRNode ref    = newNode(FieldRef.prototype, new IRNode[] {base});
        JJNode.setInfo(ref, VariableDeclarator.getId(var));
        
        IRNode assign = newNode(AssignExpression.prototype, 
                                new IRNode[] {ref, Initialization.getValue(tree, init)});
        IRNode stmt   = newNode(ExprStatement.prototype, new IRNode[] {assign});
        inits.add(stmt);           
      } else {
        // no initializer, so nothing to do
      }
    }
    addMember(members, field);
  }
}
