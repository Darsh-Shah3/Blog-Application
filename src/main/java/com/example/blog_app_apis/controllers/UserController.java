package com.example.blog_app_apis.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog_app_apis.payloads.ApiResponse;
import com.example.blog_app_apis.payloads.UserDto;
import com.example.blog_app_apis.services.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    // POST-create user
    @PostMapping("/")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDto){
        UserDto  createUserDto=this.userService.createUser(userDto);
        return new ResponseEntity<>(createUserDto,HttpStatus.CREATED);
    }
    // PUT-update user
    @PutMapping("/{userId}")
    public ResponseEntity<UserDto> updateUser(@Valid @RequestBody UserDto userDto,@PathVariable Integer userId){
        UserDto updatedUser=this.userService.updateUser(userDto, userId); 
        // return new ResponseEntity<>(updatedUser,HttpStatus.OK);
        return ResponseEntity.ok(updatedUser);
    }
    // DELETE-delete user
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Integer userId){
        this.userService.deleteUser(userId);
        return new ResponseEntity<ApiResponse>(new ApiResponse("User Deleted Successfully",true),HttpStatus.OK);
    }
    // GET-user get
    @GetMapping("/")
    public ResponseEntity<List<UserDto>> getAllUsers(){
        return ResponseEntity.ok(this.userService.getAllUsers());
    }
    // GET-user get
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getSingleUsers(@PathVariable Integer userId){
        return ResponseEntity.ok(this.userService.getUserById(userId));
    }
}
