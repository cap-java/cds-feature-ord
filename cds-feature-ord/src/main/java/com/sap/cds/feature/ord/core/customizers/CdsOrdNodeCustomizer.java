/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.core.customizers;

import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.sap.cds.services.runtime.CdsRuntimeAware;
import java.util.function.Predicate;

public interface CdsOrdNodeCustomizer extends CdsRuntimeAware {

  /**
   * Returns a predicate that determines whether the customizer should process a node with the given
   * name.
   *
   * @return the predicate
   */
  Predicate<JsonStreamContext> predicate();

  /**
   * Customize the given node.
   *
   * @param nodeName the name of the node
   * @param node     the node to process
   * @return the processed node
   */
  JsonNode customize(String nodeName, JsonNode node);
}
