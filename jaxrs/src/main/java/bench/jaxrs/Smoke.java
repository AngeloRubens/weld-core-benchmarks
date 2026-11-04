package bench.jaxrs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.resteasy.mock.MockHttpResponse;

/**
 * Correctness check run before the benchmark: every response must carry the user of its own request and fresh
 * per-request state (events=2 touched=1), also with several threads calling at once.
 */
public class Smoke {

    public static void main(String[] args) throws Exception {
        try (Server server = new Server()) {
            MockHttpResponse r = server.get("/orders/42", "alice");
            if (r.getStatus() != 200 || !Server.expected(42, "alice").equals(r.getContentAsString())) {
                throw new AssertionError("unexpected response " + r.getStatus() + " " + r.getContentAsString());
            }
            r = server.get("/shop/42", "alice");
            if (r.getStatus() != 200 || !Server.expectedShop(42, "alice").equals(r.getContentAsString())) {
                throw new AssertionError("unexpected shop response " + r.getStatus() + " " + r.getContentAsString());
            }
            // per-request state must not leak between sequential calls on one thread (it did on OpenWebBeans, see Server)
            for (int i = 0; i < 3; i++) {
                String o = server.get("/orders/42", "alice").getContentAsString();
                String sh = server.get("/shop/42", "alice").getContentAsString();
                if (!Server.expected(42, "alice").equals(o) || !Server.expectedShop(42, "alice").equals(sh)) {
                    throw new AssertionError("per-request state leaked between sequential calls: " + o + " / " + sh);
                }
            }
            int threads = 8;
            int calls = 20000;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            List<Future<Integer>> results = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                String user = "user" + t;
                results.add(pool.submit(() -> {
                    int bad = 0;
                    for (int i = 0; i < calls; i++) {
                        MockHttpResponse resp = server.get("/orders/" + i, user);
                        if (resp.getStatus() != 200 || !Server.expected(i, user).equals(resp.getContentAsString())) {
                            bad++;
                        }
                        resp = server.get("/shop/" + i, user);
                        if (resp.getStatus() != 200 || !Server.expectedShop(i, user).equals(resp.getContentAsString())) {
                            bad++;
                        }
                    }
                    return bad;
                }));
            }
            int bad = 0;
            for (Future<Integer> f : results) {
                bad += f.get();
            }
            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.MINUTES);
            long intercepted = TimedInterceptor.CALLS.sum();
            long wanted = 4L * (1 + (long) threads * calls);
            if (intercepted != wanted) {
                throw new AssertionError("Timed interceptor ran " + intercepted + " times, expected " + wanted);
            }
            System.out.println("smoke: timed interceptor ran " + intercepted + " times, as expected");
            System.out.println("smoke: " + (threads * calls * 2) + " concurrent requests, " + bad + " wrong responses");
            if (bad != 0) {
                throw new AssertionError(bad + " wrong responses");
            }
        }
    }
}
