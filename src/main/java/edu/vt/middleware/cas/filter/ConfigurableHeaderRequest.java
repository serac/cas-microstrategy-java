/* See LICENSE and NOTICE files for license, copyright, and other legal information. */
package edu.vt.middleware.cas.filter;

import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Servlet request wrapper that allows adding request headers.
 *
 * @author  Middleware Services
 */
public class ConfigurableHeaderRequest extends HttpServletRequestWrapper {

  /** Additional request headers. */
  private final Map<String, List<String>> additionalHeaders = new LinkedHashMap<String, List<String>>();


  /**
   * Create new instance that wraps the given request.
   *
   * @param request Request to wrap.
   */
  public ConfigurableHeaderRequest(HttpServletRequest request) {
    super(request);
  }


  /**
   * Adds a request header name/value pair.
   *
   * @param  name  Header name.
   * @param  value  Header value.
   */
  public void addHeader(final String name, final String value) {
    List<String> values = additionalHeaders.get(name);
    if (values == null) {
      values = new ArrayList<String>();
      additionalHeaders.put(name, values);
    }
    values.add(value);
  }


  @Override
  public String getHeader(final String name) {
    String value = super.getHeader(name);
    if (value != null) {
      return value;
    }
    final List<String> values = additionalHeaders.get(name);
    if (values != null && !values.isEmpty()) {
      value = values.get(0);
    }
    return value;
  }


  @Override
  public Enumeration getHeaders(final String name) {
    final List<String> union = new ArrayList<String>();
    final Enumeration e = super.getHeaders(name);
    while (e.hasMoreElements()) {
      union.add((String) e.nextElement());
    }
    union.addAll(additionalHeaders.get(name));
    return Collections.enumeration(union);
  }


  @Override
  public Enumeration getHeaderNames() {
    final List<String> union = new ArrayList<String>();
    final Enumeration e = super.getHeaderNames();
    while (e.hasMoreElements()) {
      union.add((String) e.nextElement());
    }
    union.addAll(additionalHeaders.keySet());
    return Collections.enumeration(union);
  }
}
