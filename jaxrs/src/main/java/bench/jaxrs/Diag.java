package bench.jaxrs;

/** Prints the responses of sequential calls on one thread, to see whether per-request state leaks between requests. */
public class Diag {

    public static void main(String[] args) throws Exception {
        try (Server server = new Server()) {
            String[] paths = { "/orders/42", "/orders/42", "/shop/42", "/shop/42", "/orders/43", "/shop/44" };
            for (int pass = 0; pass < 2; pass++) {
                for (String p : paths) {
                    System.out.println("main thread " + p + " => " + server.get(p, "alice").getContentAsString());
                }
            }
            Thread t = new Thread(() -> {
                try {
                    for (String p : paths) {
                        System.out.println("other thread " + p + " => " + server.get(p, "bob").getContentAsString());
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            });
            t.start();
            t.join();
        }
    }
}
