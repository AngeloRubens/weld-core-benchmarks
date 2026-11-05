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
        // OpenWebBeans SE leaves a request context active on the thread that boots the container, and @ActivateRequestContext
        // then reuses it for every later call on that thread. Boot on a throwaway thread so every request gets a fresh context.
        SeContainer[] booted = new SeContainer[1];
        Throwable[] failure = new Throwable[1];
        Thread boot = new Thread(() -> {
            try {
                // no classpath scanning: the Jandex that comes with RESTEasy clashes with the one Weld SE needs
                booted[0] = SeContainerInitializer.newInstance().disableDiscovery()
                        .addBeanClasses(OrderResource.class, OrderService.class, RequestInfo.class, AuditTrail.class,
                                ShopResource.class, ShopFacade.class, PricingService.class, InventoryService.class,
                                CustomerService.class, TimedInterceptor.class, LoggedInterceptor.class, TxInterceptor.class)
                        .addExtensions(new ResteasyCdiExtension())
                        .initialize();
            } catch (Throwable t) {
                failure[0] = t;
            }
        }, "cdi-boot");
        boot.start();
        try {
            boot.join();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        if (failure[0] != null) {
            throw new IllegalStateException("container failed to start", failure[0]);
        }
        container = booted[0];
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
