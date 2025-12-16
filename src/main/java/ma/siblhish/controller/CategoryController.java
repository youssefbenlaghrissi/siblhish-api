package ma.siblhish.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller pour la gestion des catégories
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Liste des catégories de l'utilisateur
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getUserCategories(@PathVariable Long userId) {
        List<CategoryDto> categories = categoryService.getUserCategories(userId);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * Créer une catégorie
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryDto>> createCategory(
            @Valid @RequestBody CategoryRequestDto request) {
        CategoryDto category = categoryService.createCategory(request);
        return ResponseEntity.status(201).body(ApiResponse.success(category));
    }

    /**
     * Mettre à jour une catégorie
     */
    @PutMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryDto>> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryUpdateDto request) {
        CategoryDto category = categoryService.updateCategory(categoryId, request);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    /**
     * Supprimer une catégorie
     */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtenir les catégories par défaut
     */
    @GetMapping("/default")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getDefaultCategories() {
        List<CategoryDto> categories = categoryService.getDefaultCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
}

