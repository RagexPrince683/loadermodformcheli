package com.Ragex.mcheliloader;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
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
        final Path modsDir = event.getModConfigurationDirectory().getParentFile().toPath().resolve("mods");
        final Path cfLoaderPath = modsDir.resolve(CF_LOADER_NAME);

        if (Files.exists(cfLoaderPath)) {
            LOGGER.info("CurseForge loader already found. Skipping download.");
            return;
        }

        LOGGER.info("CurseForge loader not found. Downloading...");

        new Thread(() -> {
            try {
                downloadFile(CF_LOADER_URL, cfLoaderPath);
                LOGGER.info("Downloaded CurseForge loader successfully to " + cfLoaderPath);

                // GUI must run on Swing EDT
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            null,
                            "Mcheli Loader was installed successfully.\nPlease restart your game to complete installation.",
                            "Mcheli Loader",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    // Force crash after user clicks OK
                    throw new RuntimeException("Mcheli Loader installed. Please restart your game.");
                });

            } catch (IOException e) {
                LOGGER.error("Failed to download CurseForge loader!", e);

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            null,
                            "Failed to download the Mcheli Loader!\nCheck your internet connection or try again later.",
                            "Mcheli Loader Error",
                            JOptionPane.ERROR_MESSAGE
                    );

                    throw new RuntimeException("Mcheli Loader download failed!", e);
                });
            }
        }, "McheliLoader-Download-Thread").start();
    }

    private void downloadFile(String fileURL, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());

        HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.connect();

        int responseCode = connection.getResponseCode();
        if (responseCode / 100 == 3) { // handle redirect manually
            String newLocation = connection.getHeaderField("Location");
            connection.disconnect();
            if (newLocation == null) throw new IOException("Redirected but no Location header found.");
            connection = (HttpURLConnection) new URL(newLocation).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.connect();
            responseCode = connection.getResponseCode();
        }

        if (responseCode != HttpURLConnection.HTTP_OK)
            throw new IOException("Failed to download file: HTTP " + responseCode);

        try (InputStream in = connection.getInputStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            connection.disconnect();
        }
    }
}
