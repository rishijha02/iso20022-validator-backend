package com.isovalidator.iso.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isovalidator.iso.DTO.Blog.BlogRequestDTO;
import com.isovalidator.iso.DTO.Blog.BlogResponseDTO;
import com.isovalidator.iso.service.Blog.BlogService;


@RestController
@RequestMapping("/v1/api/admin/blogs")
public class AdminBlogController {

    private final BlogService blogService;

    public AdminBlogController(BlogService blogService) {
        this.blogService = blogService;
    }


    @PostMapping
    public ResponseEntity<BlogResponseDTO>
    createBlog(
            @RequestBody BlogRequestDTO request
    ) {

        return ResponseEntity.ok(
                blogService.createBlog(request)
        );
    }


    @PutMapping("/{id}")
    public ResponseEntity<BlogResponseDTO>
    updateBlog(
            @PathVariable Long id,
            @RequestBody BlogRequestDTO request
    ) {

        return ResponseEntity.ok(
                blogService.updateBlog(id, request)
        );
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void>
    deleteBlog(
            @PathVariable Long id
    ) {

        blogService.deleteBlog(id);

        return ResponseEntity.noContent().build();
    }

}
