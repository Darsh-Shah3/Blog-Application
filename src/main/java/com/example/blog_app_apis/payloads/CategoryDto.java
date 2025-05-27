package com.example.blog_app_apis.payloads;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CategoryDto {
    /*
     * You're using int for categoryId, which is a primitive type.
     * 
     * Primitive types default to 0, not null.
     * 
     * So when you map CategoryDto to Category, the categoryId becomes 0, and JPA
     * interprets this as an existing entity with ID 0 — which doesn't exist.
     * 
     * Result: ObjectOptimisticLockingFailureException.
     */
    // private int categoryId;
    private Integer categoryId;

    @NotEmpty
    @Size(min = 4, message = "Min length should be of 4")
    private String categoryTitle;

    @NotEmpty
    @Size(min = 4, message = "Min length should be of 4")
    private String categoryDescription;
}
