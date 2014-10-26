package ch.fhnw.apsi.lab1;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class Demo1 {
  
  private static String mail = "test@test.com";
  private static String password = "1234";

  public static void main(String[] args) throws IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
    // Demonstration that the authentication of the client of a subsequent request works with a cookie
    
    // Step 1: start server
    SimpleSSLServer server = new SimpleSSLServer();
    server.startServer();
    
    // Step 2: initialize client and SSL context
    SimpleSSLClient client = new SimpleSSLClient();
    client.setUpConnection();

    // Step 3: post login credentials, retrieve cookie and get the secret information
    String loginResponse = client.post("mail="+mail+"&password="+password);
    System.out.println(loginResponse);
    
    // Step 4: get the secret information using the cookie as the authenticator
    String cookieResponse = client.get();
    System.out.println(cookieResponse);
     
    server.stopServer();
  }

}
