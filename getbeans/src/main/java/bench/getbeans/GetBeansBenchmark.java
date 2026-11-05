package bench.getbeans;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 * Cost of one BeanManager.getBeans() call. {@code classNoQualifier*} is the call the change under test shortcuts;
 * the other benchmarks go through code the change does not touch and act as controls (they must not move).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class GetBeansBenchmark {

    @Qualifier @Retention(RetentionPolicy.RUNTIME) public @interface Special {
    }

    static final class SpecialLiteral extends AnnotationLiteral<Special> implements Special {
    }

    public interface Service {
    }

    @ApplicationScoped public static class S0 implements Service { }
    @ApplicationScoped public static class S1 implements Service { }
    @ApplicationScoped public static class S2 implements Service { }
    @ApplicationScoped public static class S3 implements Service { }
    @ApplicationScoped public static class S4 implements Service { }
    @ApplicationScoped public static class S5 implements Service { }
    @ApplicationScoped public static class S6 implements Service { }
    @ApplicationScoped public static class S7 implements Service { }
    @ApplicationScoped @Special public static class Q0 implements Service { }

    static final Class<?>[] MANY = { S0.class, S1.class, S2.class, S3.class, S4.class, S5.class, S6.class, S7.class };

    private WeldContainer container;
    private BeanManager bm;
    private Annotation special;
    private Annotation dflt;

    @State(Scope.Thread)
    public static class Cursor {
        int next;
    }

    @Setup
    public void setup() {
        container = new Weld().disableDiscovery()
                .beanClasses(S0.class, S1.class, S2.class, S3.class, S4.class, S5.class, S6.class, S7.class, Q0.class)
                .initialize();
        bm = container.getBeanManager();
        special = new SpecialLiteral();
        dflt = Default.Literal.INSTANCE;
        // The shortcut must return what the explicit @Default lookup returns, for every type used below.
        for (Class<?> c : MANY) {
            Set<Bean<?>> a = bm.getBeans(c);
            Set<Bean<?>> b = bm.getBeans(c, dflt);
            if (a.size() != 1 || !a.equals(b)) {
                throw new IllegalStateException("getBeans(" + c + ") = " + a + " but with @Default " + b);
            }
        }
        if (bm.getBeans(Q0.class).size() != 0 || bm.getBeans(Q0.class, special).size() != 1) {
            throw new IllegalStateException("qualifier lookup is wrong");
        }
    }

    @TearDown
    public void tearDown() {
        container.close();
    }

    /** One class, no qualifiers, always the same: what RESTEasy does for its resource class on every request. */
    @Benchmark
    public Set<Bean<?>> classNoQualifier() {
        return bm.getBeans(S0.class);
    }

    /** Eight classes in turn, no qualifiers. */
    @Benchmark
    public Set<Bean<?>> classNoQualifierEight(Cursor c) {
        return bm.getBeans(MANY[(c.next++) & 7]);
    }

    /** Control: same bean with an explicit @Default; takes the old path in both builds. */
    @Benchmark
    public Set<Bean<?>> classExplicitDefault() {
        return bm.getBeans(S0.class, dflt);
    }

    /** Control: custom qualifier; old path in both builds. */
    @Benchmark
    public Set<Bean<?>> classWithQualifier() {
        return bm.getBeans(Q0.class, special);
    }
}
