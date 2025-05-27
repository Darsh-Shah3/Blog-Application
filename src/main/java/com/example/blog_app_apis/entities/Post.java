package com.example.blog_app_apis.entities;


import java.util.Date;
import java.util.HashSet;
import java.util.Set;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="posts")
@Getter
@Setter
@NoArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer postId;

    @Column(name="titles",length=100,nullable = false)
    private String title;

    private String content;
 
    private String imageName;

    private Date addedDate;

    @ManyToOne
    @JoinColumn(name = "categoryId")
    private Category category;

    @ManyToOne
    @JoinColumn(name="userId")
    private User user;

    @OneToMany(mappedBy = "post",cascade=CascadeType.ALL)
    private Set<Comment> comments=new HashSet<>();
}
