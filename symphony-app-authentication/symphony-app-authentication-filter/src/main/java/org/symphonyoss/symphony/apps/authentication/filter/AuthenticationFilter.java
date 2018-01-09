package org.symphonyoss.symphony.apps.authentication.filter;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.symphony.apps.authentication.jwt.exception.JwtProcessingException;
import org.symphonyoss.symphony.apps.authentication.jwt.JwtService;
import org.symphonyoss.symphony.apps.authentication.jwt.model.JwtPayload;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Web filter to check JWT provided on the authorization header.
 *
 * Created by rsanchez on 09/01/18.
 */
public class AuthenticationFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFilter.class);

  private static final String CACHE_TIMEOUT_PARAM = "cache-timeout";

  private static final String CACHE_SIZE_PARAM = "cache-size";

  private static final String CONNECT_TIMEOUT_PARAM = "connect-timeout";

  private static final String READ_TIMEOUT_PARAM = "read-timeout";

  private static final Integer DEFAULT_CACHE_TIMEOUT = 60;

  private static final Integer DEFAULT_CACHE_SIZE = 1000;

  private static final Integer DEFAULT_CONNECT_TIMEOUT = 5000;

  private static final Integer DEFAULT_READ_TIMEOUT = 5000;

  private static final String AUTHORIZATION_HEADER = "Authorization";

  private static final String AUTHORIZATION_HEADER_PREFIX = "BEARER ";

  private static final String INFO_KEY = "info";

  private static final String MISSING_JWT_MSG = "Missing JWT";

  private static final String INVALID_JWT_MSG = "Invalid JWT";

  private static final String UNEXPECTED_ERROR_MSG = "Unexpected error, please contact the system administrator";

  public static final String USER_INFO_ATTRIBUTE = "user_info";

  private JwtService service;

  @Override
  public void init(FilterConfig config) throws ServletException {
    Integer cacheTimeout = readParameter(config, CACHE_TIMEOUT_PARAM, DEFAULT_CACHE_TIMEOUT);
    Integer cacheSize = readParameter(config, CACHE_SIZE_PARAM, DEFAULT_CACHE_SIZE);
    Integer connectTimeout = readParameter(config, CONNECT_TIMEOUT_PARAM, DEFAULT_CONNECT_TIMEOUT);
    Integer readTimeout = readParameter(config, READ_TIMEOUT_PARAM, DEFAULT_READ_TIMEOUT);

    this.service = new JwtService(cacheTimeout, cacheSize, connectTimeout, readTimeout);
  }

  private Integer readParameter(FilterConfig config, String paramName, int defaultValue) {
    try {
      String param = config.getInitParameter(paramName);

      if (param == null) {
        LOGGER.info("Missing value for parameter " + paramName + ". Using default value " + defaultValue);
        return defaultValue;
      }

      Integer value = Integer.valueOf(param);

      LOGGER.info("Using value " + value + " for parameter " + paramName);

      return value;
    } catch (NumberFormatException e) {
      LOGGER.warn("Invalid value for parameter " + paramName + ". Using default value " + defaultValue);
      return defaultValue;
    }
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    String jwt = getJwt(request);

    if (jwt == null) {
      writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, MISSING_JWT_MSG);
      return;
    }

    try {
      JwtPayload jwtPayload = service.parseJwtPayload(jwt);
      request.setAttribute(USER_INFO_ATTRIBUTE, jwtPayload);

      chain.doFilter(request, servletResponse);
    } catch (JwtProcessingException e) {
      LOGGER.error("Fail to authenticate user", e);
      writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, INVALID_JWT_MSG);
    } catch (Exception e) {
      LOGGER.error("Unexpected error to authenticate user", e);
      writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          UNEXPECTED_ERROR_MSG);
    }
  }

  /**
   * Retrieves JWT from HTTP Authorization header.
   *
   * @param request HTTP request
   * @return JWT or null if the authorization header is not present or it's invalid
   */
  private String getJwt(HttpServletRequest request) {
    String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

    if (authorizationHeader != null && authorizationHeader.toUpperCase()
        .startsWith(AUTHORIZATION_HEADER_PREFIX)) {
      return authorizationHeader.substring(AUTHORIZATION_HEADER_PREFIX.length());
    }

    return null;
  }

  /**
   * Write the http error response.
   *
   * @param response Http response
   * @param status Status code
   * @param message Error message
   */
  private void writeErrorResponse(HttpServletResponse response, int status, String message) {
    response.setContentType(APPLICATION_JSON);
    response.setStatus(status);

    try {
      ObjectNode errorMessage = JsonNodeFactory.instance.objectNode();
      errorMessage.put(INFO_KEY, message);

      response.getWriter().write(errorMessage.toString());
    } catch (IOException e) {
      LOGGER.error("Fail to write error message", e);
    }
  }

  @Override
  public void destroy() {}

}
