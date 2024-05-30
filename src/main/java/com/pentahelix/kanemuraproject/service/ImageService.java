package com.pentahelix.kanemuraproject.service;

import com.pentahelix.kanemuraproject.entity.Menu;
import com.pentahelix.kanemuraproject.entity.User;
import com.pentahelix.kanemuraproject.repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Service
public class ImageService {

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private ResourceLoader resourceLoader;

    public final String UPLOAD_DIR = Paths.get("src/main/resources/images/").toAbsolutePath().toString();

    // UPLOAD IMAGE
    public String updateImageToFileSystem(User user, MultipartFile file, Integer id) throws IOException {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());

        // Validate file
        if (filename.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nama file tidak valid: " + filename);
        }

        // CARI MENU DENGAN ID MENU
        Menu existingMenu = menuRepository.findFirstById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu dengan id " + id + " tidak ditemukan"));

        // Delete the existing image file if it exists
        if (existingMenu.getNameImg() != null && !existingMenu.getNameImg().isEmpty()) {
            Path existingImagePath = Paths.get(UPLOAD_DIR).resolve(existingMenu.getNameImg()).toAbsolutePath().normalize();
            Files.deleteIfExists(existingImagePath);
        }

        existingMenu.setNameImg(filename);
        existingMenu.setType(file.getContentType());

        // Simpan data menu sebelum file disalin
        menuRepository.save(existingMenu);

        // Path folder gambar
        Path folderPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();

        if (!Files.exists(folderPath)) {
            Files.createDirectories(folderPath);
        }

        // Path tujuan file
        Path targetLocation = folderPath.resolve(filename);

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Return the relative path of the image
        return Paths.get("images", filename).toString();
    }

    // GET IMAGE DARI DIRECTORY
    public byte[] getImageByFileName(String filename) throws IOException {
        Path imagePath = Paths.get(UPLOAD_DIR).resolve(filename).toAbsolutePath().normalize();
        Resource resource = resourceLoader.getResource("file:" + imagePath.toString());

        if (resource.exists()) {
            return Files.readAllBytes(imagePath);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gambar tidak ditemukan dengan nama file: " + filename);
        }
    }

    // GET IMAGE DENGAN ID MENU
    public byte[] getImageByMenuId(Integer menuId) throws IOException {
        Optional<Menu> fileDataOptional = menuRepository.findFirstById(menuId);
        if (fileDataOptional.isPresent()) {
            Menu fileData = fileDataOptional.get();
            return getImageByFileName(fileData.getNameImg());
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gambar tidak ditemukan dengan id menu: " + menuId);
        }
    }
}
