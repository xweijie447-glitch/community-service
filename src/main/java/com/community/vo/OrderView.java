package com.community.vo;

import com.community.domain.ServiceOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单展示对象：订单本体 + 关联人员姓名，避免页面多次查库
 */
@Getter
@AllArgsConstructor
public class OrderView {

    private final ServiceOrder order;
    private final String residentName;
    private final String residentPhone;
    private final String staffName;
    private final String staffPhone;

    public static OrderView of(ServiceOrder order) {
        return new OrderView(order, null, null, null, null);
    }
}
