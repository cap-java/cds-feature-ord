/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.servlet;

import static com.sap.cds.feature.ord.common.Constants.HEADER_LOCAL_TENANT_ID;
import static com.sap.cds.feature.ord.common.Constants.PERSPECTIVE_SYSTEM_INSTANCE;
import static com.sap.cds.feature.ord.common.Constants.PERSPECTIVE_SYSTEM_VERSION;
import static com.sap.cds.feature.ord.common.Utils.CdsRuntimeProperties.getOrdProperties;
import static com.sap.cds.feature.ord.common.Utils.Http.handleException;
import static com.sap.cds.feature.ord.common.Utils.Http.respondWith;
import static com.sap.cds.services.ErrorStatuses.NOT_FOUND;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import com.sap.cds.CdsData;
import com.sap.cds.feature.ord.model.OrdDocument;
import com.sap.cds.feature.ord.provider.AuthenticationManagerProvider;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.utils.ErrorStatusException;
import com.sap.cds.services.utils.model.DynamicModelUtils;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class WellKnownServlet extends HttpServlet {

  private final transient CdsRuntime cdsRuntime;
  private final transient DynamicModelUtils dynamicModelUtils;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      log.debug("Received request for Open Resource Discovery (ORD)");

      process(request, response);
    } catch (Exception exception) {
      handleException(response, exception);
    }
  }

  private void process(HttpServletRequest request, HttpServletResponse response) {
    cdsRuntime.getProvider(AuthenticationManagerProvider.class).checkAuthorization(request.getRequestURI());

    if (!isEmpty(request.getPathInfo()) && !Objects.equals("/", request.getPathInfo())) {
      throw new ErrorStatusException(NOT_FOUND);
    }

    cdsRuntime
        .requestContext()
        .systemUser(request.getHeader(HEADER_LOCAL_TENANT_ID))
        .run(rc -> {
          response.setStatus(SC_OK);
          response.setContentType(APPLICATION_JSON.getMimeType());
          response.setCharacterEncoding(APPLICATION_JSON.getCharset().toString());
          respondWith(response, createResponsePayload(cdsRuntime));
        });
  }

  private String createResponsePayload(CdsRuntime cdsRuntime) {
    return CdsData.create(Map.of("openResourceDiscoveryV1", Map.of("documents", createOrdDocuments(cdsRuntime))))
        .toJson();
  }

  private List<OrdDocument> createOrdDocuments(CdsRuntime cdsRuntime) {
    return (dynamicModelUtils.useStaticModel()
            ? Stream.of(PERSPECTIVE_SYSTEM_VERSION)
            : Stream.of(PERSPECTIVE_SYSTEM_VERSION, PERSPECTIVE_SYSTEM_INSTANCE))
        .map(p -> asOrdDocument(cdsRuntime, p))
        .toList();
  }

  private OrdDocument asOrdDocument(CdsRuntime cdsRuntime, String perspective) {
    String documentsEndpoint =
        getOrdProperties(cdsRuntime).getDocumentsEndpoint().getPath();
    AuthenticationManagerProvider provider = cdsRuntime.getProvider(AuthenticationManagerProvider.class);
    String query =
        Objects.equals(PERSPECTIVE_SYSTEM_VERSION, perspective) ? "" : "?perspective=%s".formatted(perspective);

    return OrdDocument.of(Map.ofEntries(
        Map.entry("url", "%s/documents/ord-document%s".formatted(documentsEndpoint, query)),
        Map.entry("perspective", perspective),
        Map.entry(
            "accessStrategies",
            provider.getAccessStrategies().stream()
                .map(strategy -> Map.of("type", strategy))
                .toList())));
  }
}
