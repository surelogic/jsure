/*
 * Created on Jul 10, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.util.PartialMapLattice;
import edu.cmu.cs.fluid.util.Lattice;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.analysis.LocationGenerator.SimpleLocation;
import edu.cmu.cs.fluid.java.operator.*;

import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.java.JavaNode;

import java.util.HashMap;
/**
 * This lattice maps local variables and fields to their associated locations 
 * (@see edu.cmu.cs.fluid.SimpleLocation)
 * It also contains functionality to assign locations to arbitrary expressions.
 */
@Deprecated
public class LocationMap extends PartialMapLattice<Object,SimpleLocation> implements ILocationMap{

	protected IRNode methodDecl;


	protected final IBinder binder;
	protected final LocationGenerator gen;
  public final LocationMap betterTop;
  
	/** Create a new LocationMap lattice for a particular method. */
	public LocationMap(IRNode md,  IBinder binder,
				LocationGenerator gen) {
		super(gen.top());
		this.methodDecl = md;
		this.binder = binder;
		this.gen = gen;
	
		betterTop = (LocationMap)newLattice(new HashMap<Object,SimpleLocation>(),true);
	}

	protected LocationMap(IRNode md, IBinder binder,
				 HashMap<?,SimpleLocation> m, Lattice range,
				 PartialMapLattice top, PartialMapLattice bottom, boolean isTop, LocationGenerator gen) {
		super(range,m,top,bottom,isTop);
		this.methodDecl = md;
		this.binder = binder;
		this.gen = gen;
		betterTop = (top==null)?this:(LocationMap)top;
// 	assert(gen != null);
	}

	@Override
  protected PartialMapLattice<Object,SimpleLocation> newLattice(HashMap<?,SimpleLocation> newValues,boolean isTop) {
		return new LocationMap(methodDecl,binder,newValues,range,topVal,botVal,isTop,gen);
	}
   
	public ILocationMap replaceLocation(IRNode decl, SimpleLocation loc) {
		return (LocationMap)super.update(new LocationField(nulLoc(),decl),loc);
	}

	@Override
  public Lattice top(){
		return betterTop;
	}

	protected SimpleLocation getLocal(IRNode local){
			return get(new LocationField(nulLoc(),local));
	}

	protected IRNode getLocal(SimpleLocation loc){
		java.util.Iterator i = map.keySet().iterator();
		while(i.hasNext()){
			Object o = i.next();
			if(o instanceof IRNode && loc.equals(get(o))){
				return (IRNode)o;
			}
		}
		return null;
	}

	public IRNode getSource(SimpleLocation loc){
		if(map.containsValue(loc)){
			return getLocal(loc);
		}else if (loc == null){
			return null;
		}
		return gen.getSource(loc);
	}

    public ILocationMap assignPseudoLocation(LocationField lf){
      return assignPseudoLocation(lf,gen.ourAnalysis.getUserNode());
    }
  
	public ILocationMap assignPseudoLocation(LocationField lf, IRNode locale){
	  final SimpleLocation location = gen.getLocation(lf,locale);
    final ILocationMap lm = replaceLocation(lf.l,lf.fdecl,location);
/*    System.out.println("Assigning " + lf + " the value "+ location 
        + " at " + DebugUnparser.toString(locale));
*/    return lm;
	}
	
	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.ILocationMap#renameLocation(edu.cmu.cs.fluid.java.analysis.LocationField)
	 */
	public ILocationMap renameLocation(LocationField lf) {
		if(lf == null){ return this;}
        SimpleLocation l1 = /*(containsKey(lf))?*/getLocation(lf);//:gen.getLocation(lf,locale);
        if(l1.isShared() || l1.isUnique() || !LocationGenerator.isMeaningful(l1)) return this;
         SimpleLocation l2 = gen.duplicateLocation(l1);
         //System.err.println("Renaming " + lf + " from " + l1 + " to " + l2);
       
        return replaceLocation(lf.l,lf.fdecl,l2);
	}
	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.ILocationMap#renameLocation(edu.cmu.cs.fluid.ir.IRNode)
	 */
	public ILocationMap renameLocation(IRNode expr) {
		Operator op = JJNode.tree.getOperator(expr);
		final boolean isStatic = JavaNode.getModifier(methodDecl, JavaNode.STATIC);
		if (op == VariableUseExpression.prototype) {
			IRNode decl = binder.getBinding(expr);
			if (decl == null) return this;
			return replaceLocation(decl,gen.duplicateLocation(getLocal(decl)));
		} else if (op == ConditionalExpression.prototype) {
			ILocationMap s1 = renameLocation(ConditionalExpression.getIftrue(expr));
			ILocationMap s2 = renameLocation(ConditionalExpression.getIffalse(expr));
			return (ILocationMap)s1.meet(s2);
		} else if (op == CastExpression.prototype) {
			return renameLocation(CastExpression.getExpr(expr));
		} else if (op == NullLiteral.prototype) {		
			return this;
		} else if (op == FieldRef.prototype) {
			IRNode fdecl = binder.getBinding(expr);
			if (fdecl != null ) {
				SimpleLocation l = getLocation(FieldRef.getObject(expr));
				LocationField p = new LocationField(l,fdecl);
				if(containsKey(p)){
					return replaceLocation(l,fdecl,gen.duplicateLocation(get(p)));
				}
				SimpleLocation loc = gen.getLocation(expr);
				return replaceLocation(l,fdecl,loc);
//				return loc;
			}
		} else if (op == AssignExpression.prototype) {
			return renameLocation(AssignExpression.getOp2(expr));
		} else if (op == MethodCall.prototype) {
			IRNode mdecl = binder.getBinding(expr);
			if (mdecl != null) {
				gen.getLocation(expr);
				return this; //Is this the right thing to do??? TODO: decide
			}
		} else if (op instanceof ThisExpression) {
			IRNode rec = JavaPromise.getReceiverNode(methodDecl);
			gen.getLocation(rec);
			return this;
		}else if(! isStatic && expr.equals(JavaPromise.getReceiverNode(methodDecl))){
			gen.getLocation(expr);
			return this;
		} else if (op instanceof AllocationExpression){
			gen.getLocation(expr); 
			return this;
		} else if (op instanceof ParameterDeclaration){
				gen.getLocation(expr);
				return this;
		}
		return this;
	}
	/**
	 * @see edu.cmu.cs.fluid.java.analysis.ILocationMap#getLocation(IRNode)
	 */
	public SimpleLocation getLocation(IRNode expr){
		if(expr == null || !JJNode.tree.opExists(expr)){
			return gen.bottom();
		}
		Operator op = JJNode.tree.getOperator(expr);
		final boolean isStatic = JavaNode.getModifier(methodDecl, JavaNode.STATIC);
		if(op instanceof edu.cmu.cs.fluid.java.bind.IHasType &&
				binder.getJavaType(expr) instanceof edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType){
			return gen.top();
		}
		if (op == VariableUseExpression.prototype) {
			IRNode decl = binder.getBinding(expr);
			if (decl == null) return gen.bottom();
			return getLocal(decl);
		} else if (op == ConditionalExpression.prototype) {
			SimpleLocation s1 = getLocation(ConditionalExpression.getIftrue(expr));
			SimpleLocation s2 = getLocation(ConditionalExpression.getIffalse(expr));
			return (SimpleLocation)s1.meet(s2);
		} else if (op == CastExpression.prototype) {
			return getLocation(CastExpression.getExpr(expr));
		} else if (op == NullLiteral.prototype) {		
			return gen.getNull();
		} else if (op == FieldRef.prototype) {
			IRNode fdecl = binder.getBinding(expr);
			if (fdecl != null ) {
				SimpleLocation l = getLocation(FieldRef.getObject(expr));
				LocationField p = new LocationField(l,fdecl);
				if(containsKey(p)){
					return get(p);
				}
				SimpleLocation loc = gen.getLocation(expr);
				return loc;
			}
		} else if (op == AssignExpression.prototype) {
			return getLocation(AssignExpression.getOp2(expr));
		} else if (op == MethodCall.prototype) {
			IRNode mdecl = binder.getBinding(expr);
			if (mdecl != null) {
				return gen.getLocation(expr);
			}
		} else if (op instanceof ThisExpression) { // Shouldn't happen for static methods, right?
			IRNode rec = JavaPromise.getReceiverNode(methodDecl);
			return gen.getLocation(rec);
		}else if(!isStatic && expr.equals(JavaPromise.getReceiverNodeOrNull(methodDecl))){
			return gen.getLocation(expr);
		} else if (op instanceof AllocationExpression){
			return gen.getLocation(expr); 
		} else if (op instanceof ParameterDeclaration){
			return gen.getLocation(expr);
		}else if(op instanceof edu.cmu.cs.fluid.java.promise.EffectSpecification){
			return gen.getLocation(expr);
		}else if (TypeExpression.prototype.includes(op)){
			IRNode tdecl = binder.getBinding(TypeExpression.getType(expr));
			return gen.getLocation(tdecl);
		}else if(ArrayRefExpression.prototype.includes(op)){
			return gen.getLocation(expr);
		}

		return gen.bottom();
	}

	@Override
  public String toString(){
		StringBuilder sb = new StringBuilder();
		for(java.util.Iterator i = keys(); i.hasNext();){
			Object o = i.next();
			if(o instanceof IRNode){
			IRNode k = (IRNode)o;
			sb.append("   ").append(DebugUnparser.toString(k))
				.append(" : ").append(get(k).toString())
				.append("\n");
			}
			else if(o instanceof LocationField){
				LocationField p = (LocationField)o;
				//IRNode f = p.fdecl;
				sb.append("   ").append(p.toString())
					.append(" : ").append(get(p).toString())
          //.append(" (").append(get(p).renames.toString()).append(")")
					.append("\n");
			}
			else{
				System.out.println("----Not an IRNode: " +  o);
				sb.append(" ").append(o.toString())
				.append(" : ").append((get(o)).toString())
				.append("\n");
			}
		}
		return sb.toString();
	}
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.java.analysis.ILocationMap#nulLoc()
	 */
	public SimpleLocation nulLoc() {
		return gen.getNull();
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.java.analysis.ILocationMap
	 */
	public ILocationMap replaceLocation(
		SimpleLocation obj,
		IRNode fieldDecl,
		SimpleLocation loc) {
	
		return (LocationMap)super.update(new LocationField(obj,fieldDecl),loc);
	}

	public boolean contains(SimpleLocation obj, IRNode fieldDecl) {
		return containsKey(new LocationField(obj,fieldDecl));
	}
/*
	public SimpleLocation rename(IRNode decl){
		return gen.renameLocation((SimpleLocation)get(decl));
	}
	
	public SimpleLocation rename(SimpleLocation obj, IRNode fDecl){
		return gen.renameLocation((SimpleLocation)get(new LocationField(obj,fDecl)));
	}
	*/
  /* 
   * @see edu.cmu.cs.fluid.java.analysis.ILocationMap#getLocation(edu.cmu.cs.fluid.java.analysis.LocationField)
   */
  public SimpleLocation getLocation(LocationField lf) {
    return get(lf);
  }

  public void postLocationAsserters(LocationGenerator.LocationAsserter la,
      LocationGenerator.LocationAsserter ra){
    gen.postLocationAsserterLeft(la);
    gen.postLocationAsserterRight(ra);
  }
  public void clearLocationAsserter(){
    gen.clearLocationAsserter();
  }
}
