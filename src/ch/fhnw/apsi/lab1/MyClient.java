package ch.fhnw.apsi.lab1;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class MyClient {

  public static void main(String[] args) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, KeyManagementException {

    try {

      URL url = new URL("https://localhost:8000/lab1");
      HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
      HttpsURLConnection securedConnection = (HttpsURLConnection) httpConnection;
      
      char[] passphrase = "mypassphrase".toCharArray();
      KeyStore ks = KeyStore.getInstance("JKS");
      ks.load(new FileInputStream("resources/Server.keystore"), passphrase);
      
      TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
      tmf.init(ks);
      
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, tmf.getTrustManagers(), null);
      
      SSLSocketFactory sslFactory = sslContext.getSocketFactory();      
      securedConnection.setSSLSocketFactory(sslFactory);
//      securedConnection.connect();
            
      // POST credentials and get response
      String credentials = "mail=test@test.com&password=1234";

      securedConnection.setRequestMethod("POST");
      securedConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      securedConnection.setRequestProperty("Content-Length", "" + Integer.toString(credentials.getBytes().length));
      securedConnection.setUseCaches (false);
      securedConnection.setDoInput(true);
      securedConnection.setDoOutput(true);
      securedConnection.connect();

      DataOutputStream wr = new DataOutputStream (securedConnection.getOutputStream ());
      wr.writeBytes (credentials);
      wr.flush ();
      wr.close ();
      
      String cookie = "";
      List<String> cookieFieldList = securedConnection.getHeaderFields().get("Set-cookie");
      if (cookieFieldList != null) {
        if (cookieFieldList.size()==1) {
          cookie = cookieFieldList.get(0);
        }
      }
      System.out.println(cookie);

 
      InputStream is = securedConnection.getInputStream();
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      String line;
      StringBuffer response = new StringBuffer(); 
      while((line = br.readLine()) != null) {
        response.append(line);
        response.append("\n");
      }
//      rd.close();
      
      System.out.println(response.toString());
      
      // get certificate
      java.security.cert.Certificate[] certificates = securedConnection.getServerCertificates();

      if (certificates.length > 0) {  
        
        X509Certificate certificate = (X509Certificate) certificates[0];
        
        System.out.println("Certificate-Issuer: "+certificate.getIssuerDN());
        System.out.println("Certificate-Subject: "+certificate.getSubjectDN());
        System.out.println("Certificate-Signature: "+certificate.getSignature());
        
//        System.out.println(certificate);
  
        try {
          certificate.checkValidity();
        } catch (CertificateExpiredException e) {
          System.out.println("Certificate Expired");
        } catch (CertificateNotYetValidException e) {
          System.out.println("Certificate Not Yet Valid");
        } 
      }

    } catch (MalformedURLException e) {
      System.out.println("Malformed URL");
    } catch (SSLHandshakeException e) {
      System.out.println("Handshake exceptionn");
    } 
  }

}
