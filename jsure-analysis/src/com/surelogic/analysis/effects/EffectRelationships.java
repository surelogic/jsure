package com.surelogic.analysis.effects;

/**
 * Enumeration containing values describing relationships between two effects.
 */
public enum EffectRelationships
{
  /**
   * Element indicating that the two effects do not conflict.
   */
  NO_CONFLICT("No conflict"),
    
  /**
   * Element indicating that the effects conflict because both are 
   * write effects and they have overlapping targets.
   */
  WRITE_INCLUDES_WRITE("Write includes write"),
    
  /**
   * Element indicating that the effects conflict because the first
   * effect is a read and the second effect is a write, and they have
   * overlapping targets.
   */
  WRITE_B_INCLUDES_READ_A("Write B includes read A"),
    
  /**
   * Element indicating that the effects conflict because the second
   * effect is a read and the first effect is a write, and they have
   * overlapping targets.
   */
  WRITE_A_INCLUDES_READ_B("Write A includes read B");
  
      
      
  private final String id;
  
  private EffectRelationships(final String id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return id;
  }  
}
