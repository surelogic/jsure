// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/parse/AstGen.java,v 1.11 2006/05/04 15:27:54 chance Exp $
package edu.cmu.cs.fluid.java.parse;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.*;

import com.surelogic.common.SLUtility;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaOperator;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.ParseException;
import edu.cmu.cs.fluid.tree.Operator;

public abstract class AstGen extends JavaParser {
  private AstGen() {
    super((Reader) null);
  }

  private static final Reader empty = new StringReader(new String(""));
  public static final JavaParser parser = new JavaParser(empty);

  public static JavaParser Reinit(String source) {
    JavaParser.ReInit(new StringReader(source));
    return parser; // really only needed to compact the code.
  }

  public static IRNode rootNode() {
    return (IRNode) JavaParser.jjtree.rootNode();
  }
  // then call whatever you want to parse
  // IRNode ast = AstGen.parser.

  public static IRNode genExpr(String code) {
    try {
      Reinit(code);
      JavaParser.Expression();
      return AstGen.rootNode();
    } catch (ParseException e) {
      throw new FluidError(e.getMessage());
    }
  }
  public static IRNode genStmt(String code) {
    try {
      Reinit(code);
      JavaParser.Statement();
      return AstGen.rootNode();
    } catch (ParseException e) {
      throw new FluidError(e.getMessage());
    }
  }
  public static IRNode genMember(String code) {
    try {
      Reinit(code);
      JavaParser.ClassBodyDeclaration();
      return AstGen.rootNode();
    } catch (ParseException e) {
      throw new FluidError(e.getMessage());
    }
  }
  public static IRNode genTypeDecl(String code) {
    try {
      Reinit(code);
      JavaParser.TypeDeclaration();
      return AstGen.rootNode();
    } catch (ParseException e) {
      throw new FluidError(e.getMessage());
    }
  }
  public static IRNode genConstructorCall(String code) {
    try {
      Reinit(code);
      JavaParser.ExplicitConstructorInvocation();
      return AstGen.rootNode();
    } catch (ParseException e) {
      throw new FluidError(e.getMessage());
    }
  }
  public static IRNode genVarInit(String code) {
    try {
      Reinit(code);
      JavaParser.VariableInitializer();
      return AstGen.rootNode();
    } catch (ParseException e) {
      throw new FluidError(e.getMessage());
    }
  }

  // methods based on Reader

  public static IRNode genCompilationUnit(Reader r) throws ParseException {
    JavaParser.ReInit(r);
    JavaParser.CompilationUnit();
    if (!isAtEOF())
      throw new ParseException("parse error: unexpected input");
    return AstGen.rootNode();
  }

  public static IRNode[] genImportDeclarations(Reader r)
    throws ParseException {
    JavaParser.ReInit(r);

    List<IRNode> v = new Vector<IRNode>();

    while (!isAtEOF()) {
      JavaParser.jjtree.reset();
      JavaParser.ImportDeclaration();
      v.add(AstGen.rootNode());
    }

    IRNode[] tmp = new IRNode[v.size()];
    tmp = v.toArray(tmp);

    return tmp;
  }

  public static IRNode[] genTypeDecls(Reader r) throws ParseException {
    JavaParser.ReInit(r);

    List<IRNode> v = new Vector<IRNode>();

    while (!isAtEOF()) {
      JavaParser.jjtree.reset();
      JavaParser.TypeDeclaration();
      v.add(AstGen.rootNode());
    }

    IRNode[] tmp = new IRNode[v.size()];
    tmp = v.toArray(tmp);

    return tmp;
  }

  public static IRNode[] genMembers(Reader r) throws ParseException {
    JavaParser.ReInit(r);

    List<IRNode> v = new Vector<IRNode>();

    while (!isAtEOF()) {
      JavaParser.jjtree.reset();
      JavaParser.ClassBodyDeclaration();
      v.add(AstGen.rootNode());
    }

    IRNode[] tmp = new IRNode[v.size()];
    tmp = v.toArray(tmp);

    return tmp;
  }

  public static IRNode[] genStmts(Reader r) throws ParseException {
    JavaParser.ReInit(r);

    List<IRNode> v = new Vector<IRNode>();

    while (!isAtEOF()) {
      JavaParser.jjtree.reset();
      JavaParser.Statement();
      v.add(AstGen.rootNode());
    }

    IRNode[] tmp = new IRNode[v.size()];
    tmp = v.toArray(tmp);

    return tmp;
  }

  public static IRNode[] parseNodes(Reader r, Operator o)
    throws ParseException, IOException {
    if (!(o instanceof JavaOperator))
      throw new ParseException(o.name() + " is not a valid Java operator");

    IRNode[] tmp;
    JavaOperator jo = (JavaOperator) o;

    if (CompilationUnit.prototype.includes(jo)) {
      tmp = new IRNode[1];
      tmp[0] = genCompilationUnit(r);
    } else if (ImportDeclaration.prototype.includes(jo)) {
      tmp = genImportDeclarations(r);
    } else if (TypeDeclaration.prototype.includes(jo)) {
      tmp = genTypeDecls(r);
    } else if (ClassBodyDeclaration.prototype.includes(jo)) {
      tmp = genMembers(r);
    } else if (Statement.prototype.includes(jo)) {
      tmp = genStmts(r);
    } else {
      throw new ParseException(
        "Incremental parsing not yet supported for "
          + jo.name()
          + " type of java nodes.");
    }
    return tmp;
  }

  private static final JavaOperator[] opMapping =
    {
      Expression.prototype,
      Statement.prototype,
      Type.prototype,
      PackageDeclaration.prototype,
    // what about Import/TypeDeclarations?
  };

  private static final Object[][] nameMapping =
    { { VoidType.prototype, "ResultType" }, };

  private static final Object[] mArgs = SLUtility.EMPTY_OBJECT_ARRAY;

  private static JavaOperator generalizeOperator(Operator op) {
    for (int i = 0; i < opMapping.length; i++) {
      // see if I need to generalize the operator
      if (opMapping[i].includes(op)) {
        return opMapping[i];
      }
    }
    return (JavaOperator) op;
  }

  private static String getMethodName(JavaOperator op) {
    for (int i = 0; i < nameMapping.length; i++) {
      Object[] map = nameMapping[i];
      JavaOperator jo = (JavaOperator) map[0];
      // see if I need to generalize the operator
      if (jo.includes(op)) {
        return (String) map[1];
      }
    }
    return op.name();
  }

  final static Class[] noArgs = new Class[0];
  
  /**
   * Returns non-null if parseable
   */
  private static Method parseableAs(Operator op) {
    if (!(op instanceof JavaOperator)) {
      // FIX check if it's a remark?
      return null;
    }
    final JavaOperator jo = generalizeOperator(op);
    final String mName = getMethodName(jo);
    try {
      final Method m = parser.getClass().getMethod(mName, noArgs);
      return m;
    } catch (Exception e) {
      // NoSuchMethodException, SecurityException,
      // IllegalAccessException, IllegalArgumentException,
      // InvocationTargetException
      // it should never come here
      e.printStackTrace();
      return null;
    }
  }

  public static boolean isParseable(Operator op) {
    return (parseableAs(op) != null);
  }

  /** 
   * However, since it exploits the fact that the name of operator is
   * same as the name of corresponding non-terminal in the grammer, it
   * breaks the abstraction created by parser.
   */
  public static IRNode[] parseASTs(Reader r, Operator op)
    throws ParseException, IOException {
    final Method m = parseableAs(op);
    if (m == null) {
      return JavaGlobals.noNodes;
    }
    try {
      final List<IRNode> nodes = new Vector<IRNode>();
      JavaParser.ReInit(r);

      while (!isAtEOF()) {
        JavaParser.jjtree.reset();
        m.invoke(parser, mArgs);
        nodes.add(rootNode());
      }
      // convert vector to array
      return nodes.toArray(new IRNode[nodes.size()]);
    } catch (Exception e) {
      // NoSuchMethodException, SecurityException,
      // IllegalAccessException, IllegalArgumentException,
      // InvocationTargetException
      // it should never come here
      throw new FluidError(
        "Unable to invoke '" + m.getName() + "' -- " + e.getMessage());
    }
  }

}
