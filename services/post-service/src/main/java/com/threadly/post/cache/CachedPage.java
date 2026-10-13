package com.threadly.post.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedPage<T> {
    private List<T> content;
    private long totalElements;
    private int number;
    private int size;

    public static <T> CachedPage<T> from(Page<T> page) {
        return CachedPage.<T>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .number(page.getNumber())
                .size(page.getSize())
                .build();
    }

    public Page<T> toPage() {
        int pageSize = Math.max(size, 1);
        return new PageImpl<>(content == null ? List.of() : content,
                PageRequest.of(Math.max(number, 0), pageSize),
                totalElements);
    }
}
