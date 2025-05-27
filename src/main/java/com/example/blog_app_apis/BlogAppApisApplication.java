package com.example.blog_app_apis;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.blog_app_apis.config.AppConstants;
import com.example.blog_app_apis.entities.Role;
import com.example.blog_app_apis.repositories.RoleRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "com.example.blog_app_apis")
public class BlogAppApisApplication implements CommandLineRunner {

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private RoleRepositories roleRepositories;

	public static void main(String[] args) {
		SpringApplication.run(BlogAppApisApplication.class, args);
	}

	@Bean
	public ModelMapper modelMapper(){
		return new ModelMapper(); 
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Encoded Password : "+this.passwordEncoder.encode("def"));
		try {
			Role role1=new Role();
			role1.setId(AppConstants.ADMIN_USER);
			role1.setName("ROLE_ADMIN");

			Role role2=new Role();
			role2.setId(AppConstants.NORMAL_USER);
			role2.setName("ROLE_NORMAL");

			List<Role> roles=List.of(role1,role2);
			List<Role> result=this.roleRepositories.saveAll(roles);
			result.forEach(r->{
				System.out.println(r.getName());
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Bean
    public CommandLineRunner testPasswordEncoder(PasswordEncoder passwordEncoder) {
        return args -> {
            String rawPassword = "def";
            String hashedPassword = "$2a$10$621zpJoHwk9zIDoUKdXod.fpXeKipSy86C3jhcCSWURdCSk0iif7K";
            boolean matches = passwordEncoder.matches(rawPassword, hashedPassword);
            System.out.println("Password matches: " + matches); // should be true
        };
    }
}
