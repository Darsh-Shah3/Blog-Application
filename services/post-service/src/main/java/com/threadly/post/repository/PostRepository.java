package com.threadly.post.repository;

import com.threadly.post.entity.Post;
import com.threadly.post.repository.query.PostQueries;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
    @Query("select p from Post p where p.communityId = :communityId and ("
            + "p.status = 'PUBLISHED' or (p.status = 'SCHEDULED' and p.scheduledAt <= CURRENT_TIMESTAMP))")
    Page<Post> findByCommunityId(@Param("communityId") UUID communityId, Pageable pageable);

    @Query("select p from Post p where p.authorId = :authorId and ("
            + "p.status = 'PUBLISHED' or (p.status = 'SCHEDULED' and p.scheduledAt <= CURRENT_TIMESTAMP))")
    Page<Post> findByAuthorId(@Param("authorId") UUID authorId, Pageable pageable);

    @Query("select p from Post p where p.communityId in :communityIds and ("
            + "p.status = 'PUBLISHED' or (p.status = 'SCHEDULED' and p.scheduledAt <= CURRENT_TIMESTAMP))")
    Page<Post> findByCommunityIdIn(@Param("communityIds") Collection<UUID> communityIds, Pageable pageable);

    @Query("select p from Post p where "
            + "p.status = 'PUBLISHED' or (p.status = 'SCHEDULED' and p.scheduledAt <= CURRENT_TIMESTAMP)")
    Page<Post> findAllVisible(Pageable pageable);

    @Query(PostQueries.SEARCH_BY_TITLE_JPQL)
    Page<Post> searchByTitleLike(@Param("q") String q, Pageable pageable);

    @Query(value = PostQueries.FIND_ALL_HOT, countQuery = PostQueries.FIND_ALL_HOT_COUNT, nativeQuery = true)
    Page<Post> findAllByHotRank(Pageable pageable);

    @Query(value = PostQueries.FIND_BY_COMMUNITY_HOT, countQuery = PostQueries.FIND_BY_COMMUNITY_HOT_COUNT, nativeQuery = true)
    Page<Post> findByCommunityIdHot(@Param("communityId") UUID communityId, Pageable pageable);

    @Query(value = PostQueries.FIND_BY_AUTHOR_HOT, countQuery = PostQueries.FIND_BY_AUTHOR_HOT_COUNT, nativeQuery = true)
    Page<Post> findByAuthorIdHot(@Param("authorId") UUID authorId, Pageable pageable);

    @Query(value = PostQueries.FIND_BY_COMMUNITIES_HOT, countQuery = PostQueries.FIND_BY_COMMUNITIES_HOT_COUNT, nativeQuery = true)
    Page<Post> findByCommunityIdsHot(@Param("communityIds") String communityIds, Pageable pageable);

    @Query(value = PostQueries.TRENDING, countQuery = PostQueries.TRENDING_COUNT, nativeQuery = true)
    Page<Post> findTrending(@Param("since") Instant since, Pageable pageable);

    @Query(value = PostQueries.TRENDING_IN_COMMUNITIES, countQuery = PostQueries.TRENDING_IN_COMMUNITIES_COUNT, nativeQuery = true)
    Page<Post> findTrendingInCommunities(
            @Param("since") Instant since,
            @Param("communityIds") String communityIds,
            Pageable pageable);

    @Query(value = PostQueries.FOR_YOU, countQuery = PostQueries.FOR_YOU_COUNT, nativeQuery = true)
    Page<Post> findForYou(
            @Param("communityIds") String communityIds,
            @Param("followingIds") String followingIds,
            Pageable pageable);

    @Query(value = PostQueries.SEARCH_FTS, countQuery = PostQueries.SEARCH_FTS_COUNT, nativeQuery = true)
    Page<Post> searchByTitleFts(@Param("q") String q, Pageable pageable);

    @Query(value = PostQueries.RELATED, nativeQuery = true)
    List<Post> findRelated(
            @Param("communityId") UUID communityId,
            @Param("excludeId") UUID excludeId,
            @Param("titleTerms") String titleTerms,
            @Param("limit") int limit);

    List<Post> findByStatusAndScheduledAtLessThanEqual(Post.Status status, Instant scheduledAt);
}
