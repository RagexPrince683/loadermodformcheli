package com.Ragex.mcheliloader;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Mod(
        modid = "mcheliloader",
        name = "mcheliloader",
        dependencies = "required-after:Forge@[10.13.2.1230,)"
)
public class mcheliloader {
    private File minecraftDir;
    private static final Logger LOGGER = LogManager.getLogger(mcheliloader.class.getName());

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {

        minecraftDir = event.getModConfigurationDirectory().getParentFile();
        String[] fileURLs = {
                "https://github.com/RagexPrince683/mchelio/archive/refs/heads/new-vehicles.zip" // Correct URL for zip file
        };

        Path modsDir = Paths.get(minecraftDir.getPath(), "mods");

        String downloadDir = modsDir.toString();

        // Check if the mod is already installed (e.g., check if a known file exists)
        Path modFilePath = Paths.get(modsDir.toString(), "mchelio-new-vehicles");
        if (Files.exists(modFilePath)) {
            LOGGER.info("Mod is already installed. Skipping download.");
            return;
        }

        for (String fileURL : fileURLs) {
            try {
                Path zipFilePath = downloadFile(fileURL, downloadDir);
                unzipFile(zipFilePath.toString(), downloadDir);
                Files.delete(zipFilePath); // Clean up the zip file after extraction
                LOGGER.info("Downloaded and extracted: " + fileURL + " to " + downloadDir);
            } catch (IOException e) {
                LOGGER.error("Failed to download or extract: " + fileURL, e);
            }
        }
    }

    public static Path downloadFile(String urlStr, String saveDir) throws IOException {
        URL url = new URL(urlStr);
        URLConnection connection = url.openConnection();
        InputStream inputStream = new BufferedInputStream(connection.getInputStream());
        String fileName = urlStr.substring(urlStr.lastIndexOf("/") + 1);
        Path filePath = Paths.get(saveDir, fileName);
        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

        inputStream.close();
        return filePath;
    }

    public static void unzipFile(String zipFilePath, String destDir) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path filePath = Paths.get(destDir, entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zipFile.getInputStream(entry), filePath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}