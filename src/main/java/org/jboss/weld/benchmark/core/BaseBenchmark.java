/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.benchmark.core;

import static org.jboss.weld.benchmark.core.Configuration.BATCH_SIZE_NORMAL;
import static org.jboss.weld.benchmark.core.Configuration.FORKS;
import static org.openjdk.jmh.annotations.Threads.MAX;

import jakarta.enterprise.util.AnnotationLiteral;

import org.jboss.weld.context.RequestContext;
import org.jboss.weld.context.unbound.Unbound;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

@Fork(FORKS)
@Threads(MAX)
@BenchmarkMode(Mode.Throughput)
@Warmup(batchSize = BATCH_SIZE_NORMAL, iterations = Configuration.ITERATIONS)
@Measurement(batchSize = BATCH_SIZE_NORMAL, iterations = Configuration.ITERATIONS)
@State(Scope.Thread)
public abstract class BaseBenchmark<T extends BeanUnderTest> {

    private RequestContext requestContext;

    protected T instance;

    @State(Scope.Benchmark)
    public static class ContainerState {

        private Weld weld;

        private WeldContainer container;

        @Setup
        public void setup() {
            weld = new Weld();
            container = weld.initialize();
        }

        @TearDown
        public void tearDown() {
            weld.shutdown();
        }

        public WeldContainer getContainer() {
            return container;
        }

    }

    @Setup
    public void setup(ContainerState containerState) {
        instance = containerState.container.select(getBeanClass()).get();
        requestContext = containerState.getContainer()
                .select(RequestContext.class, new AnnotationLiteral<Unbound>() {
                }).get();
        requestContext.activate();
    }

    @TearDown
    public void tearDown() {
        requestContext.invalidate();
        requestContext.deactivate();
    }

    @Benchmark
    public boolean run() {
        return instance.getResult();
    }

    protected abstract Class<T> getBeanClass();

}
