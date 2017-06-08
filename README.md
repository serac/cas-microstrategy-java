# CAS Microstrategy Integration for Java

This project provides a servlet filter for authorization, _RequestAttributeFilter_, that complements
[Java CAS Client](https://github.com/apereo/java-cas-client) authentication filters. The basic approach is to define
attributes to extract from the CAS assertion that are converted to HTTP request headers. The filter supports a simple
syntax for decorating simple attribute values; for example, to append a domain name to created a scoped username.

## Building
The build system is Apache Maven, so the `mvn` binary must be available and in your shell environment path. Use the 
following command to build the software:

    mvn clean package

That produces the file target/cas-microstrategy-java-$VERSION.jar in the current directory.

## Configuration
A prerequisite is to determine what attributes are needed from the CAS assertion, and to ensure that they are released
to the Microstrategy application during the CAS ticket validation step. The following configuration steps explain how
to map assertion attributes onto HTTP request headers and then how to make those request headers available to
Microstrategy.

Add the following servlet filter definition to the Microstrategy web.xml file:

    <filter>
        <filter-name>RequestAttributeFilter</filter-name>
        <filter-class>edu.vt.middleware.cas.filter.RequestAttributeFilter</filter-class>
        <init-param>
          <description>Whitespace-delimited list of HTTP request header names.</description>
          <param-name>requestAttributes</param-name>
          <param-value>
            MSY_USER
            MSY_USERDN
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
            uid=[uid],ou=people,dc=vt,dc=edu
          </param-value>
        </init-param>
    </filter>

Use the custom HTTP request headers in custom_security.properties:
    LoginParam=MSY_USER
    DistinguishedName=MSY_USERDN


It is vitally important that some upstream component (e.g. prior servlet filter, reverse proxy)
**strips all client-provided headers set by _RequestAttributeFilter_** this from the request.
Without that protection, clients could easily spoof authorization headers. The following Apache directive
provides an example for a reverse proxy solution:

    <Location /path/to/your/MicroStrategy>
        # Make sure the client does not try to sneak their own values in here
        RequestHeader unset MSY_USER
        RequestHeader unset MSY_USERDN
    </Location>
