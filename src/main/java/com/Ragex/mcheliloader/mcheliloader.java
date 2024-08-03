package com.Ragex.mcheliloader;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.commons.io.FileUtils.copyDirectory;


//import static cpw.mods.fml.common.Loader.minecraftDir;
//import net.minecraftforge.common.



@Mod(
        modid = "mcheliloader",
        name = "mcheliloader",
        dependencies = "required-after:Forge@[10.13.2.1230,)"
)


public class mcheliloader {
    private static final Path AssetsourceDir = Paths.get("assets/mcheli");

    private static final Path CodesourceDir = Paths.get("code/mcheli");


    @Mod.EventHandler
    public void PreInit(FMLPreInitializationEvent evt) {
        Logger logger = LogManager.getLogger(mcheliloader.class.getName());
        try {
            File configDir = evt.getModConfigurationDirectory();
            File minecraftDir = configDir.getParentFile();
            Path modsDir = Paths.get(minecraftDir.getPath(), "mods");
            Path mcheliDir = Paths.get(modsDir.toString(), "mcheli");
            Path mcheliAssetsDir = Paths.get(modsDir.toString(), "mcheli/assets");
            Path mcheliCodeDir = Paths.get(mcheliDir.toString(), "mcheli");
            //Path mcheliCodeDir = Paths.get(modsDir.toString(), "mcheli");
            if (!Files.exists(mcheliDir)) {
                //assets need to go into mods/mcheli/assets/mcheli
                //code needs to go into mods/mcheli/mcheli
                Files.createDirectories(mcheliDir);
                if(!Files.exists(mcheliAssetsDir)) {
                    Files.createDirectories(mcheliAssetsDir);
                }

            }
            FileUtils.copyDirectory(CodesourceDir.toFile(), mcheliCodeDir.toFile());
            FileUtils.copyDirectory(AssetsourceDir.toFile(), mcheliAssetsDir.toFile());


            AtomicBoolean isCodeCopied = new AtomicBoolean(true);
            AtomicBoolean isAssetsCopied = new AtomicBoolean(true);

            SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path targetFile = mcheliDir.resolve(file.subpath(0, file.getNameCount()).relativize(file));
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc!= null) {
                        throw exc;
                    }
                    return FileVisitResult.CONTINUE;
                }
            };

            Files.walkFileTree(CodesourceDir, visitor);
            Files.walkFileTree(AssetsourceDir, visitor);

            // Validate if all files were copied successfully
            long originalCodeFiles = Files.walk(CodesourceDir).count();
            long copiedCodeFiles = Files.walk(mcheliDir).count();
            long originalAssetsFiles = Files.walk(AssetsourceDir).count();
            long copiedAssetsFiles = Files.walk(mcheliAssetsDir).count();

            if (originalCodeFiles!= copiedCodeFiles || originalAssetsFiles!= copiedAssetsFiles) {
                logger.error("Error: Not all files were copied correctly.");
            }

        //    System.out.println("Copying from " + CodesourceDir + " to " + mcheliDir);

      //      Files.walk(CodesourceDir).forEach(source -> {
      //          Path destination = Paths.get(mcheliDir.toString(), source.toString().substring(CodesourceDir.toString().length()));
      //          try {
      //              Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
      //          } catch (IOException e) {
      //              e.printStackTrace();
      //          }
      //      });
//
      //      System.out.println("Copying from " + AssetsourceDir + " to " + mcheliAssetsDir);


     //       Files.walk(AssetsourceDir).forEach(source -> {
     //           Path destination = Paths.get(mcheliAssetsDir.toString(), source.toString().substring(AssetsourceDir.toString().length()));
     //           try {
     //               Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
     //           } catch (IOException e) {
     //               e.printStackTrace();
     //           }
     //       });

            //boolean isCodeCopied = Files.walk(mcheliDir).count() == Files.walk(CodesourceDir).count();
           // boolean isAssetsCopied = Files.walk(mcheliAssetsDir).count() == Files.walk(AssetsourceDir).count();

     //       if (!isCodeCopied || !isAssetsCopied) {
     //           // Handle the error
     //           System.out.println("Error: Files were not copied correctly.");
     //       }



        } catch (Exception e) {
            e.printStackTrace();
            logger.error("An error occurred during pre-initialization.", e);
        }
    }
}