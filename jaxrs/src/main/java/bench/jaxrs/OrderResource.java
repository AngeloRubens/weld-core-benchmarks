package bench.jaxrs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

/**
 * A JAX-RS resource outside a servlet container: nothing activates the request context, so every call activates the
 * unbound one with {@link ActivateRequestContext}.
 */
@Path("/orders")
@ApplicationScoped
public class OrderResource {

    @Inject
    RequestInfo info;

    @Inject
    AuditTrail audit;

    @Inject
    OrderService service;

    @GET
    @Path("{id}")
    @Produces("text/plain")
    @ActivateRequestContext
    public String get(@PathParam("id") long id, @HeaderParam("X-User") String user) {
        info.setUser(user);
        audit.add("get");
        return service.describe(id);
    }
}
