/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.common;

import static com.sap.cds.services.ErrorStatuses.BAD_REQUEST;
import static com.sap.cds.services.ErrorStatuses.SERVER_ERROR;
import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.sap.cds.services.ServiceException;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import com.sap.cds.services.utils.ErrorStatusException;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UtilsTest {

  private CdsRuntime cdsRuntime;
  private ClassicHttpResponse classicHttpResponse;

  @BeforeEach
  void setUp() {
    cdsRuntime = CdsRuntimeConfigurer.create().complete();
    classicHttpResponse = mock(ClassicHttpResponse.class);
  }

  @Test
  void whenHttpAssertSuccessfulIsCalled_thenNoExceptionIsThrown() {
    doReturn(200).when(classicHttpResponse).getCode();

    assertEquals(classicHttpResponse, Utils.Http.assertSuccessful(classicHttpResponse));

    verify(classicHttpResponse, times(2)).getCode();
    verifyNoMoreInteractions(classicHttpResponse);
  }

  // six numbers
  @ParameterizedTest
  @ValueSource(ints = {100, 300, 400, 500})
  void givenStatusCodeIsError_whenHttpAssertSuccessfulIsCalled_thenExceptionIsThrown(int code) {
    doReturn(code).when(classicHttpResponse).getCode();
    doReturn("dummy").when(classicHttpResponse).getReasonPhrase();

    assertThrows(ServiceException.class, () -> Utils.Http.assertSuccessful(classicHttpResponse));

    verify(classicHttpResponse).getReasonPhrase();
    verify(classicHttpResponse, atLeast(2)).getCode();
    verifyNoMoreInteractions(classicHttpResponse);
  }

  @Test
  void whenStreamsAsStreamIsCalled_thenCorrectResultIsReturned() {
    assertEquals(List.of(1, 2, 3), Utils.Streams.asStream(List.of(1, 2, 3)).toList());
  }

  @Test
  void whenStreamsAsListIsCalled_thenCorrectResultIsReturned() {
    assertEquals(List.of(1, 2, 3), Utils.Streams.asList(List.of(1, 2, 3).iterator()));
  }

  @Test
  void whenResourcesResourceExistsIsCalled_thenCorrectResultIsReturned() {
    assertTrue(Utils.Resources.resourceExists("ord/ord-document.json"));
    assertFalse(Utils.Resources.resourceExists("ord/no-such-document.json"));
  }

  @Test
  void whenResourcesGetResourceAsStreamIsCalled_thenCorrectResultIsReturned() throws IOException {
    assertNull(Utils.Resources.getResourceAsStream("ord/no-such-document.json"));
    assertEquals(
        getResourceAsString("ord/ord-document.json"),
        IOUtils.toString(Utils.Resources.getResourceAsStream("ord/ord-document.json"), UTF_8));
  }

  @Test
  void whenExceptionsAsHttpStatusIsCalled_thenCorrectResultIsReturned() {
    assertEquals(500, Utils.Exceptions.asHttpStatus(new RuntimeException()));
    assertEquals(400, Utils.Exceptions.asHttpStatus(new ErrorStatusException(BAD_REQUEST)));
    assertEquals(500, Utils.Exceptions.asHttpStatus(new ErrorStatusException(SERVER_ERROR)));
  }

  @Test
  void whenCdsRuntimePropertiesGetOdataV4PropertiesIsCalled_thenCorrectResultIsReturned() {
    assertEquals(
        cdsRuntime.getEnvironment().getCdsProperties().getOdataV4(),
        Utils.CdsRuntimeProperties.getOdataV4Properties(cdsRuntime));
  }

  @Test
  void whenCdsRuntimePropertiesGetModelProviderPropertiesIsCalled_thenCorrectResultIsReturned() {
    assertEquals(
        cdsRuntime.getEnvironment().getCdsProperties().getModel().getProvider(),
        Utils.CdsRuntimeProperties.getModelProviderProperties(cdsRuntime));
  }

  @Test
  void whenCdsRuntimePropertiesGetOrdPropertiesIsCalled_thenCorrectResultIsReturned() {
    assertEquals(
        cdsRuntime.getEnvironment().getCdsProperties().getOrd(),
        Utils.CdsRuntimeProperties.getOrdProperties(cdsRuntime));
  }

  @Test
  void whenCdsRuntimePropertiesGetAuthenticationPropertiesIsCalled_thenCorrectResultIsReturned() {
    assertEquals(
        cdsRuntime.getEnvironment().getCdsProperties().getSecurity().getAuthentication(),
        Utils.CdsRuntimeProperties.getAuthenticationProperties(cdsRuntime));
  }

  private static String getResourceAsString(String path) throws IOException {
    return IOUtils.toString(currentThread().getContextClassLoader().getResourceAsStream(path), UTF_8);
  }
}
