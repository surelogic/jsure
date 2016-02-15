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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a new abstract region of state for the class to which this
 * annotation is applied. To declare more than one region for a class use the
 * {@link Regions} annotation.
 * <p>
 * It is a modeling error for a class to have both a {@link Regions} and a
 * {@link Region} annotation.
 * <p>
 * A &#064;Region declaration along with corresponding &#064;InRegion
 * declarations provides a name for a subset of the fields of a class. This set
 * is extensible: subclasses may add both new abstract regions via &#064;Region
 * annotations, and new fields via {@link InRegion} annotations.
 * <p>
 * A region is either {@code static} or instance (non-{@code static}). A
 * {@code static} region may contain both {@code static} and instance regions
 * and fields. An instance region may only contain other instance regions and
 * fields.
 * <p>
 * Regions are hierarchical in the form of a tree:
 * <ul>
 * <li>The root is declared in {@link java.lang.Object} and is a {@code static}
 * region named {@code All}.
 * <li>The instance region {@code Instance} is a child of {@code All} and is
 * also declared in {@code java.lang.Object}.
 * <li>Every programmer-declared region has a parent: If the parent is not
 * explicitly declared in the &#064;Region annotation, then the parent is
 * {@code All} for {@code static} regions and {@code Instance} for instance
 * regions.
 * </ul>
 * <p>
 * The set of fields in a region <em>R</em> are those fields annotated with
 * <code>&#064;InRegion(&quot;<em>R</em>&quot;)</code> together with those
 * fields that are recursively members of the region's children.
 * 
 * There are three pre-defined regions available for use:
 * 
 * <dl>
 * <dt>All</dt>
 * <dd>A region representing the entire state of the program's heap.</dd>
 * <dt>Instance</dt>
 * <dd>A region representing the instance state of an object.</dd>
 * <dt>Static</dt>
 * <dd>A region representing the static state of a class.</dd>
 * </dl>
 * <p>
 * An abstract region may also be implicitly declared by an {@link InRegion}
 * annotation. When the region named by an {@link InRegion} annotation does not
 * exist in the class being annotated (or one of its ancestors), the region is
 * considered to be implicitly declared by the annotation. This allows
 * <code>InRegion</code> to be used without first declaring regions on the
 * class. The implicit region has the least visibility necessary to contain the
 * fields that are placed into it. The region is <code>static</code> if and only
 * if one of the fields placed into it is <code>static</code>. If
 * <code>static</code> the region's parent is <code>Static</code>, otherwise it
 * is <code>Instance</code>.
 * 
 * <h3>Semantics:</h3>
 * 
 * Declaration of a region does not constrain the implementation of the program,
 * it simply gives a name to part of the program's state.
 * <p>
 * At runtime, a region represents a subset of the JVM heap, subdividing one or
 * more objects in the heap. Let {@code C.f} denote field {@code f} declared in
 * class {@code C}. Let {@code R} be a {@code static} region. The set of fields
 * in {@code R} can be partitioned into the non-intersecting sets of
 * {@code static} and instance fields <em>S</em> and <em>I</em>, respectively.
 * The region {@code R} denotes the following subset of the heap:
 * <ul>
 * <li>The field {@code f} of class {@code C} for each {@code C.f} &isin;
 * <em>S</em>.
 * <li>The field {@code f} of each instance of class {@code X} where {@code C.f}
 * &isin; <em>I</em> and {@code X instanceof C}.
 * </ul>
 * <p>
 * The Region {@code All} contains all the fields, and thus denotes the entire
 * runtime heap.
 * <p>
 * The instance region {@code Q} declared in class {@code C} is instantiated at
 * runtime for each object of class {@code X} in the heap, where
 * {@code X instanceof C} in the heap. Specifically, for an object <em>o</em> of
 * type {@code X} such that {@code X instanceof C}, there is runtime region in
 * the heap consisting of the fields <em>o</em><code>.f<sub>1</sub></code>,
 * &hellip;, <em>o</em><code>.f<sub>n</sub></code> where
 * <code>f<sub>i</sub></code> is a member of {@code Q}.
 * 
 * <h3>Examples:</h3>
 * 
 * A private region, named {@code ObserverRegion}, that includes the field
 * {@code observers} and the contents of the referenced set.
 * 
 * <pre>
 * &#064;Region(&quot;private ObserverRegion&quot;)
 * class Observer {
 *   &#064;UniqueInRegion(&quot;ObserverRegion&quot;)
 *   private Set&lt;Callback&gt; observers = new HashSet&lt;Callback&gt;()
 *   ...
 * }
 * </pre>
 * 
 * The {@link UniqueInRegion} annotation is used to aggregate referenced state.
 * The {@link Unique} annotation also allows aggregation of state, but only into
 * the default {@code Instance} region.
 * <p>
 * A private region, named {@code FinalObserverRegion}, that includes the
 * contents of the set referenced by the {@code observers} field, but not the
 * field itself. The field is not included because it is declared {@code final}.
 * 
 * <pre>
 * &#064;Region(&quot;private FinalObserverRegion&quot;)
 * class Observer {
 *   &#064;UniqueInRegion(&quot;FinalObserverRegion&quot;)
 *   private final Set&lt;Callback&gt; observers = new HashSet&lt;Callback&gt;()
 *   ...
 * }
 * </pre>
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
 * 
 * class Player extends Thing {
 *   &#064;InRegion(&quot;ThingState&quot;)
 *   private long x, y;
 *   ...
 * }
 * </pre>
 * 
 * 
 * <p>
 * Below is an example of implicitly declaring an abstract region using an
 * <code>InRegion</code> annotation:
 * 
 * <pre>
 * // No explicit Region declarations 
 * public class C {
 *   &#064;InRegion(&quot;CState&quot;) private int f1;
 *   &#064;InRegion(&quot;CState&quot;) private int f2;
 *   &#064;InRegion(&quot;CState&quot;) private int f3;
 *   ...
 * }
 * </pre>
 *
 * <p>
 * The implied region <code>CState</code> will be <code>private</code> because
 * that is the least visibility necessary to contain private fields. So the
 * above is equivalent to
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
 * <p>
 * Which style is preferred is a matter of programmer preference.
 *
 * <p>
 * This is an example of a static region declared in a simple counter utility
 * class.
 * 
 * <pre>
 * package com.surelogic;
 * 
 * import com.surelogic.*;
 * 
 * &#064;Utility
 * &#064;Region(&quot;static public CounterState&quot;)
 * public class CounterUtility {
 * 
 *   &#064;InRegion(&quot;CounterState&quot;)
 *   private static long count = 0;
 * 
 *   &#064;RegionEffects(&quot;writes CounterState&quot;)
 *   public static long incrementAndGet() {
 *     return ++count;
 *   }
 * 
 *   &#064;RegionEffects(&quot;reads CounterState&quot;)
 *   public static long get() {
 *     return count;
 *   }
 * 
 *   private CounterUtility() {
 *     // no instances
 *   }
 * }
 * 
 * </pre>
 * 
 * Here is some client code of the counter class referencing the static
 * <tt>CounterState</tt> region.
 * 
 * <pre>
 * package com.surelogic;
 * 
 * import com.surelogic.RegionEffects;
 * 
 * public class ClientCode {
 * 
 *   &#064;RegionEffects(&quot;writes com.surelogic.CounterUtility:CounterState&quot;)
 *   public static void main(String[] args) {
 *     long counter = CounterUtility.incrementAndGet();
 *   }
 * 
 * }
 * </pre>
 * 
 * @see InRegion
 * @see Regions
 * @see Unique
 * @see UniqueInRegion
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(Regions.class)
public @interface Region {
  /**
   * The value of this attribute must conform to the following grammar (in <a
   * href="http://www.ietf.org/rfc/rfc4234.txt">Augmented Backus&ndash;Naur
   * Form</a>):
   * 
   * <pre>
   * value = accessModifiers IDENTIFIER [&quot;extends&quot; regionSpecification]
   *   
   * accessModifiers = [&quot;public&quot; / &quot;protected&quot; / &quot;private&quot;] [static]
   * 
   * regionSpecification = simpleRegionSpecificaion / qualifiedRegionName
   * 
   * simpleRegionSpecification = IDENTIFIER                         ; Region of the class being annotated
   * 
   * qualifedRegionName = IDENTIFIER *(&quot;.&quot; IDENTIFIER) : IDENTIFER  ; Static region from the named, optionally qualified, class
   * 
   * IDENTIFIER = Legal Java Identifier
   * </pre>
   * 
   * <p>
   * As in Java, if neither {@code public}, {@code protected}, or
   * {@code private} is declared then the region has default visibility; if
   * {@code static} is not declared the region is an instance region.
   * 
   * <p>
   * If no explicit "extends" clause is provided the region extends from region
   * {@code Instance} if it is an instance region, or {@code All} if it is a
   * static region.
   * 
   * @return a value following the syntax described above.
   */
  String value();
}
