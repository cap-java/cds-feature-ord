/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.model;

import com.sap.cds.CdsData;
import com.sap.cds.Struct;
import java.util.List;
import java.util.Map;

public interface OrdDocument extends CdsData {

  static OrdDocument create() {
    return Struct.create(OrdDocument.class);
  }

  static OrdDocument of(Map<String, Object> attributes) {
    return Struct.access(attributes).as(OrdDocument.class);
  }

  String getUrl();

  String getPerspective();

  List<OrdAccessStrategy> getAccessStrategies();
}
