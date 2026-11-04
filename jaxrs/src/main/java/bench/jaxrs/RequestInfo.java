package bench.jaxrs;

import jakarta.enterprise.context.RequestScoped;

/** Per-request state: who is calling and how often this request touched the service. */
@RequestScoped
public class RequestInfo {

    private String user;
    private int touched;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public int touch() {
        return ++touched;
    }
}
