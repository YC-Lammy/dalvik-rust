/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.util.internal.function.pooled;

import java.util.function.Function;

/**
 * {@link Function} + {@link PooledLambda}
 *
 * @see PooledLambda
 * @hide
 */
public interface PooledFunction<A, R> extends PooledLambda, Function<A, R> {

    /**
     * Ignores the result
     */
    PooledConsumer<A> asConsumer();

    /** @inheritDoc */
    PooledFunction<A, R> recycleOnUse();
}