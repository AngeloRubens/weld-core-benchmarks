package bench.jaxrs;

import java.util.concurrent.atomic.LongAdder;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Metrics-style interceptor: counts calls and sums the time spent. */
@Timed
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class TimedInterceptor {

    static final LongAdder CALLS = new LongAdder();
    static final LongAdder NANOS = new LongAdder();

    @AroundInvoke
    public Object around(InvocationContext ctx) throws Exception {
        long start = System.nanoTime();
        try {
            return ctx.proceed();
        } finally {
            CALLS.increment();
            NANOS.add(System.nanoTime() - start);
        }
    }
}
