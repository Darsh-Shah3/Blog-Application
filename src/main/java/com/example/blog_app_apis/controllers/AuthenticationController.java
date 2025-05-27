// package com.example.blog_app_apis.controllers;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.authentication.AuthenticationManager;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.security.core.userdetails.UserDetailsService;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import com.example.blog_app_apis.payloads.JwtAuthRequest;
// import com.example.blog_app_apis.payloads.JwtAuthResponse;
// import com.example.blog_app_apis.security.JwtTokenHelper;

// @RestController
// @RequestMapping("/api/auth") // <---------------------------------------------- check the url
// public class AuthenticationController {

//     @Autowired
//     private JwtTokenHelper jwtTokenHelper;

//     @Autowired
//     private UserDetailsService userDetailsService;

//     @Autowired
//     private AuthenticationManager authenticationManager;

//     @PostMapping("/login")
//     public ResponseEntity<JwtAuthResponse> createToken(@RequestBody JwtAuthRequest request){
//         System.out.println("=== Login Endpoint Hit ===");
//         System.out.println("Request received: " + request);
//         System.out.println("Username: " + request.getUsername());
//         System.out.println("Password: " + (request.getPassword() != null ? "***PROVIDED***" : "NULL"));
//         // this.authenticate(request.getUsername(),request.getPassword());

//         // UserDetails userDetails=this.userDetailsService.loadUserByUsername(request.getUsername());
//         // String token=this.jwtTokenHelper.generateToken(userDetails);

//         // JwtAuthResponse response=new JwtAuthResponse();
//         // response.setToken(token);
//         // return new ResponseEntity<>(response,HttpStatus.OK);

//         try {
//             this.authenticate(request.getUsername(), request.getPassword());
//             System.out.println("Authentication successful");

//             UserDetails userDetails = this.userDetailsService.loadUserByUsername(request.getUsername());
//             String token = this.jwtTokenHelper.generateToken(userDetails);

//             JwtAuthResponse response = new JwtAuthResponse();
//             response.setToken(token);

//             System.out.println("Token generated successfully");
//             return new ResponseEntity<>(response, HttpStatus.OK);

//         } catch (Exception e) {
//             System.out.println("Authentication failed: " + e.getMessage());
//             e.printStackTrace();
//             throw e;
//         }
//     }

//     private void authenticate(String username, String password) {
//         UsernamePasswordAuthenticationToken authenticationToken=new UsernamePasswordAuthenticationToken(username, password);
//         this.authenticationManager.authenticate(authenticationToken);
//     }
// }

package com.example.blog_app_apis.controllers;

import com.example.blog_app_apis.exceptions.UserNotFoundException;
import com.example.blog_app_apis.payloads.JwtAuthRequest;
import com.example.blog_app_apis.payloads.JwtAuthResponse;
import com.example.blog_app_apis.payloads.UserDto;
import com.example.blog_app_apis.security.JwtTokenHelper;
import com.example.blog_app_apis.services.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @Autowired
    private JwtTokenHelper jwtTokenHelper;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> createToken(@RequestBody JwtAuthRequest request) {
        System.out.println("=== Login Endpoint Hit ===");
        System.out.println("Request received: " + request);
        System.out.println("Username: " + request.getUsername());
        System.out.println("Password: " + (request.getPassword() != null ? "***PROVIDED***" : "NULL"));

        try {
            // Try loading user
            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            } catch (UsernameNotFoundException ex) {
                throw new UserNotFoundException("User with email '" + request.getUsername() + "' not found");
            }

            // Try authentication (password check)
            authenticate(request.getUsername(), request.getPassword());
            System.out.println("Authentication successful");

            // Generate JWT token
            String token = jwtTokenHelper.generateToken(userDetails);
            JwtAuthResponse response = new JwtAuthResponse();
            response.setToken(token);

            System.out.println("Token generated successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (UserNotFoundException ex) {
            System.out.println("User not found: " + ex.getMessage());
            throw ex;
        } catch (BadCredentialsException ex) {
            System.out.println("Invalid password: " + ex.getMessage());
            throw new BadCredentialsException("Invalid email or password");
        } catch (Exception ex) {
            System.out.println("Unexpected error during login: " + ex.getMessage());
            throw new RuntimeException("Authentication failed", ex);
        }
    }

    // register new user api
    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@RequestBody UserDto userDto){
        UserDto registeredUser=this.userService.registerNewUser(userDto);
        return new ResponseEntity<UserDto>(registeredUser,HttpStatus.CREATED);
    }

    private void authenticate(String username, String password) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username,
                password);
        this.authenticationManager.authenticate(authenticationToken);
    }
}
