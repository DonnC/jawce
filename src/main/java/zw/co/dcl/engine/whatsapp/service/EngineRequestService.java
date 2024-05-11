package zw.co.dcl.engine.whatsapp.service;

import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import zw.co.dcl.engine.whatsapp.constants.EngineConstants;
import zw.co.dcl.engine.whatsapp.constants.SessionConstants;
import zw.co.dcl.engine.whatsapp.entity.DefaultHookArgs;
import zw.co.dcl.engine.whatsapp.entity.dto.*;
import zw.co.dcl.engine.whatsapp.entity.mappers.EngineDtoMapper;
import zw.co.dcl.engine.whatsapp.exceptions.EngineInternalException;
import zw.co.dcl.engine.whatsapp.exceptions.EngineWhatsappException;
import zw.co.dcl.engine.whatsapp.service.iface.ISessionManager;
import zw.co.dcl.engine.whatsapp.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.List;

import static zw.co.dcl.engine.whatsapp.constants.EngineConstants.TIMEOUT_REQUEST_RETRY_COUNT;

public class EngineRequestService {
    private final Logger logger = LoggerFactory.getLogger(EngineRequestService.class);
    private final WaEngineConfig config;
    private final EngineDtoMapper dtoMapper;
    private final RestTemplate restTemplate;

    public EngineRequestService(WaEngineConfig config) {
        this.config = config;
        this.dtoMapper = Mappers.getMapper(EngineDtoMapper.class);
        this.restTemplate = new RestTemplate();
    }

    public DefaultHookArgs processHook(String hook, HookArgs args) throws Exception {
        if (hook.startsWith(EngineConstants.TPL_REST_HOOK_PLACEHOLDER_KEY)) {
            String endpoint = CommonUtils.getDataDatumArgs(EngineConstants.TPL_REST_HOOK_PLACEHOLDER_KEY, hook).datum();
            String hookResult = processRestHook(endpoint, args);

            if (hookResult == null) throw new EngineInternalException("hook rest request returned null");
            var responseArg = CommonUtils.convertResponseToHookObj(hookResult);
            if (responseArg instanceof String result) throw new EngineInternalException(result);

            return (DefaultHookArgs) responseArg;
        }

        var response = processReflectiveHook(hook, args);

        if (response == null) throw new EngineInternalException("refl hook returned null");
        if (response instanceof String result) throw new EngineInternalException(result);

        return dtoMapper.map((HookArgs) response);
    }

    /**
     * reflective calls already have access to session obj
     * no need to process response body
     *
     * @param hook:     full path to cls method
     * @param hookArgs: engine Hook to pass downstream
     * @return HookArgs: return the same passed HookArg
     * @throws Exception: reflective api exceptions
     */
    Object processReflectiveHook(String hook, HookArgs hookArgs) throws Exception {
        DataDatumDTO args = CommonUtils.getDataDatumArgs(EngineConstants.REFL_CLS_METHOD_SPLITTER, hook);
        Class<?> hookClass = Class.forName(args.data());
        Object hookObj = hookClass.getDeclaredConstructor(HookArgs.class).newInstance(hookArgs);
        Method hookMethod = hookClass.getDeclaredMethod(args.datum());
        return hookMethod.invoke(hookObj);
    }

    /**
     * @param endpoint:  rest api hook endpoint
     * @param argsParam: engine Hook to pass downstream
     * @return String: <str>HookArgsRest
     */
    String processRestHook(String endpoint, HookArgs argsParam) {
        HookArgsRest args = dtoMapper.map(argsParam);

        if (config.requestSettings().baseUrl() == null || restTemplate == null)
            throw new EngineInternalException("could not get channel request configs");

        if (config.requestSettings().authorizationToken() == null)
            logger.warn("No hook request auth token. Provide token for extra security!");

        try {
            String url = endpoint.startsWith("http")
                    ? endpoint :
                    config.requestSettings().baseUrl() + endpoint;

            HttpHeaders fwdHeaders = new HttpHeaders();
            fwdHeaders.setContentType(MediaType.APPLICATION_JSON);
            fwdHeaders.set(EngineConstants.X_WA_ENGINE_HEADER_KEY, config.requestSettings().authorizationToken());

            if (argsParam.getSession().get(SessionConstants.HOOK_USER_SESSION_ACCESS_TOKEN, String.class) != null)
                fwdHeaders.setBearerAuth(argsParam.getSession().get(SessionConstants.HOOK_USER_SESSION_ACCESS_TOKEN, String.class));

            ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(args, fwdHeaders), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                logger.warn("hook call error response, body: {}", response.getBody());
                throw new EngineInternalException("There was a problem in hook endpoint call.");
            }
        } catch (Exception err) {
            logger.error("hook call exc: {} | msg: {}", err.getClass().getSimpleName(), err.getMessage());
            throw new EngineInternalException("failed to process hook REST request", err);
        }
    }

    String uploadMedia(MultiValueMap<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            String url = EngineConstants.CHANNEL_BASE_URL + config.settings().getApiVersion() + "/" + config.settings().getPhoneNumberId() + EngineConstants.CHANNEL_MEDIA_SUFFIX;

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("RESPONSE: {}", response.getBody());
                if (CommonUtils.objectToMap(response.getBody()).containsKey("id"))
                    return CommonUtils.objectToMap(response.getBody()).get("id").toString();
            }

            throw new EngineInternalException("Failed to upload media to WA server: " + response.getStatusCode());
        } catch (Exception e) {
            logger.error("Failed to upload media to WhatsApp cloud", e);
            throw new EngineInternalException("Error uploading media to WA server: " + e.getMessage());
        }
    }

    /**
     * @param file File obj to upload
     * @return the uploaded media ID on success
     */
    public String uploadWhatsappMedia(File file) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("messaging_product", "whatsapp");
            body.add("type", Files.probeContentType(file.toPath()));
            body.add("file", new FileSystemResource(file));

            return uploadMedia(body);
        } catch (IOException e) {
            logger.error("Failed to probe file content type", e);
            throw new EngineInternalException("Failed to probe file content type");
        }
    }

    /**
     * @param file Multipart obj to upload
     * @return the uploaded media ID on success
     */
    public String uploadWhatsappMedia(MultipartFile file) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("messaging_product", "whatsapp");
        body.add("type", file.getContentType());
        body.add("file", file.getResource());

        return uploadMedia(body);
    }

    public String sendWhatsappRequest(ChannelRequestDto requestDto, boolean handleSession, ChannelOriginConfig channelOriginConfig) {
        if (!config.settings().isLocalTesting()) {
            if (config.settings().getAccessToken() == null || config.settings().getApiVersion() == null || config.settings().getPhoneNumberId() == null)
                throw new EngineInternalException("could not get channel configs");
        }

        try {
            if (channelOriginConfig != null) {
                if (channelOriginConfig.whitelistedNumbers() instanceof List allowedNumbers) {
                    if (!allowedNumbers.contains(requestDto.response().recipient())) {
                        logger.warn("PROCESS MSG: {} is not whitelisted", requestDto.response().recipient());
                        return null;
                    }
                }

                if (channelOriginConfig.whitelistedNumbers() instanceof String matcher) {
                    if (!matcher.equals("*")) {
                        logger.warn("PROCESS MSG: any number is not whitelisted for processing!");
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

//            logger.info("whatsapp payload: {}", requestDto.response().payload());

            ResponseEntity<String> response = restTemplate
                    .postForEntity(
                            url,
                            new HttpEntity<>(CommonUtils.decodePayload(requestDto.response().payload()), fwdHeaders),
                            String.class
                    );

            if (response.getStatusCode().is2xxSuccessful()) {
                if (CommonUtils.isValidChannelResponse(response.getBody())) {
                    if (handleSession) {
                        requestDto.session().evict(SessionConstants.CURRENT_STAGE_RETRY_COUNT);
                        var stageCode = requestDto.session().get(SessionConstants.CURRENT_STAGE);
                        requestDto.session().save(SessionConstants.PREV_STAGE, stageCode);
                        requestDto.session().save(SessionConstants.CURRENT_STAGE, requestDto.response().nextRoute());
                    }
                    if (requestDto.session() != null && config.sessionSettings().isHandleSessionInactivity())
                        requestDto.session().save(
                                SessionConstants.LAST_ACTIVITY_KEY,
                                CommonUtils.formatZonedDateTime(CommonUtils.currentSystemDate())
                        );
                }
                return response.getBody();
            } else {
                logger.error("channel error response, code: {}, body: {}", response.getStatusCode(), response.getBody());
                throw new EngineInternalException("There was a problem. Unsuccessful channel response code");
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                logger.error("w.cloud authorization error: " + e.getResponseBodyAsString());
                throw new EngineWhatsappException("Unauthorized access to WhatsApp server. Check credentials");
            }

            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                logger.error("w.cloud bad request: " + e.getResponseBodyAsString());
                throw new EngineWhatsappException("Bad request to WhatsApp Cloud server. Check request data");
            }

            handleSessionOnRequestExc(requestDto, handleSession);
            throw new EngineInternalException("failed to process w.cloud request", e);
        } catch (ResourceAccessException e) {
            Throwable rootCause = e.getRootCause();
            if (rootCause instanceof java.net.ConnectException connectException) {
                logger.warn("Connection exception occurred: {}", connectException.getMessage());
                if (e.getMessage() != null && e.getMessage().contains("timed out")) {
                    if (shouldRetryRequest(requestDto, handleSession)) {
                        logger.warn("Attempting to retry w.cloud request..");
                        return this.sendWhatsappRequest(requestDto, true, channelOriginConfig);
                    } else logger.warn("Whatsapp cloud request retries exceeded!");
                }
            }

            handleSessionOnRequestExc(requestDto, handleSession);
            throw new EngineInternalException("Whatsapp cloud request caught RAE exc", e);
        } catch (Exception err) {
            logger.error("Whatsapp cloud request exc: {} | msg: {}", err.getClass().getSimpleName(), err.getMessage());
            handleSessionOnRequestExc(requestDto, handleSession);
            throw new EngineInternalException("failed to process Whatsapp Cloud request", err);
        }
    }

    void handleSessionOnRequestExc(ChannelRequestDto requestDto, boolean handleSession) {
        if (handleSession) {
            assert config.sessionSettings() != null;
            if (requestDto
                    .session()
                    .get(SessionConstants.PREV_STAGE)
                    .toString()
                    .equalsIgnoreCase(config.sessionSettings().getStartMenuStageKey()) ||
                    requestDto
                            .session()
                            .get(SessionConstants.CURRENT_STAGE)
                            .toString()
                            .equalsIgnoreCase(config.sessionSettings().getStartMenuStageKey())
            ) {
                requestDto.session().clear();
            } else {
                requestDto.session().save(SessionConstants.CURRENT_STAGE, requestDto.session().get(SessionConstants.PREV_STAGE));
            }
        }
    }

    boolean shouldRetryRequest(ChannelRequestDto requestDto, boolean handleSession) {
        if (handleSession) {
            assert config.sessionSettings() != null;
            final ISessionManager session = requestDto.session();
            var retry = session.get(SessionConstants.CURRENT_STAGE_RETRY_COUNT, Integer.class);

            if (retry == null) {
                session.save(SessionConstants.CURRENT_STAGE_RETRY_COUNT, 1);
                return true;
            }

            if (retry <= TIMEOUT_REQUEST_RETRY_COUNT) {
                session.save(SessionConstants.CURRENT_STAGE_RETRY_COUNT, retry + 1);
                return true;
            }
            return false;
        }
        return false;
    }
}
