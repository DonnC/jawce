package zw.co.dcl.jawce.engine.internal.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import zw.co.dcl.jawce.engine.api.exceptions.InternalException;
import zw.co.dcl.jawce.engine.api.iface.IClientManager;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;
import zw.co.dcl.jawce.engine.api.utils.Utils;
import zw.co.dcl.jawce.engine.api.utils.WhatsAppUtils;
import zw.co.dcl.jawce.engine.configs.JawceConfig;
import zw.co.dcl.jawce.engine.configs.WhatsAppConfig;
import zw.co.dcl.jawce.engine.constants.SessionConstant;
import zw.co.dcl.jawce.engine.model.dto.WebhookProcessorResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class WhatsAppHelperService {
    private final IClientManager clientManager;
    private final ISessionManager sessionManager;
    private final JawceConfig config;
    private final WhatsAppConfig whatsAppConfig;

    public WhatsAppHelperService(
            IClientManager clientManager, ISessionManager sessionManager,
            JawceConfig config, WhatsAppConfig whatsAppConfig
    ) {
        this.clientManager = clientManager;
        this.sessionManager = sessionManager;
        this.config = config;
        this.whatsAppConfig = whatsAppConfig;
    }

    void onWhatsappRequestSuccess(WebhookProcessorResult requestDto) {
        if(requestDto.sessionId() == null) return;
        var session = this.sessionManager.session(requestDto.sessionId());

        if(requestDto.handleSession()) {
            session.evict(requestDto.sessionId(), SessionConstant.CURRENT_STAGE_RETRY_COUNT);
            var stageCode = session.get(requestDto.sessionId(), SessionConstant.CURRENT_STAGE);
            session.save(requestDto.sessionId(), SessionConstant.PREV_STAGE, stageCode);
            session.save(requestDto.sessionId(), SessionConstant.CURRENT_STAGE, requestDto.nextRoute());
            log.debug("[onSuccess{}] Current route set to: {}", this.config.isEmulate() ? "(emulated)" : "", requestDto.nextRoute());
        }
        if(config.isHandleSessionInactivity()) {
            session.save(
                    requestDto.sessionId(),
                    SessionConstant.LAST_ACTIVITY_KEY,
                    Utils.formatZonedDateTime(Utils.currentSystemDate())
            );
        }
    }

    void onRequestError(String sessionId) {
        var session = this.sessionManager.session(sessionId);
        if(session.get(sessionId, SessionConstant.PREV_STAGE)
                .toString()
                .equalsIgnoreCase(config.getStartMenu()) ||
                session.get(sessionId, SessionConstant.CURRENT_STAGE)
                        .toString()
                        .equalsIgnoreCase(config.getStartMenu())
        ) {
            log.warn("WhatsApp request exception - clearing session");
            session.clear(sessionId);
        } else {
            session.save(sessionId, SessionConstant.CURRENT_STAGE, session.get(sessionId, SessionConstant.PREV_STAGE));
        }
    }

    public String sendWhatsAppRequest(WebhookProcessorResult requestDto) {
        try {
            var response = this.clientManager.post(
                    this.config.isEmulate() ? this.config.getEmulatorUrl() : WhatsAppUtils.getUrl(this.whatsAppConfig, false),
                    requestDto.payload(),
                    WhatsAppUtils.getHeaders(this.whatsAppConfig, false, false)
            );

            if(WhatsAppUtils.isValidRequestResponse(response.getBody()) || this.config.isEmulate()) {
                this.onWhatsappRequestSuccess(requestDto);
                return response.getBody();
            }

            log.error("WhatsApp invalid response. Code: {} | Body: {}", response.getStatusCode(), response.getBody());
            throw new InternalException("There was a problem. Unsuccessful channel response code");
        } catch (Exception e) {
            this.onRequestError(requestDto.sessionId());
            throw new InternalException("Failed to process WhatsApp Cloud request", e);
        }
    }

    public void showTypingIndicator(String messageId) {
        try {
            Map<String, Object> payload = Map.of(
                    "messaging_product", "whatsapp",
                    "status", "read",
                    "message_id", messageId,
                    "typing_indicator", Map.of("type", "text")
            );

            var requestDto = new WebhookProcessorResult(
                    payload,
                    null, null, false
            );

            var response = this.sendWhatsAppRequest(requestDto);

            var isSuccess = WhatsAppUtils.isValidRequestResponse(response);
            log.info("[showTypingIndicator] WhatsApp response status: {}", isSuccess);
        } catch (Exception e) {
            log.warn("[showTypingIndicator] Failed to show indicator: {}", e.getMessage());
        }
    }

    public void markAsRead(String messageId) {
        try {
            Map<String, Object> payload = Map.of(
                    "messaging_product", "whatsapp",
                    "status", "read",
                    "message_id", messageId
            );

            var requestDto = new WebhookProcessorResult(
                    payload,
                    null, null, false
            );

            var response = this.sendWhatsAppRequest(requestDto);

            var isSuccess = WhatsAppUtils.isValidRequestResponse(response);
            log.info("[markAsRead] WhatsApp response status: {}", isSuccess);
        } catch (Exception e) {
            log.warn("[markAsRead] Failed to mark as read: {}", e.getMessage());
        }
    }

    public void sendReaction(String recipientId, String emoji, String messageId) {
        try {
            Map<String, Object> payload = Map.of(
                    "messaging_product", "whatsapp",
                    "recipient_type", "individual",
                    "to", recipientId,
                    "type", "reaction",
                    "reaction", Map.of("message_id", messageId, "emoji", emoji)
            );

            var requestDto = new WebhookProcessorResult(
                    payload,
                    null, null, false
            );

            var response = this.sendWhatsAppRequest(requestDto);

            var isSuccess = WhatsAppUtils.isValidRequestResponse(response);
            log.info("[sendReaction] WhatsApp response status: {}", isSuccess);
        } catch (Exception e) {
            log.warn("[sendReaction] Failed to send reaction: {}", e.getMessage());
        }
    }

    public String uploadMedia(String mediaPath) {
        try {
            File file = Path.of(mediaPath).toFile();
            if(!file.exists() || !file.isFile()) {
                log.error("[uploadMedia] File not found: {}", mediaPath);
                return null;
            }

            String contentType = Files.probeContentType(file.toPath());
            if(contentType == null) {
                contentType = URLConnection.guessContentTypeFromName(file.getName());
            }
            if(contentType == null) {
                contentType = "application/octet-stream";
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            FileSystemResource fileResource = new FileSystemResource(file);

            HttpHeaders filePartHeaders = new HttpHeaders();
            filePartHeaders.setContentType(MediaType.parseMediaType(contentType));

            HttpEntity<FileSystemResource> filePart = new HttpEntity<>(fileResource, filePartHeaders);
            body.add("file", filePart);
            body.add("messaging_product", "whatsapp");
            body.add("type", contentType);

            var response = this.clientManager.post(
                    this.config.isEmulate() ? this.config.getEmulatorUrl() : WhatsAppUtils.getUrl(this.whatsAppConfig, true),
                    body,
                    WhatsAppUtils.getHeaders(this.whatsAppConfig, true, false)
            );

            if(response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                log.info("[uploadMedia] Successfully uploaded media to WhatsApp");
                return WhatsAppUtils.getSingleResponseValue(response.getBody(), "id");
            }

            log.warn("Failed to upload media to WhatsApp, response code: {}", response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("[uploadMedia] Failed to upload media: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Deletes a media resource from the cloud API.
     * Returns true when the API reports success, false otherwise.
     */
    public boolean deleteMedia(String mediaId) {
        try {
            ResponseEntity<String> response = this.clientManager.request(
                    WhatsAppUtils.getMediaIdUrl(this.whatsAppConfig, mediaId),
                    new HttpEntity<>(WhatsAppUtils.getHeaders(this.whatsAppConfig, false, true)),
                    HttpMethod.DELETE,
                    String.class
            );

            if(response.getStatusCode() == HttpStatus.OK) {
                log.info("Media {} deleted", mediaId);
                return Objects.equals(WhatsAppUtils.getSingleResponseValue(response.getBody(), "success"), "true");
            } else {
                log.error("Error deleting media, status code: {} | response: {}", response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("Exception while deleting media {}: {}", mediaId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Query media URL from a media ID.
     * Returns the URL string or null on error.
     */
    public String queryMediaUrl(String mediaId) {
        try {
            ResponseEntity<String> response = this.clientManager.request(
                    WhatsAppUtils.getMediaIdUrl(this.whatsAppConfig, mediaId),
                    new HttpEntity<>(WhatsAppUtils.getHeaders(this.whatsAppConfig, false, true)),
                    HttpMethod.GET,
                    String.class
            );

            if(response.getStatusCode() == HttpStatus.OK) {
                return WhatsAppUtils.getSingleResponseValue(response.getBody(), "url");
            } else {
                log.error("Error querying media, status code: {} | response: {}", response.getStatusCode(), response.getBody());
                return null;
            }
        } catch (Exception e) {
            log.error("Exception while querying media id {}: {}", mediaId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Download media from a media URL and save to disk.
     * Returns the absolute path to the saved file or null on failure.
     */
    public String downloadMedia(String mediaUrl, Path savePath) {
        try {
            if(Files.exists(savePath)) {
                log.warn("[downloadMedia] File: {} already exists!", savePath);
            }

            ResponseEntity<Resource> response = this.clientManager.request(
                    mediaUrl,
                    new HttpEntity<>(WhatsAppUtils.getHeaders(this.whatsAppConfig, false, true)),
                    HttpMethod.GET,
                    Resource.class
            );

            if(response.getStatusCode() == HttpStatus.OK) {
                log.info("Media url downloaded successfully: {}. Saving to local..", mediaUrl);
                Assert.notNull(response.getBody(), "No media file response body found");

                try (InputStream in = response.getBody().getInputStream();
                     FileOutputStream out = new FileOutputStream(savePath.toFile())) {
                    StreamUtils.copy(in, out);
                }
                log.debug("Media downloaded to {}", savePath.toAbsolutePath());
                return savePath.toAbsolutePath().toString();
            } else {
                log.error("Failed to download media. Status code: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Error downloading media to {}: {}", savePath, e.getMessage());
            return null;
        }
    }

    /**
     * Download a single flow media payload entry.
     * Expects a Json-like object with keys id and file_name.
     * <p>
     * [
     * {'id': 5868146111.., 'mime_type': 'image/jpeg', 'sha256': 'CiXteED..', 'file_name': '4c631dab-...jpg'},
     * {'id': 1571385113.., 'mime_type': 'image/jpeg', 'sha256': 'lV..', 'file_name': '5d70f3e...jpg'}
     * ]
     */
    public String downloadFlowMedia(Map<String, Object> flowMediaPayload, Path downloadDir) {

        if(!flowMediaPayload.containsKey("id") || !flowMediaPayload.containsKey("file_name")) {
            throw new InternalException("Invalid flow media payload: missing id or file_name");
        }

        String mediaUrl = queryMediaUrl(flowMediaPayload.get("id").toString());
        if(mediaUrl == null) {
            throw new InternalException("Failed to query media file url for id " + flowMediaPayload.get("id"));
        }

        String downloadedPath = downloadMedia(mediaUrl, downloadDir.resolve(flowMediaPayload.get("file_name").toString()));

        if(downloadedPath == null) {
            throw new InternalException("Failed to download file for media id: " + flowMediaPayload.get("id"));
        }

        return downloadedPath;
    }

    // TODO: add flow endpoint logic
}
