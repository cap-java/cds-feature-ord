/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.core.generators;

import com.fasterxml.jackson.core.JsonStreamContext;
import com.sap.cds.services.runtime.CdsRuntimeAware;
import java.util.function.Predicate;

public interface CdsOrdNodeGenerator extends CdsRuntimeAware {

  /**
   * Returns a predicate that determines whether the generator should generate a node with the given
   * name.
   *
   * @return the predicate
   */
  Predicate<JsonStreamContext> predicate();

  /**
   * Generates a node based on the given context.
   *
   * @param context the JSON stream context
   * @return the generated node as a string
   */
  String generate(JsonStreamContext context);
}
