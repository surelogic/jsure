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
 * Declares that the state referenced by this field is thread confined. By
 * <i>thread confined</i> we mean the state referenced by the field is
 * <ul>
 * <li>mutable,</li>
 * <li>accessed (read and written) by one and only one thread, <i>t1</i>,</li>
 * <li>until it is safely transferred to another thread, <i>t2</i>,</li>
 * <li>and so on.</li>
 * </ul>
 * State is <i>safely transferred</i> from <i>t1</i> to <i>t2</i> when a
 * <i>happens-before</i> exists between the last access in <i>t1</i> and the
 * first access in <i>t2</i>.
 * <p>
 * This annotation is trusted, i.e., it is <em>not verified</em>. Its use is for
 * documentation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ThreadConfined {
  // Marker annotation
}
