package ch.fhnw.apsi.lab1;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class Demo {

  public static void main(String[] args) {
    // TODO Auto-generated method stub

    SimpleSSLServer server = new SimpleSSLServer();
    try {
      server.startServer();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    SimpleSSLClient client = new SimpleSSLClient();
    try {
      client.setUpConnection();
    } catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    try {
      String loginResponse = client.post("mail=test@test.com&password=1234");
      System.out.println(loginResponse);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    try {
      String cookieResponse = client.get();
      System.out.println(cookieResponse);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    server.stopServer();
  }

}
