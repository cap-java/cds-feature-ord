/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.processor.impl;

import static com.sap.cds.feature.ord.common.Utils.CdsRuntimeProperties.getOdataV4Properties;
import static com.sap.cds.feature.ord.common.Utils.Streams.asStream;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sap.cds.feature.ord.processor.CdsOrdNodeProcessor;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Processor for the entryPoints attribute in the CDS ORD service that adapts the entry points to
 * the configured OData V4 endpoint path.
 */
public class EntryPointsProcessor implements CdsOrdNodeProcessor {

  private CdsRuntime cdsRuntime;

  @Override
  public void setCdsRuntime(CdsRuntime cdsRuntime) {
    this.cdsRuntime = cdsRuntime;
  }

  @Override
  public Predicate<String> predicate() {
    return "entryPoints"::equals;
  }

  @Override
  public <T extends TreeNode> Optional<T> process(String nodeName, T entryPoints) {
    return Optional.ofNullable(entryPoints) //
        .map(ArrayNode.class::cast) //
        .map(this::process);
  }

  @SuppressWarnings("unchecked")
  private <T extends TreeNode> T process(ArrayNode entryPoints) {
    ArrayNode result = JsonNodeFactory.instance.arrayNode();
    String oDataPath = getOdataV4Properties(cdsRuntime).getEndpoint().getPath();

    asStream(entryPoints)
        .map(node -> node.asText().replace("/odata/v4", oDataPath))
        .forEach(path -> result.add(JsonNodeFactory.instance.textNode(path)));

    return (T) result;
  }
}
