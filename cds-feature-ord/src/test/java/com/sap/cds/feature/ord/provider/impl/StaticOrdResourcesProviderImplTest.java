/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.provider.impl;

import static com.sap.cds.feature.ord.common.Constants.PERSPECTIVE_SYSTEM_INSTANCE;
import static com.sap.cds.feature.ord.common.Constants.PERSPECTIVE_SYSTEM_VERSION;
import static com.sap.cds.feature.ord.common.Utils.CdsRuntimeProperties.getOrdProperties;
import static com.sap.cds.services.runtime.CdsRuntimeConfigurer.create;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FilenameUtils.concat;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.sap.cds.adapter.edmx.EdmxV4Provider;
import com.sap.cds.impl.parser.JsonParser;
import com.sap.cds.services.runtime.CdsRuntime;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class StaticOrdResourcesProviderImplTest {

  @Test
  void givenPerspectiveIsNull_whenGetDocumentIsCalled_thenCorrectResultIsReturned() {
    CdsRuntime runtime = create().complete();
    String document =
        concat("documents", getBaseName(getOrdProperties(runtime).getOrdDocumentPath()));
    InputStream is = new StaticOrdResourcesProviderImpl(runtime, List.of()).read(document, null);

    assertNotNull(is);

    Map<String, Object> ord = (Map<String, Object>) JsonParser.map(new InputStreamReader(is));
    // just test a few properties to ensure it´s the right document
    assertEquals("1.9", ord.get("openResourceDiscovery"));
    assertEquals("this is an application description", ord.get("description"));

    List<Map<String, Object>> integrationDependencies =
        (List<Map<String, Object>>) ord.get("integrationDependencies");
    assertEquals(1, integrationDependencies.size());

    Map<String, Object> integrationDependency = integrationDependencies.get(0);
    assertEquals("sap.cdsjavacpoc:integrationDependency:RawEvent:v1", integrationDependency.get("ordId"));
  }

  @Test
  void givenPerspectiveIsSystemVersion_whenGetDocumentIsCalled_thenCorrectResultIsReturned() {
    CdsRuntime runtime = create().complete();
    String document =
        concat("documents", getBaseName(getOrdProperties(runtime).getOrdDocumentPath()));
    InputStream is =
        new StaticOrdResourcesProviderImpl(runtime, List.of()).read(document, PERSPECTIVE_SYSTEM_VERSION);

    assertNotNull(is);

    Map<String, Object> ord = (Map<String, Object>) JsonParser.map(new InputStreamReader(is));
    // just test a few properties to ensure it´s the right document
    assertEquals("1.9", ord.get("openResourceDiscovery"));
    assertEquals("this is an application description", ord.get("description"));

    List<Map<String, Object>> integrationDependencies =
        (List<Map<String, Object>>) ord.get("integrationDependencies");
    assertEquals(1, integrationDependencies.size());

    Map<String, Object> integrationDependency = integrationDependencies.get(0);
    assertEquals("sap.cdsjavacpoc:integrationDependency:RawEvent:v1", integrationDependency.get("ordId"));
  }

  @Test
  void givenPerspectiveIsSystemInstance_whenGetDocumentIsCalled_thenCorrectResultIsReturned() {
    CdsRuntime runtime = create().complete();
    String document =
        concat("documents", getBaseName(getOrdProperties(runtime).getOrdDocumentPath()));

    assertThrows(IllegalArgumentException.class, () -> new StaticOrdResourcesProviderImpl(runtime, List.of())
        .read(document, PERSPECTIVE_SYSTEM_INSTANCE));
  }

  @Test
  void givenThatDocumentDoesNotExist_whenGetDocumentIsCalled_thenNullIsReturned() {
    assertNull(new StaticOrdResourcesProviderImpl(create().complete(), List.of())
        .read("no-such-ord-document.json", null));
  }

  @Test
  void givenPerspectiveIsSystemVersion_and_edmxResource_whenGetDocumentIsCalled_thenCorrectResultIsReturned()
      throws IOException {
    EdmxV4Provider edmxV4Provider = mock(EdmxV4Provider.class);
    doReturn(toInputStream("</>", UTF_8)).when(edmxV4Provider).getEdmx("test");

    assertEquals(
        "</>",
        IOUtils.toString(
            new StaticOrdResourcesProviderImpl(
                    create().provider(edmxV4Provider).complete(), List.of())
                .read("documents/test_ord_service/test.edmx", PERSPECTIVE_SYSTEM_VERSION),
            UTF_8));

    verify(edmxV4Provider).setPrevious(null);
    verify(edmxV4Provider).getEdmx("test");
    verifyNoMoreInteractions(edmxV4Provider);
  }
}
