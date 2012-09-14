package com.surelogic.opgen.syntax;

/**
 *  Known syntax properties
 */
public enum KnownProperty implements Property {
  ID("id", "Computes the value of the 'id' attribute"),
  BINDS_TO("bindsTo", "Binds this name to I%sBinding"), 
  BINDS_TO_TYPE("bindsToType", "Binds to the type represented by I%sBinding"), 
  BINDING("binding", ""),          // Node also represents its own IFooBinding
  TYPE_BINDING("typeBinding", ""), // Node also represents its own IFooTypeBinding
  LOGICALLY_INVISIBLE("logicallyInvisible", "Is logically invisible (e.g., skipped over in favor of its children or parent)"),
  NONNULL_VARIANTS("nonnullVariants", "The sub-operator(s) which are translated as non-null"), 
  NULL_VARIANT("nullVariant", "Is a null variant of its super-operator"),
  NONCANONICAL("noncanonical", "Not valid Java code.  Placeholder until canonicalized."),
  NO_IFACE("noIface", "Do not generate any interface or implementations. Only to be generated as an operator.  Implies noImpl"),
  NO_IMPL("noImpl", "Do not generate any implementations. Only to be generated as an operator and interface"),
  EXTENDABLE("extendable", "Is extendable across node extensions (e.g. by promises)"),
  BRIDGES_TO("bridgesTo", "Is the root of its AST, but can bridge to an AST of type %s"),
  ARGS("args", "Synthesizes a method to compute the arguments");
  
  private final String val;
  private final String msg;
  
  KnownProperty(String v, String m) {
    val = v;
    msg = m;
  }
  static KnownProperty get(String v) {
    for (KnownProperty p : KnownProperty.values()) {
      if (p.val.equals(v)) {
        return p;
      }
    }
    return null;
  }
  @Override
  public String getMessage() {
    return msg;
  }
  @Override
  public String getName() {
    return val;
  }
}