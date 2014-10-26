package ch.fhnw.apsi.lab1;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLHandshakeException;

public class Demo2 {
  
  private static String mail = "test@test.com";
  private static String password = "1234";
  
  private static final Logger LOGGER = Logger.getLogger(Demo2.class.getName());

  public static void main(String[] args) throws IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
    // Demonstration that the authentication of the client of a subsequent request works with a cookie
    
    // Step 1: start server
    FakeServer fakeServer = new FakeServer();
    fakeServer.startServer();
    
    // Step 2: initialize client and SSL context
    SimpleSSLClient client = new SimpleSSLClient();
    client.setUpConnection();

    // Step 3: SSL handshake won't succeed since the server presents a certificate that we don't trust
    // --> donp't send login credentials!
    try {
      LOGGER.log(Level.SEVERE, "Authentication with login credentials to a fake server!!");
      String loginResponse = client.post("mail="+mail+"&password="+password);
      System.out.println(loginResponse);
    } catch (SSLHandshakeException e) {
      LOGGER.log(Level.INFO, "Fake server identified! Don't send login credentials!");
      System.out.println(e.getCause());
      return;
    } finally {
      fakeServer.stopServer();
    }
     
    
  }

}
