package com.nadeem.app;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import org.apache.wicket.util.time.Duration;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;

public class Start {

    public static void main(String[] args) throws Exception {
        int timeout = (int) Duration.ONE_HOUR.getMilliseconds();

        SocketConnector connector = newConnector(timeout);
        Server server = newServer(timeout, connector);

        addSecureSupport(timeout, connector, server);

        addJmxSupport(server);

        try {
            System.out.println(">>> STARTING EMBEDDED JETTY SERVER, PRESS ANY KEY TO STOP");
            server.start();
            System.in.read();
            System.out.println(">>> STOPPING EMBEDDED JETTY SERVER");
            server.stop();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void addJmxSupport(Server server) throws Exception
    {
        // START JMX SERVER
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        MBeanContainer mBeanContainer = new MBeanContainer(mBeanServer);
        server.getContainer().addEventListener(mBeanContainer);
        mBeanContainer.start();
    }

    private static void addSecureSupport(int timeout, SocketConnector connector, Server server)
    {
        Resource keystore = Resource.newClassPathResource("/keystore");
        if (keystore != null && keystore.exists()) {
            // if a keystore for a SSL certificate is available, start a SSL
            // connector on port 8443.
            // By default, the quickstart comes with a Apache Wicket Quickstart
            // Certificate that expires about half way september 2021. Do not
            // use this certificate anywhere important as the passwords are
            // available in the source.

            connector.setConfidentialPort(8443);

            SslContextFactory factory = new SslContextFactory();
            factory.setKeyStoreResource(keystore);
            factory.setKeyStorePassword("wicket");
            factory.setTrustStoreResource(keystore);
            factory.setKeyManagerPassword("wicket");
            SslSocketConnector sslConnector = new SslSocketConnector(factory);
            sslConnector.setMaxIdleTime(timeout);
            sslConnector.setPort(8443);
            sslConnector.setAcceptors(4);
            server.addConnector(sslConnector);

            System.out.println("SSL access to the quickstart has been enabled on port 8443");
            System.out.println("You can access the application using SSL on https://localhost:8443");
            System.out.println();
        }
    }

    private static Server newServer(int timeout, SocketConnector connector) {
        Server server = new Server(); 
        server.setHandler(newHandlers(server));
        server.addConnector(connector);
        return server;
    }
 
    private static HandlerList newHandlers(Server server) {
        WebAppContext appContext = newWebAppContext(server);

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {appContext, proxyHandler(), new DefaultHandler()});
        return handlers;
    }
    
    private static SocketConnector newConnector(int timeout) {
        SocketConnector connector = new SocketConnector();
 
        // Set some timeout options to make debugging easier.
        connector.setMaxIdleTime(timeout);
        connector.setSoLingerTime(-1);
        connector.setPort(8080);
        
        return connector;
        
    }
 
    private static WebAppContext newWebAppContext(Server server) {
        WebAppContext bb = new WebAppContext();
        bb.setServer(server);
        bb.setContextPath("/webApp");
        bb.setWar("src/main/webapp");
        return bb;
    }

    private static ServletContextHandler proxyHandler() {
        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setServletHandler(newServletHandler());
        
        return contextHandler;
    }
    
    private static ServletHandler newServletHandler() {
        
        
        ServletHandler handler = new ServletHandler();
        
        ServletHolder holder = handler.addServletWithMapping(org.eclipse.jetty.servlets.ProxyServlet.Transparent.class, "/proxy/*");
        holder.setInitParameter("ProxyTo", "http://localhost:8080/webApp");
        holder.setInitParameter("Prefix", "/proxy");        

        return handler;
    }
}
