import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.control.RequestContextController;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

/**
 * Prints the observable behaviour of interceptors, decorators and client proxies, one line per scenario, so the output of
 * two Weld builds can be diffed. Any difference between Weld 7.0.0 and a patched build is a semantic change.
 * Nothing here depends on generated class names or on timing.
 */
public class SelfInvocationProbe {

    static final List<String> LOG = new ArrayList<>();

    static volatile boolean quiet;

    static synchronized void log(String s) {
        if (!quiet) {
            LOG.add(s);
        }
    }

    @InterceptorBinding @Retention(RetentionPolicy.RUNTIME) @interface Trace {
    }

    @InterceptorBinding @Retention(RetentionPolicy.RUNTIME) @interface Trace2 {
    }

    @InterceptorBinding @Retention(RetentionPolicy.RUNTIME) @interface Twice {
    }

    @Trace @Interceptor @Priority(1000)
    public static class TraceInterceptor {
        @AroundInvoke
        public Object around(InvocationContext ctx) throws Exception {
            log("I1>" + ctx.getMethod().getName() + Arrays.toString(ctx.getParameters())
                    + " declaring=" + ctx.getMethod().getDeclaringClass().getSimpleName()
                    + " targetIsBean=" + (ctx.getTarget() instanceof Bean) + " ctxData=" + ctx.getContextData().keySet());
            ctx.getContextData().put("k", "v");
            try {
                Object r = ctx.proceed();
                log("I1<" + ctx.getMethod().getName() + "=" + r);
                return r;
            } catch (Exception e) {
                log("I1!" + ctx.getMethod().getName() + " " + e.getClass().getSimpleName() + ":" + e.getMessage());
                throw e;
            }
        }
    }

    @Trace2 @Interceptor @Priority(1001)
    public static class Trace2Interceptor {
        @AroundInvoke
        public Object around(InvocationContext ctx) throws Exception {
            log("I2>" + ctx.getMethod().getName() + " ctxData=" + ctx.getContextData());
            if (ctx.getMethod().getName().startsWith("mod")) {
                ctx.setParameters(new Object[] { ((Integer) ctx.getParameters()[0]) * 10 });
            }
            Object r = ctx.proceed();
            return r instanceof Integer ? (Integer) r + 1000 : r;
        }
    }

    @Twice @Interceptor @Priority(1002)
    public static class TwiceInterceptor {
        @AroundInvoke
        public Object around(InvocationContext ctx) throws Exception {
            log("T>" + ctx.getMethod().getName());
            ctx.proceed();
            return ctx.proceed();
        }
    }

    public interface Bean {
    }

    @Dependent
    public static class DepBean implements Bean {
        @Trace public int a(int x) { return x + 1; }
        public int plain(int x) { return a(x) * 2; }
        @Trace public int outer(int x) { return a(x) + 100; }
        @Trace protected int prot(int x) { return x + 2; }
        @Trace int pkg(int x) { return x + 3; }
        public int callProt(int x) { return prot(x); }
        public int callPkg(int x) { return pkg(x); }
        public int callThisExplicit(int x) { return this.a(x); }
        @Trace public void boom() throws IOException { throw new IOException("checked"); }
        @Trace public void boomRt() { throw new IllegalStateException("runtime"); }
        @Trace public int rec(int n) { return n == 0 ? 0 : 1 + rec(n - 1); }
        @Trace @Trace2 public int multi(int x) { return x; }
        @Trace @Trace2 public int modify(int x) { return x; }
        @Twice @Trace public int twice() { log("twice-body"); return 7; }
        public final int finalPlain() { return 1; }
        public static int staticM() { return 2; }
    }

    @Dependent @Trace
    public static class ClassLevel implements Bean {
        public int a() { return 1; }
        public int b() { return a() + 1; }
        private int hidden() { return 9; }
        public int callHidden() { return hidden(); }
    }

    @ApplicationScoped
    public static class AppBean implements Bean {
        boolean postConstructed;
        @PostConstruct void init() { log("pc-calls-a=" + a(1)); postConstructed = true; }
        @Trace public int a(int x) { return x + 1; }
        public int plain(int x) { return a(x) * 2; }
        @Trace public int outer(int x) { return plain(x); }
        public int unintercepted(int x) { return x; }
        public String toString2() { return "AppBean"; }
        public Class<?> whoAmI() { return this.getClass(); }
        @Trace public Object self() { return this; }
    }

    @RequestScoped
    public static class ReqBean implements Bean {
        int state;
        @Trace public int inc() { return ++state; }
        public int incTwice() { inc(); return inc(); }
    }

    @ApplicationScoped
    public static class Ping implements Bean {
        @Inject Pong pong;
        @Trace public String ping(int n) { return n == 0 ? "end" : "ping" + pong.pong(n - 1); }
    }

    @Dependent
    public static class Pong implements Bean {
        @Inject Ping ping;
        @Trace public String pong(int n) { return n == 0 ? "end" : "pong" + ping.ping(n - 1); }
    }

    public interface Greeter {
        String greet();
        String greetTwice();
    }

    @Dependent
    public static class DepGreeter implements Greeter, Bean {
        @Override public String greet() { return "hi"; }
        @Override public String greetTwice() { return greet() + greet(); }
    }

    @Decorator @Priority(1000)
    public static abstract class GreeterDecorator implements Greeter {
        @Inject @Delegate Greeter delegate;
        @Override public String greet() { return "[" + delegate.greet() + "]"; }
    }

    @ApplicationScoped
    public static class Counter implements Bean {
        final AtomicInteger calls = new AtomicInteger();
        @Trace public int hit() { return calls.incrementAndGet(); }
    }

    static void run(String name, Supplier<Object> body) {
        LOG.clear();
        Object result;
        try {
            result = body.get();
        } catch (Throwable t) {
            result = "THROWS " + t.getClass().getSimpleName() + ":" + t.getMessage()
                    + (t.getCause() != null ? " cause=" + t.getCause().getClass().getSimpleName() : "");
        }
        System.out.println(name + " => " + result + " | " + String.join(" ", LOG));
    }

    public static void main(String[] args) throws Exception {
        try (WeldContainer c = new Weld().disableDiscovery()
                .beanClasses(DepBean.class, ClassLevel.class, AppBean.class, ReqBean.class, Ping.class, Pong.class,
                        DepGreeter.class, Counter.class, GreeterDecorator.class)
                .interceptors(TraceInterceptor.class, Trace2Interceptor.class, TwiceInterceptor.class)
                .decorators(GreeterDecorator.class)
                .initialize()) {
            DepBean d = c.select(DepBean.class).get();
            run("dep.a", () -> d.a(1));
            run("dep.plain (self-invocation from unintercepted)", () -> d.plain(1));
            run("dep.outer (self-invocation from intercepted)", () -> d.outer(1));
            run("dep.callThisExplicit", () -> d.callThisExplicit(1));
            run("dep.prot via self", () -> d.callProt(1));
            run("dep.pkg via self", () -> d.callPkg(1));
            run("dep.pkg direct", () -> d.pkg(1));
            run("dep.boom checked", () -> {
                try { d.boom(); return "no"; } catch (IOException e) { return "caught " + e.getMessage(); }
            });
            run("dep.boomRt", () -> { d.boomRt(); return "no"; });
            run("dep.rec(3)", () -> d.rec(3));
            run("dep.multi", () -> d.multi(5));
            run("dep.modify", () -> d.modify(5));
            run("dep.twice", () -> d.twice());
            run("dep.finalPlain/static", () -> d.finalPlain() + "," + DepBean.staticM());

            ClassLevel cl = c.select(ClassLevel.class).get();
            run("classlevel.a", () -> cl.a());
            run("classlevel.b (self)", () -> cl.b());
            run("classlevel.callHidden", () -> cl.callHidden());

            AppBean app = c.select(AppBean.class).get();
            run("app.a", () -> app.a(1));
            run("app.plain (self)", () -> app.plain(1));
            run("app.outer", () -> app.outer(1));
            run("app.unintercepted", () -> app.unintercepted(1));
            run("app.whoAmI is proxy?", () -> app.whoAmI() != AppBean.class);
            run("app.self() == proxy?", () -> app.self() == app);
            run("app.toString2", () -> app.toString2());

            RequestContextController rcc = c.select(RequestContextController.class).get();
            rcc.activate();
            ReqBean rb = c.select(ReqBean.class).get();
            run("req.inc,inc", () -> rb.inc() + "," + rb.inc());
            run("req.incTwice (self)", () -> rb.incTwice());
            rcc.deactivate();
            run("req outside context", () -> rb.inc());
            rcc.activate();
            run("req new context", () -> rb.inc());
            rcc.deactivate();

            Ping ping = c.select(Ping.class).get();
            run("ping/pong reentrancy", () -> ping.ping(4));

            Greeter g = c.select(Greeter.class).get();
            run("decorator greet", () -> g.greet());
            run("decorator greetTwice (self)", () -> g.greetTwice());

            Counter counter = c.select(Counter.class).get();
            quiet = true;
            ExecutorService ex = Executors.newFixedThreadPool(8);
            List<Future<Integer>> fs = new ArrayList<>();
            for (int t = 0; t < 8; t++) {
                fs.add(ex.submit(() -> {
                    int ok = 0;
                    for (int i = 0; i < 20000; i++) {
                        if (counter.hit() > 0 && d.a(i) == i + 1 && d.plain(i) == (i + 1) * 2) ok++;
                    }
                    return ok;
                }));
            }
            int total = 0;
            for (Future<Integer> f : fs) total += f.get();
            ex.shutdown();
            ex.awaitTermination(1, TimeUnit.MINUTES);
            System.out.println("concurrent ok=" + total + " hits=" + counter.calls.get());
        }
    }
}
