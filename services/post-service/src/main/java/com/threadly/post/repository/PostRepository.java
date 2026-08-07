package com.threadly.post.repository;

import com.threadly.post.entity.Post;
import com.threadly.post.repository.query.PostQueries;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByCommunityId(Long communityId, Pageable pageable);
    Page<Post> findByAuthorId(Long authorId, Pageable pageable);
    Page<Post> findByCommunityIdIn(Collection<Long> communityIds, Pageable pageable);

    @Query(PostQueries.SEARCH_BY_TITLE)
    Page<Post> searchByTitle(@Param("q") String q, Pageable pageable);
}
