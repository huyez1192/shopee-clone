package com.ecommerce.minishopee.controller;

import com.ecommerce.minishopee.dto.CategoryRequest;
import com.ecommerce.minishopee.dto.CategoryResponse;
import com.ecommerce.minishopee.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.net.CacheRequest;
import java.net.CacheResponse;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> getAllCategories() {
        return categoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        return categoryService.createCategory(request);
    }

    @PutMapping("/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.updateCategory(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
    }

    @PatchMapping("/{id}/hide")
    public CategoryResponse hideCategory(@PathVariable Long id) {
        return categoryService.hideCategory(id);
    }

    @PatchMapping("/{id}/show")
    public CategoryResponse showCategory(@PathVariable Long id) {
        return categoryService.showCategory(id);
    }
}
