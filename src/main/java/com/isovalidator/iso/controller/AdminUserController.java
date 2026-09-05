package com.isovalidator.iso.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isovalidator.iso.DTO.Blog.CreateUserRequest;
import com.isovalidator.iso.model.User;
import com.isovalidator.iso.service.Blog.UserService;


@RestController
@RequestMapping("/v1/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> createUser(
            @RequestBody CreateUserRequest request) {

        try {

            User user = userService.createUser(request);

            return ResponseEntity.ok(
                    new CreateUserResponse(
                            user.getId(),
                            user.getUsername(),
                            user.getRole(),
                            user.isEnabled()
                    )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    public record CreateUserResponse(
            Long id,
            String username,
            String role,
            boolean enabled
    ) {}

    public record ErrorResponse(
            String message
    ) {}

}
