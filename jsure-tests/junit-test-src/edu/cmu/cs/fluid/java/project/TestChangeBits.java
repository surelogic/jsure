/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/project/TestChangeBits.java,v 1.1 2007/08/22 15:02:00 boyland Exp $*/
package edu.cmu.cs.fluid.java.project;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.parse.AstGen;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Tree;
import edu.cmu.cs.fluid.version.Version;
import edu.cmu.cs.fluid.version.VersionedChangeRecord;

public class TestChangeBits {

  /**
   * @param args
   */
  public static void main(String[] args) {
    Tree t = (Tree) JJNode.tree;
    VersionedChangeRecord treeChanged = (VersionedChangeRecord) JJNode.treeChanged;
    
    Version initial = Version.getVersion();
    
    IRNode little = AstGen.genTypeDecl("class Foo { }");
    
    Version v1 = Version.getVersion();
    
    System.out.println("Initial changes");
    JavaIncrementalBinder.dumpChangedTree(little, 0, initial, v1);
    
    IRNode constructor = AstGen.genMember("Foo () { super(); }");
    
    IRNode classBody = ClassDeclaration.getBody(little);
    
    t.insertChild(classBody, constructor);
    
    Version v2 = Version.getVersion();
    
    System.out.println("After addition of a constructor");
    JavaIncrementalBinder.dumpChangedTree(little, 0, v1, v2);
    
    System.out.println("Change bits:");
    System.out.println("  class decl: " + treeChanged.changed(little, v1, v2));
    System.out.println("  class body: " + treeChanged.changed(classBody, v1, v2));
  }

}
