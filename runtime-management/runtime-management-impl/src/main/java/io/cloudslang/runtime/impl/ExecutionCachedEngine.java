/*******************************************************************************
 * (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0 which accompany this distribution.
 *
 * The Apache License is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/

package io.cloudslang.runtime.impl;

import io.cloudslang.dependency.api.services.DependencyService;

import org.apache.log4j.Logger;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Genadi Rabinovich, genadi@hpe.com on 05/05/2016.
 */
public abstract class ExecutionCachedEngine<T extends Executor> extends ExecutionEngine {
    private final Logger logger = Logger.getLogger(getClass());
    // key --> dependencies concatenated
    // value --> classloader/pythoninterpreter which was build with classpath from these dependencies
    // if we reached the limit of cache we will release the least recently used
    private final Map<String, T> executors = new LinkedHashMap<>();

    private final Lock lock = new ReentrantLock();

    public T allocateExecutor(Set<String> dependencies) {
        String dependenciesKey = generatedDependenciesKey(dependencies);

        T executor;
        T candidateForRemove = null;
        lock.lock();
        try {
            executor = executors.get(dependenciesKey);
            if (executor == null) {
                int cacheSize = getCacheSize();
                if (executors.size() == cacheSize) {
                    logger.info("Reached cached executors limit[" + cacheSize + "], replacing LRU executor.");
                    Iterator<Map.Entry<String, T>> iterator = executors.entrySet().iterator();
                    candidateForRemove = iterator.next().getValue();
                    iterator.remove();
                }
                executor = createNewExecutor(getDependencyService().getDependencies(dependencies));
            } else {
                // remove it and place at the end - most recently used
                executors.remove(dependenciesKey);
            }
            executor.allocate();
            executors.put(dependenciesKey, executor);
        } finally {
            lock.unlock();
        }
        if(candidateForRemove != null) {
            candidateForRemove.close();
        }
        return executor;
    }

    protected void releaseExecutor(T executor) {
        executor.release();
    }

    protected abstract DependencyService getDependencyService();
    protected abstract int getCacheSize();
    protected abstract T createNewExecutor(Set<String> filePaths);
}
