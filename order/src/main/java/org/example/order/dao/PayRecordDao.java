package org.example.order.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.order.pojo.PayRecord;

@Mapper
public interface PayRecordDao extends BaseMapper<PayRecord> {
}
