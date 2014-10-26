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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

/**
 * TODO
 * 
 * @author TODO
 */
public class MyHttpsServer implements HttpHandler {
  private static final Map<String, String> USERS = new HashMap<>();
  private static final long COOKIE_VALID_TIME = 600; // 5 Minutes
  private static final String SHA_SALT = "oeschgerfankhauserapsilab1";
  private static KeyGenerator keyGenerator;
  private static SecretKey secretKey;

  static {
    USERS.put("test%40test.com", "1234");
    USERS.put("test@test.com", "1234");
  }

  public static void main(String[] args) throws IOException {
    HttpsServer server = HttpsServer.create(new InetSocketAddress(8000), 0);
    
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
      System.out.println("KeyStoreException");
    } catch (NoSuchAlgorithmException e) {
      System.out.println("NoSuchAlgorithmException");
    } catch (CertificateException e) {
      System.out.println("CertificateException");
    } catch (UnrecoverableKeyException e) {
      System.out.println("UnrecoverableKeyException");
    } catch (KeyManagementException e) {
      System.out.println("KeyManagementException");
    }
    System.out.println("SSL initialized");
    
    server.createContext("/lab1", new MyHttpsServer());
    server.setExecutor(null); // creates a default executor
    server.start();
  }

  public void handle(HttpExchange ex) throws IOException {
    try {
//      debugHttpExchange(ex);
//      debugHeaders(ex);
      Map<String, String> cookies = parseCookies(ex);      
      Map<String, String> params = parseRequestParams(ex);
      if (validateCookie(cookies)) {
        System.out.println("Sending secret.html (cookie)");
        writeResponse("secret.html", ex);
      } else {
        if (login(ex, params)) {
          System.out.println("Sending secret.html (login)");
          writeResponse("secret.html", ex);
        } else {
          System.out.println("Sending form.html");
          writeResponse("form.html", ex);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private boolean login(HttpExchange ex, Map<String, String> params) throws NoSuchAlgorithmException {
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
    try {
      if (cookies.containsKey("session")) {
        String session = cookies.get("session");
        Map<String, String> cookieParams = extractKeyValues(session);
        long time = Long.valueOf(cookieParams.get("exp"));
        String data = cookieParams.get("data");
        if (cookieParams.get("digest").endsWith(calculateDigest(time, data))) {
          if (time >= System.currentTimeMillis()/1000) {
            System.out.println("Cookie validated");
            System.out.println(session);
            return true;
          } else {
            System.out.println("Cookie expired");
            return false;
          }
        }
      }
    } catch (Exception e) {
    }
    return false;
  }

  private Map<String, String> parseRequestParams(HttpExchange ex) throws IOException {
    Map<String, String> params = new HashMap<>();
    // Get params
    String query = ex.getRequestURI().getQuery();
    params.putAll(extractKeyValues(query));
    // Post params
    BufferedReader br = new BufferedReader(new InputStreamReader(ex.getRequestBody()));
    StringBuilder sb = new StringBuilder();
//    while (br.ready()) {
//      sb.append(br.readLine());
//    }
    String line;
    while((line = br.readLine()) != null) {
      sb.append(line);
    }
    String body = sb.toString();
    params.putAll(extractKeyValues(body));
    return params;
  }
  
  private String createCookie(HttpExchange ex) throws NoSuchAlgorithmException, InvalidKeyException {
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
    
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(secretKey);
    mac.update(Long.toString(time).getBytes());
    mac.update(data.getBytes());
    mac.update(SHA_SALT.getBytes());
    return DatatypeConverter.printHexBinary(mac.doFinal());
    
  }
  
  private void writeResponse(String file, HttpExchange ex) throws Exception {
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
    Map<String, String> keyValues = new HashMap<>();
    if (raw != null && raw.length() > 0) {
      for (String keyValue : raw.split("&")) {
        String[] c = keyValue.trim().split("=");
        keyValues.put(c[0], c[1]);
      }
    }
    return keyValues;
  }
  
//private void debugHeaders(HttpExchange ex) throws IOException {
//  System.out.println("###################### Request - " + (new Date()) + " #############################");
//  for (String s : ex.getRequestHeaders().keySet()) {
//    System.out.println(s + ": " + ex.getRequestHeaders().get(s));
//  }
//}
  
//  private void debugHttpExchange(HttpExchange ex) throws IOException {
//    System.out.println("HttpExchange");
//    System.out.println("============");
//    System.out.println("Header:");
//    for (Entry<String, List<String>> list : ex.getRequestHeaders().entrySet()) {
//      for (String s : list.getValue()) {
//        System.out.println("\t"+s);
//      }
//    }
//    System.out.println("Body:");
//    BufferedReader rd = new BufferedReader(new InputStreamReader(ex.getRequestBody()));
//    String line;
//    while((line = rd.readLine()) != null) {
//      System.out.println("\t"+line);
//    }
//    System.out.println("Method:");
//    System.out.println("\t"+ex.getRequestMethod().toString());
//    System.out.println("URI:");
//    System.out.println("\t"+ex.getRequestURI().toString());
//    System.out.println("Query:");
//    System.out.println("\t"+ex.getRequestURI().getQuery());
//  }
}
