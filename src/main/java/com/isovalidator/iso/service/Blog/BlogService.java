
package com.isovalidator.iso.service.Blog;

import java.util.List;

import org.springframework.stereotype.Service;

import com.isovalidator.iso.DTO.Blog.BlogRequestDTO;
import com.isovalidator.iso.DTO.Blog.BlogResponseDTO;
import com.isovalidator.iso.Repository.BlogRepository;
import com.isovalidator.iso.model.Blogs;

@Service
public class BlogService {


     private final BlogRepository blogRepository;

    public BlogService(BlogRepository blogRepository) {
        this.blogRepository = blogRepository;
    }

    // PUBLIC

    public List<BlogResponseDTO> getPublishedBlogs() {

        return blogRepository
                .findByPublishedTrueOrderByPublishedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BlogResponseDTO getBlogBySlug(String slug) {

        Blogs blog = blogRepository
                .findBySlug(slug)
                .orElseThrow(() ->
                        new RuntimeException("Blog not found"));

        return toResponse(blog);
    }


    // ADMIN

    public BlogResponseDTO createBlog(BlogRequestDTO request) {

        if (blogRepository.existsBySlug(request.getSlug())) {
            throw new RuntimeException(
                    "Blog with this slug already exists"
            );
        }

        Blogs blog = Blogs.builder()
                .title(request.getTitle())
                .slug(request.getSlug())
                .summary(request.getSummary())
                .content(request.getContent())
                .category(request.getCategory())
                .author(request.getAuthor())
                .published(request.isPublished())
                .build();

        return toResponse(
                blogRepository.save(blog)
        );
    }


    public BlogResponseDTO updateBlog(
            Long id,
            BlogRequestDTO request
    ) {

        Blogs blog = blogRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Blog not found"));

        blog.setTitle(request.getTitle());
        blog.setSlug(request.getSlug());
        blog.setSummary(request.getSummary());
        blog.setContent(request.getContent());
        blog.setCategory(request.getCategory());
        blog.setAuthor(request.getAuthor());
        blog.setPublished(request.isPublished());

        return toResponse(
                blogRepository.save(blog)
        );
    }


    public void deleteBlog(Long id) {

        if (!blogRepository.existsById(id)) {
            throw new RuntimeException("Blog not found");
        }

        blogRepository.deleteById(id);
    }


    private BlogResponseDTO toResponse(Blogs blog) {

        return BlogResponseDTO.builder()
                .id(blog.getId())
                .title(blog.getTitle())
                .slug(blog.getSlug())
                .summary(blog.getSummary())
                .content(blog.getContent())
                .category(blog.getCategory())
                .author(blog.getAuthor())
                .published(blog.isPublished())
                .publishedAt(blog.getPublishedAt())
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                .build();
    }

}
