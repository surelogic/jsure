package edu.cmu.cs.fluid.java;

import com.surelogic.Immutable;
import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.JavaRef;

import edu.cmu.cs.fluid.ir.IRObjectType;

/**
 * Provides an implementation for code references within the JSure analysis and
 * infrastructure. This class implements {@link IFluidJavaRef} with items that
 * <b>do not</b> need to be transferred to the IR-free drop-sea and the Eclipse
 * user interface.
 */
@Immutable
public final class FluidJavaRef extends JavaRef implements IFluidJavaRef {

  static final IRObjectType<IFluidJavaRef> FLUID_JAVA_REF_SLOT_TYPE = new IRObjectType<IFluidJavaRef>();
  static final String FLUID_JAVA_REF_SLOT_NAME = "JavaNode.FluidJavaRef";

  /**
   * Builder for {@link IFluidJavaRef} instances. Copy-and-modify is supported
   * via {@link Builder#Builder(IFluidJavaRef)}.
   * <p>
   * The default values are listed in the table below.
   * <p>
   * <table border=1>
   * <tr>
   * <th>Method</th>
   * <th>Description</th>
   * <th>Default</th>
   * </tr>
   * <tr>
   * <td>{@link #setWorkspaceRelativePath(String)}</td>
   * <td>a path relative to the Eclipse workspace root that the code reference
   * is within</td>
   * <td>{@code null}</td>
   * </tr>
   * </table>
   */
  public static final class Builder extends JavaRef.Builder {

    private String f_workspaceRelativePath = null;
    private String f_jarRelativePath = null;

    public Builder(IFluidJavaRef copy) {
      super(copy);
      f_workspaceRelativePath = copy.getWorkspaceRelativePathOrNull();
      f_jarRelativePath = copy.getJarRelativePathOrNull();
    }

    public Builder(@NonNull IDecl declaration) {
      super(declaration);
    }

    @Override
    public Builder setWithin(Within value) {
      super.setWithin(value);
      return this;
    }

    @Override
    public Builder setDeclaration(IDecl value) {
      super.setDeclaration(value);
      return this;
    }

    @Override
    public Builder setIsOnDeclaration(boolean value) {
      super.setIsOnDeclaration(value);
      return this;
    }

    @Override
    public Builder setEclipseProjectName(String value) {
      super.setEclipseProjectName(value);
      return this;
    }

    @Override
    public Builder setLineNumber(int value) {
      super.setLineNumber(value);
      return this;
    }

    @Override
    public Builder setOffset(int value) {
      super.setOffset(value);
      return this;
    }

    @Override
    public Builder setLength(int value) {
      super.setLength(value);
      return this;
    }

    @Override
    public Builder setJavaId(String value) {
      super.setJavaId(value);
      return this;
    }

    @Override
    public Builder setEnclosingJavaId(String value) {
      super.setEnclosingJavaId(value);
      return this;
    }

    public Builder setWorkspaceRelativePath(String value) {
      f_workspaceRelativePath = value;
      return this;
    }

    public Builder setJarRelativePath(String value) {
      f_jarRelativePath = value;
      return this;
    }

    @Override
    public IFluidJavaRef build() {
      return new FluidJavaRef(f_within, f_declaration, f_isOnDeclaration, f_eclipseProjectName, f_lineNumber, f_offset, f_length,
          f_javaId, f_enclosingJavaId, f_workspaceRelativePath, f_jarRelativePath);
    }

    @Override
    public IFluidJavaRef buildOrNullOnFailure() {
      try {
        return build();
      } catch (Exception ignore) {
        // ignore
      }
      return null;
    }

  }

  protected FluidJavaRef(final @NonNull Within within, final @NonNull IDecl declaration, boolean isOnDeclaration,
      final @NonNull String eclipseProjectNameOrNull, final int lineNumber, final int offset, final int length,
      final @Nullable String javaIdOrNull, final @Nullable String enclosingJavaIdOrNull,
      final @Nullable String workspaceRelativePathOrNull, final @Nullable String jarRelativePathOrNull) {
    super(within, declaration, isOnDeclaration, eclipseProjectNameOrNull, lineNumber, offset, length, javaIdOrNull,
        enclosingJavaIdOrNull);
    f_workspaceRelativePath = workspaceRelativePathOrNull;
    f_jarRelativePath = jarRelativePathOrNull;
  }

  private final String f_workspaceRelativePath;
  private final String f_jarRelativePath;

  @Nullable
  public String getWorkspaceRelativePathOrNull() {
    return f_workspaceRelativePath;
  }

  @Nullable
  public String getJarRelativePathOrNull() {
    return f_jarRelativePath;
  }
}
