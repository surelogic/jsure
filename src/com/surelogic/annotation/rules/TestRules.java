/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/TestRules.java,v 1.4 2008/07/07 20:51:26 chance Exp $*/
package com.surelogic.annotation.rules;

import java.util.StringTokenizer;

import org.antlr.runtime.RecognitionException;

import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.*;
import com.surelogic.annotation.test.*;

import edu.cmu.cs.fluid.java.bind.*;


public class TestRules extends AnnotationRules {
  public static final String TEST_RESULT = "TestResult";
  
  private static final AnnotationRules instance = new TestRules();
  
  private static final TestResult_ParseRule testResultRule = new TestResult_ParseRule();
  
  public static AnnotationRules getInstance() {
    return instance;
  }
  
  @Override
  public void register(PromiseFramework fw) {
    fw.registerParseDropRule(testResultRule);
  }
  
  static class TestResult_ParseRule extends NullAnnotationParseRule {
    public TestResult_ParseRule() {
      super(TEST_RESULT);
    }
    @Override
    public ParseResult parse(IAnnotationParsingContext context, String contents) {
      try {
        ITestAnnotationParsingContext testContext = (ITestAnnotationParsingContext) context;
        AASTAdaptor.Node tn = parse(context, SLParse.prototype.initParser(contents));
        if (tn == null) {
          return ParseResult.FAIL;
        }
        final int end       = tn.getTokenStopIndex();
        String explanation  = ':' == contents.charAt(end) ? contents.substring(end+1) : null;
        StringTokenizer s   = new StringTokenizer(tn.finalizeId(), "&:");
        String type         = s.nextToken();
        TestResultType rt;
        try {
        	rt = TestResultType.valueOf(type);
        } catch (IllegalArgumentException e) {
        	rt = null;
        }
        if (rt == null) {
          context.reportError(0, "Unknown test result type: "+type);
          return ParseResult.FAIL;
        }        
        if (s.hasMoreTokens()) {
          testContext.setTestResultForUpcomingPromise(rt, s.nextToken(), explanation);
        } else {
          testContext.setTestResultForUpcomingPromise(rt, explanation);
        }
      } catch (Exception e) {
        context.reportException(IAnnotationParsingContext.UNKNOWN, e);
        return ParseResult.FAIL;
      }
      return ParseResult.OK;
    }
    
    private AASTAdaptor.Node parse(IAnnotationParsingContext context, 
                                   SLAnnotationsParser parser) throws RecognitionException {
      if (context.getSourceType() == AnnotationSource.JAVA_5) {
        return (AASTAdaptor.Node) parser.testResultComment().getTree();
      }
      return (AASTAdaptor.Node) parser.testResult().getTree();
    }
  }
}
