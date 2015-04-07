/*
 * Copyright (c) 2015 SureLogic, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that the field to which this annotation is applied is mapped into
 * the named region.
 * <p>
 * When annotated on a type this annotation can be used to map several fields
 * into a named region. This can be more concise than multiple {@link InRegion}
 * annotations on the individual fields, this conciseness comes at the price of
 * moving information about particular fields away from their declarations.
 * <p>
 * Which style is preferred, annotation on fields or on types, is a matter of
 * programmer preference.
 * <p>
 * When the region named by this annotation does not exist in the class being
 * annotated (or one of its ancestors), the region is considered to be
 * implicitly declared by the annotation. This allows <code>InRegion</code> to
 * be used without first declaring regions on the class. The implicit region has
 * the least visibility necessary to contain the fields that are placed into it.
 * The region is <code>static</code> if and only if one of the fields placed
 * into it is <code>static</code>. If <code>static</code> the region's parent is
 * <code>Static</code>, otherwise it is <code>Instance</code>.
 * 
 * <h3>Semantics:</h3>
 * 
 * Definition of a region does not constrain the implementation of the program,
 * it simply gives a name to part of the program's state.
 * 
 * <h3>Examples:</h3>
 * 
 * A region, named {@code AircraftState}, that contains three {@code long}
 * fields use to represent the position of the object.
 * 
 * <pre>
 * &#064;Region(&quot;private AircraftState&quot;)
 * public class Aircraft {
 * 
 *   &#064;InRegion(&quot;AircraftState&quot;)
 *   private long x, y;
 *   
 *   &#064;InRegion(&quot;AircraftState&quot;)
 *   private long altitude;
 *   ...
 * }
 * </pre>
 * 
 * A region, named {@code ThingState}, that contains two {@code long} fields use
 * to represent the position of a subclass. {@code ThingState} is empty in the
 * parent class {@code Thing} but has state added into it in the subclass
 * {@code Player}.
 * 
 * <pre>
 * &#064;Region(&quot;protected ThingState&quot;)
 * class Thing {
 *   ...
 * }
 * </pre>
 * 
 * <pre>
 * class Player extends Thing {
 * 
 *   &#064;InRegion(&quot;ThingState&quot;)
 *   private long x, y;
 *   ...
 * }
 * </pre>
 * 
 * The following example uses an {@link InRegion} annotation on the class
 * declaration for {@code C} to map the fields {@code f1}, {@code f2}, and
 * {@code f3} into the region {@code CState}.
 * 
 * <pre>
 * &#064;Region(&quot;private CState&quot;)
 * &#064;InRegion(&quot;f1, f2, f3 into CState&quot;)
 * public class C {
 *   private int f1;
 *   private int f2;
 *   private int f3;
 *   ...
 * }
 * </pre>
 * 
 * The above example is equivalent to the following use of {@code InRegion} on
 * field declarations.
 * 
 * <pre>
 * &#064;Region(&quot;private CState&quot;)
 * public class C {
 *   &#064;InRegion(&quot;CState&quot;) private int f1;
 *   &#064;InRegion(&quot;CState&quot;) private int f2;
 *   &#064;InRegion(&quot;CState&quot;) private int f3;
 *   ...
 * }
 * </pre>
 * 
 * Which style is preferred is a matter of programmer preference. In fact, in
 * this case the <code>InRegion</code> annotation can be left out altogether.
 * The region will be implicitly declared by the existence of the
 * <code>InRegion</code> annotations:
 * 
 * <pre>
 * public class C {
 *   &#064;InRegion(&quot;CState&quot;) private int f1;
 *   &#064;InRegion(&quot;CState&quot;) private int f2;
 *   &#064;InRegion(&quot;CState&quot;) private int f3;
 *   ...
 * }
 * </pre>
 * 
 * The implied region <code>CState</code> will be <code>private</code> because
 * that is the least visibility necessary to contain private fields. If you need
 * the region to be more visible, then it would have to be explicitly declared:
 * 
 * <pre>
 * &#064;Region(&quot;protected CState&quot;)
 * public class C {
 *   &#064;InRegion(&quot;CState&quot;) private int f1;
 *   &#064;InRegion(&quot;CState&quot;) private int f2;
 *   &#064;InRegion(&quot;CState&quot;) private int f3;
 *   ...
 * }
 * </pre>
 *
 * <p>
 * To apply more than one {@link InRegion} annotation to a class use the
 * {@link InRegions} annotation. It is here that implicitly declared regions
 * have an advantage. Compare
 * 
 * <pre>
 * &#064;Regions({
 *   &#064;Region(&quot;protected Location&quot;),
 *   &#064;Region(&quot;protected Size&quot;)})
 * public class Rectangle {
 *   &#064;InRegion(&quot;Location&quot;) protected int x;
 *   &#064;InRegion(&quot;Location&quot;) protected int y;
 *   &#064;InRegion(&quot;Size&quot;) protected int width;
 *   &#064;InRegion(&quot;Size&quot;) protected int height;
 *   ...
 * }
 * </pre>
 * 
 * to the equivalent, but less wordy
 * 
 * <pre>
 * public class Rectangle {
 *   &#064;InRegion(&quot;Location&quot;) protected int x;
 *   &#064;InRegion(&quot;Location&quot;) protected int y;
 *   &#064;InRegion(&quot;Size&quot;) protected int width;
 *   &#064;InRegion(&quot;Size&quot;) protected int height;
 *   ...
 * }
 * </pre>
 *
 * <p>
 * It is a modeling error for a class to have both a {@link InRegion} and a
 * {@link InRegions} annotation. It is a modeling error for a field that is
 * named in an {@code InRegion} annotation on a class declaration to also have
 * an {@code InRegion} annotation on its field declaration. That is, the below
 * class has a modeling error:
 * 
 * <pre>
 * &#064;Region(&quot;private Appearance&quot;)
 * &#064;InRegion(&quot;color into Appearance&quot;)
 * public class Sprite {
 *   &#064;InRegion(&quot;Appearance&quot;) private int color;
 *   ...
 * }
 * </pre>
 * 
 * @see InRegions
 * @see Unique
 * @see UniqueInRegion
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD })
public @interface InRegion {
  /**
   * On a field the value of this attribute indicates the abstract region that
   * is the superregion of the annotated field. The value of this attribute must
   * conform to the following grammar (in <a
   * href="http://www.ietf.org/rfc/rfc4234.txt">Augmented Backus&ndash;Naur
   * Form</a>):
   * 
   * <pre>
   * value = regionSpecification
   * 
   * regionSpecification = simpleRegionSpecificaion / qualifiedRegionName
   * 
   * simpleRegionSpecification = IDENTIFIER                          ; Region of the class being annotated
   * 
   * qualifedRegionName =  IDENTIFIER *(&quot;.&quot; IDENTIFIER) : IDENTIFER  ; Static region from the named, optionally qualified, class
   * 
   * IDENTIFIER = Legal Java Identifier
   * </pre>
   * 
   * On a type the value of this attribute lists a set of fields that are mapped
   * into a region. The value of this attribute must conform to the following
   * grammar (in <a href="http://www.ietf.org/rfc/rfc4234.txt">Augmented
   * Backus&ndash;Naur Form</a>):
   * 
   * <pre>
   * value = fieldList &quot;into&quot; regionName
   * 
   * fieldList = IDENTIFIER *("," IDENTIFIER)
   * 
   * regionName = IDENTIFIER
   * 
   * IDENTIFIER = Legal Java Identifier
   * </pre>
   * 
   * @return a value following the syntax described above.
   */
  String value();
}
