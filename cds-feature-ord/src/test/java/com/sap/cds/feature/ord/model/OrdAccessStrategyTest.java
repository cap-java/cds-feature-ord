/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class OrdAccessStrategyTest {

  static final String TEST_TYPE = "test-type";

  @Test
  void whenCreateIsCalled_thenEmptyOrdAccessStrategyIsReturned() {
    assertEquals(0, OrdAccessStrategy.create().size());
  }

  @Test
  void whenOfIsCalled_thenCorrectOrdDocumentIsReturned() {
    OrdAccessStrategy created = OrdAccessStrategy.of(Map.ofEntries(Map.entry("type", TEST_TYPE)));

    assertEquals(TEST_TYPE, created.getType());
  }
}
