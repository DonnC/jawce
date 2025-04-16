package zw.co.dcl.jawce.engine.service;

import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import zw.co.dcl.jawce.engine.constants.EngineConstants;
import zw.co.dcl.jawce.engine.constants.SessionConstants;
import zw.co.dcl.jawce.engine.exceptions.EngineInternalException;
import zw.co.dcl.jawce.engine.exceptions.EngineResponseException;
import zw.co.dcl.jawce.engine.exceptions.EngineWhatsappException;
import zw.co.dcl.jawce.engine.model.abs.AbsHookArg;
import zw.co.dcl.jawce.engine.model.core.HookArg;
import zw.co.dcl.jawce.engine.model.core.HookArgRest;
import zw.co.dcl.jawce.engine.model.dto.*;
import zw.co.dcl.jawce.engine.model.mappers.EngineDtoMapper;
import zw.co.dcl.jawce.engine.utils.CommonUtils;
import zw.co.dcl.jawce.session.ISessionManager;

import java.lang.reflect.Method;
import java.util.List;

import static zw.co.dcl.jawce.engine.constants.EngineConstants.TIMEOUT_REQUEST_RETRY_COUNT;

public class RequestService {
    private static volatile RequestService instance;
    private final Logger logger = LoggerFactory.getLogger(RequestService.class);

    private final WaEngineConfig config;
    private final EngineDtoMapper dtoMapper;
    private final RestTemplate restTemplate;

    private RequestService(WaEngineConfig config) {
        this.config = config;
        this.dtoMapper = Mappers.getMapper(EngineDtoMapper.class);
        this.restTemplate = config.client();
    }

    public static RequestService getInstance(WaEngineConfig config) {
        if(instance == null) {
            synchronized (RequestService.class) {
                if(instance == null) {
                    instance = new RequestService(config);
                }
            }
        }
        return instance;
    }

    private ResponseEntity<String> sendWaRequest(ChannelOriginConfig channelOriginConfig, OnceOffRequestDto dto) {
        if(!config.settings().isLocalTesting()) {
            if(config.settings().getAccessToken() == null ||
                    config.settings().getApiVersion() == null ||
                    config.settings().getPhoneNumberId() == null)
                throw new EngineInternalException("could not get channel configs");
        }

        if(channelOriginConfig != null) {
            if(channelOriginConfig.whitelistedNumbers() instanceof List allowedNumbers) {
                if(!allowedNumbers.contains(dto.getRecipient())) {
                    logger.warn("{}: not whitelisted", dto.getRecipient());
                    return null;
                }
            }

            if(channelOriginConfig.whitelistedNumbers() instanceof String matcher) {
                if(!matcher.equals("*")) {
                    logger.warn("Message processing is disabled");
                    return null;
                }
            }
        }

        String url = config.settings().isLocalTesting() ?
                config.settings().getLocalTestingRequestUrl() :
                EngineConstants.CHANNEL_BASE_URL
                        + config.settings().getApiVersion() + "/"
                        + config.settings().getPhoneNumberId()
                        + EngineConstants.CHANNEL_MESSAGE_SUFFIX;

        HttpHeaders fwdHeaders = new HttpHeaders();
        fwdHeaders.setContentType(MediaType.APPLICATION_JSON);
        fwdHeaders.setBearerAuth(config.settings().getAccessToken());

        return restTemplate.postForEntity(
                url,
                new HttpEntity<>(dto.getPayload(), fwdHeaders),
                String.class
        );
    }

    public AbsHookArg processHook(String hook, HookArg args) throws Exception {
        var sessionId = args.getWaUser().waId();
        logger.warn("PROCESSING HOOK: {}", hook);

        if(hook.startsWith(EngineConstants.TPL_REST_HOOK_PLACEHOLDER_KEY)) {
            String endpoint = CommonUtils.getDataDatumArgs(EngineConstants.TPL_REST_HOOK_PLACEHOLDER_KEY, hook).datum();
            String hookResult = processRestHook(sessionId, endpoint, args);

            if(hookResult == null) throw new EngineInternalException("hook rest request returned null");
            var responseArg = CommonUtils.convertResponseToHookObj(hookResult);
            if(responseArg instanceof String result) throw new EngineInternalException(result);
            return (AbsHookArg) responseArg;
        }

        var response = processReflectiveHook(hook, args);

        if(response == null) throw new EngineInternalException("reflection hook returned null");
        if(response instanceof String result) throw new EngineInternalException(result);

        return dtoMapper.map((HookArg) response);
    }

    /**
     * reflective calls already have access to session obj
     * no need to process response body
     *
     * @param hook:     full path to cls method
     * @param hookArgs: engine Hook to pass downstream
     * @return HookArg: return the same passed HookArg
     * @throws Exception: reflective api exceptions
     */
    Object processReflectiveHook(String hook, HookArg hookArgs) throws Exception {
        var clsLoader = Thread.currentThread().getContextClassLoader();

        DataDatumDTO args = CommonUtils.getDataDatumArgs(EngineConstants.REFL_CLS_METHOD_SPLITTER, hook);
        var classNamePath = args.data();
        var classMethodName = args.datum();

        try {
            Class<?> hookClass = clsLoader.loadClass(classNamePath);
            Object hookObj = hookClass.getDeclaredConstructor(HookArg.class).newInstance(hookArgs);
            Method hookMethod = hookClass.getDeclaredMethod(classMethodName);
            return hookMethod.invoke(hookObj);
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load class with context class loader, attempting default class loader");
            Class<?> hookClass = Class.forName(classNamePath);
            Object hookObj = hookClass.getDeclaredConstructor(HookArg.class).newInstance(hookArgs);
            Method hookMethod = hookClass.getDeclaredMethod(classMethodName);
            return hookMethod.invoke(hookObj);
        }
    }

    /**
     * @param endpoint:  rest api hook endpoint
     * @param argsParam: engine Hook to pass downstream
     * @return String: <str>HookArgRest
     */
    String processRestHook(String sessionId, String endpoint, HookArg argsParam) {
        HookArgRest args = dtoMapper.map(argsParam);
        var userSession = argsParam.getSession();

        if(config.requestSettings().baseUrl() == null || restTemplate == null)
            throw new EngineInternalException("could not get channel request configs");

        try {
            String url = endpoint.startsWith("http")
                    ? endpoint :
                    config.requestSettings().baseUrl() + endpoint;

            HttpHeaders fwdHeaders = new HttpHeaders();
            fwdHeaders.setContentType(MediaType.APPLICATION_JSON);
            fwdHeaders.set(EngineConstants.JAWCE_RHOOK_SESSION_HEADER_KEY, sessionId);

            if(userSession.get(sessionId, SessionConstants.HOOK_USER_SESSION_ACCESS_TOKEN, String.class) != null) {
                fwdHeaders.setBearerAuth(userSession.get(sessionId, SessionConstants.HOOK_USER_SESSION_ACCESS_TOKEN, String.class));
            } else {
                if(config.requestSettings().authorizationToken() != null) {
                    fwdHeaders.set("Authorization", config.requestSettings().authorizationToken());
                }
            }

            ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(args, fwdHeaders), String.class);

            if(response.getStatusCodeValue() == 200) {
                return response.getBody();
            } else {
                logger.warn("hook call error response, body: {}", response.getBody());
                throw new EngineInternalException("There was a problem in hook endpoint call.");
            }
        } catch (Exception err) {
            logger.error("hook call exception: {} | msg: {}", err.getClass().getSimpleName(), err.getMessage());
            throw new EngineInternalException("failed to process hook REST request", err);
        }
    }

    //    no need to user session
    public String sendOnceOffWhatsappRequest(OnceOffRequestDto requestDto, ChannelOriginConfig channelOriginConfig) {
        try {
            ResponseEntity<String> response = sendWaRequest(channelOriginConfig, requestDto);

            assert response != null;

            if(response.getStatusCodeValue() == 200) {
                if(CommonUtils.isValidChannelResponse(response.getBody())) {
                    return response.getBody();
                }

                logger.warn("WA REQUEST SUCCESS BUT INVALID CHANNEL RESPONSE: {}", response.getBody());

                throw new EngineInternalException("Request success but received invalid channel upstream response");
            } else {
                logger.error("channel error response, code: {}, body: {}", response.getStatusCode(), response.getBody());
                throw new EngineInternalException("There was a problem. Unsuccessful channel response code");
            }
        } catch (HttpClientErrorException e) {
            logger.error("Request exception: {}", e.getMessage());

            if(e.getRawStatusCode() == 401) {
                logger.error("WA.OF AUTHORIZATION error: {}", e.getResponseBodyAsString());
                throw new EngineWhatsappException("Unauthorized access to WhatsAppConfig server. Check credentials");
            }

            if(e.getRawStatusCode() == 400) {
                logger.error("WA.OF BAD REQUEST: {}", e.getResponseBodyAsString());
                throw new EngineWhatsappException("Bad request to WhatsAppConfig Cloud server. Check request data");
            }

            throw new EngineInternalException("failed to process w.cloud request", e);
        } catch (EngineResponseException e) {
            throw e;
        } catch (Exception err) {
            logger.error("WA.OF REQUEST EXCEPTION: {} | msg: {}", err.getClass().getSimpleName(), err.getMessage());
            throw new EngineInternalException("Failed to process Whatsapp Cloud request", err);
        } finally {
            MDC.remove(EngineConstants.MDC_ID_KEY);
        }
    }

    public String sendWhatsappRequest(ChannelRequestDto requestDto, boolean handleSession, ChannelOriginConfig
            channelOriginConfig) {
        ISessionManager session = requestDto.session();
        String recipient = requestDto.response().recipient();

        try {
            ResponseEntity<String> response = sendWaRequest(
                    channelOriginConfig,
                    OnceOffRequestDto.builder()
                            .recipient(recipient)
                            .payload(requestDto.response().payload())
                            .build()
            );

            assert response != null;

            if(response.getStatusCodeValue() == 200) {
                if(CommonUtils.isValidChannelResponse(response.getBody())) {
                    if(handleSession) {
                        session.evict(recipient, SessionConstants.CURRENT_STAGE_RETRY_COUNT);
                        var stageCode = session.get(recipient, SessionConstants.CURRENT_STAGE);
                        session.save(recipient, SessionConstants.PREV_STAGE, stageCode);
                        session.save(recipient, SessionConstants.CURRENT_STAGE, requestDto.response().nextRoute());
                        logger.info("Current route set to: {}", requestDto.response().nextRoute());
                    }
                    if(config.sessionSettings().isHandleSessionInactivity()) {
                        session.save(
                                recipient,
                                SessionConstants.LAST_ACTIVITY_KEY,
                                CommonUtils.formatZonedDateTime(CommonUtils.currentSystemDate())
                        );
                    }
                }
                return response.getBody();
            } else {
                logger.error("CHANNEL ERROR. Code: {} | Body: {}", response.getStatusCode(), response.getBody());
                throw new EngineInternalException("There was a problem. Unsuccessful channel response code");
            }
        } catch (HttpClientErrorException e) {
            logger.error("Request exception: {}", e.getMessage());

            if(e.getRawStatusCode() == 401) {
                logger.error("WA AUTHENTICATION error: {}", e.getResponseBodyAsString());
                throw new EngineWhatsappException("Unauthorized access to WhatsAppConfig server. Check credentials");
            }

            if(e.getRawStatusCode() == 400) {
                logger.error("WA Bad Request: {}", e.getResponseBodyAsString());
                throw new EngineWhatsappException("Bad request to WhatsAppConfig Cloud server. Check request data");
            }

            handleSessionOnRequestExc(session, recipient, handleSession);
            throw new EngineInternalException("failed to process w.cloud request", e);
        } catch (ResourceAccessException e) {
            Throwable rootCause = e.getRootCause();
            if(rootCause instanceof java.net.ConnectException connectException) {
                if(e.getMessage() != null && e.getMessage().contains("timed out")) {
                    if(shouldRetryRequest(session, recipient, handleSession)) {
                        return this.sendWhatsappRequest(requestDto, true, channelOriginConfig);
                    } else logger.warn("Whatsapp cloud request retries exceeded!");
                }
            }

            handleSessionOnRequestExc(session, recipient, handleSession);
            throw new EngineInternalException("Whatsapp cloud request caught RAE exception", e);
        } catch (EngineResponseException e) {
            throw e;
        } catch (Exception err) {
            logger.error("WA Request Error: {} | msg: {}", err.getClass().getSimpleName(), err.getMessage());
            handleSessionOnRequestExc(session, recipient, handleSession);
            throw new EngineInternalException("Failed to process Whatsapp Cloud request", err);
        } finally {
            MDC.remove(EngineConstants.MDC_ID_KEY);
        }
    }

    void handleSessionOnRequestExc(ISessionManager session, String recipient, boolean handleSession) {
        if(handleSession) {
            assert config.sessionSettings() != null;

            if(session
                    .get(recipient, SessionConstants.PREV_STAGE)
                    .toString()
                    .equalsIgnoreCase(config.sessionSettings().getStartMenuStageKey()) ||
                    session
                            .get(recipient, SessionConstants.CURRENT_STAGE)
                            .toString()
                            .equalsIgnoreCase(config.sessionSettings().getStartMenuStageKey())
            ) {
                logger.warn("WA request exception - clearing session");
                session.clear(recipient);
            } else {
                session.save(recipient, SessionConstants.CURRENT_STAGE, session.get(recipient, SessionConstants.PREV_STAGE));
            }
        }
    }

    boolean shouldRetryRequest(ISessionManager session, String recipient, boolean handleSession) {
        if(handleSession) {
            logger.warn("Attempting to retry w.cloud request..");
            assert config.sessionSettings() != null;
            var retry = session.get(recipient, SessionConstants.CURRENT_STAGE_RETRY_COUNT, Integer.class);

            if(retry == null) {
                session.save(recipient, SessionConstants.CURRENT_STAGE_RETRY_COUNT, 1);
                return true;
            }

            if(retry <= TIMEOUT_REQUEST_RETRY_COUNT) {
                session.save(recipient, SessionConstants.CURRENT_STAGE_RETRY_COUNT, retry + 1);
                return true;
            }
            return false;
        }
        return false;
    }
}
