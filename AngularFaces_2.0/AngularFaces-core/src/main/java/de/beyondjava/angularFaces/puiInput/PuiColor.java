/**
 *  (C) 2013-2014 Stephan Rauh http://www.beyondjava.net
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.beyondjava.angularFaces.puiInput;

import java.util.logging.Logger;

import javax.faces.component.FacesComponent;
import javax.faces.context.FacesContext;
import javax.faces.render.Renderer;

/**
 * PuiInput is an Angular-aware iput text field reading the JSR 303 annotations and providing a label and an error
 * message.
 */
@FacesComponent("de.beyondjava.angularFaces.puiInput.PuiColor")
public class PuiColor extends PuiInput {
    enum propertyKeys {
        label, value
    }

    private static final Logger LOGGER = Logger.getLogger("de.beyondjava.angularFaces.puiInput.PuiColor");

    static {
        LOGGER.info("AngularFaces component 'PuiColor' is available for use.");
    }

    /**
     *
     */
    public PuiColor() {
        LOGGER.info(getClass().getName() + " is initialized");
        // setRendererType("de.beyondjava.angularFaces.puiInput.PuiInputTextRenderer");
        LOGGER.info(getFamily());
        Renderer renderer = getRenderer(FacesContext.getCurrentInstance());
        LOGGER.info(renderer.getClass().getName());
    }
}
