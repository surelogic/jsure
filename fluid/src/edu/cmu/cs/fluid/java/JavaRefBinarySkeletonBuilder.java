package edu.cmu.cs.fluid.java;

import java.util.HashMap;
import java.util.Map;

import com.surelogic.common.Pair;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef.Within;
import com.surelogic.javac.adapter.ClassResource;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.util.DeclFactory;

public final class JavaRefBinarySkeletonBuilder {

  private static final Map<IRNode, JavaRefBinarySkeletonBuilder> nodeToSkeleton = new HashMap<IRNode, JavaRefBinarySkeletonBuilder>();

  public static void register(DeclFactory factory, IRNode node, ClassResource resource, int lineNumber) {
    final JavaRefBinarySkeletonBuilder b = new JavaRefBinarySkeletonBuilder(factory, resource, lineNumber);
    nodeToSkeleton.put(node, b);
  }

  /**
   * 
   * @param node
   * @return {@code true} if registered, {@code false} otherwise.
   */
  public static boolean hasRegistered(IRNode node) {
    return nodeToSkeleton.containsKey(node);
  }

  /**
   * 
   * @param from
   * @param to
   * @return {@code true} if the copy succeeded.
   */
  public static boolean copyRegistrationIfPossible(IRNode from, IRNode to) {
    final JavaRefBinarySkeletonBuilder b = nodeToSkeleton.get(from);
    if (b != null) {
      nodeToSkeleton.put(to, b);
      return true;
    }
    return false;
  }

  private final DeclFactory f_factory;
  private final int f_lineNumber;
  private final ClassResource f_resource;

  private JavaRefBinarySkeletonBuilder(DeclFactory f, ClassResource resource, int lineNumber) {
    f_factory = f;
    f_resource = resource;
    f_lineNumber = lineNumber;
  }

  public static IFluidJavaRef buildOrNullOnFailure(IRNode node) {
    final JavaRefBinarySkeletonBuilder sb = nodeToSkeleton.get(node);
    if (sb == null)
      return null;
    final Pair<IDecl, IDecl.Position> pair = sb.f_factory.getDeclAndPosition(node);
    if (pair == null)
      return null;
    final FluidJavaRef.Builder b = new FluidJavaRef.Builder(pair.first());
    b.setIsOnDeclaration(pair.second() == IDecl.Position.ON_DECL);
    b.setLineNumber(sb.f_lineNumber);
    b.setEclipseProjectName(sb.f_resource.getProjectName());
    b.setWithin(Within.JAVA_FILE);
    b.setWorkspaceRelativePath(sb.f_resource.getWorkspaceRelativePath());
    b.setJarRelativePath(sb.f_resource.getJarRelativePath());
    return b.buildOrNullOnFailure();
  }
}
