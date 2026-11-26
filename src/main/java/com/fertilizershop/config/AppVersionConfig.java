package com.fertilizershop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AppVersionConfig {

    @Value("${SHOP_APP_VERSION:1.0}")
    private String appVersion;

    @ModelAttribute
    public void addAppVersion(Model model) {
        model.addAttribute("appVersion", appVersion);
    }
}
