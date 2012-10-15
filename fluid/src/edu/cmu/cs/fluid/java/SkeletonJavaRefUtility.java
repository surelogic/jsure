package edu.cmu.cs.fluid.java;

import java.util.HashMap;
import java.util.Map;

import com.surelogic.NonNull;
import com.surelogic.Utility;
import com.surelogic.common.Pair;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef.Within;
import com.surelogic.javac.FileResource;
import com.surelogic.javac.adapter.ClassResource;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.util.DeclFactory;

@Utility
public final class SkeletonJavaRefUtility {

  private static final Map<IRNode, JavaRefSkeletonBuilder> nodeToSkeleton = new HashMap<IRNode, JavaRefSkeletonBuilder>();

  public static void registerSourceLocation(DeclFactory factory, IRNode node, FileResource fileResource, int lineNumber, int offset,
      int length) {
    final JavaRefSourceBuilder b = new JavaRefSourceBuilder(factory, fileResource, lineNumber, offset, length);
    nodeToSkeleton.put(node, b);
  }

  public static void registerBinaryCode(DeclFactory factory, IRNode node, ClassResource resource, int lineNumber) {
    final JavaRefBinaryBuilder b = new JavaRefBinaryBuilder(factory, resource, lineNumber);
    nodeToSkeleton.put(node, b);
  }

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
  static IFluidJavaRef buildOrNullOnFailure(IRNode node) {
    final JavaRefSkeletonBuilder sb = nodeToSkeleton.get(node);
    if (sb != null) {
      nodeToSkeleton.remove(node);
      return sb.buildOrNullOnFailure(node);
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
    return nodeToSkeleton.containsKey(node) || JavaNode.hasFluidJavaRef(node);
  }

  interface JavaRefSkeletonBuilder {
    IFluidJavaRef buildOrNullOnFailure(@NonNull IRNode node);
  }

  static final class JavaRefBinaryBuilder implements JavaRefSkeletonBuilder {
    private final DeclFactory f_factory;
    private final int f_lineNumber;
    private final ClassResource f_resource;

    private JavaRefBinaryBuilder(DeclFactory f, ClassResource resource, int lineNumber) {
      f_factory = f;
      f_resource = resource;
      f_lineNumber = lineNumber;
    }

    public IFluidJavaRef buildOrNullOnFailure(@NonNull IRNode node) {
      final Pair<IDecl, IDecl.Position> pair = f_factory.getDeclAndPosition(node);
      if (pair == null) {
        SLLogger.getLogger().warning(I18N.err(289, DebugUnparser.unparseCode(node), new Exception()));
        return null;
      }
      final FluidJavaRef.Builder b = new FluidJavaRef.Builder(pair.first());
      b.setIsOnDeclaration(pair.second() == IDecl.Position.ON_DECL);
      b.setLineNumber(f_lineNumber);
      b.setEclipseProjectName(f_resource.getProjectName());
      final String jarRelativePath = f_resource.getJarRelativePath();
      b.setWithin(jarRelativePath == null ? Within.CLASS_FILE : Within.JAR_FILE);
      b.setWorkspaceRelativePath(f_resource.getWorkspaceRelativePath());
      b.setJarRelativePath(jarRelativePath);
      return b.buildOrNullOnFailure();
    }
  }

  static final class JavaRefSourceBuilder implements JavaRefSkeletonBuilder {

    private final DeclFactory f_factory;
    private final int f_lineNumber;
    private final int f_offset;
    private final int f_length;
    private final FileResource f_fileResource;

    private JavaRefSourceBuilder(DeclFactory f, FileResource fileResource, int lineNumber, int offset, int length) {
      f_factory = f;
      f_fileResource = fileResource;
      f_lineNumber = lineNumber;
      f_offset = offset;
      f_length = length;
    }

    public IFluidJavaRef buildOrNullOnFailure(@NonNull IRNode node) {
      final Pair<IDecl, IDecl.Position> pair = f_factory.getDeclAndPosition(node);
      if (pair == null) {
        SLLogger.getLogger().warning(I18N.err(289, DebugUnparser.unparseCode(node), new Exception()));
        return null;
      }
      final FluidJavaRef.Builder b = new FluidJavaRef.Builder(pair.first());
      b.setIsOnDeclaration(pair.second() == IDecl.Position.ON_DECL);
      b.setLineNumber(f_lineNumber);
      b.setOffset(f_offset);
      b.setLength(f_length);
      b.setEclipseProjectName(f_fileResource.getProjectName());
      b.setWithin(Within.JAVA_FILE);
      b.setWorkspaceRelativePath(f_fileResource.getRelativePath());
      return b.buildOrNullOnFailure();
    }
  }

  private SkeletonJavaRefUtility() {
    // no instances
  }
}
