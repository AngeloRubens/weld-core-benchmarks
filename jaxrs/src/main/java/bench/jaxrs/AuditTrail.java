package bench.jaxrs;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;

/** Per-request audit events. */
@RequestScoped
public class AuditTrail {

    private final List<String> events = new ArrayList<>();

    public void add(String event) {
        events.add(event);
    }

    public int size() {
        return events.size();
    }
}
