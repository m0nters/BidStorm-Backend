package com.taitrinh.online_auction.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.taitrinh.online_auction.dto.category.CategoryResponse;
import com.taitrinh.online_auction.entity.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "isParent", expression = "java(category.isParent())")
    @Mapping(target = "childrenCount", expression = "java(category.getChildren() != null ? category.getChildren().size() : 0)")
    @Mapping(target = "children", ignore = true)
    CategoryResponse toResponse(Category category);

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "isParent", expression = "java(category.isParent())")
    @Mapping(target = "childrenCount", expression = "java(category.getChildren() != null ? category.getChildren().size() : 0)")
    @Mapping(target = "children", source = "children", qualifiedByName = "mapChildren")
    CategoryResponse toResponseWithChildren(Category category);

    @Named("mapChildren")
    default List<CategoryResponse> mapChildren(List<Category> children) {
        if (children == null || children.isEmpty()) {
            return null;
        }
        return children.stream()
                .map(this::toResponse)
                .toList();
    }
}
