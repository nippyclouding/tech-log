package com.nippyclouding.tech_log_back.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.nippyclouding.tech_log_back.category.dto.CategoryCreateRequest;
import com.nippyclouding.tech_log_back.category.dto.CategoryResponse;
import com.nippyclouding.tech_log_back.category.dto.CategoryUpdateRequest;
import com.nippyclouding.tech_log_back.category.entity.Category;
import com.nippyclouding.tech_log_back.category.repository.CategoryRepository;
import com.nippyclouding.tech_log_back.global.exception.BusinessException;
import com.nippyclouding.tech_log_back.global.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("카테고리를 생성한다")
    void create_savesCategory() {
        // given
        CategoryCreateRequest request = new CategoryCreateRequest(" Spring ", null);
        given(categoryRepository.existsByName(" Spring ")).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            ReflectionTestUtils.setField(category, "id", 1L);
            return category;
        });

        // when
        CategoryResponse response = categoryService.create(request);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Spring");
    }

    @Test
    @DisplayName("중복된 이름으로 카테고리를 생성하면 CATEGORY_DUPLICATED 예외가 발생한다")
    void create_throwsWhenNameDuplicated() {
        // given
        CategoryCreateRequest request = new CategoryCreateRequest("Spring", null);
        given(categoryRepository.existsByName("Spring")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CATEGORY_DUPLICATED);
    }

    @Test
    @DisplayName("카테고리 이름을 변경한다")
    void update_renamesCategory() {
        // given
        Category category = category(1L, "Spring");
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(categoryRepository.existsByName("Java")).willReturn(false);

        // when
        CategoryResponse response = categoryService.update(1L, new CategoryUpdateRequest("Java"));

        // then
        assertThat(response.name()).isEqualTo("Java");
        assertThat(category.getName()).isEqualTo("Java");
    }

    @Test
    @DisplayName("사용 중인 카테고리를 삭제하면 CATEGORY_IN_USE 예외가 발생한다")
    void delete_throwsWhenCategoryInUse() {
        // given
        Category category = category(1L, "Spring");
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        org.mockito.BDDMockito.willThrow(new DataIntegrityViolationException("in use"))
                .given(categoryRepository)
                .flush();

        // when & then
        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CATEGORY_IN_USE);
        verify(categoryRepository).delete(category);
    }

    private Category category(Long id, String name) {
        Category category = new Category(name);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}
