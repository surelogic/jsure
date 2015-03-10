package edu.cmu.cs.fluid.control;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaOperator;
import edu.cmu.cs.fluid.java.operator.AddExpression;
import edu.cmu.cs.fluid.java.operator.AssignExpression;
import edu.cmu.cs.fluid.java.operator.BlockStatement;
import edu.cmu.cs.fluid.java.operator.BreakStatement;
import edu.cmu.cs.fluid.java.operator.ConstantLabel;
import edu.cmu.cs.fluid.java.operator.DefaultLabel;
import edu.cmu.cs.fluid.java.operator.DivExpression;
import edu.cmu.cs.fluid.java.operator.ExprStatement;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.IntLiteral;
import edu.cmu.cs.fluid.java.operator.ReturnStatement;
import edu.cmu.cs.fluid.java.operator.SwitchBlock;
import edu.cmu.cs.fluid.java.operator.SwitchElement;
import edu.cmu.cs.fluid.java.operator.SwitchStatement;
import edu.cmu.cs.fluid.java.operator.SwitchStatements;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;

public class TestBicomponent {

	public TestBicomponent() {
	}

	Map<String,Integer> removedClasses = new HashMap<String,Integer>();
	
	BiComponentFactory fact = new AbstractBiComponentFactory() {

		@Override
		public SyntaxTreeInterface tree() {
			return JJNode.tree;
		}

		@Override
		protected Component getUnregisteredComponent(IRNode node, boolean quiet) {
			JavaOperator op = (JavaOperator)JJNode.tree.getOperator(node);
			Component c = op.createComponent(node);
			// c.registerFactory(this);
			return c;
		}
		
		@Override
		public void noteRemoval(Object x) {
			if (x != null) {
				String key = x.getClass().getName();
				Integer old = removedClasses.get(key);
				if (old == null) old = 0;
				removedClasses.put(key, ++old);
			}
		}
	};
	
	public void run() {
		JavaNode test = createSwitch(); // createBlock1();
		
		BiComponent bic = fact.getBiComponent(test, false);
		
		System.out.println("------------- BiC created --------------");
		DumpCFG dump = new DumpCFG(bic);
		dump.dump();
		System.out.println("------------- Objects removed -------------");
		int total = 0;
		for (Map.Entry<String, Integer> e : removedClasses.entrySet()) {
			total += e.getValue();
			System.out.format("%8d %s\n",e.getValue(),e.getKey());
		}
		System.out.format("\n%8d Total\n", total);
	}

	JavaNode var(String n) {
		return VariableUseExpression.createNode(n); 
	}
	
	JavaNode add(IRNode n1, IRNode n2) {
		return AddExpression.createNode(n1, n2);
	}

	JavaNode num(String n) {
		return IntLiteral.createNode(n);
	}
	
	public JavaNode createBlock1() {
		JavaNode xplusy = add(var("x"),var("y"));
		JavaNode zassign = AssignExpression.createNode(var("z"), xplusy);
		JavaNode aNode = ExprStatement.createNode(zassign);
		JavaNode adivb = DivExpression.createNode(num("4"), FieldRef.createNode(var("b"), "f"));
		JavaNode rNode = ReturnStatement.createNode(adivb);
		JavaNode block = BlockStatement.createNode(new IRNode[]{aNode,rNode});
		JavaNode test = block;
		JavaNode.tree.clearParent(test);
		return test;
	}
	
	public JavaNode createSwitch() {
		JavaNode block = createBlock1();
		JavaNode stmts = SwitchStatements.createNode(new IRNode[]{block});
		JavaNode sw1 = SwitchElement.createNode(ConstantLabel.createNode(num("13")), stmts);
		IRNode bNode = BreakStatement.prototype.createNode();
		JavaNode stmts2 = SwitchStatements.createNode(new IRNode[]{bNode});
		JavaNode sw2 = SwitchElement.createNode(DefaultLabel.prototype.createNode(), stmts2);
		JavaNode sb = SwitchBlock.createNode(new IRNode[]{sw1,sw2});
		JavaNode se = FieldRef.createNode(var("r"), "g");
		JavaNode ss = SwitchStatement.createNode(se, sb);
		JavaNode test = ss;
		JavaNode.tree.clearParent(test);
		return test;		
	}
	
	public static void main(String[] args) {
		new TestBicomponent().run();
	}
}
