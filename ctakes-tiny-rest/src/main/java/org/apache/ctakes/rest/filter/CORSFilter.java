package org.apache.ctakes.rest.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class CORSFilter extends OncePerRequestFilter {
   private static final Log log = LogFactory.getLog( CORSFilter.class );

   @Override
   protected void doFilterInternal( jakarta.servlet.http.HttpServletRequest request,
                                    jakarta.servlet.http.HttpServletResponse response,
                                    jakarta.servlet.FilterChain filterChain ) throws jakarta.servlet.ServletException, IOException {
      log.debug( "Invoking CORS filter" );
      response.addHeader( "Access-Control-Allow-Origin", "*" );
      if ( request.getHeader( "Access-Control-Request-Method" ) != null && "OPTIONS".equals( request.getMethod() ) ) {
         response.addHeader( "Access-Control-Allow-Methods", "GET, POST, PUT, DELETE" );
         response.addHeader( "Access-Control-Allow-Headers", "Content-Type" );
      }
      filterChain.doFilter( request, response );
      log.debug( "Exiting CORS filter" );
   }

}