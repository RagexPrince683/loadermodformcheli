package com.Ragex.mcheliloader;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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
                "https://github.com/RagexPrince683/MCH-mocmaster"
        };

        Path modsDir = Paths.get(minecraftDir.getPath(), "mods");

        String DownloadDir = modsDir.toString();

        for (String fileURL : fileURLs) {
            try {
                downloadFile(fileURL, DownloadDir);
                System.out.println("Downloaded: " + fileURL + " to " + DownloadDir);
            } catch (IOException e) {
                System.err.println("Failed to download: " + fileURL);
                e.printStackTrace();
            }
        }



    }

    public static void downloadFile(String urlStr, String saveDir) throws IOException {
        URL url = new URL(urlStr);
        URLConnection connection = url.openConnection();
        InputStream inputStream = new BufferedInputStream(connection.getInputStream());
        String fileName = urlStr.substring(urlStr.lastIndexOf("/") + 1);
        FileOutputStream outputStream = new FileOutputStream(Paths.get(saveDir, fileName).toString());

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        outputStream.close();
        inputStream.close();
    }

   // @Mod.EventHandler
   // public void init(FMLInitializationEvent evt) {
//
   // }
}