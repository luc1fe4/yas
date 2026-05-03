package com.yas.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.config.ServiceUrlConfig;
import com.yas.product.viewmodel.NoFileMediaVm;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private ServiceUrlConfig serviceUrlConfig;

    @InjectMocks
    private MediaService mediaService;

    private Jwt mockJwt;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        jwtToken = "test-jwt-token";
        mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn(jwtToken);

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(mockJwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void saveFile_whenValidMultipartFileProvided_thenReturnNoFileMediaVm() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        String caption = "Test Caption";
        String fileNameOverride = "test-file.jpg";
        String mediaUrl = "http://media-service/medias";

        when(serviceUrlConfig.media()).thenReturn(mediaUrl);

        NoFileMediaVm expectedVm = new NoFileMediaVm(1L, "test-file.jpg", "Test Caption", "", "http://media-service/medias/1");
        RestClient.RequestBodyUriSpec mockRequestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec mockRequestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(mockRequestBodyUriSpec);
        when(mockRequestBodyUriSpec.uri(any(URI.class))).thenReturn(mockRequestBodySpec);
        when(mockRequestBodySpec.contentType(MediaType.MULTIPART_FORM_DATA)).thenReturn(mockRequestBodySpec);
        when(mockRequestBodySpec.headers(any())).thenReturn(mockRequestBodySpec);
        when(mockRequestBodySpec.body(any())).thenReturn(mockResponseSpec);
        when(mockResponseSpec.body(NoFileMediaVm.class)).thenReturn(expectedVm);

        NoFileMediaVm result = mediaService.saveFile(multipartFile, caption, fileNameOverride);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("test-file.jpg", result.name());
        assertEquals("Test Caption", result.caption());
    }

    @Test
    void getMedia_whenValidIdProvided_thenReturnNoFileMediaVm() {
        Long mediaId = 1L;
        String mediaUrl = "http://media-service/medias";

        when(serviceUrlConfig.media()).thenReturn(mediaUrl);

        NoFileMediaVm expectedVm = new NoFileMediaVm(1L, "test-file.jpg", "Test Caption", "", "http://media-service/medias/1");
        RestClient.RequestBodyUriSpec mockRequestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(mockRequestBodyUriSpec);
        when(mockRequestBodyUriSpec.uri(any(URI.class))).thenReturn(mockRequestBodyUriSpec);
        when(mockRequestBodyUriSpec.retrieve()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.body(NoFileMediaVm.class)).thenReturn(expectedVm);

        NoFileMediaVm result = mediaService.getMedia(mediaId);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("test-file.jpg", result.name());
    }

    @Test
    void getMedia_whenNullIdProvided_thenReturnDefaultNoImageVm() {
        NoFileMediaVm result = mediaService.getMedia(null);

        assertNotNull(result);
        assertEquals("", result.url());
    }

    @Test
    void removeMedia_whenValidIdProvided_thenInvokeDeleteRestApi() {
        Long mediaId = 1L;
        String mediaUrl = "http://media-service/medias";

        when(serviceUrlConfig.media()).thenReturn(mediaUrl);

        RestClient.RequestBodyUriSpec mockRequestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec mockRequestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.delete()).thenReturn(mockRequestBodyUriSpec);
        when(mockRequestBodyUriSpec.uri(any(URI.class))).thenReturn(mockRequestBodySpec);
        when(mockRequestBodySpec.headers(any())).thenReturn(mockRequestBodySpec);
        when(mockRequestBodySpec.retrieve()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.body(Void.class)).thenReturn(null);

        mediaService.removeMedia(mediaId);

        verify(restClient).delete();
    }
}
