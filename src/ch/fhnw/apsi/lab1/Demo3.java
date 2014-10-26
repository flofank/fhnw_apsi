package ch.fhnw.apsi.lab1;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Demo3 {
  
  private static final Logger LOGGER = Logger.getLogger(Demo3.class.getName());

  public static void main(String[] args) throws IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
    // Demonstration that the server discovers if an attacker tries to get the secret information by tampering an expired cookie
    
    // Step 1: start server
    SimpleSSLServer server = new SimpleSSLServer();
    server.startServer();
    
    // Step 2: initialize evil client and SSL context
    EvilClient client = new EvilClient();
    client.setUpConnection();
    
    // Step 3: let evil client tamper an expired cookie
    String expiredCookie = "session=exp=1414348901&data=dummy&digest=FF6F8F67628B22F39F33634141B3C38A6EF25C3E83A96B87E06DF45262853426 ;HttpOnly;Secure";
    client.tamperExpiredCookie(expiredCookie);

    // Step 4: let evil client try to get the secret information using the tampered cookie as the authenticator
    // Integrity test of cookie will fail and server will send the login form but not the secret information.
    String attackResponse = client.tryToSnatchSecretInformation();
    System.out.println(attackResponse);
     
    server.stopServer();
  }

}
