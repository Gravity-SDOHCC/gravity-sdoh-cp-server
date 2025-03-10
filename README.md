# Gravity SDOH Coordination Platform Reference Implementation Server

Reference Implementation CP HAPI FHIR® based server for the Coordination
Platform (CP) system as defined in the [Gravity SDOHCC Implementation
Guide](http://hl7.org/fhir/us/sdoh-clinicalcare/CapabilityStatement-SDOHCC-CoordinationPlatform.html).

This server is currently hosted at https://sdoh-cp-server.victoriousbay-86ce63e0.southcentralus.azurecontainerapps.io

## Prerequisites

- Java JDK 17 +
- Maven 3.8 +

## Installation

Clone this repository, then cd into the project directory:

```bash
git clone https://github.com/Gravity-SDOHCC/gravity-sdoh-cp-server.git

cd gravity-sdoh-cp-server
```

## Running locally

The easiest way to run this server entirely depends on your environment requirements. The following ways are supported:

### Using jetty
```bash
mvn -Pjetty spring-boot:run
```


### Using Spring Boot
```bash
mvn spring-boot:run
```

The Server will then be accessible at http://localhost:8080/fhir and the CapabilityStatement will be found at http://localhost:8080/fhir/metadata.
If you want to run this server on a different port, you can change the port in the `src/main/resources/application.yaml` file as follows:
```yaml
server:
#  servlet:
#    context-path: /example/path
  port: 8888
```
The Server will then be accessible at http://localhost:8888/fhir and the CapabilityStatement will be found at http://localhost:8888/fhir/metadata. Remember to adjust your overlay configuration in the `application.yaml` file to the following:

```yaml
    tester:
      -
          id: home
          name: Local Tester
          server_address: 'http://localhost:8888/fhir'
          refuse_to_fetch_third_party_urls: false
          fhir_version: R4
```

### Using Spring Boot with :run
```bash
mvn clean spring-boot:run -Pboot
```
Server will then be accessible at http://localhost:8080/ and eg. http://localhost:8080/fhir/metadata. Remember to adjust you overlay configuration in the application.yaml to the following:

```yaml
    tester:
      -
          id: home
          name: Local Tester
          server_address: 'http://localhost:8080/fhir'
          refuse_to_fetch_third_party_urls: false
          fhir_version: R4
```

### Using Spring Boot
```bash
mvn clean package spring-boot:repackage -DskipTests=true -Pboot && java -jar target/ROOT.war
```
Server will then be accessible at http://localhost:8080/ and eg. http://localhost:8080/fhir/metadata. Remember to adjust your overlay configuration in the application.yaml to the following:

```yaml
    tester:
      -
          id: home
          name: Local Tester
          server_address: 'http://localhost:8080/fhir'
          refuse_to_fetch_third_party_urls: false
          fhir_version: R4
```
### Using Spring Boot and Google distroless
```bash
mvn clean package com.google.cloud.tools:jib-maven-plugin:dockerBuild -Dimage=distroless-hapi && docker run -p 8080:8080 distroless-hapi
```
Server will then be accessible at http://localhost:8080/ and eg. http://localhost:8080/fhir/metadata. Remember to adjust your overlay configuration in the application.yaml to the following:

```yaml
    tester:
      -
          id: home
          name: Local Tester
          server_address: 'http://localhost:8080/fhir'
          refuse_to_fetch_third_party_urls: false
          fhir_version: R4
```

### Using the Dockerfile and multistage build
```bash
./build-docker-image.sh && docker run -p 8080:8080 hapi-fhir/hapi-fhir-jpaserver-starter:latest
```
Server will then be accessible at http://localhost:8080/ and eg. http://localhost:8080/fhir/metadata. Remember to adjust your overlay configuration in the application.yaml to the following:

```yaml
    tester:
      -
          id: home
          name: Local Tester
          server_address: 'http://localhost:8080/fhir'
          refuse_to_fetch_third_party_urls: false
          fhir_version: R4
```

## Using this Server

This server currently supports unauthenticated access. You can obtain the
supported endpoints and search parameters for each endpoint by querying the
metadata `[base_url]/metadata` (e.g. <http://localhost:8080/fhir/metadata>).

See [the usage
documentation](https://github.com/Gravity-SDOHCC/sdoh_referral_source_client/blob/master/docs/usage.md)
for instructions on using the whole reference implementations.

## License
Copyright 2023 The MITRE Corporation

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at
```
http://www.apache.org/licenses/LICENSE-2.0
```
Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.

## Trademark Notice

HL7, FHIR and the FHIR [FLAME DESIGN] are the registered trademarks of Health
Level Seven International and their use does not constitute endorsement by HL7.


## Questions and Contributions
Questions about the project can be asked in the [Gravity SDOH stream on the FHIR Zulip Chat](https://chat.fhir.org/#narrow/stream/233957-Gravity-sdoh-cc).

This project welcomes Pull Requests. Any issues identified with the RI should be submitted via the [GitHub issue tracker](https://github.com/Gravity-SDOHCC/gravity-sdoh-cp-server/issues).

As of October 1, 2023, The Lantana Consulting Group is responsible for the management and maintenance of this Reference Implementation.
In addition to posting on FHIR Zulip Chat channel mentioned above you can contact [Corey Spears](mailto:corey.spears@lantanagroup.com) for questions or requests.
