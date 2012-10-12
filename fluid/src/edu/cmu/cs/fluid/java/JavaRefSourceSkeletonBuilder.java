package edu.cmu.cs.fluid.java;

import java.util.HashMap;
import java.util.Map;

import com.surelogic.common.Pair;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef.Within;
import com.surelogic.javac.FileResource;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.util.DeclFactory;

public final class JavaRefSourceSkeletonBuilder {

  static final Map<IRNode, JavaRefSourceSkeletonBuilder> nodeToSkeleton = new HashMap<IRNode, JavaRefSourceSkeletonBuilder>();

  public static void register(DeclFactory factory, IRNode node, 
		  FileResource fileResource, int lineNumber, int offset, int length) {
    final JavaRefSourceSkeletonBuilder b = new JavaRefSourceSkeletonBuilder(factory, fileResource, lineNumber, offset, length);
    nodeToSkeleton.put(node, b);
  }
  private final DeclFactory f_factory;
  private final int f_lineNumber;
  private final int f_offset;
  private final int f_length;
  private final FileResource f_fileResource;

  private JavaRefSourceSkeletonBuilder(DeclFactory f, FileResource fileResource, int lineNumber, int offset, int length) {
	f_factory = f;
    f_fileResource = fileResource;
    f_lineNumber = lineNumber;
    f_offset = offset;
    f_length = length;
  }

  public static IFluidJavaRef buildOrNullOnFailure(IRNode node) {
    final JavaRefSourceSkeletonBuilder sb = nodeToSkeleton.get(node);
    if (sb == null)
      return null;
    final Pair<IDecl, IDecl.Position> pair = sb.f_factory.getDeclAndPosition(node);
    if (pair == null)
      return null;
    final FluidJavaRef.Builder b = new FluidJavaRef.Builder(pair.first());
    b.setIsOnDeclaration(pair.second() == IDecl.Position.ON_DECL);
    b.setLineNumber(sb.f_lineNumber);
    b.setOffset(sb.f_offset);
    b.setLength(sb.f_length);
    b.setEclipseProjectName(sb.f_fileResource.getProjectName());
    b.setWithin(Within.JAVA_FILE);
    b.setWorkspaceRelativePath(sb.f_fileResource.getRelativePath());
    return b.buildOrNullOnFailure();
  }
}
