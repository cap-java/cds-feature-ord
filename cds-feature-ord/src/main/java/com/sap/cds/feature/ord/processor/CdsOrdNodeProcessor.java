/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.processor;

import com.fasterxml.jackson.core.TreeNode;
import com.sap.cds.services.runtime.CdsRuntimeAware;
import java.util.Optional;
import java.util.function.Predicate;

public interface CdsOrdNodeProcessor extends CdsRuntimeAware {

  /**
   * Returns a predicate that determines whether the processor should process a node with the given
   * name.
   *
   * @return the predicate
   */
  Predicate<String> predicate();

  /**
   * Returns whether the processor can also generate a node.
   *
   * @return {@code true} if the processor can generate a node, {@code false} otherwise
   */
  default boolean canGenerate() {
    return false;
  }

  /**
   * Returns the name of the generated node.
   *
   * <p>By default null is returned. A specific implementation can return a name to generate a node
   * on the root level of the ORD, if canGenerate() returns true.
   *
   * @return the name of the generated node
   */
  default String getGeneratedNodeName() {
    return null;
  }

  /**
   * Processes the given node.
   *
   * @param nodeName the name of the node
   * @param node the node to process
   * @return the processed node
   */
  <T extends TreeNode> Optional<T> process(String nodeName, T node);
}
