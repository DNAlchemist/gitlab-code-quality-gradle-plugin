# Release procedure

## Maven Central via the Sonatype Central Portal (`io.github.dnalchemist`)

Sonatype retired the legacy OSSRH service (`oss.sonatype.org` / `s01.oss.sonatype.org`) in 2025. All publishing now goes through the **Central Portal** ([central.sonatype.com](https://central.sonatype.com/)).

Artifact coordinates: **`io.github.dnalchemist:gitlab-code-quality-gradle-plugin`** (Gradle plugin marker: **`io.github.dnalchemist.gitlab-code-quality`**).

### 1. Claim the namespace

1. Sign in to [central.sonatype.com](https://central.sonatype.com/) (typically via GitHub).
2. Create the namespace **`io.github.dnalchemist`**. For `io.github.*` namespaces it is enough to confirm ownership of the matching GitHub account (`dnalchemist`).
3. Wait for approval.

### 2. Credentials

Generate a **User Token** at [central.sonatype.com](https://central.sonatype.com/) (Account → User Tokens). It produces a username/password pair distinct from your login.

Either store them locally in `~/.gradle/gradle.properties`:

```properties
centralPortalUsername=your_token_username
centralPortalPassword=your_token_password
```

…or expose them through environment variables in CI (already wired up in `.github/workflows/deploy.yml`):

```text
ORG_GRADLE_PROJECT_centralPortalUsername  # GitHub secret CENTRAL_PORTAL_USERNAME
ORG_GRADLE_PROJECT_centralPortalPassword  # GitHub secret CENTRAL_PORTAL_PASSWORD
```

### 3. Snapshots

Versions ending in **`-SNAPSHOT`** are published directly with the bundled `maven-publish` plugin to the Central Portal snapshot endpoint:

```
https://central.sonatype.com/repository/maven-snapshots/
```

Already configured in `build.gradle.kts`. Just run:

```bash
./gradlew publish
```

Snapshot uploads do not require GPG signing. The Central Portal UI does not browse snapshots; verify by fetching `maven-metadata.xml` directly:

```
https://central.sonatype.com/repository/maven-snapshots/io/github/dnalchemist/gitlab-code-quality-gradle-plugin/1.2.0-SNAPSHOT/maven-metadata.xml
```

### 4. Releases

Direct HTTP PUT for releases is **not** accepted by the Central Portal — uploads happen through a Publisher API that takes a single signed zip bundle. The plain `maven-publish` plugin cannot do this on its own.

Use a dedicated plugin. Recommended: [`com.gradleup.nmcp`](https://www.gradleup.com/nmcp/).

The repository already wires `com.gradleup.nmcp` and `com.gradleup.nmcp.aggregation` 1.4.4 in `build.gradle.kts`. They expose:

```bash
./gradlew publishAggregationToCentralPortal
```

GPG signing is required for releases. The `signing` plugin is already applied; supply the key via Gradle properties (in `~/.gradle/gradle.properties`):

```properties
signingKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----\n
signingPassword=your_gpg_passphrase
```

…or in CI:

```text
ORG_GRADLE_PROJECT_signingKey       # GitHub secret SIGNING_KEY (ASCII-armored private key)
ORG_GRADLE_PROJECT_signingPassword  # GitHub secret SIGNING_PASSWORD (passphrase)
```

Generate the ASCII-armored key with `gpg --armor --export-secret-keys <KEYID>`. Snapshots have `isRequired = false`, so missing keys do not break snapshot publishes — they only block `-` (non-SNAPSHOT) versions.

After uploading, open [central.sonatype.com](https://central.sonatype.com/) → **Deployments**, wait for validation and click **Publish**. Switch `publishingType` to `"AUTOMATIC"` in `nmcpAggregation.centralPortal` if you want the plugin to release without that manual gate.

---

## Gradle Plugin Portal (`io.github.dnalchemist.gitlab-code-quality`)

In addition to Maven Central, releases are published to the [Gradle Plugin Portal](https://plugins.gradle.org/) so consumers can apply the plugin with a single line, no extra `pluginManagement` repository needed:

```kotlin
plugins {
    id("io.github.dnalchemist.gitlab-code-quality") version "1.0.0"
}
```

### 1. API key

Sign in at [plugins.gradle.org](https://plugins.gradle.org/), open **API Keys** in the user menu, and copy the key/secret pair.

Locally in `~/.gradle/gradle.properties`:

```properties
gradle.publish.key=your_api_key
gradle.publish.secret=your_api_secret
```

In CI the `com.gradle.plugin-publish` plugin reads them from environment variables:

```text
GRADLE_PUBLISH_KEY     # GitHub secret GRADLE_PUBLISH_KEY
GRADLE_PUBLISH_SECRET  # GitHub secret GRADLE_PUBLISH_SECRET
```

### 2. Publish

```bash
./gradlew publishPlugins -Pversion=1.0.0
```

The Plugin Portal does not accept `-SNAPSHOT` versions — only releases are pushed there. The release workflow (on `v*` tags) publishes to **both** Central Portal and Plugin Portal in the same run.

---

## Versioning and tags

Versions live in `gradle.properties` (`version=…`), so the build script no longer hardcodes them. The release workflow injects the tag-derived version via `-Pversion=X.Y.Z`, which overrides whatever default `gradle.properties` carries.

Example variables:

```bash
export RELEASE_VERSION="1.0.0"
export DEVELOP_VERSION="1.1.0-SNAPSHOT"
```

A typical cycle:

```bash
# Optional: bump the snapshot default for clarity
sed -i '' "s/^version=.*/version=${DEVELOP_VERSION}/" gradle.properties
# Update :version_stable: / :version_snapshot: in README.adoc as needed
git commit -am "Preparing ${RELEASE_VERSION} release"
git tag -a -m "v${RELEASE_VERSION}" "v${RELEASE_VERSION}"
git push --follow-tags
```

The CI workflow then:

* on `master` push: publishes the version from `gradle.properties` (a `-SNAPSHOT`) to Central Portal snapshots.
* on `vX.Y.Z` tag push: derives `X.Y.Z`, runs `publishAggregationToCentralPortal` (signed bundle to Central Portal Publisher API in `USER_MANAGED` state) **and** `publishPlugins` (signed release to Gradle Plugin Portal).

For Central Portal, after the bundle uploads, click **Publish** in the Deployments view.
