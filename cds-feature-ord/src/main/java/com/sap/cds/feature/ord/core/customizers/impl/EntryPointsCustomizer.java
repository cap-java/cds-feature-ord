/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.core.customizers.impl;

import static com.sap.cds.feature.ord.common.Utils.CdsRuntimeProperties.getOdataV4Properties;
import static com.sap.cds.feature.ord.common.Utils.Streams.asStream;

import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sap.cds.feature.ord.core.customizers.CdsOrdNodeCustomizer;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Customizer for the entryPoints attribute in the CDS ORD service that adapts the entry points to
 * the configured OData V4 endpoint path.
 */
@NoArgsConstructor
@AllArgsConstructor
public class EntryPointsCustomizer implements CdsOrdNodeCustomizer {

  private CdsRuntime cdsRuntime;

  @Override
  public void setCdsRuntime(CdsRuntime cdsRuntime) {
    this.cdsRuntime = cdsRuntime;
  }

  @Override
  public Predicate<JsonStreamContext> predicate() {
    return (context) -> Objects.equals("entryPoints", context.getCurrentName());
  }

  @Override
  public JsonNode customize(String nodeName, JsonNode entryPoints) {
    return Optional.ofNullable(entryPoints) //
        .map(ArrayNode.class::cast) //
        .map(this::process) //
        .orElse(null);
  }

  private ArrayNode process(ArrayNode entryPoints) {
    ArrayNode result = JsonNodeFactory.instance.arrayNode();
    String oDataPath = getOdataV4Properties(cdsRuntime).getEndpoint().getPath();

    asStream(entryPoints)
        .map(node -> node.asText().replace("/odata/v4", oDataPath))
        .forEach(path -> result.add(JsonNodeFactory.instance.textNode(path)));

    return result;
  }
}
