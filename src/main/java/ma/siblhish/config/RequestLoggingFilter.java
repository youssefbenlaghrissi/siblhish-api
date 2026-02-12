package ma.siblhish.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        
        log.info("➡️  {} {}", request.getMethod(), request.getRequestURI());
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            String statusIcon = status >= 200 && status < 300 ? "✅" : 
                              status >= 400 && status < 500 ? "⚠️ " : "❌";
            
            log.info("{} {} {} - {}ms - Status: {}", 
                    statusIcon,
                    request.getMethod(), 
                    request.getRequestURI(),
                    duration,
                    status);
        }
    }
}

