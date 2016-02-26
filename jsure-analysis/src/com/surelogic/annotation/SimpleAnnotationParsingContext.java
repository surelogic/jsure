/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/SimpleAnnotationParsingContext.java,v 1.5 2008/06/24 19:13:15 thallora Exp $*/
package com.surelogic.annotation;

import java.lang.annotation.Annotation;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.RewriteCardinalityException;

import com.surelogic.aast.AASTRootNode;
import com.surelogic.aast.AnnotationOrigin;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.scrub.AASTStore;
import com.surelogic.annotation.test.TestResult;
import com.surelogic.annotation.test.TestResultType;
import com.surelogic.common.XUtil;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.IProposedPromiseDrop.Origin;
import com.surelogic.dropsea.ir.ModelingProblemDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.VoidType;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
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
  protected SimpleAnnotationParsingContext(IBinder b, AnnotationSource src, AnnotationOrigin origin, IRNode n, 
                                           IAnnotationParseRule<?,?> r, String text, IRNode ref) {    
    super(b, src, origin);
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
  
  private IJavaRef getJavaRef() {
    IJavaRef ref = JavaNode.getJavaRef(contextRef);
    if (ref == null) {
      ref = JavaNode.getJavaRef(annoNode);
    }
    return ref;
  }
  
  private int getOffset() {
    IJavaRef ref = getJavaRef();
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
  
  @Override
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
      root.setOrigin(getOrigin());
      
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
	  makeProblemDrop(node, offset).setMessage(txt);
  }
  
  public static ModelingProblemDrop makeProblemDrop(IRNode node, int offset) {
	  return new ModelingProblemDrop(node, offset);
  }
  
  private ModelingProblemDrop reportError(IRNode node, int offset) {
	   TestResult.checkIfMatchesResult(getTestResult(), TestResultType.UNPARSEABLE);
	   hadProblem = true;
	   return makeProblemDrop(node, offset);
  }
  
  private boolean ignoreIssue() {
	  if (XUtil.useExperimental) {
		  return false;
	  }
	  return JavaNode.isSet(getModifiers(), JavaNode.IMPLICIT);
  }
    
  private ModelingProblemDrop reportError(int offset) {
	  if (ignoreIssue()) {
		  return null;
	  }
	  final int position = mapToSource(offset);
	  return reportError(contextRef, position);
  }

  @Override
  public void reportErrorAndProposal(int offset, String msg, ProposedPromiseDrop.Builder proposal, String... moreInfo) {
	  final ModelingProblemDrop error = reportError(offset);
	  if (error != null) {
		  error.setMessage(msg);
		  if (proposal != null) {
			  proposal.forDrop(error);
			  proposal.setOrigin(Origin.PROBLEM);
			  proposal.build();
		  }
		  for(String i : moreInfo) {
			  error.addInformationHint(null, i);
		  }
	  }
  }
  
  @Override
  public void reportError(int offset, int number, Object... args) {
	  final ModelingProblemDrop error = reportError(offset);
	  if (error != null) {
		  error.setMessage(number, args);
	  }
  }
  
  @Override
  public ProposedPromiseDrop.Builder startProposal(Class<? extends Annotation> anno) {
	  IRNode decl = computeDeclNode(annoNode);
	  return new ProposedPromiseDrop.Builder(anno, decl, contextRef);
  }
  
  @Override
  public void reportException(int offset, Exception e) {  
	final int position = mapToSource(offset);
	final String txt;
	if (e instanceof RecognitionException ||
	    e instanceof RewriteCardinalityException) {
		txt = e.toString();
		LOG.warning(txt);
	} else {
		LOG.log(Level.SEVERE, "Unexpected problem while parsing promise", e);
		txt = "Unexpected problem while parsing promise: "+e.getClass()+" -- "+e.getMessage();
	}
	if (ignoreIssue()) {
		return;
	}	
	reportError(contextRef, position).setMessage(txt);
  }

  @Override
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
