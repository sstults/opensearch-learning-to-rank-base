/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.opensearch.ltr.stats;

import java.util.function.Supplier;

import org.opensearch.ltr.stats.suppliers.CounterSupplier;

/**
 * Class represents a stat the plugin keeps track of
 */
public class LTRStat<T> {
    private final boolean clusterLevel;
    private final Supplier<T> supplier;

    /**
     * Constructor
     *
     * @param clusterLevel whether the stat has clusterLevel scope or nodeLevel scope
     * @param supplier supplier that returns the stat's value
     */
    public LTRStat(Boolean clusterLevel, Supplier<T> supplier) {
        this.clusterLevel = clusterLevel;
        this.supplier = supplier;
    }

    /**
     * Determines whether the stat is cluster specific or node specific
     *
     * @return true is stat is cluster level; false otherwise
     */
    public Boolean isClusterLevel() {
        return clusterLevel;
    }

    /**
     * Get the value of the statistic
     *
     * @return T value of the stat
     */
    public T getValue() {
        return supplier.get();
    }

    /**
     * Increments the supplier if it can be incremented
     */
    public void increment() {
        if (!(supplier instanceof CounterSupplier)) {
            throw new UnsupportedOperationException("cannot increment the supplier: " + supplier.getClass().getName());
        }
        ((CounterSupplier) supplier).increment();
    }
}
