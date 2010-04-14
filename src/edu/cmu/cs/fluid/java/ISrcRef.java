package edu.cmu.cs.fluid.java;

import java.net.URI;

import edu.cmu.cs.fluid.ir.IRObjectType;
import edu.cmu.cs.fluid.java.comment.IJavadocElement;

/**
 * Information about source references within the Fluid IR from (ye olde)
 * files (for example, Eclipse projects or simple ".java" files).  This
 * capability was created to interact with the Eclipse IDE which takes a
 * file/CVS oriented view of the world, as opposed to an IR-view of the world.
 * In a full IR-based development / assurance / transformation system files
 * would not exist, hence this capability would not be needed.
 * <BR>
 * To obtain the <code>ISrcRef</code> from an <code>IRNode</code>
 * (for this example we will call <code>node</code>) you would:
 * <pre>
 * ISrcRef nodeSrcRef = JavaNode.getSrcRef(node);
 * </pre>
 * <BR>
 * The concrete implementation of <code>ISrcRef</code> is the class
 * {@link edu.cmu.cs.fluid.eclipse.adapter.SrcRef} which contains several
 * <code>static</code> factory methods to construct source references.  The
 * eAST to fAST converter actually places the source references within the
 * fAST's IRNodes (within 
 * {@link edu.cmu.cs.fluid.eclipse.adapter.JavaSourceFileAdapter}).
 * 
 * @see edu.cmu.cs.fluid.eclipse.adapter.SrcRef
 * @see edu.cmu.cs.fluid.eclipse.adapter.JavaSourceFileAdapter
 */
public interface ISrcRef {
  IRObjectType<ISrcRef> srcRefType = new IRObjectType<ISrcRef>();	  
  String srcRefName = "JavaNode.SrcRef";
  
  /**
   * Returns a reference to the file that this source reference is
   * contained within.  Within Eclipse the reference returned will be an
   * <code>IResource</code>.  Outside of Eclipse it could be a
   * <code>java.io.File</code>.
   * 
   * @return the file that this source reference is contained within
   */
  Object getEnclosingFile();
  
  URI getEnclosingURI();
  
  /**
   * Returns the relative path that identifies the source file
   * @return
   */
  String getRelativePath();

  /**
   * Returns either the leading Javadoc comment, if the node is
   * a body declaration, or the leading comment, if the node is
   * a statement, or <code>null</code>.
   * 
   * @return the leading comment or <code>null</code> if no comment exists
   */
  String getComment();

  /**
   * Returns the length, in characters, of the source reference.
   * 
   * @return the length of the source reference
   */
  int getLength();

  /**
   * Returns the line number the source reference begins on, or 0 if
   * the line number is unknown.
   * 
   * @return the line number the source reference begins on, or 0 if
   * the line number is unknown
   */
  int getLineNumber();

  /**
   * Returns the character offset, from the start of the Java
   * source file, to the start of the source reference.
   * 
   * @return the character offset of the reference from the start of
   *   the Java source code file
   */
  int getOffset();

  /**
   * Returns the original source text of the source reference from the
   * Java source code, just the code snippet not the entire compilation
   * unit source code.
   * 
   * @return the original source text of the source reference
   */
  String toString();
  
  IJavadocElement getJavadoc();
  void clearJavadoc();
  
  Long getHash();
  String getPackage();
  String getCUName();
  
  /**
   * Create a similar ISrcRef, but with the new offset
   */
  ISrcRef createSrcRef(int offset);
}