package ch.fhnw.apsi.lab1;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class SimpleSSLClient {
  
  private SSLSocketFactory sslSocketFactory;
  private URL url;
  private String cookie;
  
  private static final Logger LOGGER = Logger.getLogger(SimpleSSLClient.class.getName());

  
  public void setUpConnection() throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, KeyManagementException {
    // set SSL context of the connection and trust the self-signed certificate of the server
    
    url = new URL("https://localhost:8000/lab1");
    
    char[] passphrase = "mypassphrase".toCharArray();
    KeyStore ks = KeyStore.getInstance("JKS");
    ks.load(new FileInputStream("resources/Server.keystore"), passphrase);
    
    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
    tmf.init(ks);
    
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, tmf.getTrustManagers(), null);
    
    sslSocketFactory = sslContext.getSocketFactory();
  }
  
  public String post(String input) throws IOException {
    // post login credentials: mail address and password
    // retrieve and store (in-memory) the cookie sent from the server for subsequent requests
    
    HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
    HttpsURLConnection securedConnection = (HttpsURLConnection) httpConnection;
    securedConnection.setSSLSocketFactory(sslSocketFactory);
    
    securedConnection.setRequestMethod("POST");
    securedConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    securedConnection.setRequestProperty("Content-Length", "" + Integer.toString(input.getBytes().length));
    securedConnection.setUseCaches (false);
    securedConnection.setDoInput(true);
    securedConnection.setDoOutput(true);
    securedConnection.connect();
    
    if (!checkCertificateValidity(securedConnection)) {
      securedConnection.disconnect();
      return "CERTIFICATE NOT VALID";
    }

    DataOutputStream wr = new DataOutputStream (securedConnection.getOutputStream ());
    wr.writeBytes (input);
    wr.flush ();
    wr.close ();
    
    String cookie = "";
    List<String> cookieFieldList = securedConnection.getHeaderFields().get("Set-cookie");
    if (cookieFieldList != null) {
      if (cookieFieldList.size()==1) {
        cookie = cookieFieldList.get(0);
      }
    }
    this.cookie = cookie;
    LOGGER.log(Level.INFO, "Retrieved Cookie: " + cookie);

    InputStream is = securedConnection.getInputStream();
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    String line;
    StringBuffer response = new StringBuffer(); 
    while((line = br.readLine()) != null) {
      response.append(line);
      response.append("\n");
    }
    br.close();
    securedConnection.disconnect();
    
    return response.toString();
  }
  
  public String get() throws IOException {
    // get the secret information
    // authentication is achieved by showing the cookie retrieved from the server
    
    HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
    HttpsURLConnection securedConnection = (HttpsURLConnection) httpConnection;
    securedConnection.setSSLSocketFactory(sslSocketFactory);
    
    securedConnection.setRequestMethod("GET");
    securedConnection.setRequestProperty("Cookie", cookie);
    securedConnection.setUseCaches (false);
    securedConnection.setDoInput(true);
    securedConnection.setDoOutput(true);
    securedConnection.connect();
    
    LOGGER.log(Level.INFO, "Sent Cookie: " + cookie);
    
    if (!checkCertificateValidity(securedConnection)) {
      securedConnection.disconnect();
      return "CERTIFICATE NOT VALID";
    }
    
    InputStream is = securedConnection.getInputStream();
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    String line;
    StringBuffer response = new StringBuffer(); 
    while((line = br.readLine()) != null) {
      response.append(line);
      response.append("\n");
    }
    br.close();
    securedConnection.disconnect();
    
    return response.toString();
  }
  
  private boolean checkCertificateValidity(HttpsURLConnection securedConnection) throws IOException {
    // check validity of the server certificate
    // make sure that the self-signed server certificate has not expired yet
    // (matching address is being checked in the handshake automatically)
    
    java.security.cert.Certificate[] certificates = securedConnection.getServerCertificates();

    if (certificates.length > 0) {      
      X509Certificate certificate = (X509Certificate) certificates[0];
    
      try {
        certificate.checkValidity();
        return true;
      } catch (CertificateExpiredException e) {
        LOGGER.log(Level.INFO, "Certificate has expired");
        return false;
      } catch (CertificateNotYetValidException e) {
        LOGGER.log(Level.INFO, "Certificate is not valid yet");
        return false;
      }
    }
    
    return false;
  }
}
