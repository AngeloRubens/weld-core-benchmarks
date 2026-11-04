package bench.jaxrs;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Logging-style interceptor: reads the method and parameters (as a logger at an enabled level would), writes nothing. */
@Logged
@Interceptor
@Priority(Interceptor.Priority.APPLICATION + 10)
public class LoggedInterceptor {

    static volatile int seen;

    @AroundInvoke
    public Object around(InvocationContext ctx) throws Exception {
        seen = ctx.getMethod().getName().length() + ctx.getParameters().length;
        return ctx.proceed();
    }
}
