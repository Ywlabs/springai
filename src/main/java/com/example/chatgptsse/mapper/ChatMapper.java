package com.example.chatgptsse.mapper;

import com.example.chatgptsse.entity.Chat;
import org.apache.ibatis.annotations.Mapper;
import java.lang.Long;

@Mapper
public interface ChatMapper {
    void insert(Chat chat);
    void update(Chat chat);
    Chat findById(Long id);
} 