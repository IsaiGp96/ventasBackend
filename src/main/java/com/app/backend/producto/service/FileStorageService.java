package com.app.backend.producto.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private static final java.util.Set<String> TIPOS_PERMITIDOS = java.util.Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf");

    public String guardarImagen(MultipartFile file, String subcarpeta) {
        if (file == null || file.isEmpty())
            return null;

        String contentType = file.getContentType();
        if (!TIPOS_PERMITIDOS.contains(contentType)) {
            throw new IllegalArgumentException("Tipo de archivo no permitido. Solo JPG, PNG o WEBP.");
        }

        String extension = obtenerExtension(file.getOriginalFilename());
        String nombreArchivo = UUID.randomUUID() + extension;

        try {
            Path destino = Paths.get(uploadDir, subcarpeta);
            Files.createDirectories(destino);
            Files.copy(file.getInputStream(), destino.resolve(nombreArchivo),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la imagen: " + e.getMessage());
        }

        return "/uploads/" + subcarpeta + "/" + nombreArchivo;

    }

    public void eliminarImagen(String url) {
        if (url == null || url.isBlank())
            return;
        try {
            // url = "/uploads/productos/uuid.jpg"
            String ruta = url.replace("/uploads/", "");
            Path archivo = Paths.get(uploadDir.replace("uploads/productos", "uploads"), ruta);
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            // log pero no falla — la imagen puede no existir
            System.err.println("No se pudo eliminar imagen: " + url);
        }
    }

    private String obtenerExtension(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains("."))
            return ".jpg";
        return nombreOriginal.substring(nombreOriginal.lastIndexOf(".")).toLowerCase();
    }
}