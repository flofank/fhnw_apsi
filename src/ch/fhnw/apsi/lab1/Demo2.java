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
    // Demonstration that the client discovers a Man-In-The-Middle attack when an attacker claims to be the server,
    // but can't show a certificate during the SSL-handshake that is trustful for the client
    
    // Step 1: start server
    FakeServer fakeServer = new FakeServer();
    fakeServer.startServer();
    
    // Step 2: initialize client and SSL context
    SimpleSSLClient client = new SimpleSSLClient();
    client.setUpConnection();

    // Step 3: SSL handshake won't succeed since the server presents a certificate that we don't trust
    // --> don't send login credentials!
    try {
      String loginResponse = client.post("mail="+mail+"&password="+password);
      LOGGER.log(Level.SEVERE, "Client has sent login credentials to a fake server!!");
      System.out.println(loginResponse);
    } catch (SSLHandshakeException e) {
      LOGGER.log(Level.INFO, "Fake server identified! Don't send login credentials!");
      System.out.println(e.getCause());
    } finally {
      fakeServer.stopServer();
    }
     
    
  }

}
