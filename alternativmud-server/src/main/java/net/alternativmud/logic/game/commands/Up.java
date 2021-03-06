/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.alternativmud.logic.game.commands;

import net.alternativmud.App;
import net.alternativmud.logic.event.LocationChanged;
import net.alternativmud.logic.game.Gameplay;
import net.alternativmud.logic.world.area.Location;

/**
 *
 * @author jblew
 */
public class Up implements Command {
    @Override
    public String execute(Gameplay game, String[] parts) throws ExecutionRejectedException {
        Location.Manager locationsMgr = App.getApp().getWorld().getLocationsManager();
        Location l = locationsMgr.getLocation(game.getCharacter().getLocationCoordinates());
        
        Location target = locationsMgr.getLocation(l.getCoordinates().getUpper());
        if(target == null) throw new ExecutionRejectedException("Where are you going to?");
        game.getCharacter().setLocationCoordinates(target.getCoordinates());
        
        game.getEBus().post(new LocationChanged(target));
        return "You went up.\r\n"+target.getLook(game)+"\r\n";
    }
}
