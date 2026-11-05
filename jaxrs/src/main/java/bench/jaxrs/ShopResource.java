package bench.jaxrs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@Path("/shop")
@ApplicationScoped
public class ShopResource {

    @Inject
    RequestInfo info;

    @Inject
    ShopFacade facade;

    @GET
    @Path("{id}")
    @Produces("text/plain")
    @ActivateRequestContext
    public String get(@PathParam("id") long id, @HeaderParam("X-User") String user) {
        info.setUser(user);
        return facade.order(id);
    }
}
