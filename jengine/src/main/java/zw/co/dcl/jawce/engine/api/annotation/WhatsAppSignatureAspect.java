package zw.co.dcl.jawce.engine.api.annotation;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import zw.co.dcl.jawce.engine.configs.WhatsAppConfig;
import zw.co.dcl.jawce.engine.internal.service.WhatsAppSignatureVerifier;

import java.util.stream.Collectors;

@Aspect
@Component
public class WhatsAppSignatureAspect {
    private final WhatsAppSignatureVerifier signatureVerifier;
    private final WhatsAppConfig config;

    public WhatsAppSignatureAspect(WhatsAppSignatureVerifier signatureVerifier,
                                   WhatsAppConfig config) {
        this.signatureVerifier = signatureVerifier;
        this.config = config;
    }

    @Around("@annotation(VerifyWhatsAppPayload)")
    public Object verifySignature(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(attrs == null) {
            throw new IllegalStateException("Cannot access request attributes");
        }

        HttpServletRequest request = attrs.getRequest();
        String signature = request.getHeader("X-Hub-Signature-256");
        String rawBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

        boolean isValid = signatureVerifier.isValid(rawBody, config.getAppSecret(), signature);

        if(!isValid) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid WhatsApp signature");
        }

        return joinPoint.proceed();
    }
}
