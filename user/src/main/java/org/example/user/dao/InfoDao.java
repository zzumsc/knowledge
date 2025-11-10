package org.example.user.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.example.user.pojo.User;

@Mapper
public interface InfoDao extends BaseMapper<User> {
}
