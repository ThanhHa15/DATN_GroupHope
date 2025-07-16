package com.datn.datn.common;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute
    public void addLoggedInUserToModel(HttpSession session, Model model) {
        Object loggedInUser = session.getAttribute("loggedInUser");
        model.addAttribute("loggedInUser", loggedInUser);
    }
}
