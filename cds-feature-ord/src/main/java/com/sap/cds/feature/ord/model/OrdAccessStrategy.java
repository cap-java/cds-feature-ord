/*
 * © 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.ord.model;

import com.sap.cds.CdsData;
import com.sap.cds.Struct;
import java.util.Map;

public interface OrdAccessStrategy extends CdsData {

  static OrdAccessStrategy create() {
    return Struct.create(OrdAccessStrategy.class);
  }

  static OrdAccessStrategy of(Map<String, Object> attributes) {
    return Struct.access(attributes).as(OrdAccessStrategy.class);
  }

  String getType();
}
