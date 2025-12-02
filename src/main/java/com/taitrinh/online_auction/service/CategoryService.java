package com.taitrinh.online_auction.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taitrinh.online_auction.dto.category.CategoryResponse;
import com.taitrinh.online_auction.dto.category.CreateCategoryRequest;
import com.taitrinh.online_auction.entity.Category;
import com.taitrinh.online_auction.mapper.CategoryMapper;
import com.taitrinh.online_auction.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    /**
     * Get all categories in hierarchical structure (2 levels: parent -> children)
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategoriesHierarchy() {
        List<Category> parentCategories = categoryRepository.findByParentIsNull();

        return parentCategories.stream()
                .map(categoryMapper::toResponseWithChildren)
                .toList();
    }

    /**
     * Get all top-level categories (no children included)
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllParentCategories() {
        return categoryRepository.findByParentIsNull().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    /**
     * Get all sub-categories of a parent
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getSubCategories(Integer parentId) {
        if (!categoryRepository.existsById(parentId)) {
            throw new RuntimeException("Parent category not found with id: " + parentId);
        }

        return categoryRepository.findByParentId(parentId).stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    /**
     * Get category by ID
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        return categoryMapper.toResponseWithChildren(category);
    }

    /**
     * Create a new category (parent or sub-category)
     */
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        // Validate parent if provided
        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(
                            () -> new RuntimeException("Parent category not found with id: " + request.getParentId()));

            // Check if parent already has a parent (only 2 levels allowed)
            if (parent.getParent() != null) {
                throw new RuntimeException(
                        "Cannot create sub-category under a sub-category. Only 2 levels are allowed.");
            }
        }

        // Check for duplicate name at the same level
        boolean isDuplicate = request.getParentId() != null
                ? categoryRepository.existsByNameAndParentId(request.getName(), request.getParentId())
                : categoryRepository.existsByNameAndParentIsNull(request.getName());

        if (isDuplicate) {
            throw new RuntimeException("Category with name '" + request.getName() + "' already exists at this level");
        }

        Category category = Category.builder()
                .name(request.getName())
                .parent(parent)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    /**
     * Update category name and parent
     */
    @Transactional
    public CategoryResponse updateCategory(Integer id, CreateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        // Validate and get new parent if parentId is provided
        Category newParent = null;
        if (request.getParentId() != null) {
            // Cannot set parent to itself
            if (request.getParentId().equals(id)) {
                throw new RuntimeException("Category cannot be its own parent");
            }

            newParent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(
                            () -> new RuntimeException("Parent category not found with id: " + request.getParentId()));

            // Check if new parent already has a parent (only 2 levels allowed)
            if (!newParent.isParent()) {
                throw new RuntimeException("Cannot move category under a sub-category. Only 2 levels are allowed.");
            }

            // Check if category has children - cannot move parent category to become a
            // sub-category (because if so there will be 3 levels)
            if (category.hasChildren()) {
                throw new RuntimeException("Cannot move a parent category with children to become a sub-category");
            }
        }

        // Check for duplicates if moving to a different level or changing name
        Integer newParentId = request.getParentId();
        Integer oldParentId = !category.isParent() ? category.getParent().getId() : null;

        if (!request.getName().equals(category.getName()) ||
                (newParentId != null && !newParentId.equals(oldParentId)) ||
                (newParentId == null && oldParentId != null)) {

            boolean isDuplicate = newParentId != null
                    ? categoryRepository.existsByNameAndParentId(request.getName(), newParentId)
                    : categoryRepository.existsByNameAndParentIsNull(request.getName());

            if (isDuplicate) {
                throw new RuntimeException(
                        "Category with name '" + request.getName() + "' already exists at this level");
            }
        }

        category.setName(request.getName());
        category.setParent(newParent);
        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(updatedCategory);
    }

    /**
     * Delete category (only if it has no products)
     */
    @Transactional
    public void deleteCategory(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        // Check if category has products
        if (categoryRepository.hasProducts(id)) {
            throw new RuntimeException("Cannot delete category with existing products");
        }

        // Check if category has children
        if (category.hasChildren()) {
            throw new RuntimeException("Cannot delete category with sub-categories. Delete sub-categories first.");
        }

        categoryRepository.delete(category);
    }
}
