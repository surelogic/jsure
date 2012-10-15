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

    private String f_absolutePath = null;
    private String f_jarRelativePath = null;

    public Builder(IFluidJavaRef copy) {
      super(copy);
      f_absolutePath = copy.getAbsolutePathOrNull();
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
    public Builder setPositionRelativeToDeclaration(Position value) {
      super.setPositionRelativeToDeclaration(value);
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

    public Builder setAbsolutePath(String value) {
      f_absolutePath = value;
      return this;
    }

    public Builder setJarRelativePath(String value) {
      f_jarRelativePath = value;
      return this;
    }

    @Override
    public IFluidJavaRef build() {
      return new FluidJavaRef(f_within, f_declaration, f_positionRelativeToDeclaration, f_eclipseProjectName, f_lineNumber,
          f_offset, f_length, f_javaId, f_enclosingJavaId, f_absolutePath, f_jarRelativePath);
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

  protected FluidJavaRef(final @NonNull Within within, final @NonNull IDecl declaration,
      @NonNull Position positionRelativeToDeclaration, final @NonNull String eclipseProjectNameOrNull, final int lineNumber,
      final int offset, final int length, final @Nullable String javaIdOrNull, final @Nullable String enclosingJavaIdOrNull,
      final @Nullable String absolutePathOrNull, final @Nullable String jarRelativePathOrNull) {
    super(within, declaration, positionRelativeToDeclaration, eclipseProjectNameOrNull, lineNumber, offset, length, javaIdOrNull,
        enclosingJavaIdOrNull);
    f_absolutePath = absolutePathOrNull;
    f_jarRelativePath = jarRelativePathOrNull;
  }

  private final String f_absolutePath;
  private final String f_jarRelativePath;

  @Nullable
  public String getAbsolutePathOrNull() {
    return f_absolutePath;
  }

  @Nullable
  public String getJarRelativePathOrNull() {
    return f_jarRelativePath;
  }
}
