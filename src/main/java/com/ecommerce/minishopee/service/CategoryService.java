package com.ecommerce.minishopee.service;

import com.ecommerce.minishopee.dto.CategoryRequest;
import com.ecommerce.minishopee.dto.CategoryResponse;
import com.ecommerce.minishopee.entity.Category;
import com.ecommerce.minishopee.entity.CategoryStatus;
import com.ecommerce.minishopee.exception.BadRequestException;
import com.ecommerce.minishopee.exception.CategoryNotFoundException;
import com.ecommerce.minishopee.mapper.CategoryMapper;
import com.ecommerce.minishopee.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }


    // API public chỉ trả Category ACTIVE
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository
                .findAllByStatus(CategoryStatus.ACTIVE)
                .stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    // API public chỉ xem được Category ACTIVE
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository
                .findByIdAndStatus(id, CategoryStatus.ACTIVE)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        return CategoryMapper.toResponse(category);
    }


    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.getName().trim();
        String slug = request.getSlug().trim().toLowerCase();

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("Tên danh mục này đã tồn tại.");
        }

        if (categoryRepository.existsBySlug(slug)) {
            throw new BadRequestException("Slug danh mục này đã tồn tại.");
        }

        if (request.getStatus() == CategoryStatus.DELETED) {
            throw new BadRequestException("Không thể tạo danh mục với trạng thái DELETED.");
        }

        Category parent = resolveParent(request.getParentId(), null);

        Category category = CategoryMapper.toEntity(request);

        category.setName(name);
        category.setSlug(slug);
        category.setParent(parent);

        Category savedCategory = categoryRepository.save(category);

        return CategoryMapper.toResponse(savedCategory);
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findManageableCategory(id);

        String name = request.getName().trim();
        String slug = request.getSlug().trim().toLowerCase();

        categoryRepository.findByNameIgnoreCase(name)
                .filter(existingCategory -> !existingCategory.getId().equals(id))
                .ifPresent(existingCategory -> {
                    throw new BadRequestException("Tên danh mục này đã tồn tại.");
                });

        categoryRepository.findBySlug(slug)
                .filter(existingCategory -> !existingCategory.getId().equals(id))
                .ifPresent(existingCategory -> {
                    throw new BadRequestException("Slug danh mục này đã tồn tại.");
                });

        if (request.getStatus() == CategoryStatus.DELETED) {
            throw new BadRequestException(
                    "Sử dụng API delete để xóa danh mục."
            );
        }

        /*
         * Nếu Category hiện tại đang có Category con,
         * không cho biến nó thành Category con của Category khác.
         *
         * Nếu không sẽ tạo ra cấu trúc 3 cấp
         *
         * trong khi hệ thống chỉ hỗ trợ 2 cấp.
         */
        if (request.getParentId() != null
                && categoryRepository
                .existsByParent_IdAndStatusNot(
                        id,
                        CategoryStatus.DELETED
                )) {

            throw new BadRequestException(
                    "Danh mục cha không thể trở thành danh mục con."
            );
        }

        Category parent = resolveParent(
                request.getParentId(),
                id
        );

        CategoryMapper.updateEntity(category, request);

        category.setName(name);
        category.setSlug(slug);
        category.setParent(parent);

        Category updatedCategory = categoryRepository.save(category);

        return CategoryMapper.toResponse(updatedCategory);
    }

    /*
     * Soft delete:
     * Không xóa record thật khỏi database.
     * Chỉ chuyển status thành DELETED.
     */
    public void deleteCategory(Long id) {

        Category category = findManageableCategory(id);

        boolean hasChildren =
                categoryRepository.existsByParent_IdAndStatusNot(
                        id,
                        CategoryStatus.DELETED
                );

        if (hasChildren) {
            throw new BadRequestException(
                    "Không thể xóa danh mục này vì nó vẫn chứa danh mục con."
            );
        }

        category.setStatus(CategoryStatus.DELETED);

        categoryRepository.save(category);
    }

    public CategoryResponse hideCategory(Long id) {

        Category category = findManageableCategory(id);

        category.setStatus(CategoryStatus.INACTIVE);

        Category updatedCategory =
                categoryRepository.save(category);

        return CategoryMapper.toResponse(updatedCategory);
    }

    public CategoryResponse showCategory(Long id) {

        Category category = findManageableCategory(id);

        category.setStatus(CategoryStatus.ACTIVE);

        Category updatedCategory =
                categoryRepository.save(category);

        return CategoryMapper.toResponse(updatedCategory);
    }

    /*
     * Tìm Category dùng cho Admin quản lý.
     *
     * ACTIVE và INACTIVE đều được phép.
     * DELETED được coi là không còn khả dụng.
     */
    private Category findManageableCategory(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new CategoryNotFoundException(id)
                );

        if (category.getStatus() == CategoryStatus.DELETED) {
            throw new CategoryNotFoundException(id);
        }

        return category;
    }

    /*
     * Xử lý xử lý và kiểm tra category cha để tìm category cha trước khi tạo hoặc cập nhật category.
     */
    private Category resolveParent(Long parentId, Long currentCategoryId) {

        // parentId = null nghĩa là Category cha
        if (parentId == null) {
            return null;
        }

        // Không được tự làm cha của chính mình
        if (currentCategoryId != null
                && parentId.equals(currentCategoryId)) {

            throw new BadRequestException(
                    "Danh mục không thể là danh mục cha của chính nó"
            );
        }

        Category parent = findManageableCategory(parentId);

        /*
         * Nếu parent cũng đã có parent thì parent đang là Category con.
         *
         * Không cho:
         *
         * Điện thoại
         * └── iPhone
         *     └── iPhone 15
         *
         * Vì requirement chỉ hỗ trợ 2 cấp.
         */
        if (parent.getParent() != null) {
            throw new BadRequestException(
                    "Chỉ hỗ trợ 2 cấp danh mục."
            );
        }

        return parent;
    }
}
