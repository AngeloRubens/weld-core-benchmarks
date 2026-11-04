package bench.jaxrs;

import org.jboss.resteasy.cdi.CdiInjectorFactory;
import org.jboss.resteasy.cdi.ResteasyCdiExtension;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.ResteasyDeployment;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;

/** CDI SE (Weld or OpenWebBeans, through the standard SeContainerInitializer) + RESTEasy, dispatching requests in-process. */
public class Server implements AutoCloseable {

    private final SeContainer container;
    private final ResteasyDeployment deployment;
    private final SynchronousDispatcher dispatcher;

    public Server() {
        // no classpath scanning: the Jandex that comes with RESTEasy clashes with the one Weld SE needs
        container = SeContainerInitializer.newInstance().disableDiscovery()
                .addBeanClasses(OrderResource.class, OrderService.class, RequestInfo.class, AuditTrail.class,
                        ShopResource.class, ShopFacade.class, PricingService.class, InventoryService.class,
                        CustomerService.class, TimedInterceptor.class, LoggedInterceptor.class, TxInterceptor.class)
                .addExtensions(new ResteasyCdiExtension())
                .initialize();
        deployment = new ResteasyDeploymentImpl();
        deployment.setInjectorFactoryClass(CdiInjectorFactory.class.getName());
        deployment.getActualResourceClasses().add(OrderResource.class);
        deployment.getActualResourceClasses().add(ShopResource.class);
        deployment.start();
        dispatcher = (SynchronousDispatcher) deployment.getDispatcher();
    }

    public MockHttpResponse get(String path, String user) throws Exception {
        MockHttpRequest request = MockHttpRequest.get(path).header("X-User", user);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        return response;
    }

    public static String expected(long id, String user) {
        return "order " + id + " for " + user + " events=2 touched=1";
    }

    public static String expectedShop(long id, String user) {
        return "shop " + id + " total=" + (100 + (int) (id % 7) - user.length()) + " stock=" + (id % 3 == 0 ? "backorder" : "ok")
                + " events=3";
    }

    @Override
    public void close() {
        deployment.stop();
        container.close();
    }
}
