package com.Ragex.mcheliloader;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;

@Mod(
        modid = "modrinthloader",
        name = "Modrinth Loader",
        version = "1.0",
        dependencies = "required-after:Forge@[10.13.2.1230,)"
)
public class mcheliloader {

    private static final Logger LOGGER = LogManager.getLogger("ModrinthLoader");
    private static final String CF_LOADER_URL =
            "https://github.com/RagexPrince683/loadermodformcheli/releases/download/KILL/mcheliloader-MCHO+v1.8.2.jar";
    private static final String CF_LOADER_NAME = "mcheliloader.jar";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File mcDir = event.getModConfigurationDirectory().getParentFile();
        Path modsDir = mcDir.toPath().resolve("mods");
        Path finalFile = modsDir.resolve(CF_LOADER_NAME);

        if (Files.exists(finalFile)) {
            LOGGER.info("Mcheli loader already found. Skipping download.");
            return;
        }

        LOGGER.info("Mcheli loader not found. Starting download...");

        try {
            Files.createDirectories(modsDir);
            Path tempFile = finalFile.resolveSibling(CF_LOADER_NAME + ".tmp");

            // Blocking download with modal dialog
            downloadWithProgressBlocking(CF_LOADER_URL, tempFile);

            // Move temp file to final destination after download is complete
            Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);

            // Show completion and force restart
            JOptionPane.showMessageDialog(
                    null,
                    "Mcheli Loader installed successfully.\nPlease restart your game.",
                    "Mcheli Loader",
                    JOptionPane.INFORMATION_MESSAGE
            );
            throw new RuntimeException("Mcheli Loader installed. Please restart your game.");

        } catch (IOException e) {
            LOGGER.error("Failed to download Mcheli Loader!", e);
            JOptionPane.showMessageDialog(
                    null,
                    "Failed to download Mcheli Loader!\nCheck your internet connection or try again later.",
                    "Mcheli Loader Error",
                    JOptionPane.ERROR_MESSAGE
            );
            throw new RuntimeException("Mcheli Loader download failed!", e);
        }
    }

    private void downloadWithProgressBlocking(String fileURL, Path destination) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.connect();

        int responseCode = connection.getResponseCode();
        while (responseCode / 100 == 3) { // follow redirects
            String newLocation = connection.getHeaderField("Location");
            if (newLocation == null) throw new IOException("Redirected but no Location header found.");
            connection.disconnect();
            connection = (HttpURLConnection) new URL(newLocation).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.connect();
            responseCode = connection.getResponseCode();
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to download file: HTTP " + responseCode);
        }

        long totalSize = connection.getContentLengthLong();
        if (totalSize <= 0) {
            System.out.println("Warning: Server did not report file size, progress bar may be inaccurate.");
            totalSize = 1; // avoid divide by zero
        }

        // Setup progress bar
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        JOptionPane optionPane = new JOptionPane(progressBar, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
        JDialog dialog = optionPane.createDialog("Downloading Mcheli Loader");
        dialog.setModal(true); // block game
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        final long expectedSize = totalSize;
        final JDialog finalDialog = dialog;
        final HttpURLConnection finalConnection = connection; // final reference for thread

        Thread downloadThread = new Thread(() -> {
            try (InputStream in = finalConnection.getInputStream();
                 FileOutputStream out = new FileOutputStream(destination.toFile())) {

                byte[] buffer = new byte[8 * 1024 * 1024]; // 8 MB buffer for big files
                long totalRead = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    totalRead += read;
                    final int progress = (int) ((totalRead * 100) / expectedSize);
                    SwingUtilities.invokeLater(() -> progressBar.setValue(progress));
                }

                out.flush();
                out.getFD().sync(); // force flush to disk
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Download failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                SwingUtilities.invokeLater(finalDialog::dispose);
            }
        });

        downloadThread.start();
        dialog.setVisible(true); // block until dispose()

        try {
            downloadThread.join(); // ensure download finishes fully before continuing
        } catch (InterruptedException ignored) {}

        connection.disconnect();
    }



}
