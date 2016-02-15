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
 * Declares that the parameter, local variable, return value, or field to which
 * this annotation is applied is a reference to an object. In particular, under
 * most circumstances, its value can never be seen to be <code>null</code>.
 * <p>
 * When applied to a method declaration, this annotation means that the method's
 * return value will never be <code>null</code>. There is no need to 
 * explicitly differentiate between the return value and the receiver using a
 * <code>value</code> attribute, as we do, for example, with {@link Initialized},
 * because the receiver is always non-<code>null</code>.
 * <p>
 * This annotation can be set as the <i>default</i> for a type or package using
 * the {@link Promise} annotation. How to do this is shown in the examples
 * below.
 * <p>
 * The method {@link Cast#toNonNull(Object)} must be used to assign a partially
 * initialized object&mdash;one annotated with {@link Initialized}&mdash;to a
 * {@link NonNull} reference. While this should be avoided, an example is shown
 * below.
 * <p>
 * When this annotation is applied to a field, the enclosing class is required
 * to be annotated {@link TrackPartiallyInitialized}. In fact, all parents of
 * that class up to, but <em>not including</em> {@link Object}, are required to be annotated
 * {@link TrackPartiallyInitialized}. This ensures that object initialization
 * and, in particular, any methods called during initialization properly
 * handle the case that a {@link NonNull} field may actually be <code>null</code>
 * because it has not been initialzed yet.
 *  
 * <h3>Semantics:</h3>
 * 
 * <i>Local variable:</i> The local variable never has the value
 * <code>null</code>.
 * <p>
 * 
 * <i>Parameter:</i> At the start of the method's execution, the annotated
 * parameter must refer to a fully initialized object&mdash;it cannot be
 * <code>null</code>.
 * <p>
 * <i>Return Value:</i> The value returned by the annotated method is never
 * <code>null</code>.
 * <p>
 * <i>Field:</i> The field may never be assigned the value <code>null</code>.
 * The field may be observed to be <code>null</code> if the object is not fully
 * initialized. Specifically, if the field is declared in class <code>C</code>,
 * and a constructor for <code>C</code> has not finished executing over the
 * object, then the field may be seen to be <code>null</code>. The object must
 * be referenced by a <em>partially initialized</em> reference for this to be
 * observed, however. See {@link Initialized} for an example of this.
 * 
 * <h3>Examples:</h3>
 * 
 * Here the <code>Rectangle</code> class has two non-<code>null</code>
 * references, one to each point that defines it. The first constructor
 * initializes the fields from {@link NonNull} parameters. The second
 * constructor initializes the fields to newly constructed&mdash;and thus non-
 * <code>null</code>&mdash;<code>Point</code> objects.
 * 
 * <p>
 * The two getter methods declare that they return non-<code>null</code>
 * references.
 * 
 * <pre>
 * &#064;TrackPartiallyInitialized
 * class Rectangle {
 *   &#064;NonNull
 *   private final Point topLeft;
 *   &#064;NonNull
 *   private final Point bottomRight;
 * 
 *   public Rectangle(@NonNull Point tl, @NonNull Point br) {
 *     topLeft = tl;
 *     bottomRight = br;
 *   }
 * 
 *   public Rectangle(int x, int y, int w, int h) {
 *     topLeft = new Point(x, y);
 *     bottomRight = new Point(x + w, y + h);
 *   }
 * 
 *   &#064;NonNull
 *   public Point getTopLeft() {
 *     return topLeft;
 *   }
 * 
 *   &#064;NonNull
 *   public Point getBottomRight() {
 *     return bottomRight;
 *   }
 * }
 * </pre>
 * 
 * <p>
 * Here we can safely dereference both parameters of the method because both are
 * annotated as <code>&#064;NonNull</code>:
 * 
 * <pre>
 * public int totalLength(@NonNull String s1, @NonNull String s2) {
 *   return s1.length() + s2.length();
 * }
 * </pre>
 * 
 * Here is an example of <i>changing the default</i> of all references in a
 * package to <code>&#064;NonNull</code>. In the file
 * <tt>com/stuff/package-info.java</tt>
 * 
 * <pre>
 * &#064;Promises({ @Promise(&quot;@NonNull&quot;), @Promise(&quot;@TrackPartiallyInitialized&quot;) })
 * package com.stuff;
 * 
 * import com.surelogic.*;
 * </pre>
 * 
 * This makes all declarations in the package <code>&#064;NonNull</code> and
 * also automatically places required
 * <code>&#064;TrackPartiallyInitialized</code> annotations on type. You can
 * specifically annotate a particular declaration to make it
 * <code>&#064;Nullable</code> or <code>&#064;Initialized</code> such as is done
 * on the <code>getDestinationNameOrNull</code> method in the listing below.
 * 
 * <pre>
 * package com.stuff;
 * 
 * public class Flight {
 *   ...
 *   &#064;Nullable
 *   public String getDestinationNameOrNull() { ... }
 * }
 * </pre>
 * 
 * A second <code>&#064;Promise</code> annotation can be used to alter the
 * default for a particular type. In the <code>LegacyOldCode</code> class listed
 * below the default is changed back to <code>&#064;Nullable</code>
 * &mdash;overriding the <code>&#064;Promise</code> annotation
 * <code>package-info.java</code> file.
 * 
 * <pre>
 * package com.stuff;
 * 
 * &#064;Promise("&#064;Nullable")
 * public class LegacyOldCode {
 *   ...
 *   &#064;NonNull
 *   public String getLegacyValue() { ... }
 * }
 * </pre>
 * <p>
 * In the somewhat contrived example below the partially initialized object
 * referenced by the parameter <code>myPeer</code> is "cast" with
 * {@link Cast#toNonNull(Object)} and {@link Cast#toNullable(Object)},
 * respectively, so that it can be assigned to the non-null field
 * <code>peer1</code> and the nullable field <code>peer2</code>.
 * 
 * <pre>
 * &#064;ThreadSafe
 * &#064;TrackPartiallyInitialized
 * public class B {
 * 
 *   &#064;NonNull
 *   private final A peer1;
 * 
 *   &#064;Nullable
 *   private final A peer2;
 * 
 *   &#064;RegionEffects(&quot;none&quot;)
 *   &#064;Starts(&quot;nothing&quot;)
 *   B(@Initialized(through = &quot;Object&quot;) A myPeer) {
 *     peer1 = Cast.toNonNull(myPeer);
 *     peer2 = Cast.toNullable(myPeer);
 *   }
 * }
 * </pre>
 * 
 * A "cast" is required because the it is not deemed safe to assign a partially
 * initialized object to a field (annotated or unannotated).
 * 
 * @see TrackPartiallyInitialized
 * @see Initialized
 * @see Nullable
 * @see Promise
 * @see Vouch
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE })
public @interface NonNull {
  // marker interface
}
