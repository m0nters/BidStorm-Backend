package com.taitrinh.online_auction.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taitrinh.online_auction.dto.category.CategoryResponse;
import com.taitrinh.online_auction.dto.category.CreateCategoryRequest;
import com.taitrinh.online_auction.entity.Category;
import com.taitrinh.online_auction.exception.BadRequestException;
import com.taitrinh.online_auction.exception.ResourceNotFoundException;
import com.taitrinh.online_auction.mapper.CategoryMapper;
import com.taitrinh.online_auction.repository.CategoryRepository;
import com.taitrinh.online_auction.util.SlugUtils;

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
            throw new ResourceNotFoundException("Category cha", parentId);
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
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        return categoryMapper.toResponseWithChildren(category);
    }

    /**
     * Get category by slug
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", slug));

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
                            () -> new ResourceNotFoundException("Category cha", request.getParentId()));

            // Check if parent already has a parent (only 2 levels allowed)
            if (parent.getParent() != null) {
                throw new BadRequestException(
                        "Không thể tạo category cháu dưới category con. Chỉ cho phép 2 cấp.");
            }
        }

        // Check for duplicate name at the same level
        boolean isDuplicate = request.getParentId() != null
                ? categoryRepository.existsByNameAndParentId(request.getName(), request.getParentId())
                : categoryRepository.existsByNameAndParentIsNull(request.getName());

        if (isDuplicate) {
            throw new BadRequestException("Tên category '" + request.getName() + "' đã tồn tại");
        }

        // Generate slug
        String slug;
        if (request.getSlug() != null && !request.getSlug().trim().isEmpty()) {
            // Admin provided custom slug - use it
            slug = SlugUtils.toSlug(request.getSlug());
        } else {
            // Auto-generate from name
            slug = SlugUtils.toSlug(request.getName());
        }

        // For child categories, prepend parent slug
        if (parent != null) {
            slug = parent.getSlug() + "/" + slug;
        }

        // Ensure slug uniqueness
        slug = SlugUtils.makeUnique(slug, categoryRepository::existsBySlug);

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
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
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        // Validate and get new parent if parentId is provided
        Category newParent = null;
        if (request.getParentId() != null) {
            // Cannot set parent to itself
            if (request.getParentId().equals(id)) {
                throw new BadRequestException("Category không thể là parent của chính nó");
            }

            newParent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Category cha", request.getParentId()));

            // Check if new parent already has a parent (only 2 levels allowed)
            if (!newParent.isParent()) {
                throw new BadRequestException("Không thể chuyển category vào category con. Chỉ cho phép 2 cấp.");
            }

            // Check if category has children - cannot move parent category to become a
            // sub-category (because if so there will be 3 levels)
            if (category.hasChildren()) {
                throw new BadRequestException("Không thể chuyển category cha có con vào category con");
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
                throw new BadRequestException("Tên category '" + request.getName() + "' đã tồn tại");
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
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        // Check if category has products
        if (categoryRepository.hasProducts(id)) {
            throw new BadRequestException("Không thể xóa category có sản phẩm");
        }

        // Check if category has children
        if (category.hasChildren()) {
            throw new BadRequestException("Không thể xóa category có con. Xóa category con trước");
        }

        categoryRepository.delete(category);
    }
}
