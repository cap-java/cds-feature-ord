/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.common;

import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES;
import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES;
import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.END_OBJECT;
import static com.fasterxml.jackson.core.JsonToken.START_ARRAY;
import static com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS;
import static com.google.common.primitives.Bytes.asList;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.feature.ord.processor.CdsOrdNodeProcessor;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public class OrdJsonInputStream extends InputStream {

  // Central Object Mapper is wrapped in a class in cds4j which doesn't provide an API that we need.
  private static final ObjectMapper MAPPER = new ObjectMapper()
      .enable(ALLOW_SINGLE_QUOTES)
      .enable(ALLOW_UNQUOTED_FIELD_NAMES)
      .configure(USE_BIG_DECIMAL_FOR_FLOATS, true);

  private final JsonParser parser;
  private final InputStream inputStream;
  private final List<CdsOrdNodeProcessor> nodeProcessors;

  private JsonToken token;
  private JsonToken previous;
  private final Deque<Byte> buffer = new ArrayDeque<>();

  public OrdJsonInputStream(InputStream inputStream, List<CdsOrdNodeProcessor> nodeProcessors) throws IOException {
    this.previous = null;
    this.nodeProcessors = List.copyOf(nodeProcessors);
    this.inputStream = new BufferedInputStream(inputStream);
    this.parser = MAPPER.getFactory().createParser(this.inputStream);
    this.token = this.parser.nextToken();
  }

  @Override
  public void close() throws IOException {
    parser.close();
    inputStream.close();
  }

  @Override
  public int read() throws IOException {
    if (!buffer.isEmpty()) {
      // Return the first byte from the buffer as unsigned byte and remove it from the buffer
      return buffer.removeFirst() & 0xFF;
    }

    if (isNull(token) || parser.isClosed()) {
      return -1;
    }

    if (isFieldName(token)) {
      return handleFieldName();
    }

    if (isStructEnd(token)) {
      return handleStructEnd();
    }

    if (isStructStart(token)) {
      return handleStructStart();
    }

    return handleFieldValue();
  }

  private void appendToBuffer(String... values) {
    Arrays.stream(values) //
        .forEach(value -> buffer.addAll(asList(value.getBytes(UTF_8))));
  }

  private int handleFieldName() throws IOException {
    String current = parser.currentName();
    List<CdsOrdNodeProcessor> processors = lookupCdsOrdNodeProcessors(current);
    JsonToken next = processors.isEmpty() ? null : parser.nextToken();

    appendToBuffer(
        isStructStart(previous) ? "" : ",",
        "\"",
        current,
        "\": ",
        processors.isEmpty() ? "" : process(current, parser.readValueAsTree(), processors));

    previous = next == null ? token : (START_ARRAY == next ? END_ARRAY : END_OBJECT);
    token = parser.nextToken();

    return buffer.removeFirst() & 0xFF;
  }

  private int handleStructEnd() throws IOException {
    JsonToken next = parser.nextToken();

    if (isNull(next)) { // We're at the end of the JSON file
      nodeProcessors.stream().filter(CdsOrdNodeProcessor::canGenerate).forEach(processor -> processor
          .<JsonNode>process(null, null)
          .ifPresent(node ->
              appendToBuffer(", \"" + processor.getGeneratedNodeName() + "\": ", node.toPrettyString())));
    }

    appendToBuffer(token.asString());

    previous = token;
    token = (next != null) ? next : parser.nextToken();

    return buffer.removeFirst() & 0xFF;
  }

  private int handleFieldValue() throws IOException {
    appendToBuffer(
        isScalarValue(previous) ? "," : "",
        isValueString(token) ? "\"" : "",
        isValueString(token) ? parser.getValueAsString() : token.asString(),
        isValueString(token) ? "\"" : "");

    previous = token;
    token = parser.nextToken();

    return buffer.removeFirst() & 0xFF;
  }

  private int handleStructStart() throws IOException {
    appendToBuffer(isStructEnd(previous) ? "," : "", token.asString());

    previous = token;
    token = parser.nextToken();

    return buffer.removeFirst() & 0xFF;
  }

  private List<CdsOrdNodeProcessor> lookupCdsOrdNodeProcessors(String node) {
    return nodeProcessors.stream() //
        .filter(processor -> processor.predicate().test(node)) //
        .toList();
  }

  private static boolean isFieldName(JsonToken token) {
    return nonNull(token) && JsonToken.FIELD_NAME == token;
  }

  private static boolean isStructEnd(JsonToken token) {
    return nonNull(token) && token.isStructEnd();
  }

  private static boolean isValueString(JsonToken token) {
    return JsonToken.VALUE_STRING == token;
  }

  private static boolean isScalarValue(JsonToken token) {
    return nonNull(token) && token.isScalarValue();
  }

  private static boolean isStructStart(JsonToken token) {
    return nonNull(token) && token.isStructStart();
  }

  private static String process(String name, JsonNode node, List<CdsOrdNodeProcessor> processors) {
    return processors.stream()
        .reduce(
            node,
            (current, processor) -> processor.process(name, current).orElse(current),
            (a, b) -> a)
        .toPrettyString();
  }
}
