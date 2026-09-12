package com.isovalidator.iso.DTO.Blog;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BlogResponseDTO {

    private Long id;

    private String title;

    private String slug;

    private String summary;

    private String content;

    private String category;

    private String author;

    private boolean published;

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String coverImageUrl;

}
