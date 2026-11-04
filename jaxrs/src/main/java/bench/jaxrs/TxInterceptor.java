package bench.jaxrs;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Transaction-style interceptor: tracks the nesting depth of the current thread. */
@Tx
@Interceptor
@Priority(Interceptor.Priority.APPLICATION + 20)
public class TxInterceptor {

    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    @AroundInvoke
    public Object around(InvocationContext ctx) throws Exception {
        int[] depth = DEPTH.get();
        depth[0]++;
        try {
            return ctx.proceed();
        } finally {
            depth[0]--;
        }
    }
}
