package bench.jaxrs;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PricingService {

    @Timed
    @Logged
    public int price(long id) {
        return base(id) + 0;
    }

    int base(long id) {
        return 100 + (int) (id % 7);
    }
}
