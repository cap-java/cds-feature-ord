---
name: integrate-cds-feature-ord
description: Integrate the com.sap.cds:cds-feature-ord plugin into an existing CAP Java project. Use when the user asks to add ORD support, configure ORD endpoints, set up the ORD document, or asks what configuration options are available for the Java ORD plugin.
allowed-tools:
  - Read
  - Write
  - Edit
  - Bash(mvn *)
  - Bash(cds *)
  - Bash(npm *)
  - Bash(ls *)
  - Bash(curl *)
---

# /integrate-cds-feature-ord

Integrates `com.sap.cds:cds-feature-ord` into an existing CAP Java project to expose Open Resource Discovery (ORD) metadata endpoints.

Arguments passed: `$ARGUMENTS`

---

## Dispatch on arguments

| Argument | Action |
| -------- | ------ |
| *(none)* | Full walk-through: Steps 1 → 2a → 2b → 3 → 4 → 5 |
| `maven`  | Add Maven dependency only → Step 1 |
| `config` | Configure Java runtime properties only (endpoints, classpath paths, OData V4 path) → Step 3 |
| `auth`   | Configure authentication only → Step 4 |
| `mtx`    | Configure MTX sidecar integration only → Step 2b |

---

## Before starting

Read the project structure and resolve the latest dependency versions before taking any action:

1. Run `ls` on the project root to confirm the module layout (single-module vs multi-module with `srv/`, `db/` etc.).
2. Read the root `pom.xml` — check whether a `<dependencyManagement>` BOM already exists.
3. Read `.cdsrc.json` if it exists — check whether a `build.tasks` array is already present.
4. Read the service module's `pom.xml` — check whether `cds-maven-plugin` executions already drive `cds build` commands.
5. Resolve the latest `com.sap.cds:cds-feature-ord` version from Maven Central:
   ```bash
   curl -s "https://search.maven.org/solrsearch/select?q=g:com.sap.cds+AND+a:cds-feature-ord&rows=1&core=gav" \
     | grep -o '"v":"[^"]*"' | head -1 | cut -d'"' -f4
   ```
6. Resolve the latest `@cap-js/ord` version from the npm registry:
   ```bash
   npm view @cap-js/ord version
   ```

Use the versions obtained in steps 5 and 6 wherever `<LATEST_MAVEN_VERSION>` and `<LATEST_NPM_VERSION>` appear in the steps below. Do not substitute hardcoded versions — always resolve them fresh at integration time.

These reads and lookups determine which variant of Steps 1, 2a, and 2b applies and ensure the project uses the current releases. Do not skip them.

---

## Step 1 — Add the Maven dependency

### Single-module project

Add directly to the service module's `pom.xml`:

```xml
<dependency>
  <groupId>com.sap.cds</groupId>
  <artifactId>cds-feature-ord</artifactId>
  <version><LATEST_MAVEN_VERSION></version>
</dependency>
```

### Multi-module project (recommended pattern)

Declare the version in the root `pom.xml` `<dependencyManagement>`:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.sap.cds</groupId>
      <artifactId>cds-feature-ord</artifactId>
      <version><LATEST_MAVEN_VERSION></version>
    </dependency>
  </dependencies>
</dependencyManagement>
```

Then add the dependency (without version) in the service module's `pom.xml`:

```xml
<dependency>
  <groupId>com.sap.cds</groupId>
  <artifactId>cds-feature-ord</artifactId>
</dependency>
```

**The plugin activates automatically via Java `ServiceLoader` — no Spring `@EnableXxx` annotation or `application.yaml` activation key is needed.**

---

## Step 2a — Generate ORD resources at build time

The plugin reads a static ORD document from the classpath at `ord/ord-document.json`. This file must be generated during the Maven build using the CDS build tool.

Use whichever build mechanism the project already uses (identified in "Before starting"). Adding the ORD build step in both places causes it to run twice — once via `cds build` directly and once during `mvn package` — producing redundant work and potentially writing to two different output paths if the destinations are not identical.

**Prerequisite — `@cap-js/ord` must be available at build time.** The `cds build --for ord` command is provided by the `@cap-js/ord` Node.js package. If it is not installed, `cds build` silently skips ORD generation and produces no output. Verify it is present before configuring either approach:

```bash
# In the project root (or srv/ if that is the CDS project root):
ls node_modules/@cap-js/ord 2>/dev/null || echo "NOT INSTALLED"
```

If missing, install it as a dev dependency using the version resolved in "Before starting":

```bash
npm install --save-dev @cap-js/ord@<LATEST_NPM_VERSION>
```

---

### Approach A — `.cdsrc.json` (preferred when a `build` section already exists there)

Add an `ord` task to the `build.tasks` array in `.cdsrc.json`. Preserve all existing tasks:

```json
{
  "build": {
    "target": ".",
    "tasks": [
      { "for": "java", "src": "srv" },
      { "for": "ord",  "src": "srv", "dest": "srv/src/main/resources/ord" }
    ]
  }
}
```

- `"src"` points to the CDS service source directory (typically `srv`).
- `"dest"` is where the generated ORD files land; it must be on the classpath of the Java service module.

Running `cds build` will then produce the ORD output into the configured `dest` path.

---

### Approach B — `pom.xml` execution (preferred when CDS build is driven entirely from Maven)

Find the `cds-maven-plugin` in the service module's `pom.xml` and add an execution:

```xml
<execution>
  <id>cds-build-ord</id>
  <goals><goal>cds</goal></goals>
  <configuration>
    <commands>
      <command>build --for ord -o "${project.basedir}/src/main/resources/ord"</command>
    </commands>
  </configuration>
</execution>
```

This outputs the ORD document and resource definition files into `src/main/resources/ord/`, which is the default classpath location the plugin looks for.

---

> **Do not hand-edit files in the output directory.** They are regenerated on every build. Use the ORD configuration in `.cdsrc.json` (or `package.json` under `"cds": { "ord": { ... } }`) to control the document content.

> **Add the output directory to `.gitignore`.** The files under `src/main/resources/ord/` are generated artifacts — add that path to `.gitignore` so they are not committed.

---

## Step 2b — MTX sidecar integration (dynamic model / extensibility)

If the project uses an MTX sidecar for multitenancy or extensibility, the sidecar must also expose ORD documents.

### Condition for MTX mode activation

The plugin registers `DynamicOrdResourcesProviderImpl` (in addition to the static provider) only when **all** of the following are true:

- A model provider URL is configured: either `cds.model.provider.url` or `cds.multitenancy.sidecar.url` is non-empty.
- At least one of `cds.model.provider.extensibility=true` or `cds.model.provider.toggles=true` is set.

If only the URL is set but neither `extensibility` nor `toggles` is enabled, MTX mode is **not** activated.

### Sidecar setup

Add `@cap-js/ord` as a **production** dependency (not dev) in the sidecar's `package.json`:

```json
{
  "dependencies": {
    "@cap-js/ord": "<LATEST_NPM_VERSION>"
  }
}
```

> Using `@cap-js/ord` as a devDependency in the root `package.json` is sufficient for local development and CDS build-time generation, but the sidecar needs it at runtime to serve dynamic ORD documents per tenant.

### Well-known endpoint in MTX mode

When a request arrives and the runtime determines that a dynamic (tenant/toggle-specific) model must be used — based on the current request's tenant and enabled feature toggles — the well-known endpoint includes a second entry with `"perspective": "system-instance"`. Whether the static or dynamic model is used is evaluated per request, not at startup.

---

## Step 3 — Configuration reference

All properties are optional. The plugin works with zero configuration; the defaults below are the out-of-the-box behaviour.

### ORD endpoints

| Property | Type | Default | Description |
| -------- | ---- | ------- | ----------- |
| `cds.ord.wellKnownEndpoint.enabled` | `boolean` | `true` | Enable/disable `/.well-known/open-resource-discovery` |
| `cds.ord.wellKnownEndpoint.path` | `String` | `/.well-known/open-resource-discovery` | Path of the well-known endpoint |
| `cds.ord.documentsEndpoint.enabled` | `boolean` | `true` | Enable/disable the ORD documents endpoint |
| `cds.ord.documentsEndpoint.path` | `String` | `/ord/v1` | Base path for document and resource definition files |

### ORD document location

| Property | Type | Default | Description |
| -------- | ---- | ------- | ----------- |
| `cds.ord.ordResourcesRoot` | `String` | `ord/` | Classpath root folder for static ORD files |
| `cds.ord.ordDocumentPath` | `String` | `ord-document.json` | Relative path of the main ORD document under `ordResourcesRoot` |

The full classpath path resolved at runtime is `<ordResourcesRoot><ordDocumentPath>`, e.g. `ord/ord-document.json`.

### OData V4 entry points

The plugin rewrites `entryPoints` in the ORD document to match the configured OData V4 path. If the project uses a non-default path, set:

```yaml
cds:
  odataV4:
    endpoint:
      path: /my-custom-path
```

The plugin replaces the literal `/odata/v4` prefix in all `entryPoints` with this value.

### Minimal `application.yaml` (all defaults, nothing to add)

The plugin works with no YAML configuration. Only add properties when overriding defaults:

```yaml
cds:
  ord:
    documentsEndpoint:
      path: /ord/v1        # default — omit unless changing
    wellKnownEndpoint:
      enabled: true        # default — omit unless disabling
```

---

## Step 4 — Configure authentication

By default, the ORD endpoints enforce **mTLS certificate validation** (`sap:cmp-mtls:v1`). This is controlled by:

| Property | Type | Default | Description |
| -------- | ---- | ------- | ----------- |
| `cds.security.authentication.authenticateMetadataEndpoints` | `boolean` | `true` | `true` = mTLS required; `false` = endpoints are open |

### Production (SAP BTP Cloud Foundry with mTLS)

The default `authenticateMetadataEndpoints: true` expects UCL certificate validation. Provide the cert subject and issuer via `application.yaml` (use environment-specific config files or secret management — do not commit production cert details):

```yaml
cds:
  ucl:
    x509:
      cert-subject: "CN=cmp-prod,OU=SAP Cloud Platform Clients,OU=cmp-cf-eu10,O=SAP SE,L=Prod,C=DE"
      cert-issuer:  "CN=SAP PKI Certificate Service Client CA,OU=SAP BTP Clients,O=SAP SE,L=cf-eu10,C=DE"
```

The `accessStrategies` field in the ORD document will be set to `[{"type":"sap:cmp-mtls:v1"}]`.

### Open access (local development / non-production)

To disable mTLS and expose endpoints publicly:

```yaml
cds:
  security:
    authentication:
      authenticateMetadataEndpoints: false
```

The `accessStrategies` field in the ORD document will be set to `[{"type":"open"}]`.

> **Do not use `authenticateMetadataEndpoints: false` in production.** Always enforce mTLS when deploying to Cloud Foundry.

---

## Step 5 — Verify

Start the application:

```bash
mvn spring-boot:run
```

Check the well-known endpoint:

```bash
curl http://localhost:8080/.well-known/open-resource-discovery
```

Expected response (with `authenticateMetadataEndpoints: false`):

```json
{
  "openResourceDiscoveryV1": {
    "documents": [
      {
        "url": "/ord/v1/documents/ord-document",
        "perspective": "system-version",
        "accessStrategies": [{ "type": "open" }]
      }
    ]
  }
}
```

Check the ORD document itself:

```bash
curl http://localhost:8080/ord/v1/documents/ord-document
```

---

## Troubleshooting

| Symptom | Cause | Fix |
| ------- | ----- | --- |
| `GET /.well-known/open-resource-discovery` returns 404 | The servlet is disabled or not registered | Verify the dependency is on the classpath (`mvn dependency:tree \| grep cds-feature-ord`). Check `cds.ord.wellKnownEndpoint.enabled` is not set to `false`. |
| `GET /ord/v1/documents/ord-document` returns 404 | The classpath resource `ord/ord-document.json` is missing | The `cds build --for ord` step did not run or wrote to the wrong path. Confirm the output directory matches `cds.ord.ordResourcesRoot` + `cds.ord.ordDocumentPath`. Run `mvn clean package` and verify `target/classes/ord/ord-document.json` exists. |
| `GET /ord/v1/documents/ord-document` returns 500 | The ORD document exists but cannot be parsed | The JSON in `ord/ord-document.json` is malformed. Check the raw file in `src/main/resources/ord/`. Re-run `cds build --for ord` to regenerate it. |
| ORD endpoint returns 401 or certificate error | mTLS validation is failing | Verify `cds.ucl.x509.cert-subject` and `cds.ucl.x509.cert-issuer` match the certificate presented by the ORD consumer. For local testing set `cds.security.authentication.authenticateMetadataEndpoints: false`. |
| `system-instance` perspective is missing from the well-known response | MTX mode is not active | Both `cds.model.provider.url` (or `cds.multitenancy.sidecar.url`) and at least one of `cds.model.provider.extensibility=true` / `cds.model.provider.toggles=true` must be set (see Step 2b). |
| Resource definitions are missing from the served document | The classpath resource referenced in `resourceDefinitions[].url` does not exist | After `ResourceDefinitionsProcessor` prefixes URLs with the documents endpoint path, the servlet resolves each URL against the classpath. Verify the generated files exist under `src/main/resources/ord/` and that their paths match what the ORD document references. |

---

## Implementation notes

- **No Spring auto-configuration** — the plugin registers itself entirely via `META-INF/services/` (`ServiceLoader`). There is no `@EnableOrd` annotation or Spring bean to define.
- **`cds-feature-ord` version and `@cap-js/ord` version must be kept in sync.** Always use the versions resolved in "Before starting" — they are fetched from Maven Central and the npm registry respectively to ensure both are current. Check the library's release notes when upgrading.
- **Static provider only supports `system-version` perspective.** Requesting `?perspective=system-instance` against the static provider throws `IllegalArgumentException` and returns HTTP 500. The `system-instance` perspective is only available when MTX mode is active (see Step 2b conditions).
- **Unknown resource definition file extensions cause HTTP 500.** The documents servlet only serves `.json` (→ `application/json`), `.edmx` (→ `application/xml`), and `.graphql` (→ `text/plain`). Any other extension in the ORD document's `resourceDefinitions[].url` results in an `IllegalArgumentException` at request time. The ORD document itself (`documents/ord-document`) is always treated as JSON regardless of extension.
- **MTX sidecar calls use SAP Cloud SDK destination `com.sap.cds.mtxSidecar`.** If the sidecar request fails with a non-2xx response or `IOException`, the plugin throws a `ServiceException` that surfaces as an HTTP 500 to the ORD consumer.
- **Custom documents endpoint path propagates to the ORD document.** Setting `cds.ord.documentsEndpoint.path` to e.g. `/custom-api/ord/v1` causes all `resourceDefinitions[].url` values in the document to be prefixed with that path automatically via `ResourceDefinitionsProcessor`.

---

## Next steps — customising the generated ORD metadata

After integration, the ORD document is generated automatically from the CDS model with default values for visibility, titles, versions, package assignment, and resource identifiers. In most projects these defaults need to be adjusted to match product, namespace, and compliance requirements.

Customisation is done on the Node.js/CDS side — via CDS annotations in `.cds` files and via the `ord` block in `.cdsrc.json` — because those control what the `cds build --for ord` step produces. The Java library serves the resulting document at runtime without modifying its content (beyond endpoint path prefixing and access strategy rewriting, as documented above).

The reference for all available customisations is the `custom-ord-annotations` skill in the [`cap-js/ord`](https://github.com/cap-js/ord) repository at `.skills/custom-ord-annotations/SKILL.md`. It covers:

- Setting `visibility` (`public` / `internal` / `private`) per service or entity
- Overriding titles, descriptions, and versions
- Controlling ORD IDs and package assignment
- Configuring consumption bundles
- Defining data products and external integration dependencies
- Using `customOrdContentFile` for anything the annotations cannot express