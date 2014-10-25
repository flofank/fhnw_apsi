package ch.fhnw.apsi.lab1;

/*
 * Copyright 2013 - 2014 by PostFinance Ltd - All rights reserved
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/*
 * Copyright 2013 - 2014 by PostFinance Ltd - All rights reserved
 */
import com.sun.net.httpserver.HttpServer;

/**
 * TODO
 * 
 * @author TODO
 */
public class MyServer implements HttpHandler {
  private static final Map<String, String> USERS = new HashMap<>();
  private static final long COOKIE_VALID_TIME = 600; // 5 Minutes
  private static final String SHA_SALT = "oeschgerfankhauserapsilab1";

  static {
    USERS.put("test%40test.com", "1234");
  }

  public static void main(String[] args) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
    server.createContext("/lab1", new MyServer());
    server.setExecutor(null); // creates a default executor
    server.start();
  }

  public void handle(HttpExchange ex) throws IOException {
    try {
//      debugHeaders(ex);
      Map<String, String> cookies = parseCookies(ex);
      Map<String, String> params = parseRequestParams(ex);
      if (validateCookie(cookies)) {
        writeResponse("secret.html", ex);
      } else {
        if (login(ex, params)) {
          writeResponse("secret.html", ex);
        } else {
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
        cookies.put(c[0], c[1]);
        System.out.println(c[0] + ": " + c[1]);
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
          return true;
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
    while (br.ready()) {
      sb.append(br.readLine());
    }
    String body = sb.toString();
    params.putAll(extractKeyValues(body));
    return params;
  }
  
  private String createCookie(HttpExchange ex) throws NoSuchAlgorithmException {
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
    return sb.toString();
  }
  
  private String calculateDigest(long time, String data) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    md.update(Long.toString(time).getBytes());
    md.update(data.getBytes());
    md.update(SHA_SALT.getBytes());
    return DatatypeConverter.printHexBinary(md.digest());
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
}
