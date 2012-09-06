/*
 * Created on Aug 4, 2004
 *
 */
package edu.cmu.cs.fluid.java.target;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.java.util.OpSearch;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.promise.IPromiseRule;
import edu.cmu.cs.fluid.tree.Operator;


/**
 * @author chance
 */
public class TargetMatcherFactory implements JavaGlobals {
  static final Logger LOG = SLLogger.getLogger("FLUID.java.target");
  
  public static final TargetMatcherFactory prototype = new TargetMatcherFactory();
  
  public ITargetMatcher create(IRNode target) {
    // return new TargetMatcher(pattern);

    final Operator top = jtree.getOperator(target);    

    if (MethodDeclPattern.prototype.includes(top)) {    
      final String namePattern = MethodDeclPattern.getName(target);
      if ("**".equals(namePattern)) {      
        final IRNode typeQ = MethodDeclPattern.getType(target);
        final String type = TypeQualifierPattern.getType(typeQ);
        final String pkgName = TypeQualifierPattern.getPkg(typeQ);
        
        ITargetMatcher m1 = new MethodMatcher(target, "*");
        ITargetMatcher m2 = new ConstructorMatcher(target, pkgName, type, 
                                                     MethodDeclPattern.getSig(target), 
                                                     MethodDeclPattern.getThrowsC(target));         
        return new OrMatcher(m1, m2);  
      }      
      return new MethodMatcher(target, namePattern);
    } 
    else if (ConstructorDeclPattern.prototype.includes(top)) {    
      return new ConstructorMatcher(target, ConstructorDeclPattern.getPkg(target),
                                      ConstructorDeclPattern.getType(target),
                                      ConstructorDeclPattern.getSig(target),
                                      ConstructorDeclPattern.getThrowsC(target));
    } 
    else if (OrTarget.prototype.includes(top)) {
      return new OrMatcher(create(OrTarget.getTarget1(target)),
                           create(OrTarget.getTarget2(target)));        
    }
    else if (NotTarget.prototype.includes(top)) {
      return new NotMatcher(create(NotTarget.getTarget(target)));
    }
    else if (AndTarget.prototype.includes(top)) {
      return new AndMatcher(create(AndTarget.getTarget1(target)),
                              create(AndTarget.getTarget2(target)));   
    } 
    // Assume one decl per FD
    else if (FieldDeclPattern.prototype.includes(top)) {  
      return new FieldMatcher(target);
    }
    else if (TypeDeclPattern.prototype.includes(top)) {   
      return new TypeMatcher(target);
    }
    else if (top == PromiseTarget.prototype) {
      return new AnyMatcher(target);
    }
    LOG.warning("Trying to match against unknown target "+DebugUnparser.toString(target));
    return NullMatcher.prototype;
  }
  
  static class NullMatcher implements ITargetMatcher {
    static ITargetMatcher prototype = new NullMatcher();

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.target.ITargetMatcher#match(edu.cmu.cs.fluid.ir.IRNode)
     */
    public boolean match(IRNode decl) {
      return false;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.target.ITargetMatcher#match(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.tree.Operator)
     */
    public boolean match(IRNode decl, Operator op) {
      return false;
    }
    
    public boolean match(String s) {
      return false;
    }
  }
  
  static Pattern compileName(final String pattern) {
    final String pattern2;
    if ("*".equals(pattern) || "**".equals(pattern)) {
      pattern2 = ".*";
    }
    else if (pattern.indexOf('*') < 0) {
      // No wildcards to deal with
      pattern2 = pattern;
    }
    else {
      pattern2 = pattern.replaceAll("\\*", ".*");
    }
    return Pattern.compile(pattern2);
  }
  
  static class MethodMatcher extends AbstractTargetMatcher {
    final Pattern name;
    final TypeQualifierMatcher type;
    
    MethodMatcher (IRNode target, String name) {
      super(target);
      this.name   = compileName(name);
      type        = compileTypeQualifier(MethodDeclPattern.getType(target));
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.target.AbstractTargetMatcher#match(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.tree.Operator)
     */
    public boolean match(IRNode decl, Operator dop) {
      return MethodDeclaration.prototype.includes(dop) &&
      matchesName(MethodDeclaration.getId(decl), name) &&
      type.match(decl) &&
      matchesModifiers(decl, target) &&
      matchesType(MethodDeclaration.getReturnType(decl),
                  MethodDeclPattern.getRtype(target)) &&
      matchesSignature(MethodDeclaration.getParams(decl), 
                    MethodDeclPattern.getSig(target)) &&
      matchesThrows(MethodDeclaration.getExceptions(decl),
                    MethodDeclPattern.getThrowsC(target));
    }
  }

  static class ConstructorMatcher extends AbstractTargetMatcher {
    final TypeQualifierMatcher type;
    final IRNode sig, throwsC;
    ConstructorMatcher (IRNode target, String pkgName, String type, IRNode sig, IRNode throwsC) {    
      super(target);
      this.type = new TypeQualifierMatcher(pkgName, type, true);
      this.sig = sig;
      this.throwsC = throwsC;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.target.AbstractTargetMatcher#match(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.tree.Operator)
     */
    public boolean match(IRNode decl, Operator dop) {
      if (!ConstructorDeclaration.prototype.includes(dop)) {
        return false;
      }
      return matchesConstructor(decl, target, type, sig, throwsC);
    }
  }
  
  static class TypeMatcher extends AbstractTargetMatcher {
    final TypeQualifierMatcher type;
    TypeMatcher (IRNode target) {
      super(target); 
      type = compileTypeDecl(target);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.target.AbstractTargetMatcher#match(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.tree.Operator)
     */
    public boolean match(IRNode decl, Operator dop) {
      return TypeDeclaration.prototype.includes(dop) && 
      matchesModifiers(decl, target) &&   
      type.match(decl);
    }
    @Override
    public boolean match(String name) {      
      return type.match(name);
    }
  }
  
  static class FieldMatcher extends AbstractTargetMatcher {
    final Pattern name;
    final TypeQualifierMatcher type;
    
    FieldMatcher (IRNode target) {
      super(target); 
      name = compileName(FieldDeclPattern.getName(target));
      type = compileTypeQualifier(FieldDeclPattern.getType(target));
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.target.AbstractTargetMatcher#match(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.tree.Operator)
     */
    public boolean match(IRNode decl, Operator dop) {
      if (!FieldDeclaration.prototype.includes(dop)) {
        return false;
      }
      IRNode vdecl = VariableDeclarators.getVar(FieldDeclaration.getVars(decl), 0);
      boolean v1 = matchesModifiers(decl, target);
      boolean v2 = matchesName(VariableDeclarator.getId(vdecl), name);
      boolean v3 = type.match(decl);
      boolean v4 = matchesType(FieldDeclaration.getType(decl), FieldDeclPattern.getFtype(target));      
      return v1 && v2 && v3 && v4;
    }
  }
  
  static class AnyMatcher extends AbstractTargetMatcher {
    final Operator[] ops;

    AnyMatcher (IRNode target) {
      super(target); 
      
      IRNode scopedPromise = jtree.getParent(target);
      String promise       = ScopedPromise.getPromise(scopedPromise);
      IPromiseRule rule    = null;//PromiseFramework.getInstance().getParser().getRule(promise);
      ops                  = rule.getOps(null);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.target.AbstractTargetMatcher#match(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.tree.Operator)
     */
    public boolean match(IRNode decl, Operator dop) {
      final boolean fineIsLoggable = LOG.isLoggable(Level.FINE);
      if (fineIsLoggable) {
        LOG.fine("Trying to match against any applicable construct");
      }
      for(int i=0; i<ops.length; i++) {
        if (ops[i].includes(dop)) {
          // Rule matches the declaration
          if (fineIsLoggable) {
            LOG.fine("Matched applicable op: "+dop.name());
          }
          return true;
        }
      }
      return false;
    }
  }
  
  static class NotMatcher implements ITargetMatcher {
    final ITargetMatcher m;
    NotMatcher (ITargetMatcher m) {
      this.m = m;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.target.ITargetMatcher#match(edu.cmu.cs.fluid.ir.IRNode)
     */
    public boolean match(IRNode decl) {
      return !m.match(decl);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.target.ITargetMatcher#match(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.tree.Operator)
     */
    public boolean match(IRNode decl, Operator op) {
      return !m.match(decl, op);
    }
    
    public boolean match(String s) {
      return !m.match(s);
    }
  }
  
  static class OrMatcher implements ITargetMatcher {
    final ITargetMatcher m1, m2;
    OrMatcher (ITargetMatcher m1, ITargetMatcher m2) {
      this.m1 = m1;
      this.m2 = m2;
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.target.ITargetMatcher#match(edu.cmu.cs.fluid.ir.IRNode)
     */
    public boolean match(IRNode decl) {
      return m1.match(decl) || m2.match(decl);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.target.ITargetMatcher#match(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.tree.Operator)
     */
    public boolean match(IRNode decl, Operator op) {
      return m1.match(decl, op) || m2.match(decl, op);
    }
    
    public boolean match(String s) {
      return m1.match(s) || m2.match(s);
    }
  }
  
  static class AndMatcher implements ITargetMatcher {
    final ITargetMatcher m1, m2;
    AndMatcher (ITargetMatcher m1, ITargetMatcher m2) {
      this.m1 = m1;
      this.m2 = m2;      
    }
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.target.ITargetMatcher#match(edu.cmu.cs.fluid.ir.IRNode)
     */
    public boolean match(IRNode decl) {
      return m1.match(decl) && m2.match(decl);
    }
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.target.ITargetMatcher#match(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.tree.Operator)
     */
    public boolean match(IRNode decl, Operator op) {
      return m1.match(decl, op) && m2.match(decl, op);
    }
    
    public boolean match(String s) {
      return m1.match(s) && m2.match(s);
    }
  }

  static class TypeQualifierMatcher extends AbstractTargetMatcher { 
    final String type, pkg;
    final Pattern pkgPattern;
    final Pattern typePattern;
    final boolean useEnclosing;
    
    TypeQualifierMatcher(String pkgName, String type, final boolean useEnclosing) {
      super(null);
      this.type = type;
      pkg = pkgName;
      pkgPattern = compilePattern(pkgName);
      typePattern = compilePattern(type);
      this.useEnclosing = useEnclosing;
    }
    
    /**
     * Only used in constructor
     * @param pattern
     * @return
     */
    private static Pattern compilePattern(String pattern) {
      if (pattern.indexOf('*') < 0) {
        // No wildcards
        String pattern2 = pattern.replaceAll(".", "\\.");
        return Pattern.compile(pattern2);  
      } 
      if (pattern.equals("*")) {
        return Pattern.compile(".*");
      }
      // Otherwise, construct regex corresponding to the pattern
      final StringTokenizer st = new StringTokenizer(pattern, ".");
      // first token is identifier
      String first = st.nextToken();
      final StringBuilder buf   = new StringBuilder(); 
      processToken(buf, first);
      
      boolean lastIsStarStar = false;
      while (st.hasMoreTokens()) {
        final String seg = st.nextToken();
        if ("*".equals(seg)) {
          buf.append("\\.\\w+"); // match a dot and one or more 'word' characters
        }
        else if ("**".equals(seg)) {
          if (!lastIsStarStar) {
            // No need to repeat the pattern
            buf.append("[\\.\\w]+"); // match one or more dots or word characters
          }
          lastIsStarStar = true;  
          continue;
        }
        else if (seg.indexOf('*') >= 0) {
          processToken(buf, seg);
        }
        else {
          buf.append('.');
          buf.append(seg);
        }
        lastIsStarStar = false;
      }
      try {
        return Pattern.compile(buf.toString());    
      } catch (PatternSyntaxException e) {
        LOG.severe(e.getDescription()); 
        return null;
      }
    }
    
    private static void processToken(StringBuilder sb, String seg) {
      int first = seg.indexOf('*');
      if (first < 0) {
        // no wildcards
        sb.append(seg);
        return;
      }
      int size = seg.length();
      int last = seg.lastIndexOf('*');      
      if (first == last) {
        // only 1 star 
        sb.append(seg, 0, first);
        sb.append("\\w*");
        sb.append(seg, first+1, size);
        return;
      }
      // multiple stars
      StringTokenizer st = new StringTokenizer(seg, "*");      
      sb.append(st.nextToken());
      while (st.hasMoreTokens()) {
        sb.append("\\w*");
        sb.append(st.nextToken());
      }
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.target.ITargetMatcher#match(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public boolean match(final IRNode decl) {
      IRNode here = decl;

      if ("".equals(type)) {
        LOG.fine("No type qualifier at all -- matches anything");
        return true;
      }
      if (!"*".equals(type)) {
        final IRNode enclosingT = useEnclosing ? VisitUtil.getEnclosingType(decl) : decl;
        final String tName      = JJNode.getInfo(enclosingT);
        if (LOG.isLoggable(Level.FINE)) {
          LOG.fine("No wildcard -- need to match '"+type+"' to decl's: "+tName);      
        }
        Matcher m = typePattern.matcher(tName);
        if (!m.matches()) {
          return false;
        }
        here = enclosingT;
      }
      // otherwise: wildcard matches anything
      
      if ("".equals(pkg) || "*".equals(pkg)) {
        LOG.fine("No package qualifier -- matches any package");
        return true;
      }
      final IRNode compUnit = OpSearch.cuSearch.findEnclosing(here);
      final IRNode pkg = CompilationUnit.getPkg(compUnit);
      if (NamedPackageDeclaration.prototype.includes(jtree.getOperator(pkg))) {
        final String pkgName = NamedPackageDeclaration.getId(pkg);

        Matcher m = pkgPattern.matcher(pkgName);
        return m.matches();
      }
      return false;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.target.ITargetMatcher#match(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.tree.Operator)
     */
    public boolean match(IRNode decl, Operator op) {
      return match(decl);
    }
    
    @Override
    public boolean match(String name) {
      if ("".equals(type)) {
        LOG.fine("No type qualifier at all -- matches anything");
        return true;
      } 
      String pkg, type;
      int dot = name.indexOf('.');
      if (dot < 0) {
        pkg  = "";
        type = name;
      } else {
        pkg  = name.substring(0, dot-1);
        type = name.substring(dot+1);
      }
      return match(pkg, type);
    } 
    
    /**
     * Simplified from the original match()
     */
    private boolean match(final String pkgName, final String tName) {
      /*
      if ("".equals(type)) {
        LOG.fine("No type qualifier at all -- matches anything");
        return true;
      }
      */
      if (!"*".equals(type)) {
        if (LOG.isLoggable(Level.FINE)) {
          LOG.fine("No wildcard -- need to match '"+type+"' to decl's: "+tName);      
        }
        Matcher m = typePattern.matcher(tName);
        if (!m.matches()) {
          return false;
        }
      }
      // otherwise: wildcard matches anything
      
      if ("".equals(pkg) || "*".equals(pkg)) {
        LOG.fine("No package qualifier -- matches any package");
        return true;
      }
      Matcher m = pkgPattern.matcher(pkgName);
      return m.matches();
    }
  }
  
  static boolean matchesConstructor(IRNode decl, IRNode target, TypeQualifierMatcher type, IRNode params, IRNode throwsC) {
    return 
    type.match(decl) &&
    matchesModifiers(decl, target) &&    
    matchesSignature(ConstructorDeclaration.getParams(decl), params) &&
    matchesThrows(ConstructorDeclaration.getExceptions(decl), throwsC); 
  }
  
  static boolean matchesModifiers(IRNode decl, IRNode pattern) {  
    final int pMods = JavaNode.getModifiers(pattern);
    if (pMods == JavaNode.ALL_FALSE) {
      return true;
    }
    boolean rv = true;
    
    final int dMods = JavaNode.getModifiers(decl);    
    if (JavaNode.isSet(pMods, JavaNode.STATIC)) {
      // check if static
      rv = rv && JavaNode.isSet(dMods, JavaNode.STATIC);
    }
    if (JavaNode.isSet(pMods, JavaNode.INSTANCE)) {
      // check if instance
      rv = rv && !JavaNode.isSet(dMods, JavaNode.STATIC);
    }
    if (JavaNode.isSet(pMods, JavaNode.PUBLIC)) {
      // check if public
      rv = rv && JavaNode.isSet(dMods, JavaNode.PUBLIC);
    }    
    if (JavaNode.isSet(pMods, JavaNode.PROTECTED)) {
      // check if public
      rv = rv && JavaNode.isSet(dMods, JavaNode.PROTECTED);
    }    
    if (JavaNode.isSet(pMods, JavaNode.PRIVATE)) {
      // check if public
      rv = rv && JavaNode.isSet(dMods, JavaNode.PRIVATE);
    }   
    return rv;
  }

  static TypeQualifierMatcher compileTypeQualifier(IRNode typeQ) {
    final String type = TypeQualifierPattern.getType(typeQ);
    final String pkgName = TypeQualifierPattern.getPkg(typeQ);
    return new TypeQualifierMatcher(pkgName, type, true);
  }

  static TypeQualifierMatcher compileTypeDecl(IRNode typeQ) {
    final String type = TypeDeclPattern.getType(typeQ);
    final String pkgName = TypeDeclPattern.getPkg(typeQ);
    return new TypeQualifierMatcher(pkgName, type, false);
  }

  static boolean matchesName(final String decl, final Pattern name) {
    Matcher m = name.matcher(decl);
    return m.matches();   
  }
  
  static boolean matchesType(IRNode declT, IRNode patternT) {
    final Operator pop = jtree.getOperator(patternT);
    final Operator dop = jtree.getOperator(declT);
    if (pop instanceof PrimitiveType) {
      return pop == dop;
    }
    // TODO do real pattern matching?
    else if (pop instanceof NamedType) {
      final String pName = NamedType.getType(patternT);
      if ("".equals(pName) || "*".equals(pName)) {
        return true; // TODO Even if array?
      }       
      //FIX if (!NamedType.prototype.includes(dop)) {
      if (!(dop instanceof NamedType)) {
        return false;
      }
      final String dName = NamedType.getType(declT);
      if (pName.indexOf("*") < 0) {
        // No wildcards
        return pName.equals(dName); // FIX inner class?
      } else {
        // TODO make regex matching more efficient
        final String pattern2 = pName.replaceAll("\\*", ".*");
        return Pattern.matches(pattern2, dName);
      }
    }
    // TODO How to deal with the fact that Object[] could match higher-dim arrays?
    else if (ArrayType.prototype.includes(pop)) {
      boolean v1 = ArrayType.prototype.includes(dop);
      boolean v2 =
        ArrayType.getDims(declT) == ArrayType.getDims(patternT);
      
      //  matchesType(ArrayType.getBase(declT), ArrayType.getBase(patternT));
      IRNode b1 = ArrayType.getBase(declT);
      IRNode b2 = ArrayType.getBase(patternT);
      return v1 && v2 && matchesType(b1, b2);
    }
    return false;
  } 

  static boolean matchesSignature(IRNode formals, IRNode patterns) {  
    if ("**".equals(JJNode.getInfo(patterns))) {
      // Marked with ** pattern
      return true; 
    }
    final Iterator pEnum = MethodSigPattern.getTypeIterator(patterns);
    final Iterator fEnum = Parameters.getFormalIterator(formals);
    while (fEnum.hasNext()) {
      if (!pEnum.hasNext()) {
        return false; // more formals than patterns
      }
      IRNode formal  = (IRNode) fEnum.next();
      IRNode pattern = (IRNode) pEnum.next();
      if (!matchesType(ParameterDeclaration.getType(formal), pattern)) {
        return false;
        
      }
    }
    // fEnum should be done, so pEnum should be done also
    return !pEnum.hasNext();
  }

  static boolean matchesThrows(IRNode throwsC, IRNode patterns) {  
    Operator pop = jtree.getOperator(patterns);
    if (NoThrowsPattern.prototype.includes(pop)) {
      return true; // matches anything
    }
    else if (EmptyThrowsPattern.prototype.includes(pop)) {
      // Should be empty
      return !Throws.getTypeIterator(throwsC).hasNext(); 
    }
    else if (ThrowsPattern.prototype.includes(pop)){
      final Iterator pEnum = ThrowsPattern.getTypeIterator(patterns);
      final Iterator tEnum = Throws.getTypeIterator(throwsC);
      while (tEnum.hasNext()) {
        if (!pEnum.hasNext()) {
          return false; // more types than patterns
        }
        IRNode type    = (IRNode) tEnum.next();
        IRNode pattern = (IRNode) pEnum.next();
        if (!matchesType(type, pattern)) {
          return false;
        }
      }
      // tEnum should be done, so pEnum should be done also
      return !pEnum.hasNext();
    }
    // TODO what about wildcard?
    LOG.warning("Unknown ThrowsPattern: "+DebugUnparser.toString(patterns));
    return false;
  }
  
}
