package bench.jaxrs;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CustomerService {

    @Timed
    @Logged
    public int discount(String user) {
        return user.length();
    }
}
