package bench.jaxrs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrderService {

    @Inject
    RequestInfo info;

    @Inject
    AuditTrail audit;

    public String describe(long id) {
        audit.add("describe " + id);
        int touched = info.touch();
        return "order " + id + " for " + info.getUser() + " events=" + audit.size() + " touched=" + touched;
    }
}
