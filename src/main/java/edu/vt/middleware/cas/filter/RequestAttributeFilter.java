/* See LICENSE and NOTICE files for license, copyright, and other legal information. */
package edu.vt.middleware.cas.filter;

import java.io.IOException;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.validation.Assertion;

/**
 * Servlet filter that sets request attributes based on prinicpal attributes found in the CAS assertion.
 * The filter accepts the following initialization parameters.
 *
 * <p>
 * <em>requestAttributes</em><br>
 * Whitespace-delimited list of request attribute names. Example:
 * <pre>
     <init-param>
       <description>Whitespace-delimited list of request attribute names.</description>
       <param-name>requestAttributes</param-name>
       <param-value>
         UNIQUE_ID
         REMOTE_USER
         DISTINGUISHED_NAME
       </param-value>
     </init-param>
   </pre>
 *
 * <p>
 * <em>assertionAttributes</em><br>
 * Space-delimited list of assertion attributes or expressions that combine an attribute
 * name with a literal value. Example:
 * <pre>
    <init-param>
       <description>Whitespace-delimited list of assertion attributes or expressions.</description>
       <param-name>assertionAttributes</param-name>
       <param-value>
         uid
         uid=[uid],ou=people,dc=vt,dc=edu
         [user]@vt.edu
       </param-value>
     </init-param>
   </pre>
 *
 * Note that the attribute name in the expression MUST be enclosed in brackets.
 *
 * <p>
 * An initialization error will occur if the number of requestAttributes and assertionAttributes
 * do not match exactly.
 *
 * @author  Middleware Services
 */
public class RequestAttributeFilter implements Filter {

  /** Array of request attribute names that will be set. */
  private String[] requestAttributes;

  /** Array of assertion attribute descriptors. */
  private AttributeDescriptor[] assertionAttributes;


  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    final String p1  = filterConfig.getInitParameter("requestAttributes");
    if (p1 == null) {
      throw new IllegalArgumentException("No value defined for requestAttributes");
    }
    requestAttributes = p1.split("\\s+");

    final String p2 = filterConfig.getInitParameter("assertionAttributes");
    if (p2 == null) {
      throw new IllegalArgumentException("No value defined for assertionAttributes");
    }
    final String[] expressions = p2.split("\\s+");
    if (expressions.length != requestAttributes.length) {
      throw new IllegalArgumentException("Size of requestAttributes != assertionAttributes");
    }
    assertionAttributes = new AttributeDescriptor[expressions.length];
    int i = 0;
    for (String expression : expressions) {
      assertionAttributes[i++] = new AttributeDescriptor(expression);
    }
  }


  @Override
  public void doFilter(
      final ServletRequest servletRequest,
      final ServletResponse servletResponse,
      final FilterChain filterChain) throws IOException, ServletException {

    final HttpServletRequest request = (HttpServletRequest) servletRequest;
    Assertion assertion = (Assertion) request.getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION);
    if (assertion == null) {
      // Try session
      assertion = (Assertion) request.getSession(false).getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION);
    }
    if (assertion != null) {
      int i = 0;
      for (String attr : requestAttributes) {
        request.setAttribute(attr, assertionAttributes[i++].evaluate(assertion));
      }
    }
    filterChain.doFilter(servletRequest, servletResponse);
  }


  @Override
  public void destroy() {}


  /** Describes assertion attributes. */
  private static class AttributeDescriptor {
    /** Attribute name. */
    private final String name;

    /** Format expression. */
    private final String formatExpression;


    /**
     * Creates a formatted attribute.
     *
     * @param  expression  Attribute name or expression containing an attribute name surrounded by
     *                     brackets and a literal expression.
     */
    public AttributeDescriptor(final String expression) {
      final int ia = expression.indexOf('[');
      final int ib = expression.indexOf(']');
      if (ia < 0 || ib < 0) {
        this.name = expression;
        this.formatExpression = null;
      } else {
        if (ib - ia <= 1) {
          throw new IllegalArgumentException("Invalid expression. No attribute name found between braces.");
        }
        this.name = expression.substring(ia + 1, ib);
        this.formatExpression = expression.replace("[" + this.name + "]", "%s");
      }
    }


    /**
     * Evaluates an assertion attribute by name and returns its (formatted) value.
     *
     * @param  assertion  CAS assertion containing attribute name/value pairs.
     *
     * @return  The first (formatted) value of the given attribute.
     */
    public Object evaluate(final Assertion assertion) {
      Object value = assertion.getPrincipal().getAttributes().get(name);
      if (value instanceof List) {
        value = ((List) value).get(0);
      }
      if (formatExpression != null) {
        return String.format(formatExpression, value);
      }
      return value;
    }
  }
}
