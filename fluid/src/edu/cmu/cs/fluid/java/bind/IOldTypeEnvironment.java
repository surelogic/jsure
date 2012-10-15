/*
 * Created on Mar 28, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.*;

import com.surelogic.aast.promise.*;
import com.surelogic.analysis.IIRProject;
import com.surelogic.dropsea.ir.drops.RegionModel;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.InitDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.java.util.CogenUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.version.*;


/**
 * Extra things to help bootstrap the system.
 * What this does is all broken for versioning although it
 * tries to play right.  Everything used from this class needs to be moved
 * to IBinder and ITypeEnvironment.  Getting nodes
 * for java.lang.Object and Array is the most tricky to accomplish.
 * This class will be removed (or moved to fluid-eclipse)
 * once we come up with a clean way to handle these things.
 * <p>
 * In order to accomplish this change, we need to be able to load
 * the array class from IR persistent storage (as  a VIC).
 * This can be done once we have figured out how promises are
 * represented persistently.
 * <p>
 * TODO: fix this.
 */
public interface IOldTypeEnvironment extends ITypeEnvironment {
/// Setup 
  // 
  VersionedRegion initRegion = JJNode.versioningIsOn? new VersionedRegion():null;
  /** 
   * Must come before any IR-building operation to ensure that the IRNodes are put 
   * into the correct region
   */
  Version initVersion = DirtyTricksHelper.initVersioning();   
                          
  //public static final IRNode arrayType = DirtyTricksHelper.getArrayType();

  //public static final IRNode cloneMethod = DirtyTricksHelper.getCloneMethod();
  
  //public static final IRNode lengthField = DirtyTricksHelper.getLengthField();
  
  
  /** Must come after all IR-building operations */
  Version endVersion = DirtyTricksHelper.endVersioning();
  
  /**
   * This class encapsulates yucky things that are needed for the
   * old way to handle arrays and the special array class.
   * It will go away, so do not use it!
   * @author boyland
   */
  static class DirtyTricksHelper {
    public static Version initVersioning() {
      // final Version v = Version.getInitialVersion();
      final Version v = Version.getVersion();
      Version.saveVersion(v);
      if (JJNode.versioningIsOn) {
        PlainIRNode.pushCurrentRegion(initRegion);
      }
      return v;
    }

    public static Version endVersioning() {
      if (JJNode.versioningIsOn) {
        // VersionedRegion vr = (VersionedRegion) PlainIRNode.getCurrentRegion();
        // Version.getDefaultEra().addVersion(Version.getVersion());

        PlainIRNode.popCurrentRegion();
      }
      Version v = Version.getVersion();
      Version.restoreVersion();
      // vr.getDelta(Version.getDefaultEra());
      return v;
    }
    
    // FIX ok to create new superclass for arrays (AnonClassE)
    //private static /* final */ IRNode privateArrayType = null;

    //private static /* final */ IRNode privateCloneMethod = null;
    
    //private static /* final */ IRNode privateLengthField = null;
    /*
    static synchronized IRNode getArrayType() { 
      if (privateArrayType == null) {
    	  privateArrayType = createArrayType("");
      }
      return privateArrayType;
    }    
    
    static synchronized IRNode getLengthField() {
      if (privateArrayType == null) createArrayType();
      return privateLengthField;
    }

    static synchronized IRNode getCloneMethod() {
      if (privateArrayType == null) createArrayType();
      return privateCloneMethod;
    }    
    */   
    
    private static final Map<String,IRNode> types = new Hashtable<String, IRNode>();
    
    public static IRNode createArrayType(String project, IIRProject p) {      
      final IRNode rv = types.get(project+p.hashCode());
      if (rv != null) {
    	  return rv;
      }
      final IRNode[] noNodes = new IRNode[0];
      final IRNode tArray = ArrayType.createNode(NamedType.createNode("T"), 1);
      
      final IRNode privateCloneMethod = 
      CogenUtil.makeMethodDecl(noNodes, JavaNode.PUBLIC | JavaNode.NATIVE, noNodes,
                               tArray, "clone", noNodes, noNodes, null);
             
      final IRNode privateLengthField = CogenUtil.makeVarDecl(PromiseConstants.REGION_LENGTH_NAME, null);
      /*
      final IRNode privateArrayElement = CogenUtil.makeVarDecl(PromiseConstants.REGION_ELEMENT_NAME, null);
      */
      
      // Somehow, this needs to take prim types
      final IRNode typeParam   = TypeFormal.createNode("T", MoreBounds.createNode(noNodes));
      
      final IRNode privateArrayType = 
        CogenUtil.makeClass(false, JavaNode.PUBLIC, PromiseConstants.ARRAY_CLASS_NAME,
                            new IRNode[] { typeParam },
                            CogenUtil.makeObjectNamedT(), 
                            new IRNode[] {CogenUtil.createNamedT("java.lang.Cloneable"),
                                          CogenUtil.createNamedT("java.io.Serializable")}, 
                            new IRNode[] {
                              CogenUtil.makeFieldDecl(noNodes, JavaNode.PUBLIC | JavaNode.FINAL, 
                                IntType.prototype.jjtCreate(), privateLengthField),
          	                  /*                              
                              CogenUtil.makeFieldDecl(noNodes, JavaNode.PUBLIC | JavaNode.FINAL, 
                            	ArrayType.createNode(NamedType.createNode("T"), 1), privateArrayElement),
                              */
        	
                              // FIX alpha for the type?
                              privateCloneMethod,
                              /*
                              makeFieldDecl(CogenUtil.makeVarDecl("[]", null), 
                                            IntType.prototype.jjtCreate(), false ), 
                              */
                            });   
      // Make sure length is final
      JavaNode.setModifier(privateLengthField, JavaNode.FINAL, true);      
      
      /* Not needed since this doesn't officially exist
      final ISrcRef ref = 
    	  new NamedSrcRef(project, PromiseConstants.ARRAY_CLASS_QNAME, "java.lang", 
    			          PromiseConstants.ARRAY_CLASS_NAME);
      JavaNode.setSrcRef(privateCloneMethod, ref);
      JavaNode.setSrcRef(privateLengthField, ref);
      JavaNode.setSrcRef(privateArrayType, ref);
      */
      
      //privateArrayType.setSlotValue(qnameSI, "<array superclass>");  
      //System.out.println("arrayType = "+arrayType);
      parsetree.clearParent(privateArrayType);

      IRNode pkg = NamedPackageDeclaration.createNode(Annotations.createNode(noNodes), "java.lang");
      CompilationUnit.createNode(pkg, ImportDeclarations.createNode(new IRNode[0]), 
          TypeDeclarations.createNode(new IRNode[] {privateArrayType}));

      ReturnValueDeclaration.getReturnNode(privateCloneMethod);
      ReceiverDeclaration.makeReceiverNode(privateCloneMethod);
      ReceiverDeclaration.makeReceiverNode(privateArrayType);
      final IRNode init = InitDeclaration.getInitMethod(privateArrayType);
      ReceiverDeclaration.makeReceiverNode(init);
      
      types.put(project, privateArrayType);
      return privateArrayType;
    }
    
    static void createArrayRegion(String name, IRNode type, String project) {
    	//RegionModel.getInstance(decl).getModel();    	
        NewRegionDeclarationNode region = 
            new NewRegionDeclarationNode(-1, JavaNode.PUBLIC, name, 
                                         new RegionNameNode(-1, PromiseConstants.REGION_INSTANCE_NAME));
        region.setPromisedFor(type, null);

        RegionModel model = RegionModel.create(region, name); 
        PromiseFramework.getInstance().findSeqStorage("Region").add(type, model);        
    }
  }
}
