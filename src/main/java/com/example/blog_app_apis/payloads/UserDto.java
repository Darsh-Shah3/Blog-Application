package com.example.blog_app_apis.payloads;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class UserDto {
    private int id;

    @NotEmpty
    @Size(min=4,message="Username must be min of 4 characters !!")
    private String name;

    @Email(message="Your email is not valid !!")
    private String email;

    @NotEmpty
    @Size(min=3,max=10,message="Password must be min of 3 chars and max of 10 char !!")
    // @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
    // message = "Password must be at least 8 characters and include a digit, lowercase, uppercase, special character, and no spaces")
    private String password;

    @NotEmpty
    private String about;

    private Set<CommentDto> comments=new HashSet<>();

    private Set<RoleDto> roles=new HashSet<>();

}
