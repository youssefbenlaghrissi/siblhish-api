package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import ma.siblhish.config.CacheConfig;
import ma.siblhish.dto.*;
import ma.siblhish.entities.Category;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.CategoryRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final EntityMapper mapper;

    /*
    Lorsque ces méthodes seront réactivées, ajouter @CacheEvict(CacheConfig.CATEGORIES, allEntries = true)
    sur createCategory, updateCategory et deleteCategory pour invalider le cache.
    @Transactional
    public CategoryDto createCategory(CategoryRequestDto request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        category.setCreationDate(LocalDateTime.now());
        
        Category saved = categoryRepository.save(category);
        return mapper.toCategoryDto(saved);
    }

    @Transactional
    public CategoryDto updateCategory(Long categoryId, CategoryRequestDto request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
        
        category.setName(request.getName());
        if (request.getIcon() != null) category.setIcon(request.getIcon());
        if (request.getColor() != null) category.setColor(request.getColor());
        
        Category saved = categoryRepository.save(category);
        return mapper.toCategoryDto(saved);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
        category.setDeleted(true);
        categoryRepository.save(category);
    }
    */

    @Cacheable(CacheConfig.CATEGORIES)
    public List<CategoryDto> getAllCategories() {
        List<Category> categories = categoryRepository.findAllCategories();
        return mapper.toCategoryDtoList(categories);
    }
}

