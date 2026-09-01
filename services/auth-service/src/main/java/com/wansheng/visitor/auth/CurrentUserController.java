package com.wansheng.visitor.auth;

import java.security.Principal;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/visitor/auth")
class CurrentUserController {
    @GetMapping("/me")
    Map<String, String> me(Principal principal, Authentication authentication) {
        String role = authentication.getAuthorities().stream().findFirst().orElseThrow().getAuthority().replace("ROLE_", "");
        return Map.of("username", principal.getName(), "role", role);
    }
}
