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
import com.surelogic.common.ref.JavaRef;
import com.surelogic.javac.FileResource;
import com.surelogic.javac.adapter.ClassResource;
import com.surelogic.tree.SyntaxTreeNode;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.util.DeclFactory;
import edu.cmu.cs.fluid.parse.JJNode;

@Utility
public final class SkeletonJavaRefUtility {
  // e.g. storing these in the nodes themselves
  public static boolean useSkeletonsAsJavaRefPlaceholders = true;
  private static final Map<IRNode, JavaRefSkeletonBuilder> nodeToSkeleton = 
		  useSkeletonsAsJavaRefPlaceholders ? null : new ConcurrentHashMap<IRNode, JavaRefSkeletonBuilder>();

  public static void removeInfo(IRNode key) {
	  if (useSkeletonsAsJavaRefPlaceholders) {
		  return;
	  }
	  nodeToSkeleton.remove(key);
  }
  
  public static void removeAllInfo() {
	  nodeToSkeleton.clear();
  }
  
  public static void registerSourceLocation(DeclFactory factory, IRNode node, FileResource fileResource, int lineNumber,
      int offset, int length) {
    final JavaRefSourceBuilder b = new JavaRefSourceBuilder(factory, fileResource, lineNumber, offset, length);
    if (useSkeletonsAsJavaRefPlaceholders) {
    	node.setSlotValue(JavaNode.f_fluidJavaRefSlotInfo, b);
    } else {
    	nodeToSkeleton.put(node, b);
    }
  }

  public static void registerBinaryCode(DeclFactory factory, IRNode node, ClassResource resource, int lineNumber) {
    final JavaRefBinaryBuilder b = new JavaRefBinaryBuilder(factory, resource, lineNumber);
    if (useSkeletonsAsJavaRefPlaceholders) {
    	node.setSlotValue(JavaNode.f_fluidJavaRefSlotInfo, b);
    } else {
    	nodeToSkeleton.put(node, b);
    }
  }

  // Placeholder for skeletons being built
  private static final JavaRefSkeletonBuilder placeholder = new AbstractBuilder(null, 0) {
	  @Override
	  IJavaRef build(@NonNull Pair<IDecl, Position> pair) {
		  return null;
	  }
  };  
  
  /**
   * A marker to note that we haven't yet built the real ref 
   * Not intended to be used otherwise
   */
  public static final IJavaRef placeholderRef = new JavaRefPlaceholder();
  
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
	if (useSkeletonsAsJavaRefPlaceholders) {
		return null;
	}
	
	//final JavaRefSkeletonBuilder sb = nodeToSkeleton.remove(node);
    final JavaRefSkeletonBuilder sb = nodeToSkeleton.put(node, placeholder);
    if (sb == placeholder) {
    	return placeholderRef;
    }
    if (sb != null) {
      return sb.buildOrNullOnFailure(node, true);
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
    
	if (useSkeletonsAsJavaRefPlaceholders) {
		JavaRefSkeletonBuilder b = SyntaxTreeNode.getSkeletonBuilder(from);
    	to.setSlotValue(JavaNode.f_fluidJavaRefSlotInfo, b);
    	return b != null;
	} else {
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
  }

  public static boolean copyToTreeIfPossible(IRNode from, IRNode to) {	  
	  if (from == null || to == null)
		  return false;
	      
	  if (!useSkeletonsAsJavaRefPlaceholders) {
		  throw new IllegalStateException();
	  }
	  JavaRefSkeletonBuilder b = SyntaxTreeNode.getSkeletonBuilder(from);
	  if (b != null) {
		 for(IRNode n : JJNode.tree.bottomUp(to)) {	  
			 n.setSlotValue(JavaNode.f_fluidJavaRefSlotInfo, b);
		 }
	  }
	  return b != null;
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
	if (useSkeletonsAsJavaRefPlaceholders) {
		return JavaNode.hasJavaRef(node);
	}
    return nodeToSkeleton.containsKey(node) || JavaNode.hasJavaRef(node);
  }

  public interface JavaRefSkeletonBuilder extends IJavaRef {
    IJavaRef buildOrNullOnFailure(@NonNull IRNode node, boolean useBinder);
  }

  static abstract class AbstractBuilder extends JavaRefPlaceholder implements JavaRefSkeletonBuilder {
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
    public final IJavaRef buildOrNullOnFailure(@NonNull IRNode node, final boolean useBinder) {
      /*
      if (cache != null) {
    	return cache;
      }
      */   	
      final Pair<IDecl, IJavaRef.Position> pair = f_factory.getDeclAndPosition(node, useBinder);
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

    JavaRefBinaryBuilder(DeclFactory f, ClassResource resource, int lineNumber) {
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

    JavaRefSourceBuilder(DeclFactory f, FileResource fileResource, int lineNumber, int offset, int length) {
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

class JavaRefPlaceholder implements IJavaRef {
	@Override
	public boolean isFromSource() {
		throw new NotImplemented();
	}
	@Override
	@NonNull
	public Within getWithin() {
		throw new NotImplemented();
	}
	
	@Override
	@Nullable
	public String getTypeNameOrNull() {
		throw new NotImplemented();
	}
	
	@Override
	@NonNull
	public String getTypeNameFullyQualified() {
		throw new NotImplemented();
	}
	
	@Override
	@NonNull
	public String getSimpleFileNameWithNoExtension() {
		throw new NotImplemented();
	}
	
	@Override
	@NonNull
	public String getSimpleFileName() {
		throw new NotImplemented();
	}
	
	@Override
	@Nullable
	public String getRealEclipseProjectNameOrNull() {
		throw new NotImplemented();
	}
	
	@Override
	@NonNull
	public Position getPositionRelativeToDeclaration() {
		throw new NotImplemented();
	}
	
	@Override
	@NonNull
	public String getPackageName() {
		throw new NotImplemented();
	}
	
	@Override
	public int getOffset() {
		throw new NotImplemented();
	}
	
	@Override
	public int getLineNumber() {
		throw new NotImplemented();
	}
	
	@Override
	public int getLength() {
		throw new NotImplemented();
	}
	
	@Override
	@Nullable
	public String getJarRelativePathOrNull() {
		throw new NotImplemented();
	}
	
	@Override
	@Nullable
	public String getEclipseProjectNameOrNull() {
		throw new NotImplemented();
	}
	@Override
	@NonNull
	public String getEclipseProjectNameOrEmpty() {
		throw new NotImplemented();
	}
	@Override
	@NonNull
	public String getEclipseProjectName() {
		throw new NotImplemented();
	}
	@Override
	@NonNull
	public IDecl getDeclaration() {
		throw new NotImplemented();
	}
	@Override
	@Nullable
	public String getAbsolutePathOrNull() {
		throw new NotImplemented();
	}
	@Override
	@NonNull
	public String encodeForPersistence() {
		throw new NotImplemented();
	}
}
