package com.example.blog_app_apis.services.impl;

import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.blog_app_apis.config.AppConstants;
import com.example.blog_app_apis.entities.Role;
import com.example.blog_app_apis.entities.User;
import com.example.blog_app_apis.payloads.UserDto;
import com.example.blog_app_apis.repositories.RoleRepositories;
import com.example.blog_app_apis.repositories.UserRepositories;
import com.example.blog_app_apis.services.UserService;
import com.example.blog_app_apis.exceptions.ResourceNotFoundException;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepositories userRepo;
 
    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepositories roleRepositories;

    @Override
    public UserDto createUser(UserDto userDto) {
        User user = this.dtoToUser(userDto);
        User savedUser = this.userRepo.save(user);
        return this.userToDto(savedUser);
    }

    @Override
    public UserDto updateUser(UserDto userDto, Integer userId) {
        User user = this.userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        user.setPassword(userDto.getPassword());
        user.setAbout(userDto.getAbout());
        
        User updatedUser=this.userRepo.save(user);
        return this.userToDto(updatedUser);
    }

    @Override
    public UserDto getUserById(Integer userId) {
        User user=this.userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return this.userToDto(user);
    }

    @Override
    public List<UserDto> getAllUsers() {
        List<User> users=this.userRepo.findAll();

        /* By Lambda function */
        // List<UserDto> userDtos=users.stream().map(user->this.userToDto(user)).collect(Collectors.toList());
        
        List<UserDto> userDtos=new ArrayList<>();
        for(User user: users){
            UserDto dto=this.userToDto(user); // Convert User to UserDto
            userDtos.add(dto); // Add the converted UserDto to the list

        }
        return userDtos;
    }

    @Override
    public void deleteUser(Integer userId) {
        User user=this.userRepo.findById(userId).orElseThrow(()-> new ResourceNotFoundException("User ", "id", userId));
        this.userRepo.delete(user);
    }

    public User dtoToUser(UserDto userDto) {
        // User user = new User();
        // user.setId(userDto.getId());
        // user.setEmail(userDto.getEmail());
        // user.setPassword(userDto.getPassword());
        // user.setName(userDto.getName());
        // user.setAbout(userDto.getAbout());
        // return user;
        User user=this.modelMapper.map(userDto, User.class);
        return user;
    }

    public UserDto userToDto(User user) {
        // UserDto userDto = new UserDto();
        // userDto.setAbout(user.getAbout());
        // userDto.setName(user.getName());
        // userDto.setPassword(user.getPassword());
        // userDto.setEmail(user.getEmail());
        // userDto.setId(user.getId());
        // return userDto;
        UserDto userDto=this.modelMapper.map(user, UserDto.class);
        return userDto;
    }

    @Override
    public UserDto registerNewUser(UserDto userDto) {
        User user=this.modelMapper.map(userDto, User.class);

        //encoded the password
        user.setPassword(this.passwordEncoder.encode(user.getPassword()));

        // roles 
        Role role=this.roleRepositories.findById(AppConstants.NORMAL_USER).get();

        user.getRoles().add(role);

        User newUser=this.userRepo.save(user);

        return this.modelMapper.map(newUser, UserDto.class);
    }
}
