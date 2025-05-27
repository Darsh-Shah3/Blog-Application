package com.example.blog_app_apis.controllers;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog_app_apis.payloads.ApiResponse;
import com.example.blog_app_apis.payloads.CategoryDto;
import com.example.blog_app_apis.services.CategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    
    @Autowired
    private CategoryService categoryService;
    // create
    @PostMapping("/")
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryDto categoryDto){
        CategoryDto category=this.categoryService.createCategory(categoryDto);
        return new ResponseEntity<>(category,HttpStatus.OK);
    }

    // update
    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryDto> updateCategory(@Valid @RequestBody CategoryDto categoryDto,@PathVariable Integer categoryId){
        CategoryDto updatedCategory=this.categoryService.updateCategory(categoryDto, categoryId);
        return new ResponseEntity<>(updatedCategory,HttpStatus.OK);
    }

    // delete
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse> deleteCategory(@PathVariable Integer categoryId){
        this.categoryService.deleteCategory(categoryId);
        return new ResponseEntity<>(new ApiResponse("Category Deleted Successfully",true),HttpStatus.OK);
    }

    // getById
    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryDto> getCategoryById(@RequestBody CategoryDto categoryDto,@PathVariable Integer categoryId){
        return new ResponseEntity<>(this.categoryService.getCategory(categoryId),HttpStatus.OK);
    }

    // getAll
    @GetMapping("/")
    public ResponseEntity<List<CategoryDto>> getAllCategories(){
        return new ResponseEntity<>(this.categoryService.getAllCategories(),HttpStatus.OK);
    }
}
