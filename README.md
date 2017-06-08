# CAS Microstrategy Integration for Java

This project provides a servlet filter for authorization, _RequestAttributeFilter_, that complements
[Java CAS Client](https://github.com/apereo/java-cas-client) authentication filters. The basic approach is to define
attributes to extract from the CAS assertion that are converted to HTTP request headers. The filter supports a simple
syntax for decorating simple attribute values; for example, to append a domain name to created a scoped username.

It is vitally important that some upstream component (e.g. prior servlet filter, reverse  proxy)
**strips all client-provided headers set by _RequestAttributeFilter_** this from the request.
Without that protection, clients could easily spoof authorization headers.

## Building
The build system is Apache Maven, so the `mvn` binary must be available and in your shell environment path. Use the 
following command to build the software:

    mvn clean package

That produces the file target/cas-microstrategy-java-$VERSION.jar in the current directory.

## Configuration
Add the following servlet filter definition to the Microstrategy web.xml file:

    <filter>
        <filter-name>RequestAttributeFilter</filter-name>
        <filter-class>edu.vt.middleware.cas.filter.RequestAttributeFilter</filter-class>
        <init-param>
          <description>Whitespace-delimited list of HTTP request header names.</description>
          <param-name>requestAttributes</param-name>
          <param-value>
            UNIQUE_ID
            REMOTE_USER
            DISTINGUISHED_NAME
          </param-value>
        </init-param>
        <init-param>
          <description>
            Whitespace-delimited list of assertion attributes or expressions.
            Attribute names must be enclosed in braces if the value includes literal text.
          </description>
          <param-name>assertionAttributes</param-name>
          <param-value>
            uid
            [user]@vt.edu
            uid=[uid],ou=people,dc=vt,dc=edu
          </param-value>
        </init-param>
    </filter>

The attributes mentioned in the _assertionAttributes_ parameter must be available in the CAS assertion, which is an
external configuration concern.
