package com.Ragex.mcheliloadermodrinth;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
//modrinth
@Mod(
        modid = "modrinthloader",
        name = "Modrinth Loader",
        version = "1.0",
        dependencies = "required-after:Forge@[10.13.2.1230,)"
)
public class mcheliloadermodrinth {

    private static final Logger LOGGER = LogManager.getLogger("ModrinthLoader");

    // IMPORTANT: this must be the GitHub Releases asset URL (not the HTML page). Example below.
    private static final String CF_LOADER_URL =
            "https://github.com/RagexPrince683/loadermodformcheli/releases/download/KILL/mcheliloader-MCHO+v1.8.2.jar";

    private static final String CF_LOADER_NAME = "mcheliloader.jar";

    // Optional: if you know the exact size or SHA-256, fill these in for stronger validation.
    private static final long   MIN_EXPECTED_BYTES = 500L * 1024L * 1024L; // 500 MB minimum sanity check
    private static final String EXPECTED_SHA256    = null; // put hash here if you have it (else leave null)

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File mcDir = event.getModConfigurationDirectory().getParentFile();
        Path modsDir = mcDir.toPath().resolve("mods");
        Path finalFile = modsDir.resolve(CF_LOADER_NAME);

        try {
            Files.createDirectories(modsDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create mods directory", e);
        }

        try {
            // If already present and valid, skip
            if (Files.exists(finalFile) && isJarValid(finalFile)) {
                LOGGER.info("Mcheli loader already present and valid. Skipping download.");
                return;
            }
        } catch (IOException ignored) {}

        // Download thread
        Thread downloadThread = new Thread(() -> {
            try {
                Path tempFile = finalFile.resolveSibling(CF_LOADER_NAME + ".part");
                URL resolved = resolveFinalURL(CF_LOADER_URL);
                long remoteSize = probeRemoteSize(resolved);
                downloadToFileWithResume(resolved, tempFile, remoteSize, null); // No GUI progress bar

                if (!isJarValid(tempFile)) {
                    throw new IOException("Downloaded file is not a valid JAR");
                }

                Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                LOGGER.info("Mcheli loader installed successfully.");
            } catch (Throwable t) {
                LOGGER.error("Download failed", t);
                throw new RuntimeException("Mcheli Loader download failed: " + t.getMessage(), t);
            }
        }, "Mcheli-Loader-Downloader");

        downloadThread.start();

        // Wait for download to finish safely
        try {
            downloadThread.join(); // Blocks until fully downloaded
        } catch (InterruptedException e) {
            throw new RuntimeException("Download interrupted", e);
        }

        // Verify one last time
        try {
            if (Files.exists(finalFile) && isJarValid(finalFile)) {
                LOGGER.info("Mcheli Loader installed. Please restart your game.");
                JOptionPane.showMessageDialog(null,
                        "Mcheli Loader installed successfully.\nPlease restart your game.",
                        "Mcheli Loader",
                        JOptionPane.INFORMATION_MESSAGE);
                throw new RuntimeException("Restart required after installing Mcheli Loader.");
            } else {
                throw new RuntimeException("Mcheli Loader not installed correctly.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Verification failed", e);
        }
    }


    // === Networking helpers ===

    private static URL resolveFinalURL(String original) throws IOException {
        URL current = new URL(original);
        for (int i = 0; i < 10; i++) {
            HttpURLConnection c = (HttpURLConnection) current.openConnection();
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent", "Mozilla/5.0");
            c.setRequestProperty("Accept", "application/octet-stream");
            c.setRequestProperty("Accept-Encoding", "identity"); // avoid gzip'd HTML
            c.connect();
            int code = c.getResponseCode();
            if (code >= 300 && code < 400) {
                String loc = c.getHeaderField("Location");
                if (loc == null) throw new IOException("Redirect with no Location header");
                current = new URL(current, loc);
                c.disconnect();
            } else {
                c.disconnect();
                return current;
            }
        }
        throw new IOException("Too many redirects resolving final URL");
    }

    private static long probeRemoteSize(URL url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestProperty("User-Agent", "Mozilla/5.0");
        c.setRequestProperty("Accept", "application/octet-stream");
        c.setRequestProperty("Accept-Encoding", "identity");
        // Range trick: ask for just the first byte so server returns Content-Range: bytes 0-0/TOTAL
        c.setRequestProperty("Range", "bytes=0-0");
        c.connect();
        try {
            int code = c.getResponseCode();
            if (code == 206) {
                String cr = c.getHeaderField("Content-Range"); // e.g. bytes 0-0/123456
                if (cr != null) {
                    int slash = cr.lastIndexOf('/');
                    if (slash > 0 && slash < cr.length() - 1) {
                        try { return Long.parseLong(cr.substring(slash + 1).trim()); } catch (NumberFormatException ignored) {}
                    }
                }
            }
            // Fallback to Content-Length if server used 200
            long len = c.getContentLengthLong();
            return len > 0 ? len : -1L;
        } finally {
            c.disconnect();
        }
    }

    private static String probeContentType(URL url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestProperty("User-Agent", "Mozilla/5.0");
        c.setRequestProperty("Accept", "application/octet-stream");
        c.setRequestProperty("Accept-Encoding", "identity");
        c.setRequestMethod("HEAD");
        try {
            c.connect();
            return c.getHeaderField("Content-Type");
        } finally {
            c.disconnect();
        }
    }

    private static void downloadToFileWithResume(URL url, Path dest, long expectedSize, JProgressBar bar) throws IOException {
        long existing = Files.exists(dest) ? Files.size(dest) : 0L;
        final boolean resume = existing > 0;

        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestProperty("User-Agent", "Mozilla/5.0");
        c.setRequestProperty("Accept", "application/octet-stream");
        c.setRequestProperty("Accept-Encoding", "identity");
        if (resume) {
            c.setRequestProperty("Range", "bytes=" + existing + "-");
        }
        c.connect();

        int code = c.getResponseCode();
        if (resume && code != 206) {
            // Server didn't honor range; start fresh
            c.disconnect();
            Files.deleteIfExists(dest);
            existing = 0L;
            downloadToFileWithResume(url, dest, expectedSize, bar);
        }
        if (!resume && code != 200) {
            throw new IOException("Unexpected HTTP status " + code + " for download");
        }

        // Write stream to file (append if resuming)
        try (InputStream in = new BufferedInputStream(c.getInputStream(), 8 * 1024 * 1024);
             RandomAccessFile raf = new RandomAccessFile(dest.toFile(), "rw")) {

            if (existing > 0) raf.seek(existing);

            byte[] buffer = new byte[8 * 1024 * 1024];
            long downloaded = existing;
            int n;
            long sinceFlush = 0L;

            while ((n = in.read(buffer)) != -1) {
                raf.write(buffer, 0, n);
                downloaded += n;
                sinceFlush += n;

                if (expectedSize > 0) {
                    final int p = (int) ((downloaded * 100) / expectedSize);
                    SwingUtilities.invokeLater(() -> {
                        bar.setIndeterminate(false);
                        bar.setValue(p);
                        bar.setString(p + "%");
                    });
                }

                // Flush to disk every ~32MB to be safe with giant files
                if (sinceFlush >= 32L * 1024L * 1024L) {
                    raf.getFD().sync();
                    sinceFlush = 0L;
                }
            }

            // Final fsync to ensure data hits disk
            raf.getFD().sync();
        } finally {
            c.disconnect();
        }
    }

    // === Validation helpers ===

    private static boolean isJarValid(Path path) throws IOException {
        // Quick size sanity check first
        long size = Files.size(path);
        if (size < MIN_EXPECTED_BYTES) {
            LOGGER.warn("Downloaded file is smaller than minimum sanity size: " + size + " bytes");
        }
        try (JarFile jf = new JarFile(path.toFile(), true)) {
            // Iterate a few entries to force ZIP structures to be parsed
            Enumeration<JarEntry> it = jf.entries();
            int count = 0;
            while (it.hasMoreElements() && count < 10) {
                JarEntry e = it.nextElement();
                if (e.getName() == null) break;
                count++;
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private static String sha256(Path file) throws IOException {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(file); BufferedInputStream bis = new BufferedInputStream(is)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = bis.read(buf)) != -1) {
                    md.update(buf, 0, r);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IOException("Unable to compute SHA-256", e);
        }
    }
}
