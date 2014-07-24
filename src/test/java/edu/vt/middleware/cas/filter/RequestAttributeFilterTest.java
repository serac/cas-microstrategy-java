/* See LICENSE and NOTICE files for license, copyright, and other legal information. */
package edu.vt.middleware.cas.filter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.jasig.cas.client.authentication.AttributePrincipalImpl;
import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.AssertionImpl;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link RequestAttributeFilter}.
 */
public class RequestAttributeFilterTest {

  @Test(expected = IllegalArgumentException.class)
  public void testInitFailNoRequestAttributes() throws Exception {
    final FilterConfig mockConfig = mock(FilterConfig.class);
    when(mockConfig.getInitParameter("assertionAttributes")).thenReturn("uid [user]@vt.edu uid=[uid],ou=people,dc=vt,dc=edu");
    new RequestAttributeFilter().init(mockConfig);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInitFailNoAssertionAttributes() throws Exception {
    final FilterConfig mockConfig = mock(FilterConfig.class);
    when(mockConfig.getInitParameter("requestAttributes")).thenReturn("UNIQUE_ID REMOTE_USER DISTINGUISHED_NAME");
    new RequestAttributeFilter().init(mockConfig);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInitFailSizeNotEqual() throws Exception {
    final FilterConfig mockConfig = mock(FilterConfig.class);
    when(mockConfig.getInitParameter("requestAttributes")).thenReturn("UNIQUE_ID REMOTE_USER DISTINGUISHED_NAME");
    when(mockConfig.getInitParameter("assertionAttributes")).thenReturn("uid [user]@vt.edu");
    new RequestAttributeFilter().init(mockConfig);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInitFailBadExpression() throws Exception {
    final FilterConfig mockConfig = mock(FilterConfig.class);
    when(mockConfig.getInitParameter("requestAttributes")).thenReturn("UNIQUE_ID REMOTE_USER DISTINGUISHED_NAME");
    when(mockConfig.getInitParameter("assertionAttributes")).thenReturn("uid []@vt.edu uid=[uid],ou=people,dc=vt,dc=edu");
    new RequestAttributeFilter().init(mockConfig);
  }

  @Test
  public void testDoFilterSuccess() throws Exception {
    final RequestAttributeFilter filter = new RequestAttributeFilter();

    // Mock filter config
    final FilterConfig mockConfig = mock(FilterConfig.class);
    when(mockConfig.getInitParameter("requestAttributes")).thenReturn("UNIQUE_ID REMOTE_USER DISTINGUISHED_NAME");
    when(mockConfig.getInitParameter("assertionAttributes")).thenReturn("uid [user]@vt.edu uid=[uid],ou=people,dc=vt,dc=edu");

    filter.init(mockConfig);

    // Test assertion
    final Map<String, Object> attributes = new HashMap<String, Object>();
    attributes.put("uid", "123");
    attributes.put("user", Collections.singletonList("bob"));
    final Assertion testAssertion = new AssertionImpl(new AttributePrincipalImpl("bob", attributes));

    // Mock request
    final ConfigurableHeaderRequest mockRequest = mock(ConfigurableHeaderRequest.class);
    when(mockRequest.getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION)).thenReturn(testAssertion);

    // Mock filter chain containing assertions
    final FilterChain mockChain = mock(FilterChain.class);
    doAnswer(new Answer() {
      @Override
      public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
        final Object[] args = invocationOnMock.getArguments();
        assertTrue(args[0] instanceof ConfigurableHeaderRequest);
        final ConfigurableHeaderRequest request = (ConfigurableHeaderRequest) args[0];
        assertEquals("123", request.getHeader("UNIQUE_ID"));
        assertEquals("bob@vt.edu", request.getHeader("REMOTE_USER"));
        assertEquals("uid=123,ou=people,dc=vt,dc=edu", request.getHeader("DISTINGUISHED_NAME"));
        return null;
      }
    }).when(mockChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

    filter.doFilter(mockRequest, mock(HttpServletResponse.class), mockChain);
  }
}