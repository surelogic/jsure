package edu.cmu.cs.fluid.java;

import java.util.concurrent.*;
import java.util.Map;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.Utility;
import com.surelogic.common.Pair;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ref.IJavaRef.Within;
import com.surelogic.common.ref.JavaRef;
import com.surelogic.javac.FileResource;
import com.surelogic.javac.adapter.ClassResource;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.util.DeclFactory;

@Utility
public final class SkeletonJavaRefUtility {

  private static final Map<IRNode, JavaRefSkeletonBuilder> nodeToSkeleton = new ConcurrentHashMap<IRNode, JavaRefSkeletonBuilder>();

  public static void removeInfo(IRNode key) {
	  nodeToSkeleton.remove(key);
  }
  
  public static void removeAllInfo() {
	  nodeToSkeleton.clear();
  }
  
  public static void registerSourceLocation(DeclFactory factory, IRNode node, FileResource fileResource, int lineNumber,
      int offset, int length) {
    final JavaRefSourceBuilder b = new JavaRefSourceBuilder(factory, fileResource, lineNumber, offset, length);
    nodeToSkeleton.put(node, b);
  }

  public static void registerBinaryCode(DeclFactory factory, IRNode node, ClassResource resource, int lineNumber) {
    final JavaRefBinaryBuilder b = new JavaRefBinaryBuilder(factory, resource, lineNumber);
    nodeToSkeleton.put(node, b);
  }

  // Placeholder for skeletons being built
  private static final JavaRefSkeletonBuilder placeholder = new JavaRefSkeletonBuilder() {
	  @Override
	  public IJavaRef buildOrNullOnFailure(@NonNull IRNode node) {
		  return null;
	  }
  };
  public static final IJavaRef placeholderRef = new IJavaRef() {
	@Override
	public boolean isFromSource() {
		return false;
	}
	@Override
	@NonNull
	public Within getWithin() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	@Nullable
	public String getTypeNameOrNull() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	@NonNull
	public String getTypeNameFullyQualified() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	@NonNull
	public String getSimpleFileNameWithNoExtension() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	@NonNull
	public String getSimpleFileName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	@Nullable
	public String getRealEclipseProjectNameOrNull() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	@NonNull
	public Position getPositionRelativeToDeclaration() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	@NonNull
	public String getPackageName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int getOffset() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int getLineNumber() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int getLength() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	@Nullable
	public String getJarRelativePathOrNull() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	@Nullable
	public String getEclipseProjectNameOrNull() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	@NonNull
	public String getEclipseProjectNameOrEmpty() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	@NonNull
	public String getEclipseProjectName() {
		return null;
	}
	@Override
	@NonNull
	public IDecl getDeclaration() {
		return null;
	}
	@Override
	@Nullable
	public String getAbsolutePathOrNull() {
		return null;
	}
	@Override
	@NonNull
	public String encodeForPersistence() {
		return null;
	}
};
  
  /**
   * Tries to build a valid Java code reference from the skeleton on the passed
   * node. If it fails some warnings may be logged and {@code null} is returned.
   * <p>
   * This method should <b>only</b> be called from
   * {@link JavaNode#getFluidJavaRef(IRNode)}.
   * 
   * @param node
   *          node to build a code reference for.
   * @return a valid Java code reference, or {@code null} if one could not be
   *         constructed.
   */
  static IJavaRef buildOrNullOnFailure(IRNode node) {
	//final JavaRefSkeletonBuilder sb = nodeToSkeleton.remove(node);
    final JavaRefSkeletonBuilder sb = nodeToSkeleton.put(node, placeholder);
    if (sb == placeholder) {
    	return placeholderRef;
    }
    if (sb != null) {
      return sb.buildOrNullOnFailure(node);
    } else {
      // It really should be null
      nodeToSkeleton.remove(node);
    }
    return null;
  }

  /**
   * Used to copy the code reference on one node to another node. This method
   * works regardless if the skeleton builder is on the node or the real Java
   * code reference. It fails if no code reference or skeleton builder is on the
   * <tt>from</tt> node.
   * 
   * @param from
   *          node to copy from.
   * @param to
   *          node to copy to.
   * @return {@code true} if the copy succeeded, {@code false} otherwise.
   */
  public static boolean copyIfPossible(IRNode from, IRNode to) {
    if (from == null || to == null)
      return false;
    // skeleton builder
    final JavaRefSkeletonBuilder s = nodeToSkeleton.get(from);
    if (s != null) {
      nodeToSkeleton.put(to, s);
      return true;
    } else {
    	
      /*
      String unparse = DebugUnparser.toString(from);
      if (unparse.length() != 0 && !unparse.contains("public class []")) {
    	  System.out.println("No ref info for "+unparse);
      }
      */
    }
    if (JavaNode.copyFluidJavaRef(from, to) == null)
      return false;
    else
      return true;
  }

  /**
   * Checks if the passed node has a skeleton builder or an actual code
   * reference on it.
   * 
   * @param node
   *          a node.
   * @return {@code true} if has a Java code reference or skeleton builder,
   *         {@code false} otherwise.
   */
  public static boolean hasRegistered(IRNode node) {
	if (node == null) {
		return false;
	}
    return nodeToSkeleton.containsKey(node) || JavaNode.hasJavaRef(node);
  }

  interface JavaRefSkeletonBuilder {
    IJavaRef buildOrNullOnFailure(@NonNull IRNode node);
  }

  static abstract class AbstractBuilder implements JavaRefSkeletonBuilder {
	private final DeclFactory f_factory; 
    final int f_lineNumber;
    /**
     * Caching allows copies to return the same IJavaRef
     */
    // breaks handling of receiver/return nodes
    //private IJavaRef cache = null;
    
    AbstractBuilder(DeclFactory f, int lineNumber) {
      f_factory = f;
      f_lineNumber = lineNumber;
    }
    
    @Override
    public final IJavaRef buildOrNullOnFailure(@NonNull IRNode node) {
      /*
      if (cache != null) {
    	return cache;
      }
      */   	
      final Pair<IDecl, IJavaRef.Position> pair = f_factory.getDeclAndPosition(node);
      if (pair == null) {
    	if (!CompilationUnit.prototype.includes(node)) {
    		SLLogger.getLogger().warning(I18N.err(289, DebugUnparser.unparseCode(node), new Exception()));
    	} else {
    		SLLogger.getLogger().info(I18N.err(289, DebugUnparser.unparseCode(node), new Exception()));
    	}
    	return null;
      }   
      return /*cache =*/ build(pair);
    }
    abstract IJavaRef build(@NonNull Pair<IDecl, IJavaRef.Position> pair);
  }
  
  static final class JavaRefBinaryBuilder extends AbstractBuilder {
    private final ClassResource f_resource;

    private JavaRefBinaryBuilder(DeclFactory f, ClassResource resource, int lineNumber) {
      super(f, lineNumber);
      f_resource = resource;
    }

    @Override
    IJavaRef build(@NonNull Pair<IDecl, IJavaRef.Position> pair) {
      final JavaRef.Builder b = new JavaRef.Builder(pair.first());
      b.setPositionRelativeToDeclaration(pair.second());
      b.setLineNumber(f_lineNumber);
      b.setEclipseProjectName(f_resource.getJavaRefProjectName());
      final String jarRelativePath = f_resource.getJarRelativePath();
      b.setWithin(jarRelativePath == null ? Within.CLASS_FILE : Within.JAR_FILE);
      b.setAbsolutePath(f_resource.getAbsolutePath());
      b.setJarRelativePath(jarRelativePath);
      return b.buildOrNullOnFailure();
    }
  }

  static final class JavaRefSourceBuilder extends AbstractBuilder {
    private final int f_offset;
    private final int f_length;
    private final FileResource f_fileResource;

    private JavaRefSourceBuilder(DeclFactory f, FileResource fileResource, int lineNumber, int offset, int length) {
      super(f, lineNumber);
      f_fileResource = fileResource;
      f_offset = offset;
      f_length = length;
    }

    @Override
    IJavaRef build(@NonNull Pair<IDecl, IJavaRef.Position> pair) {
      final JavaRef.Builder b = new JavaRef.Builder(pair.first());
      b.setPositionRelativeToDeclaration(pair.second());
      b.setLineNumber(f_lineNumber);
      b.setOffset(f_offset);
      b.setLength(f_length);
      b.setEclipseProjectName(f_fileResource.getProjectName());
      b.setWithin(Within.JAVA_FILE);
      b.setAbsolutePath(f_fileResource.getAbsolutePath());
      return b.buildOrNullOnFailure();
    }
  }

  private SkeletonJavaRefUtility() {
    // no instances
  }
}
