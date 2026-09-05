package com.isovalidator.iso.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isovalidator.iso.DTO.Blog.BlogResponseDTO;
import com.isovalidator.iso.service.Blog.BlogService;

@RestController
@RequestMapping("/v1/api/blogs")
@CrossOrigin
public class blogcontroller {

    private final BlogService blogService;

    public blogcontroller(BlogService blogService) {
        this.blogService = blogService;
    }


    @GetMapping
    public ResponseEntity<List<BlogResponseDTO>>
    getPublishedBlogs() {

        return ResponseEntity.ok(
                blogService.getPublishedBlogs()
        );
    }


    @GetMapping("/{slug}")
    public ResponseEntity<BlogResponseDTO>
    getBlog(@PathVariable String slug) {

        return ResponseEntity.ok(
                blogService.getBlogBySlug(slug)
        );
    }
}
