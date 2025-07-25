package com.datn.datn.service;

import org.springframework.beans.factory.annotation.Value;
    import org.springframework.stereotype.Service;
    import org.springframework.util.StringUtils;
    import org.springframework.web.multipart.MultipartFile;

    import jakarta.annotation.PostConstruct;

    import org.springframework.core.io.Resource;
    import org.springframework.core.io.UrlResource;

    import java.io.IOException;
    import java.net.MalformedURLException;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.nio.file.Paths;
    import java.nio.file.StandardCopyOption;
    import java.util.UUID;

    @Service
    public class FileStorageService {
        private Path fileStorageLocation;
        
        @Value("${file.upload-dir}")
        private String uploadDir;
        
        @PostConstruct
        public void init() {
            this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            try {
                Files.createDirectories(this.fileStorageLocation);
            } catch (Exception ex) {
                throw new RuntimeException("Could not create upload directory", ex);
            }
        }
        
        public String storeFile(MultipartFile file) {
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            
            try {
                if (fileName.contains("..")) {
                    throw new RuntimeException("Filename contains invalid path sequence " + fileName);
                }
                
                String fileExtension = fileName.substring(fileName.lastIndexOf("."));
                String newFileName = UUID.randomUUID().toString() + fileExtension;
                Path targetLocation = this.fileStorageLocation.resolve(newFileName);
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
                
                return newFileName;
            } catch (IOException ex) {
                throw new RuntimeException("Could not store file " + fileName, ex);
            }
        }
        
        public Resource loadFileAsResource(String fileName) {
            try {
                Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
                Resource resource = new UrlResource(filePath.toUri());
                
                if (resource.exists()) {
                    return resource;
                } else {
                    throw new RuntimeException("File not found " + fileName);
                }
            } catch (MalformedURLException ex) {
                throw new RuntimeException("File not found " + fileName, ex);
            }
        }
    }
