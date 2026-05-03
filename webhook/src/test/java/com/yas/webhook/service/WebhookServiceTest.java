package com.yas.webhook.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.webhook.integration.api.WebhookApi;
import com.yas.webhook.model.Webhook;
import com.yas.webhook.model.WebhookEventNotification;
import com.yas.webhook.model.dto.WebhookEventNotificationDto;
import com.yas.webhook.model.mapper.WebhookMapper;
import com.yas.webhook.model.viewmodel.webhook.WebhookDetailVm;
import com.yas.webhook.repository.WebhookEventNotificationRepository;
import com.yas.webhook.repository.WebhookRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    WebhookRepository webhookRepository;
    @Mock
    WebhookMapper webhookMapper;
    @Mock
    WebhookEventNotificationRepository webhookEventNotificationRepository;
    @Mock
    WebhookApi webHookApi;

    @InjectMocks
    WebhookService webhookService;

    @Test
    void test_notifyToWebhook_ShouldNotException() {

        WebhookEventNotificationDto notificationDto = WebhookEventNotificationDto
            .builder()
            .notificationId(1L)
            .url("")
            .secret("")
            .build();

        WebhookEventNotification notification = new WebhookEventNotification();
        when(webhookEventNotificationRepository.findById(notificationDto.getNotificationId()))
            .thenReturn(Optional.of(notification));

        webhookService.notifyToWebhook(notificationDto);

        verify(webhookEventNotificationRepository).save(notification);
        verify(webHookApi).notify(notificationDto.getUrl(), notificationDto.getSecret(), notificationDto.getPayload());
    }

    @Test
    void test_findById_shouldReturnWebhookDetailVm() {
        Webhook webhook = new Webhook();
        WebhookDetailVm webhookDetailVm = new WebhookDetailVm();
        when(webhookRepository.findById(1L)).thenReturn(Optional.of(webhook));
        when(webhookMapper.toWebhookDetailVm(webhook)).thenReturn(webhookDetailVm);

        WebhookDetailVm result = webhookService.findById(1L);

        assertEquals(webhookDetailVm, result);
    }

    @Test
    void test_delete_shouldDeleteWebhook() {
        when(webhookRepository.existsById(1L)).thenReturn(true);

        webhookService.delete(1L);

        verify(webhookRepository).deleteById(1L);
    }
}
