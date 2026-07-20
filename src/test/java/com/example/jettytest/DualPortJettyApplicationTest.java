package com.example.jettytest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DualPortJettyApplicationTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DualPortJettyApplicationTest.class);

    private Server server;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() throws Exception {
        httpClient = new HttpClient();
        httpClient.setConnectTimeout(Duration.ofSeconds(2L).toMillis());
        httpClient.setIdleTimeout(Duration.ofMinutes(2L).toMillis());
        httpClient.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
        if (httpClient != null) {
            httpClient.stop();
        }
    }

    @Test
    void shouldReturn201AndEmptyBodyForTestingEndpoints() throws Exception {
        int port8080 = findFreePort();
        int port9090 = findFreePort();

        server = DualPortJettyApplication.createServer(port8080, port9090);
        server.start();

        String testingUrl = "http://localhost:" + port8080 + "/testing";
        ContentResponse testingResponse = get(testingUrl);

        assertEquals(201, testingResponse.getStatus());
        assertEquals("", testingResponse.getContentAsString());
        
        LOGGER.info("First call to {}", testingUrl);
        try {
        	 Thread.sleep(63000L); // Wait for a 60+s
        } catch (InterruptedException e) {
        	 LOGGER.error("Interrupted", e);
		}
       
        LOGGER.info("Second call to {}", testingUrl);
        testingResponse = get(testingUrl);
        assertEquals(201, testingResponse.getStatus());
        assertEquals("", testingResponse.getContentAsString());

    }


    private ContentResponse get(String uri) throws Exception {
        return httpClient.newRequest(URI.create(uri))
                .method("GET")
                .timeout(2L, TimeUnit.SECONDS)
                .send();
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }
}
