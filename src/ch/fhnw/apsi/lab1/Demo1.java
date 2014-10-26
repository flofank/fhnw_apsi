package ch.fhnw.apsi.lab1;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Demo1 {
  
  private static String mail = "test@test.com";
  private static String password = "1234";
  
  private static final Logger LOGGER = Logger.getLogger(Demo1.class.getName());

  public static void main(String[] args) throws IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
    // Demonstration that the authentication of the client in a subsequent request works with a cookie
    
    // Step 1: start server
    SimpleSSLServer server = new SimpleSSLServer();
    server.startServer();
    
    // Step 2: initialize client and SSL context
    SimpleSSLClient client = new SimpleSSLClient();
    client.setUpConnection();

    // Step 3: post login credentials, retrieve cookie and get the secret information
    LOGGER.log(Level.INFO, "Authenticate with login credentials");
    String loginResponse = client.post("mail="+mail+"&password="+password);
    LOGGER.log(Level.INFO, "Login response:");
    System.out.println(loginResponse);
    
    // Step 4: get the secret information using the cookie as the authenticator
    LOGGER.log(Level.INFO, "Authenticate with retrieved cookie");
    String cookieResponse = client.get();
    LOGGER.log(Level.INFO, "Cookie authentication response:");
    System.out.println(cookieResponse);
     
    server.stopServer();
  }

}
