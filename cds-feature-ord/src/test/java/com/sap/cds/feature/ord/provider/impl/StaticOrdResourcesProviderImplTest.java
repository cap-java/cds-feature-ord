/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.provider.impl;

import static com.sap.cds.feature.ord.common.Constants.PERSPECTIVE_SYSTEM_INSTANCE;
import static com.sap.cds.feature.ord.common.Constants.PERSPECTIVE_SYSTEM_VERSION;
import static com.sap.cds.feature.ord.common.Utils.CdsRuntimeProperties.getOrdProperties;
import static org.apache.commons.io.FilenameUtils.concat;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sap.cds.impl.parser.JsonParser;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class StaticOrdResourcesProviderImplTest {

  @Test
  void givenPerspectiveIsNull_whenGetDocumentIsCalled_thenCorrectResultIsReturned() {
    CdsRuntime runtime = CdsRuntimeConfigurer.create().complete();
    StaticOrdResourcesProviderImpl provider = new StaticOrdResourcesProviderImpl(runtime, List.of());
    String document =
        concat("documents", getBaseName(getOrdProperties(runtime).getOrdDocumentPath()));
    InputStream is = provider.read(document, null);

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
    CdsRuntime runtime = CdsRuntimeConfigurer.create().complete();
    StaticOrdResourcesProviderImpl provider = new StaticOrdResourcesProviderImpl(runtime, List.of());
    String document =
        concat("documents", getBaseName(getOrdProperties(runtime).getOrdDocumentPath()));
    InputStream is = provider.read(document, PERSPECTIVE_SYSTEM_VERSION);

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
    CdsRuntime runtime = CdsRuntimeConfigurer.create().complete();
    StaticOrdResourcesProviderImpl provider = new StaticOrdResourcesProviderImpl(runtime, List.of());
    String document =
        concat("documents", getBaseName(getOrdProperties(runtime).getOrdDocumentPath()));

    assertThrows(IllegalArgumentException.class, () -> provider.read(document, PERSPECTIVE_SYSTEM_INSTANCE));
  }

  @Test
  void givenThatDocumentDoesNotExist_whenGetDocumentIsCalled_thenNullIsReturned() {
    CdsRuntime runtime = CdsRuntimeConfigurer.create().complete();
    StaticOrdResourcesProviderImpl provider = new StaticOrdResourcesProviderImpl(runtime, List.of());

    assertNull(provider.read("no-such-ord-document.json", null));
  }
}
