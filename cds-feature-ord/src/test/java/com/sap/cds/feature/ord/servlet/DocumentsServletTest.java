/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.servlet;

import static com.sap.cds.feature.ord.common.Constants.HEADER_LOCAL_TENANT_ID;
import static com.sap.cds.feature.ord.common.Utils.CdsRuntimeProperties.getAuthenticationProperties;
import static com.sap.cds.services.ErrorStatuses.UNAUTHORIZED;
import static com.sap.cds.services.runtime.ExtendedServiceLoader.loadAll;
import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.apache.http.entity.ContentType.APPLICATION_XML;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.sap.cds.feature.ord.provider.AuthenticationManagerProvider;
import com.sap.cds.feature.ord.resolver.OrdResolver;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import com.sap.cds.services.runtime.ExtendedServiceLoader;
import com.sap.cds.services.utils.ErrorStatusException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class DocumentsServletTest {

  private CdsRuntime cdsRuntime;
  private OrdResolver ordResolver;
  private PrintWriter printWriter;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private ServletOutputStream servletOutputStream;
  private AuthenticationManagerProvider authenticationManagerProvider;

  private DocumentsServlet classUnderTest;

  @BeforeEach
  void setUp() {
    printWriter = mock(PrintWriter.class);
    ordResolver = mock(OrdResolver.class);
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    servletOutputStream = mock(ServletOutputStream.class);
    authenticationManagerProvider = mock(AuthenticationManagerProvider.class);
    cdsRuntime = CdsRuntimeConfigurer.create()
        .provider(authenticationManagerProvider)
        .complete();

    classUnderTest = new DocumentsServlet(cdsRuntime);
  }

  @Test
  void givenRequestForUnsupportedResourceDefinitionExtension_whenDoGetIsCalled_thenCorrectResponseIsReturned() {
    getAuthenticationProperties(cdsRuntime).setAuthenticateMetadataEndpoints(false);
    doReturn("/dummy:service:v1/Dummy.unsupported").when(request).getPathInfo();
    doReturn("/ord/v1/dummy:service:v1/Dummy.unsupported").when(request).getRequestURI();

    classUnderTest.doGet(request, response);

    verify(request).getRequestURI();
    verify(request).getHeader(HEADER_LOCAL_TENANT_ID);
    verify(authenticationManagerProvider).setPrevious(null);
    verify(request, times(2)).getPathInfo();
    verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    verify(authenticationManagerProvider).checkAuthorization("/ord/v1/dummy:service:v1/Dummy.unsupported");
    verifyNoMoreInteractions(
        request, response, printWriter, ordResolver, servletOutputStream, authenticationManagerProvider);
  }

  @Test
  void givenRequestForDynamicPerspectiveForResourceDefinition_whenDoGetIsCalled_thenCorrectResponseIsReturned()
      throws Exception {
    try (MockedStatic<ExtendedServiceLoader> extendedServiceLoaderMockedStatic =
        mockStatic(ExtendedServiceLoader.class)) {
      doReturn(servletOutputStream).when(response).getOutputStream();
      getAuthenticationProperties(cdsRuntime).setAuthenticateMetadataEndpoints(false);
      doReturn("/dummy:service:v1/Dummy.graphql").when(request).getPathInfo();
      doReturn("system-instance").when(request).getParameter("perspective");
      doReturn("/ord/v1/dummy:service:v1/Dummy.graphql").when(request).getRequestURI();
      doReturn(true).when(ordResolver).isApplicable("dummy:service:v1/Dummy.graphql", "system-instance");
      extendedServiceLoaderMockedStatic
          .when(() -> loadAll(OrdResolver.class, cdsRuntime))
          .thenReturn(List.of(ordResolver).iterator());
      doReturn(new ByteArrayInputStream("test-document".getBytes(UTF_8)))
          .when(ordResolver)
          .resolve("dummy:service:v1/Dummy.graphql");

      classUnderTest.doGet(request, response);

      verify(request).getRequestURI();
      verify(response).getOutputStream();
      verify(request).getHeader(HEADER_LOCAL_TENANT_ID);
      verify(request).getParameter("perspective");
      verify(response).setStatus(HttpServletResponse.SC_OK);
      verify(authenticationManagerProvider).setPrevious(null);
      verify(response).setContentType(TEXT_PLAIN.getMimeType());
      verify(request, times(2)).getPathInfo();
      verify(ordResolver).resolve("dummy:service:v1/Dummy.graphql");
      verify(response).setCharacterEncoding(TEXT_PLAIN.getCharset().toString());
      verify(authenticationManagerProvider).checkAuthorization("/ord/v1/dummy:service:v1/Dummy.graphql");
      verify(ordResolver).isApplicable("dummy:service:v1/Dummy.graphql", "system-instance");
      verify(servletOutputStream).write("test-document".getBytes(), 0, "test-document".length());
      verifyNoMoreInteractions(
          request, response, printWriter, ordResolver, servletOutputStream, authenticationManagerProvider);
    }
  }

  @Test
  void givenRequestForGraphqlResourceDefinition_whenDoGetIsCalled_thenCorrectResponseIsReturned() throws Exception {
    try (MockedStatic<ExtendedServiceLoader> extendedServiceLoaderMockedStatic =
        mockStatic(ExtendedServiceLoader.class)) {
      doReturn(servletOutputStream).when(response).getOutputStream();
      getAuthenticationProperties(cdsRuntime).setAuthenticateMetadataEndpoints(false);
      doReturn("/dummy:service:v1/Dummy.graphql").when(request).getPathInfo();
      doReturn("/ord/v1/dummy:service:v1/Dummy.graphql").when(request).getRequestURI();
      doReturn(true).when(ordResolver).isApplicable("dummy:service:v1/Dummy.graphql", "system-version");
      extendedServiceLoaderMockedStatic
          .when(() -> loadAll(OrdResolver.class, cdsRuntime))
          .thenReturn(List.of(ordResolver).iterator());
      doReturn(new ByteArrayInputStream("test-document".getBytes(UTF_8)))
          .when(ordResolver)
          .resolve("dummy:service:v1/Dummy.graphql");

      classUnderTest.doGet(request, response);

      verify(request).getRequestURI();
      verify(response).getOutputStream();
      verify(request).getParameter("perspective");
      verify(request).getHeader("Local-Tenant-Id");
      verify(response).setStatus(HttpServletResponse.SC_OK);
      verify(authenticationManagerProvider).setPrevious(null);
      verify(response).setContentType(TEXT_PLAIN.getMimeType());
      verify(request, times(2)).getPathInfo();
      verify(ordResolver).resolve("dummy:service:v1/Dummy.graphql");
      verify(response).setCharacterEncoding(TEXT_PLAIN.getCharset().toString());
      verify(servletOutputStream).write("test-document".getBytes(), 0, "test-document".length());
      verify(authenticationManagerProvider).checkAuthorization("/ord/v1/dummy:service:v1/Dummy.graphql");
      verify(ordResolver).isApplicable("dummy:service:v1/Dummy.graphql", "system-version");
      verifyNoMoreInteractions(
          request, response, printWriter, ordResolver, servletOutputStream, authenticationManagerProvider);
    }
  }

  @Test
  void givenRequestForEdmxResourceDefinition_whenDoGetIsCalled_thenCorrectResponseIsReturned() throws Exception {
    try (MockedStatic<ExtendedServiceLoader> extendedServiceLoaderMockedStatic =
        mockStatic(ExtendedServiceLoader.class)) {
      doReturn(servletOutputStream).when(response).getOutputStream();
      getAuthenticationProperties(cdsRuntime).setAuthenticateMetadataEndpoints(false);
      doReturn("/dummy:service:v1/Dummy.edmx").when(request).getPathInfo();
      doReturn("/ord/v1/dummy:service:v1/Dummy.edmx").when(request).getRequestURI();
      doReturn(true).when(ordResolver).isApplicable("dummy:service:v1/Dummy.edmx", "system-version");
      extendedServiceLoaderMockedStatic
          .when(() -> loadAll(OrdResolver.class, cdsRuntime))
          .thenReturn(List.of(ordResolver).iterator());
      doReturn(new ByteArrayInputStream("test-document".getBytes(UTF_8)))
          .when(ordResolver)
          .resolve("dummy:service:v1/Dummy.edmx");

      classUnderTest.doGet(request, response);

      verify(request).getRequestURI();
      verify(response).getOutputStream();
      verify(request).getParameter("perspective");
      verify(request).getHeader("Local-Tenant-Id");
      verify(response).setStatus(HttpServletResponse.SC_OK);
      verify(authenticationManagerProvider).setPrevious(null);
      verify(response).setContentType(APPLICATION_XML.getMimeType());
      verify(request, times(2)).getPathInfo();
      verify(ordResolver).resolve("dummy:service:v1/Dummy.edmx");
      verify(response).setCharacterEncoding(APPLICATION_XML.getCharset().toString());
      verify(servletOutputStream).write("test-document".getBytes(), 0, "test-document".length());
      verify(authenticationManagerProvider).checkAuthorization("/ord/v1/dummy:service:v1/Dummy.edmx");
      verify(ordResolver).isApplicable("dummy:service:v1/Dummy.edmx", "system-version");
      verifyNoMoreInteractions(
          request, response, printWriter, ordResolver, servletOutputStream, authenticationManagerProvider);
    }
  }

  @Test
  void givenRequestForJsonResourceDefinition_whenDoGetIsCalled_thenCorrectResponseIsReturned() throws Exception {
    try (MockedStatic<ExtendedServiceLoader> extendedServiceLoaderMockedStatic =
        mockStatic(ExtendedServiceLoader.class)) {
      doReturn(servletOutputStream).when(response).getOutputStream();
      getAuthenticationProperties(cdsRuntime).setAuthenticateMetadataEndpoints(false);
      doReturn("/dummy:service:v1/Dummy.json").when(request).getPathInfo();
      doReturn("/ord/v1/dummy:service:v1/Dummy.json").when(request).getRequestURI();
      doReturn(true).when(ordResolver).isApplicable("dummy:service:v1/Dummy.json", "system-version");
      extendedServiceLoaderMockedStatic
          .when(() -> loadAll(OrdResolver.class, cdsRuntime))
          .thenReturn(List.of(ordResolver).iterator());
      doReturn(new ByteArrayInputStream("test-document".getBytes(UTF_8)))
          .when(ordResolver)
          .resolve("dummy:service:v1/Dummy.json");

      classUnderTest.doGet(request, response);

      verify(request).getRequestURI();
      verify(response).getOutputStream();
      verify(request).getParameter("perspective");
      verify(request).getHeader("Local-Tenant-Id");
      verify(response).setStatus(HttpServletResponse.SC_OK);
      verify(authenticationManagerProvider).setPrevious(null);
      verify(request, times(2)).getPathInfo();
      verify(response).setContentType(APPLICATION_JSON.getMimeType());
      verify(ordResolver).resolve("dummy:service:v1/Dummy.json");
      verify(response).setCharacterEncoding(APPLICATION_JSON.getCharset().toString());
      verify(servletOutputStream).write("test-document".getBytes(), 0, "test-document".length());
      verify(authenticationManagerProvider).checkAuthorization("/ord/v1/dummy:service:v1/Dummy.json");
      verify(ordResolver).isApplicable("dummy:service:v1/Dummy.json", "system-version");
      verifyNoMoreInteractions(
          request, response, printWriter, ordResolver, servletOutputStream, authenticationManagerProvider);
    }
  }

  @Test
  void givenMtlsAuth_whenDoGetIsCalled_thenCorrectResponseIsReturned() throws Exception {
    try (MockedStatic<ExtendedServiceLoader> extendedServiceLoaderMockedStatic =
        mockStatic(ExtendedServiceLoader.class)) {
      doReturn(servletOutputStream).when(response).getOutputStream();
      doReturn("/documents/ord-document").when(request).getPathInfo();
      doNothing().when(authenticationManagerProvider).checkAuthorization(anyString());
      getAuthenticationProperties(cdsRuntime).setAuthenticateMetadataEndpoints(true);
      doReturn("/ord/v1/documents/ord-document").when(request).getRequestURI();
      doReturn(true).when(ordResolver).isApplicable("documents/ord-document", "system-version");
      extendedServiceLoaderMockedStatic
          .when(() -> loadAll(OrdResolver.class, cdsRuntime))
          .thenReturn(List.of(ordResolver).iterator());
      doReturn(new ByteArrayInputStream("test-document".getBytes(UTF_8)))
          .when(ordResolver)
          .resolve("documents/ord-document");

      classUnderTest.doGet(request, response);

      verify(request).getRequestURI();
      verify(response).getOutputStream();
      verify(request).getParameter("perspective");
      verify(request).getHeader("Local-Tenant-Id");
      verify(response).setStatus(HttpServletResponse.SC_OK);
      verify(authenticationManagerProvider).setPrevious(null);
      verify(ordResolver).resolve("documents/ord-document");
      verify(request, times(2)).getPathInfo();
      verify(response).setContentType(APPLICATION_JSON.getMimeType());
      verify(response).setCharacterEncoding(APPLICATION_JSON.getCharset().toString());
      verify(authenticationManagerProvider).checkAuthorization("/ord/v1/documents/ord-document");
      verify(servletOutputStream).write("test-document".getBytes(), 0, "test-document".length());
      verify(ordResolver).isApplicable("documents/ord-document", "system-version");
      verifyNoMoreInteractions(
          request, response, printWriter, ordResolver, servletOutputStream, authenticationManagerProvider);
    }
  }

  @Test
  void givenMtlsAuth_and_authorizationFails_whenDoGetIsCalled_thenCorrectResponseIsReturned() throws Exception {
    doReturn(printWriter).when(response).getWriter();
    doReturn("/").when(request).getPathInfo();
    doReturn("/ord/v1").when(request).getRequestURI();
    getAuthenticationProperties(cdsRuntime).setAuthenticateMetadataEndpoints(true);
    doThrow(new ErrorStatusException(UNAUTHORIZED))
        .when(authenticationManagerProvider)
        .checkAuthorization(anyString());

    classUnderTest.doGet(request, response);

    verify(printWriter).close();
    verify(response).getWriter();
    verify(request).getRequestURI();
    verify(printWriter).write("No authentication");
    verify(authenticationManagerProvider).setPrevious(null);
    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(authenticationManagerProvider).checkAuthorization("/ord/v1");
    verifyNoMoreInteractions(
        request, response, printWriter, ordResolver, servletOutputStream, authenticationManagerProvider);
  }

  @Test
  void givenFileIsNotFound_whenDoGetIsCalled_thenCorrectResponseIsReturned() throws IOException {
    try (MockedStatic<ExtendedServiceLoader> extendedServiceLoaderMockedStatic =
        mockStatic(ExtendedServiceLoader.class)) {
      doReturn(printWriter).when(response).getWriter();
      doReturn("/documents/ord-document").when(request).getPathInfo();
      doNothing().when(authenticationManagerProvider).checkAuthorization(anyString());
      getAuthenticationProperties(cdsRuntime).setAuthenticateMetadataEndpoints(true);
      doReturn("/ord/v1/documents/ord-document").when(request).getRequestURI();
      doReturn(false).when(ordResolver).isApplicable("documents/ord-document", "system-version");
      extendedServiceLoaderMockedStatic
          .when(() -> loadAll(OrdResolver.class, cdsRuntime))
          .thenReturn(List.of(ordResolver).iterator());

      classUnderTest.doGet(request, response);

      verify(printWriter).close();
      verify(response).getWriter();
      verify(request).getRequestURI();
      verify(response).setStatus(SC_NOT_FOUND);
      verify(printWriter).write("Not Found");
      verify(request).getParameter("perspective");
      verify(request).getHeader("Local-Tenant-Id");
      verify(authenticationManagerProvider).setPrevious(null);
      verify(request, times(2)).getPathInfo();
      verify(ordResolver).isApplicable("documents/ord-document", "system-version");
      verify(authenticationManagerProvider).checkAuthorization("/ord/v1/documents/ord-document");
      verifyNoMoreInteractions(
          request, response, printWriter, ordResolver, servletOutputStream, authenticationManagerProvider);
    }
  }
}
