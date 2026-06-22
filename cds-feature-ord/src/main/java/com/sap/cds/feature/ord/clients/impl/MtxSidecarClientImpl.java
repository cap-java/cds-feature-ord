/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.clients.impl;

import static com.sap.cds.feature.ord.common.Utils.CdsRuntimeProperties.getModelProviderProperties;
import static com.sap.cds.feature.ord.common.Utils.Http.assertSuccessful;
import static com.sap.cds.services.request.RequestContext.getCurrent;
import static com.sap.cloud.sdk.cloudplatform.connectivity.ApacheHttpClient5Accessor.getHttpClient;
import static com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor.getDestination;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.apache.hc.core5.http.ContentType.APPLICATION_JSON;

import com.sap.cds.CdsData;
import com.sap.cds.feature.ord.clients.MtxSidecarClient;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.request.FeatureToggle;
import com.sap.cds.services.request.RequestContext;
import com.sap.cds.services.runtime.CdsRuntime;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

@RequiredArgsConstructor
public class MtxSidecarClientImpl implements MtxSidecarClient {

  private static final String MTX_PROVISIONING_SERVICE_DESTINATION = "com.sap.cds.mtxSidecar";
  private static final String API_PATH_GET_ORD_DOCUMENT = "/-/cds/ord-provider-service/getOrdDocument";
  private static final String API_PATH_GET_ORD_RESOURCE_DEFINITION =
      "/-/cds/ord-provider-service/getOrdResourceDefinition";

  private final CdsRuntime cdsRuntime;

  @Override
  public String getOrdDocument() {
    return doPost(API_PATH_GET_ORD_DOCUMENT, asGetOrdDocumentRequestEntity(getCurrent(cdsRuntime)));
  }

  @Override
  public String getOrdResourceDefinition(String resource) {
    return doPost(
        API_PATH_GET_ORD_RESOURCE_DEFINITION,
        asGetOrdResourceDefinitionRequestEntity(getCurrent(cdsRuntime), resource));
  }

  private String doPost(String path, HttpEntity entity) {
    try {
      return getHttpClient(getDestination(MTX_PROVISIONING_SERVICE_DESTINATION))
          .execute(
              ClassicRequestBuilder.post(path).setEntity(entity).build(),
              response -> IOUtils.toString(
                  assertSuccessful(response).getEntity().getContent(), UTF_8));
    } catch (IOException exception) {
      throw new ServiceException("Request to sidecar failed", exception);
    }
  }

  private String extractTenant(RequestContext requestContext) {
    return !getModelProviderProperties(cdsRuntime).isExtensibility()
        ? null
        : requestContext.getUserInfo().getTenant();
  }

  private Set<String> extractToggles(RequestContext requestContext) {
    return !getModelProviderProperties(cdsRuntime).isToggles()
        ? emptySet()
        : requestContext
            .getFeatureTogglesInfo()
            .getEnabledFeatureToggles()
            .map(FeatureToggle::getName)
            .collect(toSet());
  }

  private StringEntity asGetOrdDocumentRequestEntity(RequestContext requestContext) {
    CdsData body = CdsData.create();

    body.put("tenant", extractTenant(requestContext));
    body.put("toggles", extractToggles(requestContext));

    return new StringEntity(body.toJson(), APPLICATION_JSON);
  }

  private StringEntity asGetOrdResourceDefinitionRequestEntity(RequestContext requestContext, String resource) {
    CdsData body = CdsData.create();

    body.put("resource", resource);
    body.put("tenant", extractTenant(requestContext));
    body.put("toggles", extractToggles(requestContext));

    return new StringEntity(body.toJson(), APPLICATION_JSON);
  }
}
