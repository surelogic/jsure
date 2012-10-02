package edu.cmu.cs.fluid.java;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.JavaRef;

public final class FluidJavaRef extends JavaRef implements IFluidJavaRef {

  public static final class Builder extends JavaRef.Builder {

    public Builder(IFluidJavaRef copy) {
      super(copy);
    }

    public Builder(@NonNull String typeNameFullyQualifiedSureLogic) {
      super(typeNameFullyQualifiedSureLogic);
    }

    @Override
    public Builder setWithin(Within value) {
      super.setWithin(value);
      return this;
    }

    @Override
    public Builder setTypeType(TypeType value) {
      super.setTypeType(value);
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

    @Override
    public IFluidJavaRef build() {
      return new FluidJavaRef(f_within, f_typeNameFullyQualifiedSureLogic, f_typeType, f_eclipseProjectName, f_lineNumber,
          f_offset, f_length, f_javaId, f_enclosingJavaId);
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

  protected FluidJavaRef(final @NonNull Within within, final @NonNull String typeNameFullyQualifiedSureLogic,
      final @Nullable TypeType typeTypeOrNullifUnknown, final @Nullable String eclipseProjectNameOrNullIfUnknown,
      final int lineNumber, final int offset, final int length, final @Nullable String javaIdOrNull,
      final @Nullable String enclosingJavaIdOrNull) {
    super(within, typeNameFullyQualifiedSureLogic, typeTypeOrNullifUnknown, eclipseProjectNameOrNullIfUnknown, lineNumber, offset,
        length, javaIdOrNull, enclosingJavaIdOrNull);
  }
}
