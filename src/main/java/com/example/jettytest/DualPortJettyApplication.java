package com.example.jettytest;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.VirtualThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DualPortJettyApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(DualPortJettyApplication.class);

    private DualPortJettyApplication() {
    }

    public static void main(String[] args) throws Exception {
        int testingPort = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        int testing2Port = args.length > 1 ? Integer.parseInt(args[1]) : 9090;

        Server server = createServer(testingPort, testing2Port);
        try {
            server.start();
            LOGGER.info("Jetty started with virtual threads; /testing on {} and /testing2 on {}", testingPort, testing2Port);
            server.join();
        } finally {
            server.stop();
        }
    }

    static Server createServer(int testingPort, int testing2Port) {
        VirtualThreadPool virtualThreadPool = new VirtualThreadPool();
        virtualThreadPool.setName("jetty-virtual-threads");

        Server server = new Server(virtualThreadPool);

        ServerConnector firstConnector = new ServerConnector(server);
        firstConnector.setPort(testingPort);

        ServerConnector secondConnector = new ServerConnector(server);
        secondConnector.setPort(testing2Port);

        server.setConnectors(new Connector[]{firstConnector, secondConnector});
        server.setHandler(new EndpointHandler(testingPort, testing2Port));
        return server;
    }

    private static final class EndpointHandler extends Handler.Abstract {
        private final int testingPort;
        private final int testing2Port;

        private EndpointHandler(int testingPort, int testing2Port) {
            this.testingPort = testingPort;
            this.testing2Port = testing2Port;
        }

        @Override
        public boolean handle(Request request, Response response, Callback callback) {
            if (!HttpMethod.GET.is(request.getMethod())) {
                Response.writeError(request, response, callback, HttpStatus.METHOD_NOT_ALLOWED_405);
                return true;
            }

            int localPort = Request.getLocalPort(request);
            String path = Request.getPathInContext(request);

            boolean isTestingEndpoint = localPort == testingPort && "/testing".equals(path);
            boolean isTesting2Endpoint = localPort == testing2Port && "/testing2".equals(path);

            if (isTestingEndpoint || isTesting2Endpoint) {
                response.setStatus(HttpStatus.CREATED_201);
                response.getHeaders().put(HttpHeader.CONTENT_LENGTH, "0");
                LOGGER.info("Handled {} on port {} with 201 and isVirtual '{}'", path, localPort, Thread.currentThread().isVirtual());
                callback.succeeded();
                return true;
            }

            LOGGER.info("No route for {} on port {}", path, localPort);
            Response.writeError(request, response, callback, HttpStatus.NOT_FOUND_404);
            return true;
        }
    }
}
