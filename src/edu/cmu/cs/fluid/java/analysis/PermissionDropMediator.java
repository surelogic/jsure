/*
 * Created on Mar 24, 2005
 */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.JavaGlobals;

import java.util.logging.*;
import java.util.*;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.util.*;
/**
 *
 */
@Deprecated
public class PermissionDropMediator {

	private boolean resultsOff;
	
	private static Logger LOG = SLLogger.getLogger("PermissionAnalysis");
	
	private final IRNode pdecl;
	
	public final PermProcDrop res;

    private final ResultDrop dummy;
  
	PermissionDropMediator(IRNode pd){
		resultsOff = false;
		pdecl = pd;
		res = this.createProcedureDrop(pd);
		resultsOff = true;
      dummy = new BoolDrop();
    }
	
	void startReporting(){
		resultsOff = false;
	}
	void stopReporting(){
		resultsOff = true;
	}

	ResultDrop createUniqueAssertionDrop(boolean passed, IRNode locale, IRNode fdecl, ResultDrop supported, Collection receivedFrom){
		if(resultsOff) return null;
		ResultDrop ret = new UniqueAssertionDrop();
//		ret.addDependent(UniquenessAnnotation.getUniqueDrop(fdecl));
		PromiseDrop pvd = null;
		if(UniquenessAnnotation.isUnique(fdecl))
      pvd = UniquenessAnnotation.getUniqueDrop(fdecl);
      
	  if(pvd != null)
	    res.addCheckedPromise(pvd);
		establishDrop(passed, locale, fdecl, supported, receivedFrom, ret, 
		    ""+DebugUnparser.toString(fdecl) + " is unique",
        "Cannot make " + DebugUnparser.toString(fdecl) + " be unique",pvd);

		return ret;
	}
/**
   * @param passed
   * @param locale
   * @param fdecl
   * @param supported
   * @param receivedFrom
   * @param ret
   */
  private void establishDrop(boolean passed, IRNode locale, 
      IRNode fdecl, ResultDrop supported, 
      Collection receivedFrom, ResultDrop ret, 
      String good, String bad, PromiseDrop pvd) {
    if(supported == null){
			LOG.severe("Nonexistant supported drop");
		}else{
			supported.addDependent(ret);
	//		ret.
		}
		ret.addCheckedPromises(receivedFrom);
		ret.setConsistent(passed);
		ret.setNode(locale);
		if(passed){
			ret.setMessage(good);
			if(res.isConsistent() || pvd == null){}
			else{
			  ret.addTrustedPromise(pvd);
			}
		}else{
			ret.setMessage(bad);
		  //System.out.println("Failed promise check of " + pvd);
			if(pvd != null)
			  ret.addCheckedPromise(pvd);			  
			//inconsistentizeDrop(res);
		}
  }

  /* */
	PromiseDrop getImplicitSharedDrop(IRNode fdecl){
	  //if(resultsOff) return null;
	  PromiseDrop ret = ImplicitSharedDrop.getImplicitSharedDrop(fdecl);
		return ret;
	}
	/* */
	ResultDrop createSharedAssertionDrop(boolean passed, IRNode locale, IRNode fdecl, ResultDrop supported, Collection receivedFrom){
		if(resultsOff) return null;
		ResultDrop ret = new SharedAssertionDrop();
    
    PromiseDrop pvd = getImplicitSharedDrop(fdecl);
    
		establishDrop(passed,locale,fdecl,supported,receivedFrom,ret,
		    ""+DebugUnparser.toString(fdecl) + " is shared",
        "Cannot make "+DebugUnparser.toString(fdecl) + " be shared", pvd);
		return ret;
		
	}

	ResultDrop createReadDrop(boolean passed, IRNode locale, IRNode fdecl, ResultDrop supported, Collection receivedFrom){
		if(resultsOff) return null;
		ResultDrop ret = new PermReadDrop();
		PromiseDrop med = EffectsAnnotation.getMethodEffectsDrop(pdecl);
		if(med != null){
		  res.addCheckedPromise(med);
		}
		establishDrop(passed,locale,fdecl,supported,receivedFrom,ret,
		    "Read permission for field " + DebugUnparser.toString(fdecl) + " present", 
			"Read permission for field " + DebugUnparser.toString(fdecl) + " absent", med);
		return ret;
		
	}
	ResultDrop createWriteDrop(boolean passed, IRNode locale, IRNode fdecl, ResultDrop supported, Collection receivedFrom){
		if(resultsOff) return null;
		ResultDrop ret = new PermWriteDrop();
		PromiseDrop med = EffectsAnnotation.getMethodEffectsDrop(pdecl);
		if(med != null){
		  res.addCheckedPromise(med);
		}
		establishDrop(passed,locale,fdecl,supported,receivedFrom,ret,
			"Write permission for field " + DebugUnparser.toString(fdecl) + " present", 
			"Write permission for field " + DebugUnparser.toString(fdecl) + " absent", med);
		return ret;		
	}

	ResultDrop createAnnoDrop(boolean passed, IRNode locale, IRNode fdecl, 
	    ResultDrop supported, Collection receivedFrom, ChainLattice perm){
		if(resultsOff) return null;
		ResultDrop ret = new AnnoPermDrop();

		PromiseDrop med = EffectsAnnotation.getMethodEffectsDrop(pdecl);
		if(med != null){
		  res.addCheckedPromise(med);
		}
		establishDrop(passed,locale,fdecl,supported,receivedFrom,ret,
		    "Annotated " + PermissionSet.toString(perm) + 
		    	" permission for field " + DebugUnparser.toString(fdecl) + " present",
		    "Annotated " + PermissionSet.toString(perm) + 
		    	" permission for field " + DebugUnparser.toString(fdecl) + " absent", med);
		return ret;		

	}
	
	ResultDrop createCallDrop(IRNode locale, PermProcDrop pd){
		if(resultsOff) return null;
		ResultDrop ret = new PermCallDrop();
		ret.setConsistent();
		if(pd == null){
			LOG.severe("Null Method Call Drop");
		}else{
			ret.addDependent(pd);
		}
		ret.setNode(locale);
		ret.setMessage("Method call possesses all promised permissions");
		return ret;
	}

	ResultDrop createFieldDrop(IRNode locale, PermProcDrop pd){
		if(resultsOff) return null;
		ResultDrop ret = new PermCallDrop();
		ret.setConsistent();
		if(pd == null){
			LOG.severe("Null Field Drop");
		}else{
			ret.addDependent(pd);
		}
		ret.setNode(locale);
		ret.setMessage("Field possesses all promised permissions");
		return ret;
	}
	ResultDrop createArrayDrop(IRNode locale, PermProcDrop pd){
		if(resultsOff) return null;
		ResultDrop ret = new PermCallDrop();
		ret.setConsistent();
		if(pd == null){
			LOG.severe("Null Array Drop");
		}else{
			ret.addDependent(pd);
		}
		ret.setNode(locale);
		ret.setMessage("Array reference possesses all promised permissions");
		return ret;
	}
	ResultDrop createRetDrop(IRNode locale, PermProcDrop pd){
		if(resultsOff) return null;
		ResultDrop ret = new PermCallDrop();
		//ret.setConsistent();
		if(pd == null){
			LOG.severe("Null Method Return Drop");
		}else{
			ret.addDependent(pd);
		}
		ret.setNode(locale);
		ret.setMessage("Method returns all promised permissions");
		return ret;
	}
  ResultDrop createMeetDrop(IRNode locale){
    if(resultsOff){
      dummy.setConsistent();
      return dummy;
    }
    ResultDrop ret = new PermCallDrop();
    ret.setConsistent();
    
    ret.setNode(locale);
    ret.setMessage("All permissions met successfully");
    return ret;
  }

	
	
  void inconsistentizeDrop(ResultDrop d){
    if(d == dummy){d.setInconsistent();}
    if(resultsOff) return;
    if(d == null){
      LOG.severe("Cannot make null drop inconsistent.");
      return;	
    }
    d.setInconsistent();
    if(d instanceof PermCallDrop){
      d.setMessage("Method call lacks certain promised permissions");
    }else if(d instanceof PermFieldDrop){
      d.setMessage("Field lacks certain promised permissions");
    }else if(d instanceof PermRetDrop){
      d.setMessage("Method unable to return promised permissions");		
    }else if(d instanceof PermProcDrop){
      d.setMessage("Procedure body does not use permissions correctly");
    }else if(d instanceof PermMeetDrop){
      d.setMessage("Unable to rebox permissions at join");
    }else{
      LOG.warning("Unknown drop made inconsistent.");
	}
  }

	PermProcDrop createProcedureDrop(IRNode root){
		if(resultsOff) return null;
		PermProcDrop ret = new PermProcDrop();
		ret.setNode(root);
		ret.setConsistent();
		ret.setMessage("Procedure body correctly uses permissions");
		return ret;
	}
	
	
	public static PromiseDrop getEffectDrop(IRNode effect){
	
	  PromiseDrop ret = new PermEffectDrop();
	  ret.setNode(effect);
	  ret.setMessage(DebugUnparser.toString(effect));
	  return ret;
	}
	
  
  public static BorrowedOKDrop getBorrowedOKDrop(){
    return BorrowedOKDrop.getInstance();
  }
}
interface PermissionDrop{
	
}

class BorrowedOKDrop extends ResultDrop implements PermissionDrop{
  private static BorrowedOKDrop instance = new BorrowedOKDrop();
  private BorrowedOKDrop(){
    setConsistent();
    setMessage("borrowed requires no assurance");
  }
  static BorrowedOKDrop getInstance(){ return instance; }
}
class ImplicitSharedDrop extends PromiseDrop implements PermissionDrop{
	private static Map<IRNode,ImplicitSharedDrop> drops 
    = new HashMap<IRNode,ImplicitSharedDrop>();
  private ImplicitSharedDrop(IRNode fdecl){
    setMessage("shared (no annotation) " + DebugUnparser.toString(fdecl));
    setNode(fdecl);
    this.setCategory(JavaGlobals.UNIQUENESS_CAT);
  }
  static ImplicitSharedDrop getImplicitSharedDrop(IRNode fdecl){
    ImplicitSharedDrop drop = drops.get(fdecl); 
    if(drop == null){
      drop = new ImplicitSharedDrop(fdecl);
      drops.put(fdecl,drop);
    }
    return drop;
  }
}

class PermEffectDrop extends PromiseDrop implements PermissionDrop{
    
}

class UniqueAssertionDrop extends ResultDrop implements PermissionDrop{
	
}

class UniqueCheckDrop extends ResultDrop implements PermissionDrop{
	
}

class SharedAssertionDrop extends ResultDrop implements PermissionDrop{
	
}

class PermReadDrop extends ResultDrop implements PermissionDrop{
	
}

class PermWriteDrop extends ResultDrop implements PermissionDrop{
	
}

class AnnoPermDrop extends ResultDrop implements PermissionDrop{
  
}

class PermCallDrop extends ResultDrop implements PermissionDrop{
	
}

class PermFieldDrop extends ResultDrop implements PermissionDrop{
	
}
class PermArrayDrop extends ResultDrop implements PermissionDrop{
	
}

class PermRetDrop extends ResultDrop implements PermissionDrop{
	
}

class PermProcDrop extends ResultDrop implements PermissionDrop{
	
}
class PermMeetDrop extends ResultDrop implements PermissionDrop{
  
}
class BoolDrop extends ResultDrop implements PermissionDrop{

}