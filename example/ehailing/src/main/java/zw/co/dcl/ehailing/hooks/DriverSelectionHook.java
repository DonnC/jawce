package zw.co.dcl.ehailing.hooks;

import org.springframework.stereotype.Service;
import zw.co.dcl.jawce.engine.api.annotation.FlowHookType;
import zw.co.dcl.jawce.engine.api.annotation.NamedFlowHook;
import zw.co.dcl.jawce.engine.api.pagination.PaginationMode;
import zw.co.dcl.jawce.engine.api.pagination.PaginationRequest;
import zw.co.dcl.jawce.engine.api.pagination.PaginationSelection;
import zw.co.dcl.jawce.engine.api.pagination.PaginationSupport;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.dto.DynamicChoice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DriverSelectionHook {
    static final String PAGINATION_STATE_KEY = "availableDrivers";
    static final String RERENDER_STAGE = "AVAILABLE-DRIVERS";
    private static final String DRIVER_ID_KEY = "selectedDriverId";
    private static final String DRIVER_NAME_KEY = "selectedDriverName";
    private static final String DRIVER_VEHICLE_KEY = "selectedDriverVehicle";
    private static final String DRIVER_PLATE_KEY = "selectedDriverPlate";
    private static final String DRIVER_ETA_KEY = "selectedDriverEtaMins";
    private static final String DRIVER_PRICE_NOTE_KEY = "selectedDriverPriceNote";

    @NamedFlowHook(value = "renderAvailableDrivers", type = FlowHookType.DYNAMIC)
    public Hook render(Hook hook) {
        RideDriverScenario scenario = scenarioFor(resolveRideType(hook));
        List<DynamicChoice> choices = PaginationSupport.mapChoices(
                scenario.drivers(),
                DriverCandidate::id,
                DriverCandidate::name,
                driver -> driver.vehicle() + " | " + driver.etaMins() + " min away",
                (driver, ordinal) -> Map.of(
                        "driverId", driver.id(),
                        "driverName", driver.name(),
                        "vehicle", driver.vehicle(),
                        "plate", driver.plate(),
                        "etaMins", driver.etaMins(),
                        "priceNote", driver.priceNote()
                )
        );

        hook.setTemplateDynamicBody(PaginationSupport.render(hook, PaginationRequest.builder()
                .stateKey(PAGINATION_STATE_KEY)
                .mode(scenario.mode())
                .pageSize(10)
                .title("Nearby Drivers")
                .prompt(scenario.prompt())
                .buttonLabel("Drivers")
                .sectionTitle("Available " + scenario.rideLabel() + " drivers")
                .footer("Choose a driver to continue")
                .emptyMessage("No drivers are available right now. Please try again shortly.")
                .choices(choices)
                .build()));
        return hook;
    }

    @NamedFlowHook(value = "captureSelectedDriver", type = FlowHookType.RECEIVE)
    @SuppressWarnings("unchecked")
    public Hook capture(Hook hook) {
        var selection = PaginationSupport.handleSelection(hook);
        if(selection.isEmpty() || !selection.get().isItem()) {
            return hook;
        }

        Object rawDynamicChoice = hook.getAdditionalData() == null ? null : hook.getAdditionalData().get("dynamicChoice");
        if(!(rawDynamicChoice instanceof Map<?, ?> dynamicChoice)) {
            return hook;
        }

        Map<String, Object> normalizedChoice = (Map<String, Object>) rawDynamicChoice;
        Map<String, Object> metadata = normalizeMap(normalizedChoice.get("metadata"));

        hook.getSession().save(hook.getSessionId(), DRIVER_ID_KEY, metadata.getOrDefault("driverId", normalizedChoice.get("id")));
        hook.getSession().save(hook.getSessionId(), DRIVER_NAME_KEY, metadata.getOrDefault("driverName", normalizedChoice.get("label")));
        hook.getSession().save(hook.getSessionId(), DRIVER_VEHICLE_KEY, metadata.getOrDefault("vehicle", "Vehicle pending"));
        hook.getSession().save(hook.getSessionId(), DRIVER_PLATE_KEY, metadata.getOrDefault("plate", "TBA"));
        hook.getSession().save(hook.getSessionId(), DRIVER_ETA_KEY, metadata.getOrDefault("etaMins", 6));
        hook.getSession().save(hook.getSessionId(), DRIVER_PRICE_NOTE_KEY, metadata.getOrDefault("priceNote", "Metered fare"));
        PaginationSupport.clear(hook, PAGINATION_STATE_KEY);
        return hook;
    }

    @NamedFlowHook(value = "routeDriverSelection", type = FlowHookType.ROUTER)
    public Hook route(Hook hook) {
        PaginationSelection selection = PaginationSupport.selection(hook).orElse(null);
        if(selection != null && selection.isNavigation()) {
            hook.setRedirectTo(RERENDER_STAGE);
        }
        return hook;
    }

    static RideDriverScenario scenarioFor(String rideType) {
        String normalized = rideType == null ? "ride" : rideType.trim().toLowerCase();
        return switch (normalized) {
            case "standard" -> new RideDriverScenario(
                    "Standard",
                    PaginationMode.LIST,
                    "Choose a nearby Standard driver. If you do not see your preference, open the next page.",
                    List.of(
                            new DriverCandidate("drv-201", "Tafadzwa M.", "Toyota Aqua", "AEH 2011", 3, "Economy fare"),
                            new DriverCandidate("drv-202", "Linda S.", "Honda Fit", "AEH 2022", 4, "Economy fare"),
                            new DriverCandidate("drv-203", "Craig D.", "Mazda Demio", "AEH 2033", 4, "Economy fare"),
                            new DriverCandidate("drv-204", "Melissa T.", "Toyota Vitz", "AEH 2044", 5, "Economy fare"),
                            new DriverCandidate("drv-205", "Tawanda Z.", "Suzuki Swift", "AEH 2055", 5, "Economy fare"),
                            new DriverCandidate("drv-206", "Rudo P.", "Toyota Passo", "AEH 2066", 6, "Economy fare"),
                            new DriverCandidate("drv-207", "Elton K.", "Nissan Note", "AEH 2077", 6, "Economy fare"),
                            new DriverCandidate("drv-208", "Sharon B.", "Toyota Aqua", "AEH 2088", 7, "Economy fare"),
                            new DriverCandidate("drv-209", "Victor N.", "Mazda Axela", "AEH 2099", 7, "Economy fare"),
                            new DriverCandidate("drv-210", "Nyasha G.", "Honda Shuttle", "AEH 2100", 8, "Economy fare"),
                            new DriverCandidate("drv-211", "Munashe C.", "Toyota Fielder", "AEH 2111", 8, "Economy fare"),
                            new DriverCandidate("drv-212", "Tracy H.", "Nissan Tiida", "AEH 2122", 9, "Economy fare")
                    )
            );
            case "luxury" -> new RideDriverScenario(
                    "Luxury",
                    PaginationMode.TEXT,
                    "Choose a chauffeur. Luxury drivers show richer notes, so this stage uses paginated text.",
                    List.of(
                            new DriverCandidate("drv-301", "M. Chikore", "Mercedes C200", "LUX 301", 2, "Premium meet-and-greet"),
                            new DriverCandidate("drv-302", "A. Dube", "BMW 320i", "LUX 302", 3, "Premium meet-and-greet"),
                            new DriverCandidate("drv-303", "T. Ncube", "Mercedes E250", "LUX 303", 3, "Premium meet-and-greet"),
                            new DriverCandidate("drv-304", "R. Moyo", "Audi A4", "LUX 304", 4, "Premium meet-and-greet"),
                            new DriverCandidate("drv-305", "K. Sibanda", "Lexus IS250", "LUX 305", 4, "Premium meet-and-greet"),
                            new DriverCandidate("drv-306", "G. Maregere", "Mercedes C180", "LUX 306", 5, "Premium meet-and-greet"),
                            new DriverCandidate("drv-307", "N. Chari", "BMW X1", "LUX 307", 5, "Premium meet-and-greet"),
                            new DriverCandidate("drv-308", "P. Chuma", "Mercedes CLA", "LUX 308", 6, "Premium meet-and-greet"),
                            new DriverCandidate("drv-309", "L. Zhou", "Audi Q3", "LUX 309", 6, "Premium meet-and-greet"),
                            new DriverCandidate("drv-310", "S. Ndlovu", "Mercedes GLA", "LUX 310", 7, "Premium meet-and-greet"),
                            new DriverCandidate("drv-311", "C. Mlambo", "BMW 520d", "LUX 311", 7, "Premium meet-and-greet"),
                            new DriverCandidate("drv-312", "B. Chisango", "Lexus ES250", "LUX 312", 8, "Premium meet-and-greet"),
                            new DriverCandidate("drv-313", "J. Gora", "Mercedes E200", "LUX 313", 8, "Premium meet-and-greet"),
                            new DriverCandidate("drv-314", "D. Tafirenyika", "Audi A6", "LUX 314", 9, "Premium meet-and-greet")
                    )
            );
            default -> new RideDriverScenario(
                    "Ride",
                    PaginationMode.BUTTON,
                    "Choose one of the closest drivers near your pickup point.",
                    List.of(
                            new DriverCandidate("drv-101", "Blessing M.", "Toyota Aqua", "RIDE 101", 2, "Meter starts on pickup"),
                            new DriverCandidate("drv-102", "Rufaro K.", "Honda Fit", "RIDE 102", 3, "Meter starts on pickup"),
                            new DriverCandidate("drv-103", "Nigel T.", "Mazda Demio", "RIDE 103", 4, "Meter starts on pickup")
                    )
            );
        };
    }

    private String resolveRideType(Hook hook) {
        Object rideType = hook.getSession().getUserProps(hook.getSessionId()).get("rideType");
        return rideType == null ? "Ride" : String.valueOf(rideType);
    }

    private Map<String, Object> normalizeMap(Object value) {
        if(!(value instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        rawMap.forEach((key, rawValue) -> normalized.put(String.valueOf(key), rawValue));
        return normalized;
    }

    record DriverCandidate(String id, String name, String vehicle, String plate, int etaMins, String priceNote) {
    }

    record RideDriverScenario(String rideLabel, PaginationMode mode, String prompt, List<DriverCandidate> drivers) {
    }
}
