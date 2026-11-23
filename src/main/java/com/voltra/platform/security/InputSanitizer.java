package com.voltra.platform.security;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class InputSanitizer {
    public String clean(String input) {
        if (input == null) return null;
        return HtmlUtils.htmlEscape(input.trim());
    }
}
