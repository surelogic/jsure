package edu.cmu.cs.fluid.java.bind;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.java.JavaOperator;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.*;
import edu.cmu.cs.fluid.util.SimpleApp;
import edu.cmu.cs.fluid.version.Version;

/**
 */
public interface PromiseConstants {
  /**
	 * Logger for this class
	 */
  static final Logger BIND = SLLogger.getLogger("FLUID.bind");

  public static final SyntaxTreeInterface tree = JJNode.tree;

  public static final String REGION_ALL_NAME = "All";
  public static final String ROOT_REGION_NAME = REGION_ALL_NAME;
  public static final String REGION_INSTANCE_NAME = "Instance";
  public static final String ARRAY_CLASS_NAME = "[]";
  public static final String REGION_LENGTH_NAME = "length";
  public static final String ARRAY_CLASS_QNAME = "java.lang.[]";
  
  final Object logInit =
    SimpleApp.logInit(
      BIND,
      "PromiseConstants at version " + Version.getVersion());
  // new Throwable());

  /*
	 * public static final IRNode REGION_ALL =
	 * NewRegionDeclaration.createNode(JavaNode.STATIC, REGION_ALL_NAME, null);
	 * 
	 * public static final IRNode REGION_INSTANCE =
	 * NewRegionDeclaration.createNode(JavaNode.ALL_FALSE, REGION_INSTANCE_NAME,
	 * RegionName.createNode( REGION_ALL_NAME ) );
	 * 
	 * 
	 * final Object test = JJNode.tree.getChild(REGION_ALL, 0); final Object
	 * allInit = SimpleApp.logInit(BIND, "REGION_ALL is "+REGION_ALL);
	 * 
	 * public static final IRNode REGION_ELEMENT =
	 * NewRegionDeclaration.createNode( JavaNode.ALL_FALSE, REGION_ELEMENT_NAME,
	 * RegionName.createNode(REGION_INSTANCE_NAME));
	 */
  public static final Operator[] noOps = new Operator[0];
  
  public static final Operator[] declOps = {
    // AnonClassExpression.prototype,
    PackageDeclaration.prototype,
	TypeDeclaration.prototype, 
      MethodDeclaration.prototype,
      FieldDeclaration.prototype,
      };
  
  public static final Operator[] declOrConstructorOps = {
                                                         // AnonClassExpression.prototype,
                                                         PackageDeclaration.prototype,
                                                         TypeDeclaration.prototype, 
                                                         SomeFunctionDeclaration.prototype,
                                                         FieldDeclaration.prototype, };
  public static final Operator[] fieldFuncTypeOps = {
      TypeDeclaration.prototype, 
      SomeFunctionDeclaration.prototype,
      FieldDeclaration.prototype, };
  
  public static final Operator[] ptFuncOps = {
	  // AnonClassExpression.prototype,
      PackageDeclaration.prototype,
      TypeDeclaration.prototype, 
      SomeFunctionDeclaration.prototype,};

  public static final Operator[] typeDeclOps = {
    // AnonClassExpression.prototype,
	TypeDeclaration.prototype, 
  };

  public static final Operator[] typeFuncVarDeclOps = {
	  TypeDeclaration.prototype, 
	  SomeFunctionDeclaration.prototype,
	  FieldDeclaration.prototype,
	  ParameterDeclaration.prototype,
  };
  
	public static final Operator[] packageTypeDeclOps = {
		// AnonClassExpression.prototype,
		PackageDeclaration.prototype,
		TypeDeclaration.prototype,  };  
  
  public static final Operator[] methodDeclOp =
    { MethodDeclaration.prototype, };

  public static final Operator[] constructorOp =
    { ConstructorDeclaration.prototype, };

  public static final Operator[] functionDeclOps = {
    // AnonClassExpression.prototype,
    ClassInitDeclaration.prototype,
    SomeFunctionDeclaration.prototype, };

  public static final Operator[] methodDeclOps = {
	  // AnonClassExpression.prototype,
	  ClassInitDeclaration.prototype,
	  MethodDeclaration.prototype, };
  
  public static final Operator[] methodOrParamDeclOps = {
    // AnonClassExpression.prototype,
    ClassInitDeclaration.prototype,
    SomeFunctionDeclaration.prototype,
    ParameterDeclaration.prototype 
  };

  public static final Operator[] methodOrClassDeclOps = {
    // AnonClassExpression.prototype,
	SomeFunctionDeclaration.prototype,
	TypeDeclaration.prototype, 
  };
  
  public static final Operator[] fieldMethodDeclOps = {
    FieldDeclaration.prototype, 
    SomeFunctionDeclaration.prototype, };
  
  public static final Operator[] fieldParamDeclOps = {
    FieldDeclaration.prototype, ParameterDeclaration.prototype, };
  
  public static final Operator[] fieldMethodParamDeclOps = {
	    FieldDeclaration.prototype, ParameterDeclaration.prototype,
	    MethodDeclaration.prototype, };
  
  public static final Operator[] fieldFuncParamDeclOps = {
    FieldDeclaration.prototype, ParameterDeclaration.prototype,
    SomeFunctionDeclaration.prototype, };
  
  public static final Operator[] fieldMethodParamTypeDeclOps = {
    FieldDeclaration.prototype, ParameterDeclaration.prototype,
    SomeFunctionDeclaration.prototype, NestedTypeDeclaration.prototype };

  public static final Operator[] fieldMethodParamInnerTypeDeclOps = {
	    FieldDeclaration.prototype, ParameterDeclaration.prototype,
	    NestedTypeDeclaration.prototype, 
	    SomeFunctionDeclaration.prototype, };
  
  public static final Operator[] fieldDeclOp = { FieldDeclaration.prototype, /*VariableDeclarator.prototype*/ };

  public static final Operator[] fieldOrTypeOp = { 
    FieldDeclaration.prototype, 
	TypeDeclaration.prototype, 
  };
  
  public static final Operator[] statementOp = { Statement.prototype, };
  
  public static final Operator[] blockOps = { BlockStatement.prototype, };
  
  public static final Operator[] declOrStatementOps = {
		// AnonClassExpression.prototype,
		PackageDeclaration.prototype,
		TypeDeclaration.prototype, 
			MethodDeclaration.prototype,
			FieldDeclaration.prototype,
		    Statement.prototype,
  };
  
  public static final Operator[] methodDeclOrStatementOps = {
		// AnonClassExpression.prototype,
	  SomeFunctionDeclaration.prototype,
		    Statement.prototype,
  };

  public static final Operator[] packageOps = {
		PackageDeclaration.prototype,
  };
  
  public static final Operator[] statementOps =
    {
      AssertMessageStatement.prototype,
      AssertStatement.prototype,
      BlockStatement.prototype,
      BreakStatement.prototype,
      ContinueStatement.prototype,
      DeclStatement.prototype,
      DoStatement.prototype,
      EmptyStatement.prototype,
      ExprStatement.prototype,
      ForStatement.prototype,
      LabeledBreakStatement.prototype,
      LabeledContinueStatement.prototype,
      LabeledStatement.prototype,
      ReturnStatement.prototype,
      Statement.prototype,
      SwitchStatement.prototype,
      SwitchStatements.prototype,
      SynchronizedStatement.prototype,
      ThrowStatement.prototype,
      TryStatement.prototype,
      TypeDeclarationStatement.prototype,
      VoidReturnStatement.prototype,
      WhileStatement.prototype,
      };

  public static final Operator[] varDeclOps = {
  	VariableDeclaration.prototype, 
    ReceiverDeclaration.prototype,
  	ReturnValueDeclaration.prototype,
  };
  
	public static final Operator[] varDeclaratorOps = {
		VariableDeclarator.prototype, 
	};

	public static final Operator[] paramDeclOp = {
		ParameterDeclaration.prototype, 
	};

	public static final Operator[] returnValueOp = {
		ReturnValueDeclaration.prototype, 
	};	

	public static final Operator[] receiverOp = {
		ReceiverDeclaration.prototype, 
	};
	
	public static final Operator[] varDeclOrParamDeclOps = {
	    VariableDeclaration.prototype, 
	    ReceiverDeclaration.prototype,
	  	ReturnValueDeclaration.prototype,
	  	ParameterDeclaration.prototype,
	};
	
  public static final Operator[] anyOp = { JavaOperator.prototype };
}
