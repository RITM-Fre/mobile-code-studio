package com.example.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lightweight embedded HTTP/1.1 local development server implemented in Java.
 * Serves real project files from app-private storage exclusively on 127.0.0.1.
 */
public final class LocalHttpServerEngine {

    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("html", "text/html; charset=utf-8");
        MIME_TYPES.put("htm", "text/html; charset=utf-8");
        MIME_TYPES.put("css", "text/css; charset=utf-8");
        MIME_TYPES.put("js", "application/javascript; charset=utf-8");
        MIME_TYPES.put("mjs", "application/javascript; charset=utf-8");
        MIME_TYPES.put("json", "application/json; charset=utf-8");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("xml", "application/xml; charset=utf-8");
        MIME_TYPES.put("txt", "text/plain; charset=utf-8");
        MIME_TYPES.put("md", "text/markdown; charset=utf-8");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("webp", "image/webp");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("woff", "font/woff");
        MIME_TYPES.put("woff2", "font/woff2");
        MIME_TYPES.put("ttf", "font/ttf");
        MIME_TYPES.put("otf", "font/otf");
        MIME_TYPES.put("wasm", "application/wasm");
        MIME_TYPES.put("mp3", "audio/mpeg");
        MIME_TYPES.put("mp4", "video/mp4");
        MIME_TYPES.put("webm", "video/webm");
    }

    private final int port;
    private volatile File rootDirectory;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private ExecutorService workerPool;

    public LocalHttpServerEngine(File rootDirectory, int port) {
        this.rootDirectory = rootDirectory;
        this.port = port;
    }

    public void setRootDirectory(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return running.get();
    }

    public synchronized void start() throws IOException {
        if (running.get()) {
            return;
        }
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        serverSocket = new ServerSocket(port, 50, loopback);
        serverSocket.setReuseAddress(true);
        workerPool = Executors.newCachedThreadPool();
        running.set(true);

        acceptThread = new Thread(() -> {
            while (running.get()) {
                try {
                    Socket client = serverSocket.accept();
                    ExecutorService pool = workerPool;
                    if (pool != null && !pool.isShutdown()) {
                        pool.execute(() -> handleClient(client));
                    } else {
                        safeClose(client);
                    }
                } catch (IOException ignored) {
                    if (!running.get()) {
                        break;
                    }
                }
            }
        }, "MobileCodeStudio-HttpAccept-" + port);
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public synchronized void stop() {
        running.set(false);
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            serverSocket = null;
        }
        if (workerPool != null) {
            workerPool.shutdownNow();
            workerPool = null;
        }
        if (acceptThread != null) {
            acceptThread.interrupt();
            acceptThread = null;
        }
    }

    private void handleClient(Socket socket) {
        try {
            socket.setSoTimeout(10000);
            InputStream rawIn = new BufferedInputStream(socket.getInputStream());
            OutputStream rawOut = new BufferedOutputStream(socket.getOutputStream());

            String requestLine = readLine(rawIn);
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }

            // Drain remaining request headers
            String headerLine;
            while ((headerLine = readLine(rawIn)) != null && !headerLine.isEmpty()) {
                // Consume HTTP headers cleanly
            }

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                sendError(rawOut, 400, "Bad Request", "Malformed HTTP request line.");
                return;
            }

            String method = parts[0].toUpperCase(Locale.ROOT);
            String rawTarget = parts[1];

            if ("OPTIONS".equals(method)) {
                sendOptions(rawOut);
                return;
            }

            if (!"GET".equals(method) && !"HEAD".equals(method)) {
                sendError(rawOut, 405, "Method Not Allowed", "Only GET, HEAD, and OPTIONS are supported.");
                return;
            }

            boolean headOnly = "HEAD".equals(method);
            serveFile(rawOut, rawTarget, headOnly);
        } catch (IOException ignored) {
            // Client closed connection or timed out
        } finally {
            safeClose(socket);
        }
    }

    private void serveFile(OutputStream out, String rawTarget, boolean headOnly) throws IOException {
        File currentRoot = this.rootDirectory;
        if (currentRoot == null || !currentRoot.exists() || !currentRoot.isDirectory()) {
            sendError(out, 500, "Project Root Unavailable", "The selected project directory does not exist.");
            return;
        }

        int queryIndex = rawTarget.indexOf('?');
        String pathOnly = queryIndex >= 0 ? rawTarget.substring(0, queryIndex) : rawTarget;
        int fragmentIndex = pathOnly.indexOf('#');
        if (fragmentIndex >= 0) {
            pathOnly = pathOnly.substring(0, fragmentIndex);
        }

        String decodedPath;
        try {
            decodedPath = URLDecoder.decode(pathOnly, StandardCharsets.UTF_8.name());
        } catch (IllegalArgumentException e) {
            sendError(out, 400, "Bad Request", "Invalid URL encoding in path.");
            return;
        }

        if (decodedPath.startsWith("/")) {
            decodedPath = decodedPath.substring(1);
        }

        File rootCanonical = currentRoot.getCanonicalFile();
        File target = decodedPath.isEmpty()
            ? new File(rootCanonical, "index.html")
            : new File(rootCanonical, decodedPath);

        if (target.isDirectory()) {
            target = new File(target, "index.html");
        }

        File targetCanonical = target.getCanonicalFile();
        String rootPathWithSep = rootCanonical.getPath().endsWith(File.separator)
            ? rootCanonical.getPath()
            : rootCanonical.getPath() + File.separator;

        boolean insideRoot = targetCanonical.getPath().equals(rootCanonical.getPath())
            || targetCanonical.getPath().startsWith(rootPathWithSep);

        if (!insideRoot) {
            sendError(out, 403, "Forbidden", "Access outside the active project workspace is blocked.");
            return;
        }

        if (!targetCanonical.exists() || !targetCanonical.isFile()) {
            sendError(
                out,
                404,
                "404 Not Found",
                "Requested resource <code>" + escapeHtml(decodedPath.isEmpty() ? "index.html" : decodedPath) + "</code> was not found in this project."
            );
            return;
        }

        String mimeType = guessMimeType(targetCanonical.getName());
        long length = targetCanonical.length();

        StringBuilder headers = new StringBuilder();
        headers.append("HTTP/1.1 200 OK\r\n");
        headers.append("Content-Type: ").append(mimeType).append("\r\n");
        headers.append("Content-Length: ").append(length).append("\r\n");
        headers.append("Cache-Control: no-store, no-cache, must-revalidate\r\n");
        headers.append("Pragma: no-cache\r\n");
        headers.append("X-Content-Type-Options: nosniff\r\n");
        headers.append("Connection: close\r\n\r\n");

        out.write(headers.toString().getBytes(StandardCharsets.US_ASCII));

        if (!headOnly) {
            byte[] buffer = new byte[16384];
            try (FileInputStream fis = new FileInputStream(targetCanonical)) {
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
        }
        out.flush();
    }

    private void sendOptions(OutputStream out) throws IOException {
        String response = "HTTP/1.1 204 No Content\r\n"
            + "Allow: GET, HEAD, OPTIONS\r\n"
            + "Connection: close\r\n\r\n";
        out.write(response.getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    private void sendError(OutputStream out, int code, String title, String detailHtml) throws IOException {
        String html = "<!DOCTYPE html><html><head><meta charset='utf-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<title>" + code + " " + escapeHtml(title) + "</title>"
            + "<style>body{margin:0;min-height:100vh;display:grid;place-items:center;"
            + "font-family:system-ui,-apple-system,sans-serif;background:#090f1d;color:#f8fafc;padding:24px}"
            + ".box{max-width:460px;padding:24px;border-radius:18px;background:rgba(255,255,255,.07);"
            + "border:1px solid rgba(255,255,255,.16)}h1{margin:0 0 8px;color:#38bdf8;font-size:20px}"
            + "p{margin:0;line-height:1.55;color:#cbd5e1}code{color:#f472b6}</style></head>"
            + "<body><div class='box'><h1>" + code + " &middot; " + escapeHtml(title) + "</h1>"
            + "<p>" + detailHtml + "</p></div></body></html>";

        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + code + " " + title + "\r\n"
            + "Content-Type: text/html; charset=utf-8\r\n"
            + "Content-Length: " + body.length + "\r\n"
            + "Cache-Control: no-store\r\n"
            + "Connection: close\r\n\r\n";
        out.write(headers.getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }

    private static String guessMimeType(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0 && dot < fileName.length() - 1) {
            String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
            String found = MIME_TYPES.get(ext);
            if (found != null) {
                return found;
            }
        }
        return "application/octet-stream";
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int prev = -1;
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') {
                break;
            }
            if (prev == '\r') {
                buffer.write('\r');
            }
            if (b != '\r') {
                buffer.write(b);
            }
            prev = b;
            if (buffer.size() > 8192) {
                break;
            }
        }
        if (b == -1 && buffer.size() == 0) {
            return null;
        }
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    private static String escapeHtml(String input) {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private static void safeClose(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
