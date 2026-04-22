package com.yas.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.mapper.OrderMapper;
import com.yas.order.model.Order;
import com.yas.order.model.OrderAddress;
import com.yas.order.model.OrderItem;
import com.yas.order.model.enumeration.DeliveryMethod;
import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.repository.OrderItemRepository;
import com.yas.order.repository.OrderRepository;
import com.yas.order.viewmodel.order.PaymentOrderStatusVm;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {OrderService.class})
class OrderServiceTest {

    @MockitoBean
    OrderRepository orderRepository;

    @MockitoBean
    OrderItemRepository orderItemRepository;

    @MockitoBean
    ProductService productService;

    @MockitoBean
    CartService cartService;

    @MockitoBean
    OrderMapper orderMapper;

    @MockitoBean
    PromotionService promotionService;

    @Autowired
    OrderService orderService;

    @Test
    void testGetLatestOrders_whenCountIsZero_returnEmptyList() {
        var result = orderService.getLatestOrders(0);
        assertThat(result).isEmpty();
    }

    @Test
    void testGetLatestOrders_whenNoData_returnEmptyList() {
        when(orderRepository.getLatestOrders(any())).thenReturn(List.of());

        var result = orderService.getLatestOrders(5);

        assertThat(result).isEmpty();
    }

    @Test
    void testGetLatestOrders_whenHasData_returnOrderBriefList() {
        List<Order> orders = List.of(
                createOrder(1L, "first@yopmail.com", OrderStatus.ACCEPTED),
                createOrder(2L, "second@yopmail.com", OrderStatus.PAID)
        );
        when(orderRepository.getLatestOrders(any())).thenReturn(orders);

        var result = orderService.getLatestOrders(2);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().id()).isEqualTo(1L);
        assertThat(result.getFirst().email()).isEqualTo("first@yopmail.com");
        assertThat(result.getLast().id()).isEqualTo(2L);
    }

    @Test
    void testUpdateOrderPaymentStatus_whenCompleted_thenSetPaidStatus() {
        Order order = createOrder(10L, "payment@yopmail.com", OrderStatus.ACCEPTED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentOrderStatusVm request = PaymentOrderStatusVm.builder()
                .orderId(10L)
                .paymentId(101L)
                .paymentStatus(PaymentStatus.COMPLETED.name())
                .build();

        var result = orderService.updateOrderPaymentStatus(request);

        assertThat(order.getPaymentId()).isEqualTo(101L);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.PAID.getName());
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED.name());
    }

    @Test
    void testUpdateOrderPaymentStatus_whenPending_thenKeepOrderStatus() {
        Order order = createOrder(11L, "payment-pending@yopmail.com", OrderStatus.ACCEPTED);
        when(orderRepository.findById(11L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentOrderStatusVm request = PaymentOrderStatusVm.builder()
                .orderId(11L)
                .paymentId(111L)
                .paymentStatus(PaymentStatus.PENDING.name())
                .build();

        var result = orderService.updateOrderPaymentStatus(request);

        assertThat(order.getPaymentId()).isEqualTo(111L);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.ACCEPTED.getName());
    }

    @Test
    void testUpdateOrderPaymentStatus_whenOrderNotFound_throwNotFound() {
        PaymentOrderStatusVm request = PaymentOrderStatusVm.builder()
                .orderId(1000L)
                .paymentId(1L)
                .paymentStatus(PaymentStatus.PENDING.name())
                .build();
        when(orderRepository.findById(1000L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.updateOrderPaymentStatus(request));
    }

    @Test
    void testAcceptOrder_whenOrderFound_updateStatus() {
        Order order = createOrder(20L, "accept@yopmail.com", OrderStatus.PENDING);
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));

        orderService.acceptOrder(20L);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ACCEPTED);
        verify(orderRepository).save(order);
    }

    @Test
    void testAcceptOrder_whenOrderNotFound_throwNotFound() {
        when(orderRepository.findById(2000L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.acceptOrder(2000L));
    }

    @Test
    void testRejectOrder_whenOrderFound_updateStatusAndReason() {
        Order order = createOrder(30L, "reject@yopmail.com", OrderStatus.PENDING);
        when(orderRepository.findById(30L)).thenReturn(Optional.of(order));

        orderService.rejectOrder(30L, "Out of stock");

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.REJECT);
        assertThat(order.getRejectReason()).isEqualTo("Out of stock");
        verify(orderRepository).save(order);
    }

    @Test
    void testRejectOrder_whenOrderNotFound_throwNotFound() {
        when(orderRepository.findById(3000L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.rejectOrder(3000L, "invalid"));
    }

    @Test
    void testFindOrderByCheckoutId_whenFound_returnOrder() {
        Order order = createOrder(40L, "checkout@yopmail.com", OrderStatus.ACCEPTED);
        order.setCheckoutId("checkout-40");
        when(orderRepository.findByCheckoutId("checkout-40")).thenReturn(Optional.of(order));

        var result = orderService.findOrderByCheckoutId("checkout-40");

        assertThat(result.getId()).isEqualTo(40L);
        assertThat(result.getCheckoutId()).isEqualTo("checkout-40");
    }

    @Test
    void testFindOrderByCheckoutId_whenNotFound_throwNotFound() {
        when(orderRepository.findByCheckoutId("missing-checkout")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.findOrderByCheckoutId("missing-checkout"));
    }

    @Test
    void testFindOrderVmByCheckoutId_whenHasItems_returnMappedVm() {
        Order order = createOrder(50L, "checkout-vm@yopmail.com", OrderStatus.ACCEPTED);
        order.setCheckoutId("checkout-50");

        List<OrderItem> orderItems = List.of(
                OrderItem.builder()
                        .id(1L)
                        .orderId(50L)
                        .productId(500L)
                        .productName("Keyboard")
                        .quantity(2)
                        .productPrice(BigDecimal.valueOf(99.9))
                        .build()
        );

        when(orderRepository.findByCheckoutId("checkout-50")).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(50L)).thenReturn(orderItems);

        var result = orderService.findOrderVmByCheckoutId("checkout-50");

        assertThat(result.id()).isEqualTo(50L);
        assertThat(result.orderItems()).hasSize(1);
        assertThat(result.orderItems().getFirst().productId()).isEqualTo(500L);
    }

    private Order createOrder(Long id, String email, OrderStatus orderStatus) {
        return Order.builder()
                .id(id)
                .email(email)
                .billingAddressId(createBillingAddress())
                .totalPrice(BigDecimal.valueOf(120.5))
                .orderStatus(orderStatus)
                .deliveryMethod(DeliveryMethod.GRAB_EXPRESS)
                .deliveryStatus(DeliveryStatus.PREPARING)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
    }

    private OrderAddress createBillingAddress() {
        return OrderAddress.builder()
                .id(1L)
                .contactName("Order Test")
                .phone("0900000000")
                .addressLine1("123 Test Street")
                .city("HCM")
                .zipCode("700000")
                .districtId(1L)
                .districtName("District 1")
                .stateOrProvinceId(1L)
                .stateOrProvinceName("Ho Chi Minh")
                .countryId(84L)
                .countryName("Vietnam")
                .build();
    }
}
