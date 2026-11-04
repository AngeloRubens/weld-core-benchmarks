package bench.jaxrs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Service layer: every public method of every service carries interceptors, as in an application with metrics, logging and transactions. */
@ApplicationScoped
public class ShopFacade {

    @Inject
    PricingService pricing;

    @Inject
    InventoryService inventory;

    @Inject
    CustomerService customers;

    @Inject
    RequestInfo info;

    @Inject
    AuditTrail audit;

    @Timed
    @Tx
    public String order(long id) {
        int price = pricing.price(id);
        audit.add("price");
        String stock = inventory.reserve(id);
        audit.add("stock");
        int discount = customers.discount(info.getUser());
        audit.add("discount");
        return "shop " + id + " total=" + (price - discount) + " stock=" + stock + " events=" + audit.size();
    }
}
