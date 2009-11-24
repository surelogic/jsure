/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/parse/AnnotationVisitor.java,v 1.53 2008/11/03 16:03:48 chance Exp $*/
package com.surelogic.annotation.parse;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.runtime.*;

import com.surelogic.aast.*;
import com.surelogic.annotation.*;
import com.surelogic.annotation.rules.*;
import com.surelogic.annotation.test.*;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.comment.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Iteratable;

public class AnnotationVisitor extends Visitor<Void> {
  static final Logger LOG = SLLogger.getLogger("sl.annotation.parse");
  private static final String promisePrefix = "com.surelogic.";
  private static final String jcipPrefix = "net.jcip.annotations.";
  
  public static final boolean allowJavadoc = true;
  public static final boolean onlyUseAnnotate = true;

  final boolean inEclipse = IDE.getInstance().getClass().getName().contains("Eclipse");
  final ITypeEnvironment tEnv;
  final String name;
  TestResult nextResult = null;
  boolean clearResult = true;
  
  public AnnotationVisitor(ITypeEnvironment te, String label) {
    tEnv = te;  
    name = label;
  }
  
  @Override
  public Void visit(IRNode node) { 
    doAcceptForChildren(node);
    return null;
  }
  
  /**
   * @return The simple name of the SL annotation (capitalized)
   */
  private String mapToPromiseName(IRNode anno) {
	String id;
	if (inEclipse) {
		// Already fully qualified
		id = Annotation.getId(anno);
	} else {
		IJavaDeclaredType type = (IJavaDeclaredType) tEnv.getBinder().getJavaType(anno);
		id = JavaNames.getQualifiedTypeName(type); 
	}
    if (id.startsWith(promisePrefix) || (id.startsWith(jcipPrefix) && 
    		                             (id.endsWith("ThreadSafe") || id.endsWith(".Immutable")))) {
      int lastDot = id.lastIndexOf('.');      
      return id.substring(lastDot+1);
    }
    if (!id.equals("java.lang.Deprecated")) {
      // FIX currently ignoring other annotations
//      System.out.println("Ignoring "+id);
    }
    return null;
  }
  
  // FIX needs more info about where the contents are coming from
  private boolean createPromise(IRNode node, String promise, String c, AnnotationSource src, int offset) { 
	  /* Bad things happen if contents is null */
	  String contents = (c == null) ? "" : c;
	 
//	  System.out.println("Got "+promise+" : "+contents);
    TestResult.setPromise(nextResult, promise, contents);
    
    IAnnotationParseRule<?,?> r = PromiseFramework.getInstance().getParseDropRule(promise);
    try {
      if (r != null) {
        Context context = new Context(src, node, r, contents, offset);
        r.parse(context, contents);
        return context.createdAAST();
      } else {
        // FIX throw new Error("No rule for "+promise);
//        System.out.println("No rule for "+promise);
      }
    } catch(Exception e) {
      if (e instanceof RecognitionException) {
        System.err.println(e.getMessage());
      } else {
        LOG.log(Level.WARNING, "Unable to create promise", e);
      }
    } finally {
    	if (clearResult) {
    		//System.out.println("Clearing result");
    		nextResult = null;
    	} else {
    		clearResult = true;
    	}
    }
    return false;
  }
  
  class Context extends SimpleAnnotationParsingContext {
    Context(AnnotationSource src, IRNode n, IAnnotationParseRule<?,?> r, String text, int offset) {    
      super(src, n, r, text, offset);
    }
    @Override
    protected String getName() {
      return name;
    }
    @Override
    public TestResult getTestResult() {
      return nextResult;
    }
    @Override
    public void setTestResultForUpcomingPromise(TestResult r) {
      if (r == null) {
    	clearTestResult();
    	return;
      }
      /*
      if (nextResult == r) {
    	  System.out.println("Same TestResult");
      }
      System.out.println("Set to "+r.hashCode());
      */
      nextResult = r;
      clearResult = false;
    }
    @Override
    public void clearTestResult() {
      /*
      if (nextResult != null) {
    	  System.out.println("Cleared "+nextResult.hashCode());
      } else {
    	  System.out.println("Already cleared");
      }
      */
      nextResult = null;
      clearResult = true;
    }
		/* (non-Javadoc)
		 * @see com.surelogic.annotation.SimpleAnnotationParsingContext#postAASTCreate(com.surelogic.aast.AASTRootNode)
		 */
		@Override
		protected void postAASTCreate(AASTRootNode root) {
			//Nothing to do
		}
  }
  
  private void checkForTestResult(IRNode node) {
    String result = JavaNode.getComment(node);
    if (result != null && result != "" && result.startsWith("/*")) { // /**/ minimum
      // trim off the ending */
      result = result.substring(0, result.length() - 2);
      createPromise(node, TestRules.TEST_RESULT, result, AnnotationSource.JAVA_5, -1);
    }
  }
  
  @Override
  public Void visitMarkerAnnotation(IRNode node) {
    String promise = mapToPromiseName(node);
    if (promise != null) {
      handleJava5Promise(node, promise, "");
    }
    return null;
  }

  @Override
  public Void visitSingleElementAnnotation(IRNode node) {
    String promise = mapToPromiseName(node);
    if (promise == null) {
      // FIX ignoring other annos    
      return null;
    }
    boolean plural = promise.endsWith("s");
    IRNode value   = SingleElementAnnotation.getElt(node);
    Operator op    = JJNode.tree.getOperator(value);
    
    if (Initializer.prototype.includes(op)) {     
      /* Not true for @starts
      if (plural) {
        throw new Error(promise+" doesn't contains Annotations: "+DebugUnparser.toString(value));
      }
      */
      // Should be a String?
      if (ArrayInitializer.prototype.includes(op)) { 
        Iteratable<IRNode> it = ArrayInitializer.getInitIterator(value);
        if (it.hasNext()) {
          for(IRNode v : it) {          
            handleJava5Promise(node, v, promise, StringLiteral.getToken(v));
          }
        } else {
          handleJava5Promise(node, value, promise, "");
        }
      }
      else if (StringLiteral.prototype.includes(op)) { 
        handleJava5Promise(node, value, promise, StringLiteral.getToken(value));
      }
      else throw new IllegalArgumentException("Unexpected value: "+op.name());
    } 
    else {
      if (!plural) {
        throw new Error(promise+" contains Annotations: "+DebugUnparser.toString(value));
      }
      if (ElementValueArrayInitializer.prototype.includes(op)) { 
        doAcceptForChildren(value);
      }
      else if (Annotation.prototype.includes(op)) { 
        doAccept(value);
      }
      else throw new IllegalArgumentException("Unexpected value: "+op.name());
    }
    return null;
  }
  
  @Override
  public Void visitNormalAnnotation(IRNode node) {
    String promise = mapToPromiseName(node);
    if (promise != null) {
      // We should never have any of these
      // but we might want to convert other ppl's into ours
      
      // Assume that we only car
      IRNode pairsNode         = NormalAnnotation.getPairs(node);
      Iteratable<IRNode> pairs = ElementValuePairs.getPairIterator(pairsNode);
      if (pairs.hasNext()) {
        for(IRNode valuePair : pairs) {
          if ("value".equals(ElementValuePair.getId(valuePair))) {
            IRNode value = ElementValuePair.getValue(valuePair);
            if (StringLiteral.prototype.includes(value)) {
              handleJava5Promise(node, value, promise, StringLiteral.getToken(value));
              return null;
            }
          }
        }
      } else {
        handleJava5Promise(node, promise, "");
        return null;
      }
      throw new Error("A NormalAnnotation in a SL package?!?");
    }
    return null;
  }
  
  @Override
  public Void visitAnnotation(IRNode node) {
    throw new Error("Unknown Annotation type: "+JJNode.tree.getOperator(node).name());
  }
  
  @Override
  public Void visitFieldDeclaration(IRNode node) {
    ISrcRef ref = JavaNode.getSrcRef(node);
    if (ref == null) {
      return super.visitVariableDeclList(node);
    }
    checkForJavadoc(node, ref);
    return super.visitVariableDeclList(node);
  }
  
  @Override
  public Void visitDeclaration(IRNode node) {
    ISrcRef ref = JavaNode.getSrcRef(node);
    if (ref == null) {
      return super.visitDeclaration(node);
    }
    checkForJavadoc(node, ref);
    return super.visitDeclaration(node);
  }

  private void checkForJavadoc(IRNode node, ISrcRef ref) {
	if (!allowJavadoc) {
		return;
	}
    IJavadocElement elt = ref.getJavadoc();
    if (elt != null) {
      for(Object o : elt) {
        if (o instanceof IJavadocTag) {
          handleJavadocTag(node, (IJavadocTag) o);
        }
      }
      ref.clearJavadoc();
    }
  }

  private void handleJavadocTag(IRNode decl, IJavadocTag tag) {
    if (tag.getTag() == null) {
      return; // Leading text
    }
    if (onlyUseAnnotate && !"annotate".equals(tag.getTag())) {
    	return; // ignore other tags
    }
    String contents = null;
    for(Object o : tag) {
      if (o instanceof String) {
        if (contents != null) {
          LOG.fine("New contents: "+o);
        }
        contents = o.toString();
      }
      else if (o instanceof IJavadocTag) {
        handleJavadocTag(decl, (IJavadocTag) o);
      } else {
        System.out.println("Unknown: "+o);
      }
    }    
    if (onlyUseAnnotate) {
    	handleJavadocPromise(decl, contents, tag.getOffset());
    } else { 
    	createPromise(decl, capitalize(tag.getTag()), contents, 
    			      AnnotationSource.JAVADOC, tag.getOffset());
    }
  }

  public static String capitalize(String tag) {
    if (tag.length() <= 0) {
      return tag;
    }
    char first = tag.charAt(0);
    if (Character.isLowerCase(first)) {
      return Character.toUpperCase(first) + tag.substring(1);
    }
    return tag;
  }
  
  public boolean handleJava5Promise(IRNode node, String promise, String c) {
    return handleJava5Promise(node, node, promise, c);
  }
  
  public boolean handleJava5Promise(IRNode anno, IRNode here, String promise, String c) {
    checkForTestResult(anno);
    
    ISrcRef src = JavaNode.getSrcRef(here);
    int offset  = src == null ? 0 : src.getOffset();  
    return createPromise(here, promise, c, AnnotationSource.JAVA_5, offset);
  }
  
  public boolean handleXMLPromise(IRNode node, String promise, String c) {
    return createPromise(node, capitalize(promise), c, AnnotationSource.XML, 
                         Integer.MAX_VALUE);
  }
  
  /**
   * Assumes that text looks like Foo("...")
   */
  private boolean handleJavadocPromise(IRNode decl, String text, int offset) {
	  // Test result?
	  final int startContents = text.indexOf("(\"");
	  final int endContents = text.lastIndexOf("\")");
	  if (startContents < 0 || endContents < 0) {		  
		  SimpleAnnotationParsingContext.reportError(decl, offset, "Syntax not matching Foo(\"...\"): "+text);
		  return false;
	  }
	  // Check if the rest is whitespace
	  for(int i=endContents+2; i<text.length(); i++) {
		  if (!Character.isWhitespace(text.charAt(i))) {
			  SimpleAnnotationParsingContext.reportError(decl, offset, 
					  "Non-whitespace after annotation: "+text);
			  return false;
		  }
	  }
	  final String tag = text.substring(0, startContents).trim();
	  final String contents = text.substring(startContents+2, endContents);
	  return createPromise(decl, tag, contents, AnnotationSource.JAVADOC, offset);
  }
}
