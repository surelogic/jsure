package testBinder.nestedAnnoEnum;

import java.lang.annotation.Documented;

/**
 * Annotation to inform users of a package, class or method's intended audience.
 */
public class InterfaceAudience {
  /**
   * Intended for use by any project or application.
   */
  @Documented public @interface Public {};
  
  /**
   * Intended only for the project(s) specified in the annotation
   */
  @Documented public @interface LimitedPrivate {
    public enum Project {COMMON, AVRO, CHUKWA, HBASE, HDFS, 
                         HIVE, MAPREDUCE, PIG, ZOOKEEPER};
    
    Project[] value();
  };
  
  /**
   * Intended for use only within Hadoop itself.
   */
  @Documented public @interface Private {};

  private InterfaceAudience() {} // Audience can't exist on its own
}
