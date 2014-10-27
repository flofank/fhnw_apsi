package ch.fhnw.apsi.lab1;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Demo0 {
  
  private static final Logger LOGGER = Logger.getLogger(Demo0.class.getName());

  public static void main(String[] args) throws IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, InvalidKeyException {
    // Demonstration that the server will not send the secret information if the client shows an expired cookie
    
    // Step 1: start server
    ExpiredCookieServer server = new ExpiredCookieServer();
    server.startServer();
    
    // Step 2: initialize client and SSL context
    ExpiredCookieClient client = new ExpiredCookieClient();
    client.setUpConnection();
    
    // Step 3: simulate that the client has an expired cookie from the server
    String expiredCookie = server.getExpiredCookie();
    client.setCookie(expiredCookie);
    
    // Step 3: try to get the secret information using an expired cookie as the authenticator
    // Server will detect expired cookie and not send the secret information but the login form 
    LOGGER.log(Level.INFO, "Trying to authenticate with retired cookie");
    String cookieResponse = client.get();
    LOGGER.log(Level.INFO, "Cookie authentication response:");
    System.out.println(cookieResponse);
     
    server.stopServer();
  }

}
