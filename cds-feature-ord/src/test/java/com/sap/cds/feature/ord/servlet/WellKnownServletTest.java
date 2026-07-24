/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.servlet;

import static com.sap.cds.feature.ord.common.Constants.HEADER_LOCAL_TENANT_ID;
import static com.sap.cds.feature.ord.common.Utils.CdsRuntimeProperties.getAuthenticationProperties;
import static com.sap.cds.feature.ord.common.Utils.CdsRuntimeProperties.getOrdProperties;
import static com.sap.cds.services.ErrorStatuses.UNAUTHORIZED;
import static com.sap.cds.services.utils.cert.UclAuthUtils.AccessStrategy.MTLS;
import static com.sap.cds.services.utils.cert.UclAuthUtils.AccessStrategy.OPEN;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.feature.ord.provider.AuthenticationManagerProvider;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import com.sap.cds.services.utils.ErrorStatusException;
import com.sap.cds.services.utils.model.DynamicModelUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

class WellKnownServletTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private CdsRuntime cdsRuntime;
  private PrintWriter printWriter;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private DynamicModelUtils dynamicModelUtils;
  private AuthenticationManagerProvider authenticationManagerProvider;

  private WellKnownServlet classUnderTest;

  @BeforeEach
  void setUp() {
    printWriter = mock(PrintWriter.class);
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    dynamicModelUtils = mock(DynamicModelUtils.class);
    cdsRuntime = CdsRuntimeConfigurer.create().complete();
    authenticationManagerProvider = mock(AuthenticationManagerProvider.class);
    cdsRuntime = CdsRuntimeConfigurer.create()
        .provider(authenticationManagerProvider)
        .complete();

    classUnderTest = new WellKnownServlet(cdsRuntime, dynamicModelUtils);
  }

  @Test
  void givenWrongPath_whenDoGetIsCalled_thenCorrectResponseIsReturned() throws Exception {
    doReturn(printWriter).when(response).getWriter();
    doReturn("/broken-path").when(request).getPathInfo();
    getAuthenticationProperties(cdsRuntime).setAuthenticateMetadataEndpoints(false);
    doReturn("/.well-known/open-resource-discovery/broken-path")
        .when(request)
        .getRequestURI();

    classUnderTest.doGet(request, response);

    verify(printWriter).close();
    verify(response).getWriter();
    verify(request).getRequestURI();
    verify(printWriter).write("Not found");
    verify(authenticationManagerProvider).setPrevious(null);
    verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    verify(request, times(2)).getPathInfo();
    verify(authenticationManagerProvider).checkAuthorization("/.well-known/open-resource-discovery/broken-path");
    verifyNoMoreInteractions(request, response, printWriter, dynamicModelUtils, authenticationManagerProvider);
  }

  @Test
  void givenMtlsAuth_and_staticModelIsToBeUsed_whenDoGetIsCalled_thenCorrectResponseIsReturned() throws Exception {
    doReturn(printWriter).when(response).getWriter();
    doReturn("/").when(request).getPathInfo();
    doReturn(true).when(dynamicModelUtils).useStaticModel();
    getAuthenticationProperties(cdsRuntime).setAuthenticateMetadataEndpoints(true);
    doReturn("/.well-known/open-resource-discovery").when(request).getRequestURI();
    doReturn(List.of(MTLS.getValue())).when(authenticationManagerProvider).getAccessStrategies();

    classUnderTest.doGet(request, response);

    verify(printWriter).close();
    verify(response).getWriter();
    verify(request).getRequestURI();
    verify(dynamicModelUtils).useStaticModel();
    verify(request).getHeader(HEADER_LOCAL_TENANT_ID);
    verify(response).setStatus(HttpServletResponse.SC_OK);
    verify(authenticationManagerProvider).setPrevious(null);
    verify(authenticationManagerProvider).getAccessStrategies();
    verify(request, times(2)).getPathInfo();
    verify(response).setContentType(APPLICATION_JSON.getMimeType());
    verify(response).setCharacterEncoding(APPLICATION_JSON.getCharset().toString());
    verify(authenticationManagerProvider).checkAuthorization("/.well-known/open-resource-discovery");
    verify(printWriter)
        .write(
            argThat(
                matchesJson(
                    "{\"openResourceDiscoveryV1\":{\"documents\":[{\"accessStrategies\":[{\"type\":\"sap:cmp-mtls:v1\"}],\"perspective\":\"system-version\",\"url\":\"/ord/v1/documents/ord-document\"}]}}")));
    verifyNoMoreInteractions(request, response, printWriter, dynamicModelUtils, authenticationManagerProvider);
  }

  @Test
  void givenMtlsAuth_and_dynamicModelIsToBeUsed_whenDoGetIsCalled_thenCorrectResponseIsReturned() throws Exception {
    doReturn(printWriter).when(response).getWriter();
    doReturn("/").when(request).getPathInfo();
    doReturn(false).when(dynamicModelUtils).useStaticModel();
    getAuthenticationProperties(cdsRuntime).setAuthenticateMetadataEndpoints(true);
    doReturn("/.well-known/open-resource-discovery").when(request).getRequestURI();
    doReturn(List.of(MTLS.getValue())).when(authenticationManagerProvider).getAccessStrategies();

    classUnderTest.doGet(request, response);

    verify(printWriter).close();
    verify(response).getWriter();
    verify(request).getRequestURI();
    verify(dynamicModelUtils).useStaticModel();
    verify(request).getHeader(HEADER_LOCAL_TENANT_ID);
    verify(response).setStatus(HttpServletResponse.SC_OK);
    verify(authenticationManagerProvider).setPrevious(null);
    verify(request, times(2)).getPathInfo();
    verify(response).setContentType(APPLICATION_JSON.getMimeType());
    verify(response).setCharacterEncoding(APPLICATION_JSON.getCharset().toString());
    verify(authenticationManagerProvider, times(2)).getAccessStrategies();
    verify(authenticationManagerProvider).checkAuthorization("/.well-known/open-resource-discovery");
    verify(printWriter)
        .write(
            argThat(
                matchesJson(
                    "{\"openResourceDiscoveryV1\":{\"documents\":[{\"accessStrategies\":[{\"type\":\"sap:cmp-mtls:v1\"}],\"perspective\":\"system-version\",\"url\":\"/ord/v1/documents/ord-document\"},{\"accessStrategies\":[{\"type\":\"sap:cmp-mtls:v1\"}],\"perspective\":\"system-instance\",\"url\":\"/ord/v1/documents/ord-document?perspective=system-instance\"}]}}")));
    verifyNoMoreInteractions(request, response, printWriter, dynamicModelUtils, authenticationManagerProvider);
  }

  @Test
  void givenMtlsAuth_and_authorizationFails_whenDoGetIsCalled_thenCorrectResponseIsReturned() throws Exception {
    doReturn(printWriter).when(response).getWriter();
    doReturn("/").when(request).getPathInfo();
    doReturn(true).when(dynamicModelUtils).useStaticModel();
    getAuthenticationProperties(cdsRuntime).setAuthenticateMetadataEndpoints(true);
    doReturn("/.well-known/open-resource-discovery").when(request).getRequestURI();
    doThrow(new ErrorStatusException(UNAUTHORIZED))
        .when(authenticationManagerProvider)
        .checkAuthorization(any());

    classUnderTest.doGet(request, response);

    verify(printWriter).close();
    verify(response).getWriter();
    verify(request).getRequestURI();
    verify(printWriter).write("No authentication");
    verify(authenticationManagerProvider).setPrevious(null);
    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(authenticationManagerProvider).checkAuthorization("/.well-known/open-resource-discovery");
    verifyNoMoreInteractions(request, response, printWriter, dynamicModelUtils, authenticationManagerProvider);
  }

  @Test
  void givenNoAuth_and_staticModelIsToBeUsed_whenDoGetIsCalled_thenCorrectResponseIsReturned() throws Exception {
    doReturn(printWriter).when(response).getWriter();
    doReturn("/").when(request).getPathInfo();
    doReturn(true).when(dynamicModelUtils).useStaticModel();
    getAuthenticationProperties(cdsRuntime).setAuthenticateMetadataEndpoints(false);
    doReturn("/.well-known/open-resource-discovery").when(request).getRequestURI();
    doReturn(List.of(OPEN.getValue())).when(authenticationManagerProvider).getAccessStrategies();

    classUnderTest.doGet(request, response);

    verify(printWriter).close();
    verify(response).getWriter();
    verify(request).getRequestURI();
    verify(dynamicModelUtils).useStaticModel();
    verify(request).getHeader(HEADER_LOCAL_TENANT_ID);
    verify(response).setStatus(HttpServletResponse.SC_OK);
    verify(authenticationManagerProvider).setPrevious(null);
    verify(authenticationManagerProvider).getAccessStrategies();
    verify(request, times(2)).getPathInfo();
    verify(response).setContentType(APPLICATION_JSON.getMimeType());
    verify(response).setCharacterEncoding(APPLICATION_JSON.getCharset().toString());
    verify(authenticationManagerProvider).checkAuthorization("/.well-known/open-resource-discovery");
    verify(printWriter)
        .write(
            argThat(
                matchesJson(
                    "{\"openResourceDiscoveryV1\":{\"documents\":[{\"accessStrategies\":[{\"type\":\"open\"}],\"perspective\":\"system-version\",\"url\":\"/ord/v1/documents/ord-document\"}]}}")));
    verifyNoMoreInteractions(request, response, printWriter, dynamicModelUtils, authenticationManagerProvider);
  }

  @Test
  void givenNoAuth_and_dynamicModelIsToBeUsed_whenDoGetIsCalled_thenCorrectResponseIsReturned() throws Exception {
    doReturn(printWriter).when(response).getWriter();
    doReturn("/").when(request).getPathInfo();
    doReturn(false).when(dynamicModelUtils).useStaticModel();
    getAuthenticationProperties(cdsRuntime).setAuthenticateMetadataEndpoints(false);
    doReturn("/.well-known/open-resource-discovery").when(request).getRequestURI();
    doReturn(List.of(OPEN.getValue())).when(authenticationManagerProvider).getAccessStrategies();

    classUnderTest.doGet(request, response);

    verify(printWriter).close();
    verify(response).getWriter();
    verify(request).getRequestURI();
    verify(dynamicModelUtils).useStaticModel();
    verify(request).getHeader(HEADER_LOCAL_TENANT_ID);
    verify(response).setStatus(HttpServletResponse.SC_OK);
    verify(authenticationManagerProvider).setPrevious(null);
    verify(request, times(2)).getPathInfo();
    verify(response).setContentType(APPLICATION_JSON.getMimeType());
    verify(response).setCharacterEncoding(APPLICATION_JSON.getCharset().toString());
    verify(authenticationManagerProvider, times(2)).getAccessStrategies();
    verify(authenticationManagerProvider).checkAuthorization("/.well-known/open-resource-discovery");
    verify(printWriter)
        .write(
            argThat(
                matchesJson(
                    "{\"openResourceDiscoveryV1\":{\"documents\":[{\"accessStrategies\":[{\"type\":\"open\"}],\"perspective\":\"system-version\",\"url\":\"/ord/v1/documents/ord-document\"},{\"accessStrategies\":[{\"type\":\"open\"}],\"perspective\":\"system-instance\",\"url\":\"/ord/v1/documents/ord-document?perspective=system-instance\"}]}}")));
    verifyNoMoreInteractions(request, response, printWriter, dynamicModelUtils, authenticationManagerProvider);
  }

  @Test
  void givenCustomApiPath_whenDoGetIsCalled_thenCorrectResponseIsReturned() throws Exception {
    doReturn(printWriter).when(response).getWriter();
    doReturn("/").when(request).getPathInfo();
    doReturn(true).when(dynamicModelUtils).useStaticModel();
    getAuthenticationProperties(cdsRuntime).setAuthenticateMetadataEndpoints(false);
    getOrdProperties(cdsRuntime).getDocumentsEndpoint().setPath("/custom-api/ord/v1");
    doReturn("/.well-known/open-resource-discovery").when(request).getRequestURI();
    doReturn(List.of(OPEN.getValue())).when(authenticationManagerProvider).getAccessStrategies();

    classUnderTest.doGet(request, response);

    verify(printWriter).close();
    verify(response).getWriter();
    verify(request).getRequestURI();
    verify(dynamicModelUtils).useStaticModel();
    verify(request).getHeader(HEADER_LOCAL_TENANT_ID);
    verify(response).setStatus(HttpServletResponse.SC_OK);
    verify(authenticationManagerProvider).setPrevious(null);
    verify(authenticationManagerProvider).getAccessStrategies();
    verify(request, times(2)).getPathInfo();
    verify(response).setContentType(APPLICATION_JSON.getMimeType());
    verify(response).setCharacterEncoding(APPLICATION_JSON.getCharset().toString());
    verify(authenticationManagerProvider).checkAuthorization("/.well-known/open-resource-discovery");
    verify(printWriter)
        .write(
            argThat(
                matchesJson(
                    "{\"openResourceDiscoveryV1\":{\"documents\":[{\"accessStrategies\":[{\"type\":\"open\"}],\"perspective\":\"system-version\",\"url\":\"/custom-api/ord/v1/documents/ord-document\"}]}}")));
    verifyNoMoreInteractions(request, response, printWriter, dynamicModelUtils, authenticationManagerProvider);
  }

  private static Map<String, Object> parse(String json) {
    try {
      return MAPPER.readValue(json, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private static ArgumentMatcher<String> matchesJson(String expected) {
    return found -> Objects.equals(parse(expected), parse(found));
  }
}
