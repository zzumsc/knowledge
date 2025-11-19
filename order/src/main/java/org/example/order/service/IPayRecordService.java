package org.example.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.order.pojo.PayRecord;

public interface IPayRecordService extends IService<PayRecord> {
    PayRecord queryByOrderId(Long id);

    Boolean savePayRecord(Long orderId);

}
