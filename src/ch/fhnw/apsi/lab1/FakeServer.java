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
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

/**
 * TODO
 * 
 * @author TODO
 */
public class FakeServer implements HttpHandler {
  private static final Map<String, String> USERS = new HashMap<>();
  private static final long COOKIE_VALID_TIME = 600; // 5 Minutes
  private static final String SHA_SALT = "oeschgerfankhauserapsilab1";
  private static final Logger LOGGER = Logger.getLogger(FakeServer.class.getName());
  private static KeyGenerator keyGenerator;
  private static SecretKey secretKey;
  private HttpsServer server;

  static {
    USERS.put("test%40test.com", "1234");
    USERS.put("test@test.com", "1234");
  }

  public void startServer() throws IOException {
    // ATTACKER CLAIMS TO BE THE SERVER (MAN-IN-THE-MIDDLE ATTACK)
    // initialize SSL context with a self-singed server certificate (stored in resources/Fake.keyStore)
    // server listens to https://localhost:8000/lab1
    
    server = HttpsServer.create(new InetSocketAddress(8000), 0);
    
    try {
      char[] passphrase = "mypassphrase".toCharArray();
      KeyStore ks = KeyStore.getInstance("JKS");
      ks.load(new FileInputStream("resources/Fake.keystore"), passphrase);
  
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
    
    server.createContext("/lab1", new FakeServer());
    server.setExecutor(null); // creates a default executor
    
    LOGGER.log(Level.INFO,"Fake Server initialized");
    
    server.start();
  }

  public void handle(HttpExchange ex) throws IOException {
    // handle client requests
    // THIS SHOULD NEVER BE CALLED, CLIENT MUST DETECT ATTACK!!    
    LOGGER.log(Level.SEVERE, "Fake Server is actually handling a client request!!");
  }
  
  public void stopServer() {
     server.stop(0);
  }

}
