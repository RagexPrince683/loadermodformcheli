package com.Ragex.mcheliloader;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;

// modrinth loader mod (installs the CF loader mod)

@Mod(
        modid = "modrinthloader",
        name = "Modrinth Loader",
        version = "1.0",
        dependencies = "required-after:Forge@[10.13.2.1230,)"
)
public class mcheliloader {
    private static final Logger LOGGER = LogManager.getLogger("ModrinthLoader");

    // Direct link to your CF loader JAR
    private static final String CF_LOADER_URL =
            "https://github.com/RagexPrince683/loadermodformcheli/releases/download/KILL/mcheliloader-MCHO+v1.8.2.jar";

    // What to name it inside mods/
    private static final String CF_LOADER_NAME = "mcheliloader.jar";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File mcDir = event.getModConfigurationDirectory().getParentFile();
        Path modsDir = mcDir.toPath().resolve("mods");
        Path cfLoaderPath = modsDir.resolve(CF_LOADER_NAME);

        // If the CurseForge loader already exists, skip
        if (Files.exists(cfLoaderPath)) {
            LOGGER.info("CurseForge loader already found in mods folder. Skipping download.");
            return;
        }

        LOGGER.info("CurseForge loader not found. Downloading...");

        try {
            downloadFile(CF_LOADER_URL, cfLoaderPath);
            LOGGER.info("Downloaded CurseForge loader successfully to " + cfLoaderPath);

            // Force crash after successful install
            throw new RuntimeException(
                    "Mcheli loader was just installed.\n" +
                            "Please restart your game to complete the installation."
            );

        } catch (IOException e) {
            LOGGER.error("Failed to download CurseForge loader!", e);
        }
    }

    private void downloadFile(String fileURL, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());

        HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.connect();

        if (connection.getResponseCode() != 200) {
            throw new IOException("Failed to download file: HTTP " + connection.getResponseCode());
        }

        try (InputStream in = connection.getInputStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            connection.disconnect();
        }
    }
}
