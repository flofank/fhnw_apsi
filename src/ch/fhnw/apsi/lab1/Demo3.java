package ch.fhnw.apsi.lab1;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Demo3 {
  
  private static final Logger LOGGER = Logger.getLogger(Demo3.class.getName());

  public static void main(String[] args) throws IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, InvalidKeyException {
    // Demonstration that the server discovers if an attacker tries to get the secret information by tampering an expired cookie
    
    // Step 1: start server
    ExpiredCookieServer server = new ExpiredCookieServer();
    server.startServer();
    
    // Step 2: initialize evil client and SSL context
    EvilClient client = new EvilClient();
    client.setUpConnection();
    
    // Step 3: let evil client tamper an expired cookie
    String expiredCookie = server.getExpiredCookie();
    client.tamperExpiredCookie(expiredCookie);

    // Step 4: let evil client try to get the secret information using the tampered cookie as the authenticator
    // Integrity test of cookie will fail and server will send the login form but not the secret information.
    String attackResponse = client.tryToSnatchSecretInformation();
    System.out.println(attackResponse);
     
    server.stopServer();
  }

}
