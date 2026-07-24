/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.adapter;

import static com.sap.cds.feature.ord.common.Utils.CdsRuntimeProperties.getOrdProperties;
import static com.sap.cds.services.utils.path.UrlPathUtil.normalizeBasePath;
import static com.sap.cds.services.utils.path.UrlResourcePathBuilder.path;

import com.sap.cds.adapter.ServletAdapterFactory;
import com.sap.cds.adapter.UrlResourcePath;
import com.sap.cds.feature.ord.servlet.WellKnownServlet;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeAware;
import com.sap.cds.services.utils.model.DynamicModelUtils;

/** Factory for the Open Resource Discovery adapter. */
public class WellKnownAdapterFactory implements ServletAdapterFactory, CdsRuntimeAware {

  private CdsRuntime cdsRuntime;

  @Override
  public void setCdsRuntime(CdsRuntime cdsRuntime) {
    this.cdsRuntime = cdsRuntime;
  }

  @Override
  public Object create() {
    return new WellKnownServlet(cdsRuntime, new DynamicModelUtils(cdsRuntime));
  }

  @Override
  public boolean isEnabled() {
    return getOrdProperties(cdsRuntime).getWellKnownEndpoint().isEnabled();
  }

  @Override
  public String getBasePath() {
    return normalizeBasePath(
        getOrdProperties(cdsRuntime).getWellKnownEndpoint().getPath());
  }

  @Override
  public String[] getMappings() {
    return new String[] {path(getBasePath()).recursive().build().getPath()};
  }

  @Override
  public UrlResourcePath getServletPath() {
    // Certificate is checked by service handler (authentication + authorization)
    return path(getBasePath()).recursive().isPublic(true).build();
  }
}
