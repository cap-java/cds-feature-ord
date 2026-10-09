/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.core.customizers.impl;

import static com.sap.cds.feature.ord.common.Utils.CdsRuntimeProperties.getOrdProperties;
import static com.sap.cds.feature.ord.common.Utils.Streams.asStream;
import static org.apache.commons.io.FilenameUtils.concat;

import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.sap.cds.feature.ord.core.customizers.CdsOrdNodeCustomizer;
import com.sap.cds.feature.ord.provider.AuthenticationManagerProvider;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Processor for resource definitions in the CDS ORD service that (a) removes all openApi resource
 * definitions because there is no runtime support for openApi documents (b) replaces
 * "accessStrategies" element to comply with the configured security model for metadata endpoints.
 */
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDefinitionsCustomizer implements CdsOrdNodeCustomizer {

  private CdsRuntime cdsRuntime;

  @Override
  public void setCdsRuntime(CdsRuntime cdsRuntime) {
    this.cdsRuntime = cdsRuntime;
  }

  @Override
  public Predicate<JsonStreamContext> predicate() {
    return (context) -> Objects.equals("resourceDefinitions", context.getCurrentName());
  }

  @Override
  public JsonNode customize(String nodeName, JsonNode node) {
    return Optional.ofNullable(node) //
        .map(ArrayNode.class::cast) //
        .map(this::process)
        .orElse(null);
  }

  private ArrayNode process(ArrayNode resourceDefinitions) {
    ArrayNode result = JsonNodeFactory.instance.arrayNode();
    String apiRoot = getOrdProperties(cdsRuntime).getDocumentsEndpoint().getPath();
    AuthenticationManagerProvider provider = cdsRuntime.getProvider(AuthenticationManagerProvider.class);
    JsonNode accessStrategiesNode = asAccessStrategiesNode(provider.getAccessStrategies());

    asStream(resourceDefinitions)
        .forEach(resourceDefinition -> result.add(resourceDefinition
            .<ObjectNode>deepCopy()
            .<ObjectNode>set("accessStrategies", accessStrategiesNode.deepCopy())
            .<ObjectNode>set(
                "url",
                asTextNode(concat(
                    apiRoot, resourceDefinition.get("url").asText())))));

    return result;
  }

  private static TextNode asTextNode(String value) {
    return JsonNodeFactory.instance.textNode(value);
  }

  private static ArrayNode asAccessStrategiesNode(List<String> accessStrategies) {
    return JsonNodeFactory.instance
        .arrayNode()
        .addAll(accessStrategies.stream()
            .map(strategy -> JsonNodeFactory.instance.objectNode().put("type", strategy))
            .toList());
  }
}
