package com.yas.order.service;

import static com.yas.order.utils.Constants.ErrorCode.ORDER_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;


import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.commonlibrary.utils.AuthenticationUtils;
import com.yas.order.mapper.OrderMapper;
import com.yas.order.model.Order;
import com.yas.order.model.OrderItem;
import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.model.request.OrderRequest;
import com.yas.order.repository.OrderItemRepository;
import com.yas.order.repository.OrderRepository;
import com.yas.order.viewmodel.order.*;
import com.yas.order.viewmodel.orderaddress.OrderAddressPostVm;
import com.yas.order.viewmodel.product.ProductVariationVm;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.junit.jupiter.api.Disabled;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductService productService;
    @Mock private CartService cartService;
    @Mock private OrderMapper orderMapper;
    @Mock private PromotionService promotionService;

    @InjectMocks
    private OrderService orderService;

    // ── helpers ──────────────────────────────────────────────────────────────

    private Order buildOrder(Long id, OrderStatus status) {
        Order o = new Order();
        o.setId(id);
        o.setEmail("customer@yas.com");
        o.setOrderStatus(status);
        o.setDeliveryStatus(DeliveryStatus.PREPARING);
        o.setPaymentStatus(PaymentStatus.PENDING);
        o.setTotalPrice(BigDecimal.valueOf(500_000));
        o.setCouponCode("SAVE10");

        // BẮT ĐẦU THÊM MỚI: Khởi tạo địa chỉ giả để tránh NullPointerException
        com.yas.order.model.OrderAddress address = new com.yas.order.model.OrderAddress();
        address.setId(1L);
        // Gán địa chỉ vào order (Tùy theo Dev đặt tên biến là shippingAddress hay deliveryAddress, bạn chỉnh lại cho khớp nhé)
        o.setShippingAddressId(address); 
        o.setBillingAddressId(address);
        // KẾT THÚC THÊM MỚI

        return o;
    }

    private OrderAddressPostVm buildAddressVm() {
        return new OrderAddressPostVm(
                "0901234567", "Nguyen Van A",
                "123 Le Loi", "", "Ho Chi Minh",
                "70000", 1L, "District 1",
                1L, "HCM", 1L, "Vietnam"
        );
    }

    private OrderPostVm buildOrderPostVm() {
        // Thêm 3 tham số BigDecimal.ZERO vào cuối
        OrderItemPostVm item = new OrderItemPostVm(
                101L, "Laptop", 1, BigDecimal.valueOf(500_000), "",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        
        // Cập nhật lại thứ tự và số lượng các tham số cho khớp với yêu cầu mới
        return new OrderPostVm(
                "customer@yas.com", "no note",
                buildAddressVm(), buildAddressVm(),
                "checkout-001", 0f, 0f, 0,
                BigDecimal.valueOf(500_000), BigDecimal.ZERO, "SAVE10",
                null, null, PaymentStatus.PENDING,
                List.of(item)
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getOrderWithItemsById
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getOrderWithItemsById")
    class GetOrderWithItemsById {

        @Test
        @DisplayName("Tìm thấy order → trả về OrderVm kèm items")
        void whenFound_shouldReturnOrderVm() {
            Order order = buildOrder(1L, OrderStatus.PENDING);
            OrderItem item = new OrderItem();
            item.setId(1L);
            item.setProductId(101L);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderItemRepository.findAllByOrderId(1L)).thenReturn(List.of(item));

            OrderVm result = orderService.getOrderWithItemsById(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.orderItemVms()).hasSize(1);
            verify(orderRepository).findById(1L);
            verify(orderItemRepository).findAllByOrderId(1L);
        }

        @Test
        @DisplayName("Không tìm thấy order → ném NotFoundException")
        void whenNotFound_shouldThrowNotFoundException() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderWithItemsById(99L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getLatestOrders
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getLatestOrders")
    class GetLatestOrders {

        @Test
        @DisplayName("count hợp lệ → trả về danh sách")
        void whenValidCount_shouldReturnOrders() {
            Order order = buildOrder(1L, OrderStatus.ACCEPTED);
            when(orderRepository.getLatestOrders(any(Pageable.class)))
                    .thenReturn(List.of(order));

            List<OrderBriefVm> result = orderService.getLatestOrders(5);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("count <= 0 → trả về danh sách rỗng ngay, không gọi repository")
        void whenCountZeroOrNegative_shouldReturnEmpty() {
            assertThat(orderService.getLatestOrders(0)).isEmpty();
            assertThat(orderService.getLatestOrders(-1)).isEmpty();
            verifyNoInteractions(orderRepository);
        }

        @Test
        @DisplayName("Repository trả về rỗng → trả về danh sách rỗng")
        void whenRepositoryEmpty_shouldReturnEmpty() {
            when(orderRepository.getLatestOrders(any(Pageable.class)))
                    .thenReturn(Collections.emptyList());

            assertThat(orderService.getLatestOrders(5)).isEmpty();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // findOrderByCheckoutId
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findOrderByCheckoutId")
    class FindOrderByCheckoutId {

        @Test
        @DisplayName("Tìm thấy → trả về Order")
        void whenFound_shouldReturnOrder() {
            Order order = buildOrder(1L, OrderStatus.PENDING);
            when(orderRepository.findByCheckoutId("checkout-001"))
                    .thenReturn(Optional.of(order));

            Order result = orderService.findOrderByCheckoutId("checkout-001");

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Không tìm thấy → ném NotFoundException")
        void whenNotFound_shouldThrowNotFoundException() {
            when(orderRepository.findByCheckoutId("invalid-id"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.findOrderByCheckoutId("invalid-id"))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // findOrderVmByCheckoutId
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findOrderVmByCheckoutId")
    class FindOrderVmByCheckoutId {

        @Test
        @DisplayName("Tìm thấy → trả về OrderGetVm kèm items")
        void whenFound_shouldReturnOrderGetVm() {
            Order order = buildOrder(1L, OrderStatus.PENDING);
            when(orderRepository.findByCheckoutId("checkout-001"))
                    .thenReturn(Optional.of(order));
            when(orderItemRepository.findAllByOrderId(1L)).thenReturn(List.of());

            OrderGetVm result = orderService.findOrderVmByCheckoutId("checkout-001");

            assertThat(result).isNotNull();
            verify(orderItemRepository).findAllByOrderId(1L);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // updateOrderPaymentStatus
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateOrderPaymentStatus")
    class UpdateOrderPaymentStatus {

        @Test
        @DisplayName("Payment COMPLETED → OrderStatus chuyển sang PAID")
        void whenPaymentCompleted_shouldSetOrderStatusPaid() {
            Order order = buildOrder(1L, OrderStatus.ACCEPTED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any())).thenReturn(order);

            PaymentOrderStatusVm input = PaymentOrderStatusVm.builder()
                    .orderId(1L)
                    .paymentId(1L)
                    .paymentStatus(PaymentStatus.COMPLETED.name())
                    .build();

            PaymentOrderStatusVm result = orderService.updateOrderPaymentStatus(input);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(result.paymentId()).isEqualTo(1L);
            assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED.name());
        }

        @Test
        @DisplayName("Payment PENDING → OrderStatus KHÔNG thay đổi sang PAID")
        void whenPaymentPending_shouldNotSetOrderStatusPaid() {
            Order order = buildOrder(1L, OrderStatus.ACCEPTED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any())).thenReturn(order);

            PaymentOrderStatusVm input = PaymentOrderStatusVm.builder()
                    .orderId(1L)
                    .paymentId(2L)
                    .paymentStatus(PaymentStatus.PENDING.name())
                    .build();

            orderService.updateOrderPaymentStatus(input);

            assertThat(order.getOrderStatus()).isNotEqualTo(OrderStatus.PAID);
        }

        @Test
        @DisplayName("Order không tồn tại → ném NotFoundException")
        void whenOrderNotFound_shouldThrow() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            PaymentOrderStatusVm input = PaymentOrderStatusVm.builder()
                    .orderId(99L).paymentStatus("PENDING").build();

            assertThatThrownBy(() -> orderService.updateOrderPaymentStatus(input))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // rejectOrder
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("rejectOrder")
    class RejectOrder {

        @Test
        @DisplayName("Reject thành công → OrderStatus = REJECT, rejectReason được set")
        void whenFound_shouldRejectOrder() {
            Order order = buildOrder(1L, OrderStatus.ACCEPTED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            orderService.rejectOrder(1L, "Hết hàng");

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.REJECT);
            assertThat(order.getRejectReason()).isEqualTo("Hết hàng");
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("Order không tồn tại → ném NotFoundException")
        void whenNotFound_shouldThrow() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.rejectOrder(99L, "reason"))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // acceptOrder
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("acceptOrder")
    class AcceptOrder {

        @Test
        @DisplayName("Accept thành công → OrderStatus = ACCEPTED")
        void whenFound_shouldAcceptOrder() {
            Order order = buildOrder(1L, OrderStatus.PENDING);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            orderService.acceptOrder(1L);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ACCEPTED);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("Order không tồn tại → ném NotFoundException")
        void whenNotFound_shouldThrow() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.acceptOrder(99L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getAllOrder
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAllOrder")
    class GetAllOrder {

        @Test
        @DisplayName("Có dữ liệu → trả về OrderListVm với đúng totalElements")
        @SuppressWarnings("unchecked")
        void whenOrdersExist_shouldReturnOrderListVm() {
            Order order = buildOrder(1L, OrderStatus.PENDING);
            Page<Order> page = new PageImpl<>(List.of(order));
            when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn((Page<Order>) page);

            ZonedDateTime now = ZonedDateTime.now();
            OrderListVm result = orderService.getAllOrder(
                    Pair.of(now.minusDays(7), now),
                    "", List.of(),
                    Pair.of("", ""),
                    "", Pair.of(0, 10)
            );

            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.orderList()).hasSize(1);
        }

        @Test
        @DisplayName("Không có dữ liệu → trả về OrderListVm rỗng")
        @SuppressWarnings("unchecked")
        void whenNoOrders_shouldReturnEmptyListVm() {
            Page<Order> emptyPage = new PageImpl<>(List.of());
            when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(emptyPage);

            ZonedDateTime now = ZonedDateTime.now();
            OrderListVm result = orderService.getAllOrder(
                    Pair.of(now.minusDays(7), now),
                    "", List.of(),
                    Pair.of("", ""),
                    "", Pair.of(0, 10)
            );

            assertThat(result.totalElements()).isEqualTo(0);
            assertThat(result.orderList()).isNull();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // isOrderCompletedWithUserIdAndProductId
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("isOrderCompletedWithUserIdAndProductId")
    class IsOrderCompleted {

        @Test
        @DisplayName("Không có variations → dùng productId gốc, trả về true nếu tồn tại")
        @SuppressWarnings("unchecked")
        void whenNoVariations_andOrderExists_shouldReturnTrue() {
            try (MockedStatic<AuthenticationUtils> auth =
                         mockStatic(AuthenticationUtils.class)) {
                auth.when(AuthenticationUtils::extractUserId).thenReturn("user-001");

                when(productService.getProductVariations(101L)).thenReturn(List.of());
                when(orderRepository.findOne(any(Specification.class)))
                        .thenReturn(Optional.of(buildOrder(1L, OrderStatus.COMPLETED)));

                OrderExistsByProductAndUserGetVm result =
                        orderService.isOrderCompletedWithUserIdAndProductId(101L);

                assertThat(result.isPresent()).isTrue();
            }
        }

        @Test
        @DisplayName("Có variations → dùng danh sách variation ids, trả về false nếu không tồn tại")
        @SuppressWarnings("unchecked")
        void whenVariationsExist_andNoOrder_shouldReturnFalse() {
            try (MockedStatic<AuthenticationUtils> auth =
                         mockStatic(AuthenticationUtils.class)) {
                auth.when(AuthenticationUtils::extractUserId).thenReturn("user-001");

                ProductVariationVm var1 = new ProductVariationVm(201L, "Size M", "SKU123");
                when(productService.getProductVariations(101L)).thenReturn(List.of(var1));
                when(orderRepository.findOne(any(Specification.class)))
                        .thenReturn(Optional.empty());

                OrderExistsByProductAndUserGetVm result =
                        orderService.isOrderCompletedWithUserIdAndProductId(101L);

                assertThat(result.isPresent()).isFalse();
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getMyOrders
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getMyOrders")
    class GetMyOrders {

        @Test
        @DisplayName("Có orders → trả về danh sách OrderGetVm")
        @SuppressWarnings("unchecked")
        void whenOrdersExist_shouldReturnList() {
            try (MockedStatic<AuthenticationUtils> auth =
                         mockStatic(AuthenticationUtils.class)) {
                auth.when(AuthenticationUtils::extractUserId).thenReturn("user-001");

                Order order = buildOrder(1L, OrderStatus.ACCEPTED);
                when(orderRepository.findAll(any(Specification.class), any(Sort.class)))
                        .thenReturn(List.of(order));

                List<OrderGetVm> result =
                        orderService.getMyOrders("Laptop", OrderStatus.ACCEPTED);

                assertThat(result).hasSize(1);
            }
        }

        @Test
        @DisplayName("Không có orders → trả về danh sách rỗng")
        @SuppressWarnings("unchecked")
        void whenNoOrders_shouldReturnEmptyList() {
            try (MockedStatic<AuthenticationUtils> auth =
                         mockStatic(AuthenticationUtils.class)) {
                auth.when(AuthenticationUtils::extractUserId).thenReturn("user-001");

                when(orderRepository.findAll(any(Specification.class), any(Sort.class)))
                        .thenReturn(List.of());

                List<OrderGetVm> result = orderService.getMyOrders("", null);

                assertThat(result).isEmpty();
            }
        }
    }
    
    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("Tạo order thành công → lưu và trả về OrderVm")
        void whenValidRequest_shouldCreateAndReturnOrderVm() {
            OrderPostVm postVm = buildOrderPostVm();
            Order savedOrder = buildOrder(1L, OrderStatus.PENDING);

            when(orderMapper.toModel(any(OrderRequest.class))).thenReturn(savedOrder);
            when(orderRepository.save(any())).thenReturn(savedOrder);
            when(orderItemRepository.saveAll(anyList())).thenReturn(List.of());

            OrderVm result = orderService.createOrder(postVm);

            assertThat(result).isNotNull();
            verify(orderRepository).save(any(Order.class));

            // CartItemService xóa từng productId riêng lẻ, không nhận List
            List<Long> productIds = postVm.orderItemPostVms()
                    .stream()
                    .map(OrderItemPostVm::productId)
                    .toList();
            for (Long productId : productIds) {
                verify(cartItemService).deleteCartItem(productId);
            }
        }

        @Test
        @DisplayName("Tạo order với promotion → gọi verifyPromotion với couponCode đúng")
        void whenCouponProvided_shouldApplyPromotion() {
            OrderPostVm postVm = buildOrderPostVm(); // giả sử couponCode = "SAVE10"
            Order savedOrder = buildOrder(1L, OrderStatus.PENDING);

            PromotionVerifyResultDto verifyResult = new PromotionVerifyResultDto(
                    true,
                    1L,
                    "SAVE10",
                    DiscountType.PERCENTAGE,
                    10.0
            );

            when(orderMapper.toModel(any())).thenReturn(savedOrder);
            when(orderRepository.save(any())).thenReturn(savedOrder);

            // verifyPromotion nhận PromotionVerifyVm chứa couponCode + productIds + orderPrice
            when(promotionService.verifyPromotion(any(PromotionVerifyVm.class)))
                    .thenReturn(verifyResult);

            orderService.createOrder(postVm);

            // Verify đúng signature của PromotionService
            verify(promotionService).verifyPromotion(argThat(vm ->
                    "SAVE10".equals(vm.couponCode())
            ));
        }
    }
    
}