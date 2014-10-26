package ch.fhnw.apsi.lab1;

import java.io.IOException;

public class Main {

  public static void main(String[] args) throws IOException {
    // Main class that starts the server listening to https://localhost:8000/lab1
    // Can be used to test server functionality with a browser as the client:
    // use the following login credentials:
    // - mail: test@test.com
    // - password: 1234
    // must trust the self-signed server certificate
    
    SimpleSSLServer server = new SimpleSSLServer();
    server.startServer();

  }

}
