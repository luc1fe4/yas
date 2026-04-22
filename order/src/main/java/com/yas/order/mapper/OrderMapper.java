package com.yas.order.mapper;

import com.yas.order.model.csv.OrderItemCsv;
import com.yas.order.viewmodel.order.OrderBriefVm;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.WARN  
)
public interface OrderMapper {
    @Mapping(target = "someField", ignore = true)  
    OrderVm toVm(Order order);
}
