package com.yas.order.mapper;

import com.yas.order.model.csv.OrderItemCsv;
import com.yas.order.viewmodel.order.OrderBriefVm;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;
import org.mapstruct.ReportingPolicy;
import com.yas.order.model.Order;
import com.yas.order.viewmodel.order.OrderVm;
import com.yas.order.viewmodel.order.OrderBriefVm;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.WARN  
)
public interface OrderMapper {
    OrderItemCsv toCsv(OrderBriefVm orderBriefVm);
}
