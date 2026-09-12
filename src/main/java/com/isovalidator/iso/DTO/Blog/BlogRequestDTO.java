package com.isovalidator.iso.DTO.Blog;

import lombok.Data;

@Data
public class BlogRequestDTO {

    private String title;

    private String slug;

    private String summary;

    private String content;

    private String category;

    private String author;

    private boolean published;

    private String coverImageUrl;
    

}
