package org.example.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.common.utils.UserContext;
import org.example.order.dao.PayRecordDao;
import org.example.order.pojo.PayRecord;
import org.example.order.service.IPayRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PayRecordServiceImpl extends ServiceImpl<PayRecordDao,PayRecord> implements IPayRecordService {
    @Override
    public PayRecord queryByOrderId(Long id) {
        return query().eq("order_id", id).one();
    }

    @Override
    public Boolean savePayRecord(Long orderId) {
        PayRecord payRecord = new PayRecord();
        payRecord.setOrderId(orderId);
        payRecord.setPayTime(LocalDateTime.now());
        //TODO 微信支付单号
        payRecord.setPayNo(null);
        return save(payRecord);
    }

}
