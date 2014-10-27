package ch.fhnw.apsi.lab1;

/*
 * Copyright 2013 - 2014 by PostFinance Ltd - All rights reserved
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.bind.DatatypeConverter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/*
 * Copyright 2013 - 2014 by PostFinance Ltd - All rights reserved
 */
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

/**
 * TODO
 * 
 * @author TODO
 */
public class ExpiredCookieServer implements HttpHandler {
  private static final Map<String, String> USERS = new HashMap<>();
  private static final long COOKIE_VALID_TIME = 300; // 5 Minutes
  private static final String SHA_SALT = "oeschgerfankhauserapsilab1";
  private static final Logger LOGGER = Logger.getLogger(ExpiredCookieServer.class.getName());
  private static KeyGenerator keyGenerator;
  private static SecretKey secretKey;
  private HttpsServer server;

  static {
    USERS.put("test%40test.com", "1234");
    USERS.put("test@test.com", "1234");
  }

  public void startServer() throws IOException {
    // initialize SSL context with a self-singed server certificate (stored in resources/Server.keyStore)
    // server listens to https://localhost:8000/lab1
    
    server = HttpsServer.create(new InetSocketAddress(8000), 0);
    
    try {
      char[] passphrase = "mypassphrase".toCharArray();
      KeyStore ks = KeyStore.getInstance("JKS");
      ks.load(new FileInputStream("resources/Server.keystore"), passphrase);
  
      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(ks, passphrase);
  
      TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
      tmf.init(ks);
  
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
      
      server.setHttpsConfigurator(new HttpsConfigurator(sslContext));
      
      keyGenerator = KeyGenerator.getInstance("HmacSHA256");
      secretKey = keyGenerator.generateKey();
    } catch (KeyStoreException e) {
      LOGGER.log(Level.SEVERE,"KeyStoreException");
    } catch (NoSuchAlgorithmException e) {
      LOGGER.log(Level.SEVERE,"NoSuchAlgorithmException");
    } catch (CertificateException e) {
      LOGGER.log(Level.SEVERE,"CertificateException");
    } catch (UnrecoverableKeyException e) {
      LOGGER.log(Level.SEVERE,"UnrecoverableKeyException");
    } catch (KeyManagementException e) {
      LOGGER.log(Level.SEVERE,"KeyManagementException");
    }
    
    server.createContext("/lab1", new ExpiredCookieServer());
    server.setExecutor(null); // creates a default executor
    
    LOGGER.log(Level.INFO,"Https Server initialized");
    
    server.start();
  }

  public void handle(HttpExchange ex) throws IOException {
    // handle client requests
    // - show login form if client can't authenticate with a valid cookie
    // - send secret information if login successful and send client a cookie for subsequent requests
    // - send secret information if client authenticates with valid cookie
    
    try {
      Map<String, String> cookies = parseCookies(ex);
      Map<String, String> params = parseRequestParams(ex);
      if (validateCookie(cookies)) {
        LOGGER.log(Level.INFO,"Sending secret.html (cookie validated)");
        writeResponse("secret.html", ex);
      } else {
        if (login(ex, params)) {
          LOGGER.log(Level.INFO,"Sending secret.html (login successfull)");
          writeResponse("secret.html", ex);
        } else {
          LOGGER.log(Level.INFO,"Sending form.html");
          writeResponse("form.html", ex);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private boolean login(HttpExchange ex, Map<String, String> params) throws NoSuchAlgorithmException {
    // check login credentials retrieved from client 
    
    try {
      if (params.containsKey("mail") && params.containsKey("password")) {
        if (USERS.get(params.get("mail")).equals(params.get("password"))) {
          ex.getResponseHeaders().set("Set-Cookie", createCookie(ex));
          return true;
        }
      }
    } catch (Exception e) {
    }
    return false;
  }
  
  private Map<String, String> parseCookies(HttpExchange ex) {
    // extract cookies from the request header 
    
    Map<String, String> cookies = new HashMap<>();
    if (ex.getRequestHeaders().containsKey("Cookie")) {      
      String cookiesString = (String) ((LinkedList) ex.getRequestHeaders().get("Cookie")).getFirst();
      for (String cookieString : cookiesString.split(";")) {
        String[] c = cookieString.trim().split("=", 2);
        if (c.length == 2) {
          cookies.put(c[0], c[1]);
        }
      }
    }
    return cookies;
  }

  private boolean validateCookie(Map<String, String> cookies) {
    // validate cookie retrieved from client
    // - check that all necessary parts are there
    // - check integrity of cookie by comparing the MAC
    // - check if cookie has expired
    
    try {
      if (cookies.containsKey("session")) {
        String session = cookies.get("session");
        Map<String, String> cookieParams = extractKeyValues(session);
        long time = Long.valueOf(cookieParams.get("exp"));
        String data = cookieParams.get("data");
        if (cookieParams.get("digest").endsWith(calculateDigest(time, data))) {
          if (time >= System.currentTimeMillis()/1000) {
            LOGGER.log(Level.INFO,"Cookie validated");
            return true;
          } else {
            LOGGER.log(Level.INFO,"Cookie expired");
            return false;
          }
        } else {
          LOGGER.log(Level.INFO, "Cookie integrity check failed");
          return false;
        }
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Exception when validating cookie");
    }
    return false;
  }

  private Map<String, String> parseRequestParams(HttpExchange ex) throws IOException {
    // extract the parameters of the client request (e.g. login credentials sent in post request)  
    
    Map<String, String> params = new HashMap<>();
    String query = ex.getRequestURI().getQuery();
    params.putAll(extractKeyValues(query));
    BufferedReader br = new BufferedReader(new InputStreamReader(ex.getRequestBody()));
    StringBuilder sb = new StringBuilder();
    String line;
    while((line = br.readLine()) != null) {
      sb.append(line);
    }
    String body = sb.toString();
    params.putAll(extractKeyValues(body));
    return params;
  }
  
  private String createCookie(HttpExchange ex) throws NoSuchAlgorithmException, InvalidKeyException {
    // create a cookie that the client can use in subsequent requests to authenticate
    
    StringBuilder sb = new StringBuilder();
    sb.append("session=exp=");
    long time = System.currentTimeMillis() / 1000 + COOKIE_VALID_TIME; // Seconds since 1970 + valid time
    sb.append(time);
    sb.append("&");
    String data = "dummy";//TODO: Replace with something useful
    sb.append("data="); 
    sb.append(data);
    sb.append("&");
    sb.append("digest=");
    sb.append(calculateDigest(time, data));
    sb.append(" ;HttpOnly;Secure");
    return sb.toString();
  }
  
  private String calculateDigest(long time, String data) throws NoSuchAlgorithmException, InvalidKeyException {
    // calculate MAC of the cookie content to prove its integrity

    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(secretKey);
    mac.update(Long.toString(time).getBytes());
    mac.update(data.getBytes());
    mac.update(SHA_SALT.getBytes());
    return DatatypeConverter.printHexBinary(mac.doFinal());
    
  }
  
  private void writeResponse(String file, HttpExchange ex) throws Exception {
    // write response for the client request
    
    BufferedReader br = new BufferedReader(new FileReader(new File(getClass().getClassLoader().getResource(file)
        .getFile())));
    StringBuilder sb = new StringBuilder();
    while (br.ready()) {
      sb.append(br.readLine());
    }
    br.close();
    String html = sb.toString();
    ex.sendResponseHeaders(200, html.getBytes().length);
    ex.getResponseBody().write(html.getBytes());
    ex.getResponseBody().close();
  }
  
  /**
   * Extracts key value pairs from a String of form "a=b&c=124&data=adsfads2"
   * @param raw
   * @return
   */
  private Map<String, String> extractKeyValues(String raw) {
    // helper function for parsing cookie / request parameters
    
    Map<String, String> keyValues = new HashMap<>();
    if (raw != null && raw.length() > 0) {
      for (String keyValue : raw.split("&")) {
        String[] c = keyValue.trim().split("=");
        keyValues.put(c[0], c[1]);
      }
    }
    return keyValues;
  }
  
  public void stopServer() {
    server.stop(0);
  }
  
  public String getExpiredCookie() throws InvalidKeyException, NoSuchAlgorithmException {
    // create a cookie that is already expired
    
    StringBuilder sb = new StringBuilder();
    sb.append("session=exp=");
    long time = System.currentTimeMillis() / 1000 - 100; // Seconds since 1970 - 100 ==> already expired!
    sb.append(time);
    sb.append("&");
    String data = "dummy";//TODO: Replace with something useful
    sb.append("data="); 
    sb.append(data);
    sb.append("&");
    sb.append("digest=");
    sb.append(calculateDigest(time, data));
    sb.append(" ;HttpOnly;Secure");
    return sb.toString();
  }

}
