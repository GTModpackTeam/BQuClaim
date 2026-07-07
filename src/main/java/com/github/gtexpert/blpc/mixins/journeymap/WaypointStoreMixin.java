package com.github.gtexpert.blpc.mixins.journeymap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.gtexpert.blpc.integration.jmap.JMapWaypointOutgoing;

import journeymap.client.model.Waypoint;
import journeymap.client.waypoint.WaypointStore;

/**
 * Detects local waypoint changes made through JourneyMap's own UI ({@code WaypointEditor}) so
 * they can be shared with the local player's party. Only {@code save}/{@code remove} are
 * hooked — {@code add} is used for JourneyMap's own startup load of previously-saved waypoints
 * and would falsely trigger a re-share of every waypoint on every login if hooked too.
 * <p>
 * {@code WaypointEditor.save()} always calls {@code WaypointStore.remove(original)} followed by
 * {@code WaypointStore.save(edited)} — even for a brand-new waypoint (editing is modeled as
 * "delete old, add new" in JourneyMap). {@link JMapWaypointOutgoing} debounces this so an edit
 * produces one shared update instead of a spurious remove+add pair.
 */
@Mixin(value = WaypointStore.class, remap = false)
public class WaypointStoreMixin {

    @Inject(method = "save", at = @At("RETURN"))
    private void blpc$onSave(Waypoint waypoint, CallbackInfo ci) {
        JMapWaypointOutgoing.onLocalSave(waypoint);
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void blpc$onRemove(Waypoint waypoint, CallbackInfo ci) {
        JMapWaypointOutgoing.onLocalRemove(waypoint);
    }
}
