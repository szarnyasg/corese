/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.inria.edelweiss.kgramserver.webservice;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.vfs.AllFileSelector;
import org.apache.commons.vfs.FileDepthSelector;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.VFS;
import org.apache.log4j.Logger;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

/**
 * Embedded HTTP server for Corese. Using Jetty implementation.
 * Modified: add the options to enable SSL connection, Fuqi Song, 21 Jan 2015
 * 
 * @author alban.gaignard@cnrs.fr
 */
public class EmbeddedJettyServer {

    private static Logger logger = Logger.getLogger(EmbeddedJettyServer.class);
    static int port = 8080;
    private static boolean entailments = false;
    private static boolean owlrl = false;
    private static String dataPath = null;
    private static String localProfile = null;
    
    // profile.ttl may contain data to be loaded
    // use -lp option to effectively load profile data
    // default is do not load
    private static boolean loadProfileData = false;
    
    //options for SSL connection
    private static boolean enableSsl = false;
    private static int portSsl = 8443;
    private static String keystore, password;
    
    public static URI resourceURI;
    public final static String HOME_PAGE = SPARQLRestAPI.isAjax? "demo_new.html" : "demo.html";

    public static void main(String args[]) throws Exception {

//        PropertyConfigurator.configure(EmbeddedJettyServer.class.getClassLoader().getResource("log4j.properties"));
//        logger.debug("Started.");
        Options options     = new Options();
        Option portOpt      = new Option("p", "port", true, "specify the server port");
        Option helpOpt      = new Option("h", "help", false, "print this message");
        Option entailOpt    = new Option("e", "entailments", false, "enable RDFS entailments");
        Option owlrlOpt     = new Option("o", "owlrl", false, "enable OWL RL entailments");
        Option dataOpt      = new Option("l", "load", true, "data file or directory to be loaded");
        Option profileOpt   = new Option("lp", "profile", false, "load profile data");
        Option locProfileOpt= new Option("pp", "profile", true, "local profile");
        Option versionOpt   = new Option("v", "version", false, "print the version information and exit");
        
        Option sslOpt       = new Option("ssl", "ssl", false, "enable ssl connection ?");
        Option portSslOpt   = new Option("pssl", "pssl", true, "port of ssl connection");
        Option keystoreOpt  = new Option("jks", "keystore", true, "java key store name (../keystore/xxx)");
        Option keypasswordOpt = new Option("pwd", "password", true, "java key store password (key, store, trust store)");
       
        options.addOption(portOpt);
        options.addOption(entailOpt);
        options.addOption(owlrlOpt);
        options.addOption(dataOpt);
        options.addOption(profileOpt);
        options.addOption(locProfileOpt);
        options.addOption(helpOpt);
        options.addOption(versionOpt);
        
        options.addOption(sslOpt);
        options.addOption(portSslOpt);
        options.addOption(keystoreOpt);
        options.addOption(keypasswordOpt);

        String header = "Once launched, the server can be managed through a web user interface, available at http://localhost:<PortNumber>\n\n";
        String footer = "\nPlease report any issue to alban.gaignard@cnrs.fr";

        try {
            CommandLineParser parser = new BasicParser();
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("kgserver", header, options, footer, true);
                System.exit(0);
            }
            if (cmd.hasOption("p")) {
                port = Integer.parseInt(cmd.getOptionValue("p"));
            }
            if (cmd.hasOption("v")) {
                logger.info("version 1.0.7");
                System.exit(0);
            }
            if (cmd.hasOption("e")) {
                entailments = true;
            }
            if (cmd.hasOption("o")) {
                owlrl = true;
            }
            if (cmd.hasOption("l")) {
                // deprecated load
                dataPath = cmd.getOptionValue("l");
                System.out.println("Server: " + dataPath);
            }
            if (cmd.hasOption("lp")) {
                // load st:default server content into SPARQL endpoint
                loadProfileData = true;
            }
            //Option for SSL connection
            if (cmd.hasOption("ssl")) {
                enableSsl = true;
                if (cmd.hasOption("pssl")) {
                    portSsl = Integer.parseInt(cmd.getOptionValue("pssl"));
                }

                if (cmd.hasOption("jks")) {
                    keystore = cmd.getOptionValue("jks");
                } else {
                    throw new ParseException("Please specify the path of keystore for SSL.");
                }

                if (cmd.hasOption("pwd")) {
                    password = cmd.getOptionValue("pwd");
                } else {
                    throw new ParseException("Please specify the password of keystore for SSL.");
                }
            }
            if (cmd.hasOption("pp")) {
                // user defined profile.ttl to define additional servers
                localProfile = cmd.getOptionValue("pp");
                System.out.println("Profile: " + localProfile);
            }  
            URI webappUri = EmbeddedJettyServer.extractResourceDir("webapp", true);
            System.out.println("Jetty: " + webappUri);
            Server server = new Server(port);

            ServletHolder jerseyServletHolder = new ServletHolder(ServletContainer.class);
            jerseyServletHolder.setInitParameter("com.sun.jersey.config.property.resourceConfigClass", "com.sun.jersey.api.core.PackagesResourceConfig");
            jerseyServletHolder.setInitParameter("com.sun.jersey.config.property.packages", "fr.inria.edelweiss.kgramserver.webservice");
            jerseyServletHolder.setInitParameter("requestBufferSize", "8192");
            jerseyServletHolder.setInitParameter("headerBufferSize", "8192");
            //Context servletCtx = new Context(server, "/kgram", Context.SESSIONS);
            Context servletCtx = new Context(server, "/", Context.SESSIONS);
            servletCtx.addServlet(jerseyServletHolder, "/*");
            logger.info("----------------------------------------------");
            //logger.info("Corese/KGRAM endpoint started on http://localhost:" + port + "/kgram");
            logger.info("Corese/KGRAM endpoint started on http://localhost:" + port + "/");
            logger.info("----------------------------------------------");

            ResourceHandler resource_handler = new ResourceHandler();
            resource_handler.setWelcomeFiles(new String[]{HOME_PAGE, "index.html"});
//            resource_handler.setResourceBase("/Users/gaignard/Documents/Dev/svn-kgram/Dev/trunk/kgserver/src/main/resources/webapp");
            resource_handler.setResourceBase(webappUri.getRawPath());
            ContextHandler staticContextHandler = new ContextHandler();
            staticContextHandler.setContextPath("/");
            staticContextHandler.setHandler(resource_handler);
            logger.info("----------------------------------------------");
            logger.info("Corese/KGRAM webapp UI started on http://localhost:" + port);
            logger.info("----------------------------------------------");

            HandlerList handlers = new HandlerList();
            handlers.setHandlers(new Handler[]{staticContextHandler, servletCtx});
            server.setHandler(handlers);

            // === SSL Connector begin ====
            if (enableSsl) {
                SslSocketConnector sslConnector = new SslSocketConnector();
                sslConnector.setPort(portSsl);
                sslConnector.setServer(server);
                sslConnector.setKeystore(webappUri.getRawPath() + "/keystore/" + keystore);
                sslConnector.setKeyPassword(password);
                sslConnector.setPassword(password);
                sslConnector.setTruststore(webappUri.getRawPath() + "/keystore/" + keystore);
                sslConnector.setTrustPassword(password);

                server.addConnector(sslConnector);
                logger.info("Corese SSL connection https://localhost:" + portSsl);
            }
            // === SSL Connector end ====

            server.start();

            //server initialization
            ClientConfig config = new DefaultClientConfig();
            Client client = Client.create(config);
            //WebResource service = client.resource(new URI("http://localhost:" + port+"/kgram"));
            WebResource service = client.resource(new URI("http://localhost:" + port + "/"));

            MultivaluedMap formData = new MultivaluedMapImpl();
            formData.add("entailments", Boolean.toString(entailments));
            formData.add("owlrl", Boolean.toString(owlrl));
            formData.add("load", Boolean.toString(loadProfileData));
            if (localProfile != null){
                formData.add("profile", localProfile);
            }
            
            service.path("sparql").path("reset").post(formData);

            if (dataPath != null) {
                String[] lp = dataPath.split(";");
                for (String p : lp) {
                    formData = new MultivaluedMapImpl();
                    formData.add("remote_path", p);
                    service.path("sparql").path("load").post(formData);
                }
            }

            server.join();

        } catch (ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
        }
    }

    public static URI extractResourceDir(String dirname, boolean overwrite) throws FileSystemException, URISyntaxException {
        URL dir_url = EmbeddedJettyServer.class.getClassLoader().getResource(dirname);
        FileObject dir_jar = VFS.getManager().resolveFile(dir_url.toString());
        String tempDir = FileUtils.getTempDirectory() + File.separator + System.getProperty("user.name");
        FileObject tmpF = VFS.getManager().resolveFile(tempDir);
        FileObject localDir = tmpF.resolveFile(dirname);
        if (!localDir.exists()) {
            logger.info("Extracting directory " + dirname + " to " + tmpF.getName());
            localDir.createFolder();
            localDir.copyFrom(dir_jar, new AllFileSelector());
        } else {
            if (overwrite) {
                logger.info("Overwritting directory " + dirname + " in " + tmpF.getName());
                localDir.delete(new FileDepthSelector(0, 5));
                localDir.createFolder();
                localDir.copyFrom(dir_jar, new AllFileSelector());
            }
        }
        resourceURI = localDir.getURL().toURI();
        return resourceURI;
    }
}
