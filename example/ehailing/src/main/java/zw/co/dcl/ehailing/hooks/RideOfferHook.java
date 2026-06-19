package zw.co.dcl.ehailing.hooks;

import org.springframework.stereotype.Service;
import zw.co.dcl.jawce.engine.api.annotation.FlowHookType;
import zw.co.dcl.jawce.engine.api.annotation.NamedFlowHook;
import zw.co.dcl.jawce.engine.api.utils.SerializeUtils;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class RideOfferHook {
    private static final String QUOTE_AMOUNT_KEY = "quoteAmount";
    private static final String QUOTE_WAIT_MINS_KEY = "quoteWaitMins";
    private static final String QUOTE_LABEL_KEY = "quoteLabel";
    private static final String DRIVER_NAME_KEY = "selectedDriverName";
    private static final String DRIVER_ETA_KEY = "selectedDriverEtaMins";
    private static final String DRIVER_PRICE_NOTE_KEY = "selectedDriverPriceNote";

    @NamedFlowHook(value = "prepareRideOffer", type = FlowHookType.GENERATE)
    public Hook prepare(Hook hook) {
        var rideType = String.valueOf(hook.getSession().getUserProps(hook.getSessionId()).getOrDefault("rideType", "Ride"));
        var quote = quoteFor(rideType);

        hook.getSession().save(hook.getSessionId(), QUOTE_AMOUNT_KEY, quote.amount().toPlainString());
        hook.getSession().save(hook.getSessionId(), QUOTE_WAIT_MINS_KEY, quote.waitMins());
        hook.getSession().save(hook.getSessionId(), QUOTE_LABEL_KEY, quote.label());
        return hook;
    }

    @NamedFlowHook(value = "renderRideOffer", type = FlowHookType.DYNAMIC)
    public Hook render(Hook hook) {
        String amount = hook.getSession().get(hook.getSessionId(), QUOTE_AMOUNT_KEY, String.class);
        Integer waitMins = hook.getSession().get(hook.getSessionId(), QUOTE_WAIT_MINS_KEY, Integer.class);
        String label = hook.getSession().get(hook.getSessionId(), QUOTE_LABEL_KEY, String.class);
        String driverName = hook.getSession().get(hook.getSessionId(), DRIVER_NAME_KEY, String.class);
        Integer driverEta = hook.getSession().get(hook.getSessionId(), DRIVER_ETA_KEY, Integer.class);
        String priceNote = hook.getSession().get(hook.getSessionId(), DRIVER_PRICE_NOTE_KEY, String.class);

        Map<String, Object> templateMap = Map.of(
                "type", "button",
                "message", Map.of(
                        "title", "Ride Fee",
                        "body", "Your " + safe(label, "Ride") + " fee to your destination is USD $" +
                                safe(amount, "3.50") + "\nYou will arrive in ~" + safe(waitMins, 8) + "mins",
                        "footer", "Driver " + safe(driverName, "is being matched") +
                                " | " + safe(driverEta, waitMins == null ? 8 : waitMins) + " mins away" +
                                " | " + safe(priceNote, "Metered fare"),
                        "buttons", java.util.List.of("Accept", "Counter Offer")
                )
        );

        hook.setTemplateDynamicBody(
                TemplateDynamicBody.builder()
                        .template(SerializeUtils.toTemplate(templateMap))
                        .build()
        );
        return hook;
    }

    private RideQuote quoteFor(String rideType) {
        String normalized = rideType == null ? "ride" : rideType.trim().toLowerCase();

        return switch (normalized) {
            case "standard" -> new RideQuote("Standard", amount(4.25), 6);
            case "luxury" -> new RideQuote("Luxury", amount(8.75), 4);
            default -> new RideQuote("Ride", amount(3.50), 8);
        };
    }

    private BigDecimal amount(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private int safe(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private record RideQuote(String label, BigDecimal amount, int waitMins) {
    }
}
