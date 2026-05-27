package com.nippyclouding.tech_log_back.admin.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class AdminConsoleController {

    private final String frontendOrigin;

    public AdminConsoleController(@Value("${app.frontend-origin:http://localhost:3000}") String frontendOrigin) {
        this.frontendOrigin = frontendOrigin;
    }

    @GetMapping("/admin-console")
    public RedirectView redirectToReactAdmin() {
        return new RedirectView(frontendOrigin + "/admin");
    }
}
