package com.isovalidator.iso.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.isovalidator.iso.model.Blogs;

public interface BlogRepository 

    extends JpaRepository<Blogs, Long> {

    List<Blogs> findByPublishedTrueOrderByPublishedAtDesc();

    Optional<Blogs> findBySlug(String slug);

    boolean existsBySlug(String slug);

}
