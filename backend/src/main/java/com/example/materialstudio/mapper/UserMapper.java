package com.example.materialstudio.mapper;

import com.example.materialstudio.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    void insert(@Param("id") String id, @Param("nickname") String nickname);

    User findById(@Param("id") String id);

    void updateNickname(@Param("id") String id, @Param("nickname") String nickname);
}
