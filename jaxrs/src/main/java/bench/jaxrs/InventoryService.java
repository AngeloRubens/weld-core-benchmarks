package bench.jaxrs;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InventoryService {

    @Timed
    @Tx
    public String reserve(long id) {
        return id % 3 == 0 ? "backorder" : "ok";
    }
}
