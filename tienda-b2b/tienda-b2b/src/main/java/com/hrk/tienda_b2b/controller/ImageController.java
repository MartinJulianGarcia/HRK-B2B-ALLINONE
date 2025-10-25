package com.hrk.tienda_b2b.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/uploads")
@CrossOrigin(origins = "*")
public class ImageController {

    private static final String UPLOAD_DIR = "uploads";

    @PostMapping
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("🔵 [BACKEND] ImageController - Archivo recibido: " + file.getOriginalFilename());
            System.out.println("🔵 [BACKEND] ImageController - Tamaño: " + file.getSize() + " bytes");
            System.out.println("🔵 [BACKEND] ImageController - Tipo: " + file.getContentType());

            // Validar que el archivo no esté vacío
            if (file.isEmpty()) {
                System.out.println("🔴 [BACKEND] ImageController - Error: Archivo vacío");
                return ResponseEntity.badRequest().body("Archivo vacío");
            }

            // Validar tipo de archivo
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                System.out.println("🔴 [BACKEND] ImageController - Error: No es una imagen");
                return ResponseEntity.badRequest().body("Solo se permiten archivos de imagen");
            }

            // Crear directorio si no existe
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("🔵 [BACKEND] ImageController - Directorio creado: " + uploadPath.toAbsolutePath());
            }

            // Generar nombre único para el archivo
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // Guardar el archivo
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("✅ [BACKEND] ImageController - Imagen subida exitosamente: " + filename);
            System.out.println("✅ [BACKEND] ImageController - Ruta completa: " + filePath.toAbsolutePath());

            // Devolver solo el nombre del archivo
            return ResponseEntity.ok(filename);

        } catch (IOException e) {
            System.out.println("🔴 [BACKEND] ImageController - Error al subir imagen: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error al guardar el archivo: " + e.getMessage());
        }
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            System.out.println("🔵 [BACKEND] ImageController - Solicitando imagen: " + filename);

            Path file = Paths.get(UPLOAD_DIR).resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                System.out.println("✅ [BACKEND] ImageController - Imagen encontrada: " + file.toAbsolutePath());
                return ResponseEntity.ok().body(resource);
            } else {
                System.out.println("🔴 [BACKEND] ImageController - Imagen no encontrada: " + file.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

        } catch (MalformedURLException e) {
            System.out.println("🔴 [BACKEND] ImageController - Error de URL: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}