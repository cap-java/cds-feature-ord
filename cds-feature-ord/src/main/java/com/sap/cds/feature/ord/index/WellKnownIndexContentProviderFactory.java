/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.index;

import static com.sap.cds.feature.ord.common.Utils.CdsRuntimeProperties.getOrdProperties;
import static com.sap.cds.services.utils.path.UrlPathUtil.normalizeBasePath;

import com.sap.cds.adapter.IndexContentProvider;
import com.sap.cds.adapter.IndexContentProviderFactory;
import com.sap.cds.services.environment.CdsProperties.OpenResourceDiscovery;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeAware;
import java.io.PrintWriter;

public class WellKnownIndexContentProviderFactory implements IndexContentProviderFactory, CdsRuntimeAware {

  private static final String TEMPLATE = """
<h3 class="header">
<a href="%s"><span>%s</span></a>
</h3>
""";

  private CdsRuntime cdsRuntime;

  @Override
  public void setCdsRuntime(CdsRuntime cdsRuntime) {
    this.cdsRuntime = cdsRuntime;
  }

  @Override
  public boolean isEnabled() {
    return getOrdProperties(cdsRuntime).getWellKnownEndpoint().isEnabled();
  }

  @Override
  public IndexContentProvider create() {
    return new IndexContentProvider() {

      @Override
      public String getSectionTitle() {
        return "Open Resource Discovery well-known endpoints";
      }

      @Override
      public void writeContent(PrintWriter out, String contextPath) {
        OpenResourceDiscovery ordProperties = getOrdProperties(cdsRuntime);
        String path =
            normalizeBasePath(ordProperties.getWellKnownEndpoint().getPath());

        out.format(TEMPLATE, path, path);
      }
    };
  }
}
