import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.net.URL;
import java.net.InetSocketAddress;
import java.security.ProtectionDomain;

public class JettyLauncher {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        String host = null;
        int port = 8080;
        int sslPort = 8443;
        InetSocketAddress address = null;
        String contextPath = "/";
        String tmpDirPath = "";
        String keystoreFilePath = "";
        String keystorePassword = "gitbucket";
        String keyPassword = "gitbucket";
        boolean forceHttps = false;

        for(String arg: args) {
            if(arg.startsWith("--") && arg.contains("=")) {
                String[] dim = arg.split("=");
                if(dim.length >= 2) {
                    switch (dim[0]) {
                        case "--host":
                            host = dim[1];
                            break;
                        case "--port":
                            port = Integer.parseInt(dim[1]);
                            break;
                        case "--prefix":
                            contextPath = dim[1];
                            if (!contextPath.startsWith("/")) {
                                contextPath = "/" + contextPath;
                            }
                            break;
                        case "--gitbucket.home":
                            System.setProperty("gitbucket.home", dim[1]);
                            break;
                        case "--temp_dir":
                            tmpDirPath = dim[1];
                            break;
                        case "--keystore_file":
                            keystoreFilePath = dim[1];
                            break;
                        case "--ssl_port":
                            sslPort = Integer.parseInt(dim[1]);
                            break;
                        case "--keystore_password":
                            keystorePassword = dim[1];
                            break;
                        case "--key_password":
                            keyPassword = dim[1];
                            break;
                    }
                }
            }
        }

        if(host != null) {
            address = new InetSocketAddress(host, port);
        } else {
            address = new InetSocketAddress(port);
        }

        Server server = new Server(address);

//        SelectChannelConnector connector = new SelectChannelConnector();
//        if(host != null) {
//            connector.setHost(host);
//        }
//        connector.setMaxIdleTime(1000 * 60 * 60);
//        connector.setSoLingerTime(-1);
//        connector.setPort(port);
//        server.addConnector(connector);

        WebAppContext context = new WebAppContext();

        File tmpDir;
        if(tmpDirPath.equals("")){
            tmpDir = new File(getGitBucketHome(), "tmp");
            if(!tmpDir.exists()){
                tmpDir.mkdirs();
            }
        } else {
            tmpDir = new File(tmpDirPath);
            if(!tmpDir.exists()){
                throw new java.io.FileNotFoundException(
                    String.format("temp_dir \"%s\" not found", tmpDirPath));
            } else if(!tmpDir.isDirectory()) {
                throw new IllegalArgumentException(
                    String.format("temp_dir \"%s\" is not a directory", tmpDirPath));
            }
        }
        context.setTempDirectory(tmpDir);

        ProtectionDomain domain = JettyLauncher.class.getProtectionDomain();
        URL location = domain.getCodeSource().getLocation();

        context.setContextPath(contextPath);
        context.setDescriptor(location.toExternalForm() + "/WEB-INF/web.xml");
        context.setServer(server);
        context.setWar(location.toExternalForm());
        if (forceHttps) {
            context.setInitParameter("org.scalatra.ForceHttps", "true");
        }

        // SSL
        if(!keystoreFilePath.isEmpty()) {
            HttpConfiguration http_config = new HttpConfiguration();

            HttpConfiguration https_config = new HttpConfiguration(http_config);
            https_config.addCustomizer(new SecureRequestCustomizer());

            SslContextFactory sslContextFactory = new SslContextFactory(keystoreFilePath);
            sslContextFactory.setKeyStorePassword(keystorePassword);
            sslContextFactory.setKeyManagerPassword(keyPassword);
            sslContextFactory.setTrustStorePath(keystoreFilePath);
            sslContextFactory.setTrustStorePassword(keystorePassword);
            sslContextFactory.setExcludeCipherSuites(
                "SSL_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_DSS_WITH_DES_CBC_SHA",
                "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA"
            );
            sslContextFactory.addExcludeProtocols("SSLv3");

            ServerConnector sslConnector = new ServerConnector(
              server,
              new SslConnectionFactory(sslContextFactory, "http/1.1"),
              new HttpConnectionFactory(https_config)
            );
            sslConnector.setPort(sslPort);
            sslConnector.setIdleTimeout(50000);
            server.addConnector(sslConnector);
        }

        server.setHandler(context);
        server.setStopAtShutdown(true);
        server.setStopTimeout(7_000);
        server.start();
        server.join();
    }

    private static File getGitBucketHome(){
        String home = System.getProperty("gitbucket.home");
        if(home != null && home.length() > 0){
            return new File(home);
        }
        home = System.getenv("GITBUCKET_HOME");
        if(home != null && home.length() > 0){
            return new File(home);
        }
        return new File(System.getProperty("user.home"), ".gitbucket");
    }

    private static void deleteDirectory(File dir){
        for(File file: dir.listFiles()){
            if(file.isFile()){
                file.delete();
            } else if(file.isDirectory()){
                deleteDirectory(file);
            }
        }
        dir.delete();
    }
}
