package bench.jaxrs;

import org.jboss.resteasy.cdi.CdiInjectorFactory;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

/** Weld SE + RESTEasy, dispatching requests in-process (no sockets). */
public class Server implements AutoCloseable {

    private final WeldContainer container;
    private final ResteasyDeployment deployment;
    private final SynchronousDispatcher dispatcher;

    public Server() {
        container = new Weld().initialize();
        deployment = new ResteasyDeploymentImpl();
        deployment.setInjectorFactoryClass(CdiInjectorFactory.class.getName());
        deployment.getActualResourceClasses().add(OrderResource.class);
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

    @Override
    public void close() {
        deployment.stop();
        container.shutdown();
    }
}
