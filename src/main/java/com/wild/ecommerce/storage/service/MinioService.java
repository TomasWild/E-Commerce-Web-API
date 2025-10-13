package com.wild.ecommerce.storage.service;

import org.springframework.web.multipart.MultipartFile;

public interface MinioService {

    String uploadImage(MultipartFile file);

    void deleteImage(String imageUrl);
}
