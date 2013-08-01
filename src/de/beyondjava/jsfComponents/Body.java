package de.beyondjava.jsfComponents;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponentBase;

/**
 * This is an AngularJS-enabled html body tag.
 * @author Stephan Rauh http://www.beyondjava.net
 *
 */
@FacesComponent("de.beyondjava.Body")
public class Body extends UIComponentBase
{
   public static final String COMPONENT_FAMILY = "de.beyondJava.angularFaces.body";

   public String getFamily()
   {
      return COMPONENT_FAMILY;
   }
}
