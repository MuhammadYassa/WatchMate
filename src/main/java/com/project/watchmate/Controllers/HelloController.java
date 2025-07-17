package com.project.watchmate.Controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Models.UserPrincipal;

@RestController
public class HelloController {
    @GetMapping("/hello")
    public String getGreeting() {
        return new String("Hello");
    }
    @GetMapping("/api/test-auth")
    public String testAuth(@AuthenticationPrincipal UserPrincipal user) {
        return "Logged in as: " + user.getUser().getUsername();
    }
}
