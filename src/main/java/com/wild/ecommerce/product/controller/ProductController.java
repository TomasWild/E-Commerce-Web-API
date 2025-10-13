package com.wild.ecommerce.product.controller;

import com.wild.ecommerce.common.dto.PageResponse;
import com.wild.ecommerce.product.dto.CreateProductRequest;
import com.wild.ecommerce.product.dto.ProductDTO;
import com.wild.ecommerce.product.dto.UpdateProductRequest;
import com.wild.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Endpoints for managing products")
public class ProductController {

    private final ProductService productService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UUID> createProduct(@Valid @ModelAttribute CreateProductRequest request) {
        ProductDTO product = productService.createProduct(request);

        return new ResponseEntity<>(product.id(), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<PagedModel<ProductDTO>> getAllProducts(
            @RequestParam(value = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "sortOrder", defaultValue = "ASC") String sortOrder,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "categoryId", required = false) UUID categoryId
    ) {
        Sort.Direction direction = sortOrder.equalsIgnoreCase("DESC")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        PageResponse<ProductDTO> productsResponse = productService.getAllProducts(pageable, name, brand, categoryId);
        Page<ProductDTO> products = productsResponse.toPage();

        return new ResponseEntity<>(new PagedModel<>(products), HttpStatus.OK);
    }

    @GetMapping("{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable("id") UUID id) {
        ProductDTO product = productService.getProductById(id);

        return new ResponseEntity<>(product, HttpStatus.OK);
    }

    @PatchMapping(value = "{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable("id") UUID id,
            @Valid @ModelAttribute UpdateProductRequest request
    ) {
        ProductDTO product = productService.updateProduct(id, request);

        return new ResponseEntity<>(product, HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") UUID id) {
        productService.deleteProduct(id);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
