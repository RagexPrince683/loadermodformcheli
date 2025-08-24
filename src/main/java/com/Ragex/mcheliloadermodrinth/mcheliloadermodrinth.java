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

        LOGGER.info("Mcheli loader not found. Starting blocking download...");

        // Use a .part file while downloading
        Path tempFile = finalFile.resolveSibling(CF_LOADER_NAME + ".part");

        // Build a modal progress dialog that blocks the game until finished
        final JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setValue(0);

        final JOptionPane optionPane = new JOptionPane(progressBar, JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
        final JDialog dialog = optionPane.createDialog("Downloading Mcheli Loader (do not close)");
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        final Thread worker = new Thread(() -> {
            try {
                URL resolved = resolveFinalURL(CF_LOADER_URL);
                long remoteSize = probeRemoteSize(resolved);

                // If remote size unknown, show indeterminate progress
                if (remoteSize <= 0) {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setIndeterminate(true);
                        progressBar.setString("Downloading (size unknown)...");
                    });
                } else {
                    final long expected = remoteSize;
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setIndeterminate(false);
                        progressBar.setValue(0);
                        progressBar.setString("0%");
                    });

                    // If there's an existing .part file, show resumed percentage immediately
                    long existing = 0L;
                    if (Files.exists(tempFile)) {
                        try { existing = Files.size(tempFile); } catch (IOException ignored) {}
                        if (existing > 0 && existing < expected) {
                            final int p = (int) ((existing * 100) / expected);
                            SwingUtilities.invokeLater(() -> {
                                progressBar.setIndeterminate(false);
                                progressBar.setValue(p);
                                progressBar.setString(p + "% (resuming)");
                            });
                        }
                    }
                }

                // Perform the actual download (supports resume). Pass progressBar so it updates.
                downloadToFileWithResume(resolved, tempFile, remoteSize, progressBar);

                // Extra integrity checks before renaming
                if (remoteSize > 0) {
                    long got = Files.size(tempFile);
                    if (got != remoteSize) {
                        throw new IOException("Size mismatch: expected " + remoteSize + ", got " + got);
                    }
                }

                if (!isJarValid(tempFile)) {
                    throw new IOException("Downloaded file is not a valid JAR (zip parse failed)");
                }

                if (EXPECTED_SHA256 != null && !EXPECTED_SHA256.trim().isEmpty()) {
                    String gotHash = sha256(tempFile);
                    if (!EXPECTED_SHA256.equalsIgnoreCase(gotHash)) {
                        throw new IOException("SHA-256 mismatch: got " + gotHash + ", expected " + EXPECTED_SHA256);
                    }
                }

                // Move into place (no ATOMIC_MOVE to avoid platform issues)
                Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);

                LOGGER.info("Mcheli loader installed successfully.");
            } catch (Throwable t) {
                LOGGER.error("Mcheli loader download failed", t);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        null,
                        "Failed to download Mcheli Loader:\n" + t.getMessage(),
                        "Mcheli Loader Error",
                        JOptionPane.ERROR_MESSAGE
                ));
            } finally {
                // Ensure dialog goes away
                SwingUtilities.invokeLater(dialog::dispose);
            }
        }, "Mcheli-Loader-Downloader");

        // Start worker and show modal dialog (dialog blocks current thread until disposed)
        worker.start();
        dialog.setVisible(true); // BLOCK here until worker disposes

        // Ensure worker finished (should be, since it disposes dialog at the end)
        try {
            worker.join();
        } catch (InterruptedException e) {
            throw new RuntimeException("Download interrupted", e);
        }

        // Verify one last time, then prompt the user to restart. Only after they dismiss the dialog we throw to force restart.
        try {
            if (Files.exists(finalFile) && isJarValid(finalFile)) {
                LOGGER.info("Mcheli Loader installed. Prompting user to restart.");
                int res = JOptionPane.showOptionDialog(
                        null,
                        "Mcheli Loader installed successfully.\nClick Restart to close the game and install.\n(Seriously: restart the game.)",
                        "Mcheli Loader",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        new Object[] {"Restart", "Cancel"},
                        "Restart"
                );
                // If user clicked the Restart button (index 0), throw runtime to force crash/restart.
                if (res == 0) {
                    throw new RuntimeException("Mcheli Loader installed. Please restart your game.");
                } else {
                    LOGGER.info("User chose not to restart now.");
                }
            } else {
                throw new RuntimeException("Mcheli Loader not installed; see logs above.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Mcheli Loader verification failed", e);
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
        c.setConnectTimeout(15_000);
        c.setReadTimeout(15_000);
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
        c.setConnectTimeout(15_000);
        c.setReadTimeout(15_000);
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
        c.setConnectTimeout(60_000);  // connect timeout
        c.setReadTimeout(60_000);     // read timeout to avoid hanging forever on stalled connections
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
            return;
        }
        if (!resume && code != 200) {
            throw new IOException("Unexpected HTTP status " + code + " for download");
        }

        // Write stream to file (append if resuming)
        try (InputStream in = new BufferedInputStream(c.getInputStream(), 1 * 1024 * 1024); // 1 MB buffer
             RandomAccessFile raf = new RandomAccessFile(dest.toFile(), "rw")) {

            if (existing > 0) raf.seek(existing);

            byte[] buffer = new byte[1 * 1024 * 1024]; // 1 MB buffer
            long downloaded = existing;
            int n;
            long sinceFlush = 0L;

            while ((n = in.read(buffer)) != -1) {
                raf.write(buffer, 0, n);
                downloaded += n;
                sinceFlush += n;

                if (expectedSize > 0) {
                    final int p = (int) ((downloaded * 100) / expectedSize);
                    if (bar != null) {
                        SwingUtilities.invokeLater(() -> {
                            bar.setIndeterminate(false);
                            bar.setValue(p);
                            bar.setString(p + "%");
                        });
                    }
                } else {
                    // if size unknown, keep indeterminate text updated occasionally
                    if (bar != null) {
                        final long mb = downloaded / (1024L * 1024L);
                        SwingUtilities.invokeLater(() -> bar.setString("Downloaded ~" + mb + " MB"));
                    }
                }

                // Flush to disk every ~8MB to be safe with giant files
                if (sinceFlush >= 8L * 1024L * 1024L) {
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
