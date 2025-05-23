package com.example.chatgptsse.util;

import org.springframework.util.StringUtils;

public class XssFilterUtil {
    
    public static String filter(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        
        return value.replaceAll("<", "&lt;")
                   .replaceAll(">", "&gt;")
                   .replaceAll("\"", "&quot;")
                   .replaceAll("'", "&#x27;")
                   .replaceAll("&", "&amp;")
                   .replaceAll("\\(", "&#40;")
                   .replaceAll("\\)", "&#41;")
                   .replaceAll("eval\\((.*)\\)", "")
                   .replaceAll("[\\\"\\\'][\\s]*javascript:(.*)[\\\"\\\']", "\"\"")
                   .replaceAll("script", "");
    }
} 