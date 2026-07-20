# Jetty Virtual Threads Bug(?)

This project is a focused reproduction for a Jetty behavior:

- Embedded Jetty uses `VirtualThreadPool`
- **Two connectors** are configured on the same server
- Platform is JVM 21+ **on Windows**
- After about **61 seconds of inactivity**, the application will stop responding to requests

The sample starts a single embedded Jetty server with two connectors:

- `8080` exposes `GET /testing`
- `9090` exposes `GET /testing2`

Both endpoints return `201 Created` with an empty body while the server is responsive.

Jetty uses `org.eclipse.jetty.util.thread.VirtualThreadPool` as the server thread pool.

## Requirements

- Java 25+
- Maven 3.9+

## Run

```powershell
mvn clean compile
mvn exec:java
```

Optional custom ports (first argument is `/testing` port, second is `/testing2` port):

```powershell
mvn exec:java "-Dexec.args=8080 9090"
```

## Reproduce

```powershell
curl -i http://localhost:8080/testing
curl -i http://localhost:9090/testing2
```

Expected immediate results:

- First call: `HTTP/1.1 201 Created`
- Second call: `HTTP/1.1 201 Created`

Now wait for at least 60 seconds without sending requests, then call again:

```powershell
curl -i http://localhost:8080/testing
```

Expected repro behavior:

- Request may hang or time out after the inactivity period
- This demonstrates the issue when **all** conditions are true:
    - two connectors are configured
    - virtual threads are enabled via `VirtualThreadPool`
    - JVM 21+ on Windows

## Test

```powershell
mvn test
```

The test will make a call to `/testing` endpoint, then wait for 63 seconds and call again. On Windows this test will fail with an error on a second call:

```powershell
[ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 65.65 s <<< FAILURE! -- in com.example.jettytest.DualPortJettyApplicationTest
[ERROR] com.example.jettytest.DualPortJettyApplicationTest.shouldReturn201AndEmptyBodyForTestingEndpoints -- Time elapsed: 65.54 s <<< ERROR!
java.util.concurrent.TimeoutException: Total timeout 2000 ms elapsed
```

