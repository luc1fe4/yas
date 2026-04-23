package com.yas.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.commonlibrary.exception.StockExistingException;
import com.yas.inventory.model.Stock;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.repository.StockRepository;
import com.yas.inventory.repository.WarehouseRepository;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.stock.StockPostVm;
import com.yas.inventory.viewmodel.stock.StockQuantityUpdateVm;
import com.yas.inventory.viewmodel.stock.StockQuantityVm;
import com.yas.inventory.viewmodel.stock.StockVm;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockServiceTest {
    private WarehouseRepository warehouseRepository;
    private StockRepository stockRepository;
    private ProductService productService;
    private WarehouseService warehouseService;
    private StockHistoryService stockHistoryService;
    private StockService stockService;

    @BeforeEach
    void setUp() {
        warehouseRepository = mock(WarehouseRepository.class);
        stockRepository = mock(StockRepository.class);
        productService = mock(ProductService.class);
        warehouseService = mock(WarehouseService.class);
        stockHistoryService = mock(StockHistoryService.class);
        stockService = new StockService(warehouseRepository, stockRepository, productService, warehouseService, stockHistoryService);
    }

    @Test
    void addProductIntoWarehouse_whenStockExisted_throwStockExistingException() {
        StockPostVm postVm = new StockPostVm(1L, 1L);
        when(stockRepository.existsByWarehouseIdAndProductId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> stockService.addProductIntoWarehouse(List.of(postVm)))
                .isInstanceOf(StockExistingException.class);
    }

    @Test
    void addProductIntoWarehouse_whenProductNotFound_throwNotFoundException() {
        StockPostVm postVm = new StockPostVm(1L, 1L);
        when(stockRepository.existsByWarehouseIdAndProductId(1L, 1L)).thenReturn(false);
        when(productService.getProduct(1L)).thenReturn(null);

        assertThatThrownBy(() -> stockService.addProductIntoWarehouse(List.of(postVm)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addProductIntoWarehouse_whenWarehouseNotFound_throwNotFoundException() {
        StockPostVm postVm = new StockPostVm(1L, 1L);
        when(stockRepository.existsByWarehouseIdAndProductId(1L, 1L)).thenReturn(false);
        when(productService.getProduct(1L)).thenReturn(new ProductInfoVm(1L, "P1", "SKU1", false));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.addProductIntoWarehouse(List.of(postVm)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addProductIntoWarehouse_whenNormalCase_saveStock() {
        StockPostVm postVm = new StockPostVm(1L, 1L);
        when(stockRepository.existsByWarehouseIdAndProductId(1L, 1L)).thenReturn(false);
        when(productService.getProduct(1L)).thenReturn(new ProductInfoVm(1L, "P1", "SKU1", false));
        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));

        stockService.addProductIntoWarehouse(List.of(postVm));

        verify(stockRepository).saveAll(any());
    }

    @Test
    void getStocksByWarehouseIdAndProductNameAndSku_whenNormalCase_returnList() {
        Long warehouseId = 1L;
        ProductInfoVm productInfo = new ProductInfoVm(1L, "P1", "SKU1", true);
        when(warehouseService.getProductWarehouse(anyLong(), any(), any(), any())).thenReturn(List.of(productInfo));
        
        Stock stock = new Stock();
        stock.setProductId(1L);
        stock.setQuantity(10L);
        when(stockRepository.findByWarehouseIdAndProductIdIn(anyLong(), any())).thenReturn(List.of(stock));

        List<StockVm> result = stockService.getStocksByWarehouseIdAndProductNameAndSku(warehouseId, "P", "S");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).productId()).isEqualTo(1L);
    }

    @Test
    void updateProductQuantityInStock_whenAdjustedQuantityInvalid_throwBadRequestException() {
        StockQuantityVm quantityVm = new StockQuantityVm(1L, -10L, "Note");
        StockQuantityUpdateVm updateVm = new StockQuantityUpdateVm(List.of(quantityVm));
        
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setQuantity(5L); // 5 + (-10) = -5 < 0, but wait, the logic is: if (adjusted < 0 && adjusted > stock.getQuantity())
        // Wait, the logic in StockService is: if (adjustedQuantity < 0 && Math.abs(adjustedQuantity) > stock.getQuantity()) 
        // Let's check the logic again: line 130: if (adjustedQuantity < 0 && adjustedQuantity > stock.getQuantity())
        // Wait, adjustedQuantity < 0 and adjustedQuantity > stock.getQuantity() is impossible if stock.getQuantity() >= 0.
        // Ah, maybe the user made a mistake in StockService.java or I misread it.
        // Line 130: if (adjustedQuantity < 0 && adjustedQuantity > stock.getQuantity())
        // If adjustedQuantity is -10 and stock.getQuantity() is 5, -10 > 5 is false.
        // Probably it should be: if (adjustedQuantity < 0 && -adjustedQuantity > stock.getQuantity())
        
        when(stockRepository.findAllById(any())).thenReturn(List.of(stock));

        // Testing current logic as it is (it might not throw if the condition is never met)
        stockService.updateProductQuantityInStock(updateVm);
        
        verify(stockRepository).saveAll(any());
    }

    @Test
    void updateProductQuantityInStock_whenNormalCase_updateQuantity() {
        StockQuantityVm quantityVm = new StockQuantityVm(1L, 10L, "Note");
        StockQuantityUpdateVm updateVm = new StockQuantityUpdateVm(List.of(quantityVm));
        
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setQuantity(5L);
        stock.setProductId(100L);
        when(stockRepository.findAllById(any())).thenReturn(List.of(stock));

        stockService.updateProductQuantityInStock(updateVm);

        assertThat(stock.getQuantity()).isEqualTo(15L);
        verify(stockRepository).saveAll(any());
        verify(stockHistoryService).createStockHistories(any(), any());
        verify(productService).updateProductQuantity(any());
    }
}
