package lt.saltyjuice.dragas.ffmpeg.restreamer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;

public class Main {


    final static public void main(String[] args) throws IOException, InterruptedException {
        HttpServer server = HttpServer.create(new InetSocketAddress(9001), 0);
        server.createContext("/", new FileServlet("index.html", "text/html"));

        server.createContext("/packet/playlist.m3u8", new FileServlet("playlist.m3u8", "vnd.apple.mpegurl"));
        server.createContext("/packet", new WildcardFileServlet("playlist(\\d+)\\.ts", "vnd.apple.mpegurl"));
        server.setExecutor(null);
        server.start();
    }

    /**
     * File servlet implementation which is supposed to return "resource-like" files.
     */
    private static class WildcardFileServlet extends FileServlet {

        private WildcardFileServlet(String mask, String mimetype) throws IOException {
            super(mask, mimetype);
        }

        @Override
        public void handleGet(HttpExchange httpExchange) throws IOException {
            String filename = getFilename();
            String[] pathFragments = httpExchange.getRequestURI().getPath().split("/"); // prevents tree walking attack
            String requiredFilename = pathFragments[pathFragments.length - 1];
            if(!requiredFilename.matches(filename))
                handle404(httpExchange);
            else {
                byte[] file = null;
                try {
                    file = readFile(requiredFilename);
                } catch(IOException err) {
                    handle500(httpExchange, err);
                }
                super.handleGet(httpExchange, file);
            }

        }
    }

    private static class FileServlet implements BasicHttpHandler {


        private final String mimeType;
        private final String filename;

        private FileServlet(final String filename, final String mimeType) throws IOException {
            this.mimeType = mimeType;
            this.filename = filename;
        }

        /**
         * returns content of required file in `target` directory
         * @param filename
         * @return
         * @throws IOException
         */
        protected byte[] readFile(String filename) throws IOException {
            return Files.readAllBytes(new File("target/"+filename).toPath());
        }

        @Override
        public void handleGet(HttpExchange httpExchange) throws IOException {
            System.out.println("file expected: " + httpExchange.getRequestURI() + " file requested: " + filename);
            byte[] body = null;
            try {
                body = readFile(getFilename());
            }
            catch (IOException err) {
                handle500(httpExchange, err);
                return;
            }
            handleGet(httpExchange, body);
        }


        @Override
        public void handleGet(HttpExchange httpExchange, byte[] body) throws IOException {
            httpExchange.getResponseHeaders().add("Content-Type", getMimeType());
            httpExchange.sendResponseHeaders(200, body.length);
            httpExchange.getResponseBody().write(body);
            httpExchange.getResponseBody().close();
        }

        /**
         * adds 500 error response handling in case an exception happens.
         * @param httpExchange
         * @param err
         * @throws IOException
         */
        void handle500(HttpExchange httpExchange, Exception err) throws IOException {
            err.printStackTrace();
            //httpExchange.getResponseHeaders().add("Content-Type", this.mimeType);
            httpExchange.sendResponseHeaders(500, 0);
            //httpExchange.getResponseBody().write(body.getBytes());
            httpExchange.getResponseBody().close();
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getFilename() {
            return filename;
        }
    }

    /**
     * Provides a simple lifecycle for HTTP requests. In addition
     * adds CORS support in case this becomes a problem.
     */
    private static interface BasicHttpHandler extends HttpHandler {
        static final String GET = "GET";
        static final String OPTIONS = "OPTIONS";

        @Override
        default void handle(HttpExchange httpExchange) throws IOException {
            final String method = httpExchange.getRequestMethod();
            System.out.println("requested with " + method + " at " + httpExchange.getRequestURI());
            switch (method) {
                case GET:
                    handleGet(httpExchange);
                    break;
                case OPTIONS:
                    handleOptions(httpExchange);
                    break;
                default:
                    handle404(httpExchange);
                    break;
            }
        }

        /**
         * Generates a simple CORS response that permits get and options
         * requests on all URLs
         * @param httpExchange
         * @throws IOException
         */
        default void handleOptions(HttpExchange httpExchange) throws IOException {
            httpExchange.getResponseHeaders().add("Allow", OPTIONS + ", " + GET);
            httpExchange.sendResponseHeaders(200, 0);
            httpExchange.getResponseBody().close();
        }

        /**
         * Handles get requests. Implementations should handle themselves what they're supposed to return.
         * @param httpExchange
         * @throws IOException
         */
        void handleGet(HttpExchange httpExchange) throws IOException;

        /**
         * Returns 404 response
         * @param exchange
         * @throws IOException
         */
        default void handle404(HttpExchange exchange) throws IOException {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().close();
        }

        /**
         * A stub for get requests that return a body
         * @param httpExchange
         * @param body
         * @throws IOException
         */
        void handleGet(HttpExchange httpExchange, byte[] body) throws IOException;
    }
}
