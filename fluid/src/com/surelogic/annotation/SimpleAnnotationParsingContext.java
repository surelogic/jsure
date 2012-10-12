/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/SimpleAnnotationParsingContext.java,v 1.5 2008/06/24 19:13:15 thallora Exp $*/
package com.surelogic.annotation;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.RewriteCardinalityException;

import com.surelogic.aast.*;
import com.surelogic.annotation.scrub.AASTStore;
import com.surelogic.annotation.test.TestResult;
import com.surelogic.annotation.test.TestResultType;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.ModelingProblemDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public abstract class SimpleAnnotationParsingContext extends AbstractAnnotationParsingContext {
  static final Logger LOG = SLLogger.getLogger("sl.annotation");
  
  /**
   * The annotation being parsed
   */
  final IRNode annoNode;
  final IAnnotationParseRule<?,?> rule;
  final String contents;
  final IRNode contextRef;
  
  /**
   * @param offset The offset in the source text to the location of the IRNode
   */
  protected SimpleAnnotationParsingContext(AnnotationSource src, IRNode n, 
                                           IAnnotationParseRule<?,?> r, String text, IRNode ref) {    
    super(src);
    annoNode = n;
    rule = r;
    contents = text;  
    contextRef = ref;
  }
  @Override
  protected final IRNode getAnnoNode() {
    return annoNode;
  }
  
  public IAnnotationParseRule<?,?> getRule() {
	  return rule;
  }
  
  private ISrcRef getSrcRef() {
	ISrcRef ref = JavaNode.getSrcRef(contextRef);
	if (ref == null) {
		ref = JavaNode.getSrcRef(annoNode);
	}
	return ref;
  }
  
  private int getOffset() {
	ISrcRef ref = getSrcRef();
	return ref == null ? -1 : ref.getOffset();
  }
  
  @Override
  public int mapToSource(int relativeOffset) {
	final int offset = getOffset();
	if (getSourceType() == AnnotationSource.JAVA_5 && offset >= 0 && relativeOffset >= 0) {		
		return offset + relativeOffset;
	}
    return offset;
  }
  
  @Override
  public String getSelectedText(int start, int stop) {
	return contents.substring(start, stop);
  }
  
  @Override
  public String getAllText() {
	return contents;
  }
  
  protected abstract String getName();
  
  /**
   * Abstract method that is called after an AAST is created
   * @param root The AASTRootNode that was created
   */
  protected abstract void postAASTCreate(final AASTRootNode root);
  
  public <T extends IAASTRootNode> 
  void reportAAST(int offset, AnnotationLocation loc, Object o, T ast) {
    if (ast == null) {
      throw new IllegalArgumentException("Null ast");
    }
    // Check for qualified receivers
    if (loc == AnnotationLocation.QUALIFIED_RECEIVER) {
    	final String context = (String) o;
    	final IRNode referencedType = findEnclosingType(context);
    	if (referencedType == null) {
    		reportError(offset, "Cannot find type referenced in qualified receiver: "+context);
    		return;
    	}
    	IRNode closestType = VisitUtil.getClosestType(annoNode);
    	if (referencedType != closestType && TypeUtil.isStatic(closestType)) {
    		reportError(offset, 
    				"Cannot refer to qualified receiver from static inner type "+
    				JavaNames.getRelativeTypeName(closestType));
    		return;
    	}
    	IRNode nextEnclosingType = VisitUtil.getEnclosingType(closestType);    	
    	if (referencedType != nextEnclosingType) {
    		reportError(offset, "Cannot reference the qualified receiver for "+context+" from here");
    		return;
    	}
    	o = referencedType;
    }
        
    // In some cases, we need the results of the parse to tell us 
    // what this node should be
    TestResult result = getTestResult();     
    boolean first = true;
    for(IRNode declNode : computeDeclNode(annoNode, loc, o)) {
      AASTRootNode root;
      TestResult tr;
      
      if (first) {
        first = false;
        root  = (AASTRootNode) ast;
        tr    = result;
      } else {
        root  = (AASTRootNode) ast.cloneTree();      
        tr    = result == null ? null : result.cloneResult();
      }
      root.setSrcType(getSourceType());
      // CHANGED from pointing at the promise node
      // FIX drop.setNodeAndCompilationUnitDependency(declNode); 
      if (declNode == null) {
    	final IRNode decl = VisitUtil.getEnclosingDecl(annoNode);
    	final Operator op = JJNode.tree.getOperator(decl);
    	final boolean isFunction = SomeFunctionDeclaration.prototype.includes(op);
    	final String msg;
    	switch (loc) {
    	case RECEIVER:
    		if (isFunction) {
    			if (TypeUtil.isStatic(decl)) {
    				msg = "Cannot use @"+rule.name()+"("+o+") on a static method, since it does not have a receiver";
    				break;
    			}
    		}
    		msg = "No receiver on this "+getUnit(op)+" to annotate with @"+rule.name();    		
			break;
    	case RETURN_VAL:    
    		if (isFunction && MethodDeclaration.prototype.includes(op)) {
    			IRNode rtype = MethodDeclaration.getReturnType(decl);
    			if (VoidType.prototype.includes(rtype)) {
    				msg = "Cannot use @"+rule.name()+"("+o+") on a void method, since it does not have a return value";
    				break;
    			}
    		}
    		msg = "No return value on this "+getUnit(op)+" to annotate with @"+rule.name();   
    		break;
    	default:
    		msg = "Couldn't find '"+o+"' to annotate with @"+rule.name();
    	}
    	reportError(offset, msg);
        root.markAsUnbound();      
      } else {        
        root.setPromisedFor(declNode, contextRef == null ? annoNode : contextRef);
        
        //final PromiseFramework pw = PromiseFramework.getInstance();
        //pw.addSlotValue(rule, declNode, drop);
        TestResult.addAAST(tr, root);
         
        AASTStore.add(root);
        AASTStore.associateTestResult(root, tr);
        createdAAST = true;
        postAASTCreate(root);
      }
    }
    clearTestResult();
  }  
  
  private IRNode findEnclosingType(String pattern) {
	final boolean isQualified = pattern.indexOf('.') >= 0;
	IRNode here = annoNode;
	IRNode type;
	String typeName;
	do {
		type = VisitUtil.getEnclosingType(here);
		if (isQualified) {
			typeName = JavaNames.getFullTypeName(type);
		} else {
			typeName = JavaNames.getTypeName(type);
		}
		here = type;
	} 
	while (here != null && !typeName.equals(pattern));
	
	return type;
  }
  
  public static String getUnit(Operator op) {
	  if (ConstructorDeclaration.prototype.includes(op)) {
		  return "constructor";
	  }
	  else if (MethodDeclaration.prototype.includes(op)) {
		  return "method";
	  }
	  else if (FieldDeclaration.prototype.includes(op)) {
		  return "field";
	  }
	  else if (TypeDeclaration.prototype.includes(op)) {
		  return "type";		  
	  }	  
	  return "declaration";
  }
  
  public static void reportError(IRNode node, String txt) {	  
	  reportError(node, UNKNOWN, txt);
  }
  
  public static void reportError(IRNode node, int offset, String txt) {	    
	  ModelingProblemDrop d = new ModelingProblemDrop(node, offset);
	  d.setMessage(txt);
  }
  
  public void reportError(int offset, String msg) {
    TestResult.checkIfMatchesResult(getTestResult(), TestResultType.UNPARSEABLE);
    
	final int position = mapToSource(offset);
    reportError(contextRef, position, msg);
    hadProblem = true;
  }

  public void reportException(int offset, Exception e) {
    TestResult.checkIfMatchesResult(getTestResult(), TestResultType.UNPARSEABLE);
    
	final int position = mapToSource(offset);
	final String txt;
	if (e instanceof RecognitionException ||
	    e instanceof RewriteCardinalityException) {
		txt = e.toString();
		LOG.warning(txt);
	} else {
		LOG.log(Level.SEVERE, "Unexpected problem while parsing promise", e);
		txt = "Unexpected problem while parsing promise: "+e.getMessage();
	}
	reportError(contextRef, position, txt);


    hadProblem = true;
  }

  public Operator getOp() {
    IRNode decl = computeDeclNode(annoNode);
    return JJNode.tree.getOperator(decl);
  }  
  
  @Override
  public TestResult getTestResult() {
    return null;
  }
  @Override
  public void setTestResultForUpcomingPromise(TestResult r) {
    // nothing to do
  }
  @Override
  public void clearTestResult() {
    // nothing to do
  }
}
