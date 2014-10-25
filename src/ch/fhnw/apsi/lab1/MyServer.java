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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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

  static {
    USERS.put("fankiflo%40gmail.com", "1234");
    USERS.put("admin%40fhnw.ch", "1234");
  }

  public static void main(String[] args) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
    server.createContext("/lab1", new MyServer());
    server.setExecutor(null); // creates a default executor
    server.start();
  }

  public void handle(HttpExchange ex) throws IOException {
    try {
      debugHeaders(ex);
      Map<String, String> cookies = parseCookies(ex);
      Map<String, String> params = parseRequestParams(ex);
      if (validateCookie(cookies)) {
        writeResponse("secret.html", ex);
      } else {
        if (login(ex, params)) {
          writeResponse("secret.html", ex);
        }
        writeResponse("form.html", ex);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private boolean login(HttpExchange ex, Map<String, String> params) {
    if (params.containsKey("mail") && params.containsKey("password")) {
      if (USERS.get(params.get("mail")).equals(params.get("password"))) {
        ex.getResponseHeaders().set("Set-Cookie", "session=123456789");
        return true;
      }
    }
    return false;
  }

  private boolean validateCookie(Map<String, String> cookies) {
    if (cookies.containsKey("session")) {
      String session = cookies.get("session");
      return "123456789".equals(session);
    }
    return false;
  }

  public void debugHeaders(HttpExchange ex) throws IOException {
    System.out.println("###################### Request - " + (new Date()) + " #############################");
    for (String s : ex.getRequestHeaders().keySet()) {
      System.out.println(s + ": " + ex.getRequestHeaders().get(s));
    }
  }

  public void writeResponse(String file, HttpExchange ex) throws Exception {
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

  public Map<String, String> parseCookies(HttpExchange ex) {
    Map<String, String> cookies = new HashMap<>();
    if (ex.getRequestHeaders().containsKey("Cookie")) {
      System.out.println("------ Cookies parsed -----");
      String cookiesString = (String) ((LinkedList) ex.getRequestHeaders().get("Cookie")).getFirst();
      for (String cookieString : cookiesString.split(";")) {
        String[] c = cookieString.trim().split("=");
        cookies.put(c[0], c[1]);
        System.out.println(c[0] + ": " + c[1]);
      }
    }
    return cookies;
  }

  public Map<String, String> parseRequestParams(HttpExchange ex) throws IOException {
    Map<String, String> params = new HashMap<>();
    System.out.println("------ Params parsed -----");
    // Get params
    String query = ex.getRequestURI().getQuery();
    if (query.length() > 0) {
      for (String cookieString : query.split("&")) {
        String[] c = cookieString.trim().split("=");
        params.put(c[0], c[1]);
        System.out.println(c[0] + ": " + c[1]);
      }
    }
    
    // Post params
    BufferedReader br = new BufferedReader(new InputStreamReader(ex.getRequestBody()));
    StringBuilder sb = new StringBuilder();
    while (br.ready()) {
      sb.append(br.readLine());
    }
    String body = sb.toString();
    if (body.length() > 0) {
      System.out.println("------ Params parsed -----");
      for (String cookieString : body.split("&")) {
        String[] c = cookieString.trim().split("=");
        params.put(c[0], c[1]);
        System.out.println(c[0] + ": " + c[1]);
      }
    }
    return params;
  }
}
