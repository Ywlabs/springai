package com.ywlabs.springai.mapper;

import com.ywlabs.springai.entity.Chat;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import java.lang.Long;

@Mapper
@Repository
public interface ChatMapper {
    void insert(Chat chat);
    void update(Chat chat);
    Chat findById(Long id);
} 