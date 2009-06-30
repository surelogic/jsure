package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.java.analysis.LocationGenerator.SimpleLocation;
import edu.cmu.cs.fluid.java.bind.PromiseConstants;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 * Like a Pair, but typed.
 */
@Deprecated
class LocationField{
	public final SimpleLocation l;
	public final IRNode fdecl;
  //private static IBinder b;
  
	public LocationField(SimpleLocation l, IRNode fdecl){
    this.l = l;
		this.fdecl = fdecl;
	}
	@Override
  public boolean equals(Object other){
		if(other instanceof LocationField){
			LocationField lf = (LocationField)other;
			return l==null?lf.l==null:l.equals(lf.l) && (fdecl == null?lf.fdecl==null:fdecl.equals(lf.fdecl));
		}
		return false;
	}
	@Override
  public int hashCode(){
		return l==null?0:l.hashCode() + ((fdecl == null)?0:fdecl.hashCode());
	}
	@Override
  public String toString(){
		return l+"."+((fdecl == null)?"ALL":DebugUnparser.toString(fdecl));
	}
	
	public static LocationField allField(SimpleLocation loc, IBinder binder){
    IRNode object = binder.getTypeEnvironment()
    .findNamedType("java.lang.Object");

    return new LocationField(
        loc,
        binder.findRegionInType(object,PromiseConstants.REGION_INSTANCE_NAME));    
 	}
  public static IRNode arrayRegion(IBinder b){
    return (b.findRegionInType(b.getTypeEnvironment().getArrayClassDeclaration(),
        PromiseConstants.REGION_ELEMENT_NAME));
  }
	/*
  public static void postBinder(IBinder ib){
    b = ib;
  }
  */
	public static IRNode sharedRegion(IBinder b){ 
   return b.findRegionInType(b.getTypeEnvironment().getObjectType().getDeclaration(),
       PromiseConstants.REGION_ALL_NAME);
  }
}
