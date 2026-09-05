package com.isovalidator.iso.DTO.Blog;

import lombok.Data;

@Data
public class CreateUserRequest {


    private String username;

    private String password;

    private String role;
}
