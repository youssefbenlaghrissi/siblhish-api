package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.entities.Category;
import ma.siblhish.entities.User;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.CategoryRepository;
import ma.siblhish.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EntityMapper mapper;

    public List<CategoryDto> getUserCategories(Long userId) {
        List<Category> categories = categoryRepository.findCategoriesByUserId(userId);
        return mapper.toCategoryDtoList(categories);
    }

    @Transactional
    public CategoryDto createCategory(CategoryRequestDto request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        
        Category saved = categoryRepository.save(category);
        return mapper.toCategoryDto(saved);
    }

    @Transactional
    public CategoryDto updateCategory(Long categoryId, CategoryUpdateDto request) {
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
        categoryRepository.delete(category);
    }

    public List<CategoryDto> getAllCategories() {
        List<Category> categories = categoryRepository.findAllCategories();
        return mapper.toCategoryDtoList(categories);
    }

    public Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
    }
}

