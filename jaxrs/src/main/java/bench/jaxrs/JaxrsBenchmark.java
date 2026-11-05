package bench.jaxrs;

import org.jboss.resteasy.mock.MockHttpResponse;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.concurrent.TimeUnit;

/** One GET request through JAX-RS, CDI resource, service and three request scoped beans. */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class JaxrsBenchmark {

    private Server server;

    @Setup
    public void setup() throws Exception {
        server = new Server();
        MockHttpResponse r = server.get("/orders/1", "bench");
        if (r.getStatus() != 200 || !Server.expected(1, "bench").equals(r.getContentAsString())) {
            throw new IllegalStateException("unexpected response " + r.getStatus() + " " + r.getContentAsString());
        }
    }

    @TearDown
    public void tearDown() {
        server.close();
    }

    /** Service layer where every call goes through a Timed/Logged/Tx interceptor: 8 interceptor calls per request. */
    @Benchmark
    public int shop() throws Exception {
        MockHttpResponse r = server.get("/shop/7", "bench");
        return r.getStatus();
    }

    @Benchmark
    public int get() throws Exception {
        MockHttpResponse r = server.get("/orders/7", "bench");
        return r.getStatus();
    }
}
