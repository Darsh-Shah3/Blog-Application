package com.example.blog_app_apis.services.impl;

import java.util.ArrayList;
import java.util.List;


import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.blog_app_apis.entities.Category;
import com.example.blog_app_apis.exceptions.ResourceNotFoundException;
import com.example.blog_app_apis.payloads.CategoryDto;
import com.example.blog_app_apis.repositories.CategoryRepositories;
import com.example.blog_app_apis.services.CategoryService;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepositories categoryRepo;

    @Autowired
    private ModelMapper modelMapper;
    
    @Override
    public CategoryDto createCategory(CategoryDto categoryDto) {
        
        Category category = this.modelMapper.map(categoryDto, Category.class); 
        Category addedCategory=this.categoryRepo.save(category);
        return this.modelMapper.map(addedCategory, CategoryDto.class);
    }

    @Override
    public CategoryDto updateCategory(CategoryDto categoryDto, Integer categoryId) {

        Category category=this.categoryRepo.findById(categoryId).orElseThrow(()->new ResourceNotFoundException("Category","Category Id",categoryId));
        category.setCategoryTitle(categoryDto.getCategoryTitle());
        category.setCategoryDescription(categoryDto.getCategoryDescription());
        Category updatedCategory = this.categoryRepo.save(category);
        return this.modelMapper.map(updatedCategory, CategoryDto.class);
    }

    @Override
    public void deleteCategory(Integer categoryId) {
        Category category=this.categoryRepo.findById(categoryId).orElseThrow(()->new ResourceNotFoundException("Category","Category Id",categoryId));
        this.categoryRepo.delete(category);
    }

    @Override
    public CategoryDto getCategory(Integer categoryId) {
        Category category=this.categoryRepo.findById(categoryId).orElseThrow(()->new ResourceNotFoundException("Category", "Category Id", categoryId));
        return this.modelMapper.map(category, CategoryDto.class);
    }

    @Override
    public List<CategoryDto> getAllCategories() {
        List<Category> catList=this.categoryRepo.findAll();
        List<CategoryDto> dto=new ArrayList<>();
        for (Category category : catList) {
            CategoryDto categoryDto=this.modelMapper.map(category,CategoryDto.class);
            dto.add(categoryDto);
        }
        return dto;
    }
    
}
