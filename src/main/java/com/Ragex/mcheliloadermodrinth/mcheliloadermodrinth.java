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

    private static final String CF_LOADER_URL =
            "https://github.com/RagexPrince683/loadermodformcheli/releases/download/KILL/mcheliloader-MCHO+v1.8.2.jar";

    private static final String CF_LOADER_NAME = "mcheliloader.jar";

    private static final long MIN_EXPECTED_BYTES = 50L * 1024L * 1024L; // sanity check lowered to 50MB
    private static final String EXPECTED_SHA256 = null;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File mcDir = event.getModConfigurationDirectory().getParentFile();
        Path modsDir = mcDir.toPath().resolve("mods");
        Path finalFile = modsDir.resolve(CF_LOADER_NAME);

        try {
            Files.createDirectories(modsDir);
        } catch (IOException e) {
            LOGGER.error("Cannot create mods directory", e);
            return; // don’t crash game here
        }

        if (Files.exists(finalFile)) {
            try {
                if (isJarValid(finalFile)) {
                    LOGGER.info("Mcheli loader already present and valid.");
                    return;
                } else {
                    LOGGER.warn("Existing Mcheli loader invalid, deleting...");
                    Files.deleteIfExists(finalFile);
                }
            } catch (IOException ignored) {}
        }

        LOGGER.info("Mcheli loader not found. Starting async download...");

        Path tempFile = finalFile.resolveSibling(CF_LOADER_NAME + ".part");

        Thread worker = new Thread(() -> {
            try {
                URL resolved = resolveFinalURL(CF_LOADER_URL);
                downloadToFileWithResume(resolved, tempFile);

                if (!isJarValid(tempFile)) {
                    LOGGER.error("Downloaded file invalid.");
                    return;
                }

                Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Mcheli loader installed successfully. Please restart your game.");
                JOptionPane.showMessageDialog(null,
                        "Mcheli Loader installed successfully. Please restart your game.",
                        "Mcheli Loader",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (Throwable t) {
                LOGGER.error("Mcheli loader download failed", t);
                JOptionPane.showMessageDialog(null,
                        "Failed to download Mcheli Loader:\n" + t.getMessage(),
                        "Mcheli Loader Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }, "Mcheli-Loader-Downloader");

        worker.setDaemon(true);
        worker.start();
    }

    private static URL resolveFinalURL(String original) throws IOException {
        URL current = new URL(original);
        for (int i = 0; i < 10; i++) {
            HttpURLConnection c = (HttpURLConnection) current.openConnection();
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent", "Mozilla/5.0");
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

    private static void downloadToFileWithResume(URL url, Path dest) throws IOException {
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestProperty("User-Agent", "Mozilla/5.0");
        c.setRequestProperty("Accept", "application/octet-stream");
        c.connect();

        try (InputStream in = new BufferedInputStream(c.getInputStream());
             OutputStream out = Files.newOutputStream(dest, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
        } finally {
            c.disconnect();
        }
    }

    private static boolean isJarValid(Path path) throws IOException {
        long size = Files.size(path);
        if (size < MIN_EXPECTED_BYTES) {
            return false;
        }
        try (JarFile jf = new JarFile(path.toFile(), true)) {
            Enumeration<JarEntry> it = jf.entries();
            while (it.hasMoreElements()) {
                JarEntry e = it.nextElement();
                if (e.getName() != null) break;
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
