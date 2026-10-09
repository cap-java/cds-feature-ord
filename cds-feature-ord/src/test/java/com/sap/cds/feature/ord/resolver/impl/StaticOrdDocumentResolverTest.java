/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.resolver.impl;

import static com.sap.cds.feature.ord.common.Constants.PERSPECTIVE_SYSTEM_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class StaticOrdDocumentResolverTest {

  @Test
  void resolvesConfiguredOrdDocument() throws IOException {
    var classUnderTest =
        new StaticOrdDocumentResolver(CdsRuntimeConfigurer.create().complete());

    assertEquals(Integer.MAX_VALUE, classUnderTest.order());
    try (var result = classUnderTest.resolve("documents/ord-document", PERSPECTIVE_SYSTEM_VERSION)) {
      assertNotNull(result);
      assertTrue(result.read() >= 0);
    }
  }

  @Test
  void appliesOnlyToSystemVersionDocument() {
    var resolver = new StaticOrdDocumentResolver();

    assertTrue(resolver.isApplicable("documents/ord-document", PERSPECTIVE_SYSTEM_VERSION));
    assertFalse(resolver.isApplicable("documents/other", PERSPECTIVE_SYSTEM_VERSION));
    assertFalse(resolver.isApplicable("documents/ord-document", "system-instance"));
  }
}
