/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrdDocumentTest {

  private static final String TEST_URL = "test-url";
  private static final OrdAccessStrategy TEST_ACCESS_STRATEGY = OrdAccessStrategy.of(Map.of("type", "test-type"));

  @Test
  void whenCreateIsCalled_thenEmptyOrdDocumentIsReturned() {
    assertEquals(0, OrdDocument.create().size());
  }

  @Test
  void whenOfIsCalled_thenCorrectOrdDocumentIsReturned() {
    OrdDocument created = OrdDocument.of(Map.ofEntries(
        Map.entry("url", TEST_URL), Map.entry("accessStrategies", List.of(TEST_ACCESS_STRATEGY))));

    assertEquals(TEST_URL, created.getUrl());
    assertEquals(List.of(TEST_ACCESS_STRATEGY), created.getAccessStrategies());
  }
}
