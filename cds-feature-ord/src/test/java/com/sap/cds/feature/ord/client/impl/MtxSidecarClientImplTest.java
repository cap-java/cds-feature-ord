/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.client.impl;

import static com.sap.cds.services.ErrorStatuses.SERVER_ERROR;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.ord.clients.impl.MtxSidecarClientImpl;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.environment.CdsEnvironment;
import com.sap.cds.services.environment.CdsProperties;
import com.sap.cds.services.environment.CdsProperties.Model;
import com.sap.cds.services.environment.CdsProperties.Model.Provider;
import com.sap.cds.services.request.FeatureTogglesInfo;
import com.sap.cds.services.request.RequestContext;
import com.sap.cds.services.request.UserInfo;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cloud.sdk.cloudplatform.connectivity.ApacheHttpClient5Accessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.MockedStatic;

class MtxSidecarClientImplTest {

  private static final String TEST_TENANT = "test-tenant";

  private Model model;
  private Provider provider;
  private UserInfo userInfo;
  private CdsRuntime cdsRuntime;
  private HttpClient httpClient;
  private HttpEntity httpEntity;
  private Destination destination;
  private CdsProperties cdsProperties;
  private CdsEnvironment cdsEnvironment;
  private RequestContext requestContext;
  private ClassicHttpResponse classicHttpResponse;

  private MtxSidecarClientImpl classUnderTest;

  @BeforeEach
  void setUp() {
    model = mock(Model.class);
    provider = mock(Provider.class);
    userInfo = mock(UserInfo.class);
    cdsRuntime = mock(CdsRuntime.class);
    httpClient = mock(HttpClient.class);
    httpEntity = mock(HttpEntity.class);
    destination = mock(Destination.class);
    cdsProperties = mock(CdsProperties.class);
    cdsEnvironment = mock(CdsEnvironment.class);
    requestContext = mock(RequestContext.class);
    classicHttpResponse = mock(ClassicHttpResponse.class);
    classUnderTest = new MtxSidecarClientImpl(cdsRuntime);

    doReturn(provider).when(model).getProvider();
    doReturn(model).when(cdsProperties).getModel();
    doReturn(cdsEnvironment).when(cdsRuntime).getEnvironment();
    lenient().doReturn(TEST_TENANT).when(userInfo).getTenant();
    doReturn(cdsProperties).when(cdsEnvironment).getCdsProperties();
    lenient().doReturn(userInfo).when(requestContext).getUserInfo();
    lenient()
        .doReturn(FeatureTogglesInfo.create(Map.of("test-toggle", true)))
        .when(requestContext)
        .getFeatureTogglesInfo();
  }

  @Test
  void whenGetOrdDocumentIsCalled_thenCorrectResultIsReturned() throws Throwable {
    doReturn(false).when(provider).isToggles();
    doReturn(httpEntity).when(classicHttpResponse).getEntity();
    doReturn(false).when(provider).isExtensibility();
    doReturn(200).when(classicHttpResponse).getCode();
    doReturn(new ByteArrayInputStream("{}".getBytes(UTF_8)))
        .when(httpEntity)
        .getContent();
    when(httpClient.execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class)))
        .thenAnswer(
            in -> in.getArgument(1, HttpClientResponseHandler.class).handleResponse(classicHttpResponse));

    withMockedDestinationAccessor(
        () -> withMockedApacheHttpClient5Accessor(() -> assertEquals("{}", classUnderTest.getOrdDocument())));

    verifyNoOtherMockInteractions(() -> {
      verify(provider).isToggles();
      verify(httpEntity).getContent();
      verify(provider).isExtensibility();
      verify(classicHttpResponse).getEntity();
      verify(model, times(2)).getProvider();
      verify(cdsProperties, times(2)).getModel();
      verify(cdsRuntime, times(2)).getEnvironment();
      verify(classicHttpResponse, times(2)).getCode();
      verify(cdsEnvironment, times(2)).getCdsProperties();
      verify(httpClient)
          .execute(
              argThat(matchesPostRequest(
                  "/-/cds/ord-provider-service/getOrdDocument", "{\"toggles\":[],\"tenant\":null}")),
              any(HttpClientResponseHandler.class));
    });
  }

  @Test
  void givenResponseNotSuccess_whenGetOrdDocumentIsCalled_thenExceptionIsThrown() throws Throwable {
    doReturn(false).when(provider).isToggles();
    doReturn(httpEntity).when(classicHttpResponse).getEntity();
    doReturn(false).when(provider).isExtensibility();
    doReturn(SERVER_ERROR.getHttpStatus()).when(classicHttpResponse).getCode();
    doReturn(SERVER_ERROR.getDescription()).when(classicHttpResponse).getReasonPhrase();
    when(httpClient.execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class)))
        .thenAnswer(
            in -> in.getArgument(1, HttpClientResponseHandler.class).handleResponse(classicHttpResponse));

    withMockedDestinationAccessor(() -> withMockedApacheHttpClient5Accessor(
        () -> assertThrows(ServiceException.class, () -> classUnderTest.getOrdDocument())));

    verifyNoOtherMockInteractions(() -> {
      verify(provider).isToggles();
      verify(provider).isExtensibility();
      verify(classicHttpResponse).getReasonPhrase();
      verify(model, times(2)).getProvider();
      verify(cdsProperties, times(2)).getModel();
      verify(cdsRuntime, times(2)).getEnvironment();
      verify(classicHttpResponse, times(3)).getCode();
      verify(cdsEnvironment, times(2)).getCdsProperties();
      verify(httpClient)
          .execute(
              argThat(matchesPostRequest(
                  "/-/cds/ord-provider-service/getOrdDocument", "{\"toggles\":[],\"tenant\":null}")),
              any(HttpClientResponseHandler.class));
    });
  }

  @Test
  void giveIOException_whenGetOrdDocumentIsCalled_thenServiceExceptionIsThrown() throws Throwable {
    doReturn(false).when(provider).isToggles();
    doReturn(false).when(provider).isExtensibility();
    doThrow(new IOException())
        .when(httpClient)
        .execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class));

    withMockedDestinationAccessor(() -> withMockedApacheHttpClient5Accessor(
        () -> assertThrows(ServiceException.class, () -> classUnderTest.getOrdDocument())));

    verifyNoOtherMockInteractions(() -> {
      verify(provider).isToggles();
      verify(provider).isExtensibility();
      verify(model, times(2)).getProvider();
      verify(cdsProperties, times(2)).getModel();
      verify(cdsRuntime, times(2)).getEnvironment();
      verify(cdsEnvironment, times(2)).getCdsProperties();
      verify(httpClient)
          .execute(
              argThat(matchesPostRequest(
                  "/-/cds/ord-provider-service/getOrdDocument", "{\"toggles\":[],\"tenant\":null}")),
              any(HttpClientResponseHandler.class));
    });
  }

  @Test
  void givenThatExtensibilityIsEnabled_whenGetOrdDocumentIsCalled_thenCorrectResultIsReturned() throws Throwable {
    doReturn(false).when(provider).isToggles();
    doReturn(httpEntity).when(classicHttpResponse).getEntity();
    doReturn(true).when(provider).isExtensibility();
    doReturn(200).when(classicHttpResponse).getCode();
    doReturn(new ByteArrayInputStream("{}".getBytes(UTF_8)))
        .when(httpEntity)
        .getContent();
    when(httpClient.execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class)))
        .thenAnswer(
            in -> in.getArgument(1, HttpClientResponseHandler.class).handleResponse(classicHttpResponse));

    withMockedRequestContext(() -> withMockedDestinationAccessor(
        () -> withMockedApacheHttpClient5Accessor(() -> assertEquals("{}", classUnderTest.getOrdDocument()))));

    verifyNoOtherMockInteractions(() -> {
      verify(provider).isToggles();
      verify(userInfo).getTenant();
      verify(httpEntity).getContent();
      verify(provider).isExtensibility();
      verify(classicHttpResponse).getEntity();
      verify(this.requestContext).getUserInfo();
      verify(model, times(2)).getProvider();
      verify(cdsProperties, times(2)).getModel();
      verify(cdsRuntime, times(2)).getEnvironment();
      verify(classicHttpResponse, times(2)).getCode();
      verify(cdsEnvironment, times(2)).getCdsProperties();
      verify(httpClient)
          .execute(
              argThat(matchesPostRequest(
                  "/-/cds/ord-provider-service/getOrdDocument",
                  "{\"toggles\":[],\"tenant\":\"test-tenant\"}")),
              any(HttpClientResponseHandler.class));
    });
  }

  @Test
  void givenThatTogglesAreEnabled_whenGetOrdDocumentIsCalled_thenCorrectResultIsReturned() throws Throwable {
    doReturn(true).when(provider).isToggles();
    doReturn(httpEntity).when(classicHttpResponse).getEntity();
    doReturn(false).when(provider).isExtensibility();
    doReturn(200).when(classicHttpResponse).getCode();
    doReturn(new ByteArrayInputStream("{}".getBytes(UTF_8)))
        .when(httpEntity)
        .getContent();
    when(httpClient.execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class)))
        .thenAnswer(
            in -> in.getArgument(1, HttpClientResponseHandler.class).handleResponse(classicHttpResponse));

    withMockedRequestContext(() -> withMockedDestinationAccessor(
        () -> withMockedApacheHttpClient5Accessor(() -> assertEquals("{}", classUnderTest.getOrdDocument()))));

    verifyNoOtherMockInteractions(() -> {
      verify(provider).isToggles();
      verify(httpEntity).getContent();
      verify(provider).isExtensibility();
      verify(classicHttpResponse).getEntity();
      verify(this.requestContext).getFeatureTogglesInfo();
      verify(model, times(2)).getProvider();
      verify(cdsProperties, times(2)).getModel();
      verify(cdsRuntime, times(2)).getEnvironment();
      verify(classicHttpResponse, times(2)).getCode();
      verify(cdsEnvironment, times(2)).getCdsProperties();
      verify(httpClient)
          .execute(
              argThat(matchesPostRequest(
                  "/-/cds/ord-provider-service/getOrdDocument",
                  "{\"toggles\":[\"test-toggle\"],\"tenant\":null}")),
              any(HttpClientResponseHandler.class));
    });
  }

  @Test
  void givenThatTogglesAreEnabled_and_extensibilityIsEnabled_whenGetOrdDocumentIsCalled_thenCorrectResultIsReturned()
      throws Throwable {
    doReturn(true).when(provider).isToggles();
    doReturn(httpEntity).when(classicHttpResponse).getEntity();
    doReturn(true).when(provider).isExtensibility();
    doReturn(200).when(classicHttpResponse).getCode();
    doReturn(new ByteArrayInputStream("{}".getBytes(UTF_8)))
        .when(httpEntity)
        .getContent();
    when(httpClient.execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class)))
        .thenAnswer(
            in -> in.getArgument(1, HttpClientResponseHandler.class).handleResponse(classicHttpResponse));

    withMockedRequestContext(() -> withMockedDestinationAccessor(
        () -> withMockedApacheHttpClient5Accessor(() -> assertEquals("{}", classUnderTest.getOrdDocument()))));

    verifyNoOtherMockInteractions(() -> {
      verify(userInfo).getTenant();
      verify(provider).isToggles();
      verify(httpEntity).getContent();
      verify(provider).isExtensibility();
      verify(classicHttpResponse).getEntity();
      verify(this.requestContext).getUserInfo();
      verify(this.requestContext).getFeatureTogglesInfo();
      verify(model, times(2)).getProvider();
      verify(cdsProperties, times(2)).getModel();
      verify(cdsRuntime, times(2)).getEnvironment();
      verify(classicHttpResponse, times(2)).getCode();
      verify(cdsEnvironment, times(2)).getCdsProperties();
      verify(httpClient)
          .execute(
              argThat(matchesPostRequest(
                  "/-/cds/ord-provider-service/getOrdDocument",
                  "{\"toggles\":[\"test-toggle\"],\"tenant\":\"test-tenant\"}")),
              any(HttpClientResponseHandler.class));
    });
  }

  @Test
  void whenGetOrdResourceDefinitionIsCalled_thenCorrectResultIsReturned() throws Throwable {
    doReturn(false).when(provider).isToggles();
    doReturn(httpEntity).when(classicHttpResponse).getEntity();
    doReturn(false).when(provider).isExtensibility();
    doReturn(200).when(classicHttpResponse).getCode();
    doReturn(new ByteArrayInputStream("{}".getBytes(UTF_8)))
        .when(httpEntity)
        .getContent();
    when(httpClient.execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class)))
        .thenAnswer(
            in -> in.getArgument(1, HttpClientResponseHandler.class).handleResponse(classicHttpResponse));

    withMockedDestinationAccessor(() -> withMockedApacheHttpClient5Accessor(() ->
        assertEquals("{}", classUnderTest.getOrdResourceDefinition("dummy:ord:definition/Dummy.oas3.json"))));

    verifyNoOtherMockInteractions(() -> {
      verify(provider).isToggles();
      verify(httpEntity).getContent();
      verify(provider).isExtensibility();
      verify(classicHttpResponse).getEntity();
      verify(model, times(2)).getProvider();
      verify(cdsProperties, times(2)).getModel();
      verify(cdsRuntime, times(2)).getEnvironment();
      verify(classicHttpResponse, times(2)).getCode();
      verify(cdsEnvironment, times(2)).getCdsProperties();
      verify(httpClient)
          .execute(
              argThat(
                  matchesPostRequest(
                      "/-/cds/ord-provider-service/getOrdResourceDefinition",
                      "{\"resource\":\"dummy:ord:definition/Dummy.oas3.json\",\"toggles\":[],\"tenant\":null}")),
              any(HttpClientResponseHandler.class));
    });
  }

  @Test
  void givenResponseNotSuccess_whenGetOrdResourceDefinitionIsCalled_thenExceptionIsThrown() throws Throwable {
    doReturn(false).when(provider).isToggles();
    doReturn(httpEntity).when(classicHttpResponse).getEntity();
    doReturn(false).when(provider).isExtensibility();
    doReturn(SERVER_ERROR.getHttpStatus()).when(classicHttpResponse).getCode();
    doReturn(SERVER_ERROR.getDescription()).when(classicHttpResponse).getReasonPhrase();
    when(httpClient.execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class)))
        .thenAnswer(
            in -> in.getArgument(1, HttpClientResponseHandler.class).handleResponse(classicHttpResponse));

    withMockedDestinationAccessor(() -> withMockedApacheHttpClient5Accessor(() -> assertThrows(
        ServiceException.class,
        () -> classUnderTest.getOrdResourceDefinition("dummy:ord:definition/Dummy.oas3.json"))));

    verifyNoOtherMockInteractions(() -> {
      verify(provider).isToggles();
      verify(provider).isExtensibility();
      verify(classicHttpResponse).getReasonPhrase();
      verify(model, times(2)).getProvider();
      verify(cdsProperties, times(2)).getModel();
      verify(cdsRuntime, times(2)).getEnvironment();
      verify(classicHttpResponse, times(3)).getCode();
      verify(cdsEnvironment, times(2)).getCdsProperties();
      verify(httpClient)
          .execute(
              argThat(
                  matchesPostRequest(
                      "/-/cds/ord-provider-service/getOrdResourceDefinition",
                      "{\"resource\":\"dummy:ord:definition/Dummy.oas3.json\",\"toggles\":[],\"tenant\":null}")),
              any(HttpClientResponseHandler.class));
    });
  }

  @Test
  void giveIOException_whenGetOrdResourceDefinitionIsCalled_thenServiceExceptionIsThrown() throws Throwable {
    doReturn(false).when(provider).isToggles();
    doReturn(false).when(provider).isExtensibility();
    doThrow(new IOException())
        .when(httpClient)
        .execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class));

    withMockedDestinationAccessor(() -> withMockedApacheHttpClient5Accessor(() -> assertThrows(
        ServiceException.class,
        () -> classUnderTest.getOrdResourceDefinition("dummy:ord:definition/Dummy.oas3.json"))));

    verifyNoOtherMockInteractions(() -> {
      verify(provider).isToggles();
      verify(provider).isExtensibility();
      verify(model, times(2)).getProvider();
      verify(cdsProperties, times(2)).getModel();
      verify(cdsRuntime, times(2)).getEnvironment();
      verify(cdsEnvironment, times(2)).getCdsProperties();
      verify(httpClient)
          .execute(
              argThat(
                  matchesPostRequest(
                      "/-/cds/ord-provider-service/getOrdResourceDefinition",
                      "{\"resource\":\"dummy:ord:definition/Dummy.oas3.json\",\"toggles\":[],\"tenant\":null}")),
              any(HttpClientResponseHandler.class));
    });
  }

  @Test
  void givenThatExtensibilityIsEnabled_whenGetOrdResourceDefinitionIsCalled_thenCorrectResultIsReturned()
      throws Throwable {
    doReturn(false).when(provider).isToggles();
    doReturn(httpEntity).when(classicHttpResponse).getEntity();
    doReturn(true).when(provider).isExtensibility();
    doReturn(200).when(classicHttpResponse).getCode();
    doReturn(new ByteArrayInputStream("{}".getBytes(UTF_8)))
        .when(httpEntity)
        .getContent();
    when(httpClient.execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class)))
        .thenAnswer(
            in -> in.getArgument(1, HttpClientResponseHandler.class).handleResponse(classicHttpResponse));

    withMockedRequestContext(() -> withMockedDestinationAccessor(() -> withMockedApacheHttpClient5Accessor(() ->
        assertEquals("{}", classUnderTest.getOrdResourceDefinition("dummy:ord:definition/Dummy.oas3.json")))));

    verifyNoOtherMockInteractions(() -> {
      verify(provider).isToggles();
      verify(userInfo).getTenant();
      verify(httpEntity).getContent();
      verify(provider).isExtensibility();
      verify(classicHttpResponse).getEntity();
      verify(this.requestContext).getUserInfo();
      verify(model, times(2)).getProvider();
      verify(cdsProperties, times(2)).getModel();
      verify(cdsRuntime, times(2)).getEnvironment();
      verify(classicHttpResponse, times(2)).getCode();
      verify(cdsEnvironment, times(2)).getCdsProperties();
      verify(httpClient)
          .execute(
              argThat(
                  matchesPostRequest(
                      "/-/cds/ord-provider-service/getOrdResourceDefinition",
                      "{\"resource\":\"dummy:ord:definition/Dummy.oas3.json\",\"toggles\":[],\"tenant\":\"test-tenant\"}")),
              any(HttpClientResponseHandler.class));
    });
  }

  @Test
  void givenThatTogglesAreEnabled_whenGetOrdResourceDefinitionIsCalled_thenCorrectResultIsReturned()
      throws Throwable {
    doReturn(true).when(provider).isToggles();
    doReturn(httpEntity).when(classicHttpResponse).getEntity();
    doReturn(false).when(provider).isExtensibility();
    doReturn(200).when(classicHttpResponse).getCode();
    doReturn(new ByteArrayInputStream("{}".getBytes(UTF_8)))
        .when(httpEntity)
        .getContent();
    when(httpClient.execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class)))
        .thenAnswer(
            in -> in.getArgument(1, HttpClientResponseHandler.class).handleResponse(classicHttpResponse));

    withMockedRequestContext(() -> withMockedDestinationAccessor(() -> withMockedApacheHttpClient5Accessor(() ->
        assertEquals("{}", classUnderTest.getOrdResourceDefinition("dummy:ord:definition/Dummy.oas3.json")))));

    verifyNoOtherMockInteractions(() -> {
      verify(provider).isToggles();
      verify(httpEntity).getContent();
      verify(provider).isExtensibility();
      verify(classicHttpResponse).getEntity();
      verify(this.requestContext).getFeatureTogglesInfo();
      verify(model, times(2)).getProvider();
      verify(cdsProperties, times(2)).getModel();
      verify(cdsRuntime, times(2)).getEnvironment();
      verify(classicHttpResponse, times(2)).getCode();
      verify(cdsEnvironment, times(2)).getCdsProperties();
      verify(httpClient)
          .execute(
              argThat(
                  matchesPostRequest(
                      "/-/cds/ord-provider-service/getOrdResourceDefinition",
                      "{\"resource\":\"dummy:ord:definition/Dummy.oas3.json\",\"toggles\":[\"test-toggle\"],\"tenant\":null}")),
              any(HttpClientResponseHandler.class));
    });
  }

  @Test
  void
      givenThatTogglesAreEnabled_and_extensibilityIsEnabled_whenGetOrdResourceDefinitionIsCalled_thenCorrectResultIsReturned()
          throws Throwable {
    doReturn(true).when(provider).isToggles();
    doReturn(httpEntity).when(classicHttpResponse).getEntity();
    doReturn(true).when(provider).isExtensibility();
    doReturn(200).when(classicHttpResponse).getCode();
    doReturn(new ByteArrayInputStream("{}".getBytes(UTF_8)))
        .when(httpEntity)
        .getContent();
    when(httpClient.execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class)))
        .thenAnswer(
            in -> in.getArgument(1, HttpClientResponseHandler.class).handleResponse(classicHttpResponse));

    withMockedRequestContext(() -> withMockedDestinationAccessor(() -> withMockedApacheHttpClient5Accessor(() ->
        assertEquals("{}", classUnderTest.getOrdResourceDefinition("dummy:ord:definition/Dummy.oas3.json")))));

    verifyNoOtherMockInteractions(() -> {
      verify(provider).isToggles();
      verify(userInfo).getTenant();
      verify(httpEntity).getContent();
      verify(provider).isExtensibility();
      verify(classicHttpResponse).getEntity();
      verify(this.requestContext).getUserInfo();
      verify(this.requestContext).getFeatureTogglesInfo();
      verify(model, times(2)).getProvider();
      verify(cdsProperties, times(2)).getModel();
      verify(cdsRuntime, times(2)).getEnvironment();
      verify(classicHttpResponse, times(2)).getCode();
      verify(cdsEnvironment, times(2)).getCdsProperties();
      verify(httpClient)
          .execute(
              argThat(
                  matchesPostRequest(
                      "/-/cds/ord-provider-service/getOrdResourceDefinition",
                      "{\"resource\":\"dummy:ord:definition/Dummy.oas3.json\",\"toggles\":[\"test-toggle\"],\"tenant\":\"test-tenant\"}")),
              any(HttpClientResponseHandler.class));
    });
  }

  private void verifyNoOtherMockInteractions(ThrowingRunnable runnable) throws Throwable {
    runnable.run();

    verifyNoMoreInteractions(
        model,
        provider,
        userInfo,
        cdsRuntime,
        httpClient,
        httpEntity,
        destination,
        cdsProperties,
        cdsEnvironment,
        this.requestContext,
        classicHttpResponse);
  }

  private void withMockedRequestContext(ThrowingRunnable runnable) throws Throwable {
    try (MockedStatic<RequestContext> mock = mockStatic(RequestContext.class)) {

      mock.when(() -> RequestContext.getCurrent(cdsRuntime)).thenReturn(this.requestContext);

      runnable.run();
    }
  }

  private void withMockedDestinationAccessor(ThrowingRunnable runnable) throws Throwable {
    try (MockedStatic<DestinationAccessor> destinationAccessor = mockStatic(DestinationAccessor.class)) {

      destinationAccessor
          .when(() -> DestinationAccessor.getDestination("com.sap.cds.mtxSidecar"))
          .thenReturn(destination);

      runnable.run();
    }
  }

  private void withMockedApacheHttpClient5Accessor(ThrowingRunnable runnable) throws Throwable {
    try (MockedStatic<ApacheHttpClient5Accessor> apacheHttpClient5Accessor =
        mockStatic(ApacheHttpClient5Accessor.class)) {

      apacheHttpClient5Accessor
          .when(() -> ApacheHttpClient5Accessor.getHttpClient(destination))
          .thenReturn(httpClient);

      runnable.run();
    }
  }

  private static ArgumentMatcher<ClassicHttpRequest> matchesPostRequest(String path, String body) {
    return request -> {
      try {
        return Objects.equals(path, request.getPath())
            && Objects.equals("POST", request.getMethod())
            && Objects.equals(
                body, IOUtils.toString(request.getEntity().getContent(), UTF_8));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  private interface ThrowingRunnable {
    void run() throws Throwable;
  }
}
