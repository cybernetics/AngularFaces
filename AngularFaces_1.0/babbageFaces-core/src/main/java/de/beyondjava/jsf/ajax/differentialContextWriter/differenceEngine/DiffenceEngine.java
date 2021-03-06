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

package de.beyondjava.jsf.ajax.differentialContextWriter.differenceEngine;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.*;

import javax.faces.application.ProjectStage;
import javax.faces.context.FacesContext;

import de.beyondjava.jsf.ajax.differentialContextWriter.parser.*;

/**
 * @author Stephan Rauh http://www.beyondjava.net
 * 
 */
public class DiffenceEngine {
    private static long DEBUG_optimizedBytesCumulated = 0l;

    private static long DEBUG_originalBytesCumulated = 0l;

    private static boolean differentialEngineActive = true;

    private static final Logger LOGGER = Logger
            .getLogger("de.beyondjava.jsf.ajax.differentialContextWriter.differenceEngine.DiffenceEngine");

    final boolean isDeveloperMode = (FacesContext.getCurrentInstance() != null)
            && (FacesContext.getCurrentInstance().getApplication().getProjectStage() == ProjectStage.Development);
    final String LAST_KNOWN_HTML_KEY = "com.beyondEE.faces.diff.lastKnownHTML";

    /**
    * 
    */
    public DiffenceEngine() {
    }

    /**
     * @param newPartialChanges
     */
    // private String boast(List<HTMLTag> newPartialChanges) {
    // String boasting;
    // boasting = "<table>";
    // for (HTMLTag c : newPartialChanges) {
    // boasting += "<tr><td>" + c.getNodeName() + "=" + c.getId() + "</td></tr>";
    // }
    // boasting += "</table>";
    // return boasting;
    // }

    /**
     * @param change
     * @param lastKnownDOMTree
     */
    private List<HTMLTag> determineNecessaryChangeFromResponse(HTMLTag change, HTMLTag lastKnownDOMTree) {

        if (change.getNodeName().equals("update")) {
            String id = change.getId();
            HTMLTag changingHTML;
            if (change.getFirstChild().isCDATANode() || change.getFirstChild().isTextNode()) {
                changingHTML = new HTMLTag(change.getFirstChild().getInnerHTML().toString().trim());
            }
            else {
                changingHTML = new HTMLTag(change.getFirstChild().toString().trim());
            }

            HTMLTag lastKnownCorrespondingHTMLTag = lastKnownDOMTree.findByID(id);
            if (null == lastKnownCorrespondingHTMLTag) {
                LOGGER.severe("Couldn't find id " + id + " in the last known DOM tree");
                throw new IllegalArgumentException("Couldn't find id " + id + " in the last known DOM tree");
            }
            return determineNecessaryChanges(changingHTML, lastKnownCorrespondingHTMLTag);
        }
        else {
            LOGGER.fine(change.getNodeName() + " - remains unchanged");
        }
        return null;

    }

    /**
     * @param changingHTML
     * @param lastKnownCorrespondingHTMLTag
     * @return
     */
    private List<HTMLTag> determineNecessaryChanges(HTMLTag changingHTML, HTMLTag lastKnownCorrespondingHTMLTag) {
        List<String> deletions = new ArrayList<>();
        List<String> attributeChanges = new ArrayList<>();
        List<String> inserts = new ArrayList<>();
        List<HTMLTag> updates = new ArrayList<>();
        determineNecessaryChanges(changingHTML, lastKnownCorrespondingHTMLTag, updates, deletions, attributeChanges,
                inserts);
        List<HTMLTag> partialChanges = new ArrayList<>();
        if ((null != inserts) && (inserts.size() > 0)) {
            for (String insert : inserts) {
                partialChanges.add(new HTMLTag(insert));
            }
        }

        if (null != deletions) {
            for (String partialID : deletions) {
                String partialChange = "<delete id=\"" + partialID + "\"/>";
                HTMLTag partialChangeHTMLTag = new HTMLTag(partialChange);
                partialChanges.add(partialChangeHTMLTag);
            }
        }
        if (null != updates) {
            for (HTMLTag n : updates) {
                n = findNodeThatCanBeReplaced(n);
                String partialUpdate = n.toCompactString();
                String partialID = n.getId();
                HTMLTag partialChangeHTMLTag = new HTMLTag("update", partialID, partialUpdate);
                partialChanges.add(partialChangeHTMLTag);
            }
        }
        if (null != attributeChanges) {
            for (String partialChange : attributeChanges) {
                HTMLTag partialChangeHTMLTag = new HTMLTag(partialChange);
                partialChanges.add(partialChangeHTMLTag);
            }
        }

        return partialChanges;
    }

    /**
     * @param changingHTML
     * @param lastKnownCorrespondingHTMLTag
     * @param deletions2
     * @param changes2
     */
    protected List<HTMLTag> determineNecessaryChanges(HTMLTag newDOM, HTMLTag lastKnownCorrespondingHTMLTag,
            List<HTMLTag> updates, List<String> deletions, List<String> attributeChanges, List<String> inserts) {
        XmlDiff.tagsAreEqualOrCanBeChangedLocally(lastKnownCorrespondingHTMLTag, newDOM, updates, deletions,
                attributeChanges, inserts);
        for (HTMLTag d : updates) {
            if (isDeveloperMode) {
                LOGGER.fine("Updates: " + d);
                // JUnitTestCreator.generateJUnitTest(newDOM,
                // lastKnownCorrespondingHTMLTag, updates);
            }
        }
        for (String d : deletions) {
            if (isDeveloperMode) {
                LOGGER.fine("Deletion: " + d);
            }
        }
        for (String d : attributeChanges) {
            if (isDeveloperMode) {
                LOGGER.fine("Change: " + d);
            }
        }
        for (String d : inserts) {
            if (isDeveloperMode) {
                LOGGER.fine("Insertion: " + d);
            }
        }
        return updates;
    }

    /**
     * @param partialResponseAsDOMTree
     * @return
     */
    private List<HTMLTag> extractChangesFromPartialResponse(String partialResponse) {

        List<HTMLTag> partialResponses = new ArrayList<>();
        partialResponse = partialResponse.replace("\n", "").replace("\r", "");
        Pattern pattern = Pattern
                .compile("(<update id=\".*?\">.*?</update>)|(<attributes id=\")|(<delete id=\")|(<eval>)|(<insert id=\")|(<extension )|(<error>)|(<redirect url=\")");
        Matcher matcher = pattern.matcher(partialResponse);
        while (matcher.find()) {
            String group = matcher.group();
            if (group.startsWith("<update")) {
                int start = "<update id=\"".length();
                int end = group.indexOf('"', start);
                if (end > start) {

                    String id = group.substring(start, end);
                    start = group.indexOf("<![CDATA[", end);
                    end = group.lastIndexOf("]]>");
                    if ((start >= 0) && (end > start)) {
                        String html = group.substring(start + "<![CDATA[".length(), end);
                        HTMLTag update = new HTMLTag("<update id=\"" + id + "\">" + html + "</update>");
                        partialResponses.add(update);
                    }
                    else {
                        partialResponses.add(new HTMLTag(group));
                    }
                }
                else {
                    partialResponses.add(new HTMLTag(group));
                }
            }
            else {
                LOGGER.severe("This tag is not supported by BabbageFaces. " + group);
            }
        }

        return partialResponses;
    }

    /**
     * @param n
     * @return
     */
    private HTMLTag findNodeThatCanBeReplaced(HTMLTag n) {
        // special treatment for PrimeFaces comboboxes
        if ("select".equals(n.getNodeName())) {
            if ((n.getParent() != null) && (n.getParent().getParent() != null)) {
                HTMLTag grandpa = n.getParent().getParent();
                final HTMLAttribute c = grandpa.getAttribute("class");
                if ((c != null) && c.value.contains("ui-selectonemenu")) {
                    n = grandpa;
                }
            }
        }

        while ((n.getId() == null) || (n.getId().length() == 0)) {
            if (n.getParent() == null) {
                LOGGER.severe("ID of update shouldn't be void - can't be fixed because parent tag is null");
            }
            else {
                LOGGER.severe("ID of update shouldn't be void - using the parent instead");
            }
            if (isDeveloperMode) {
                LOGGER.fine(n.toCompactString());
            }

            n = n.getParent();

        }
        return n;
    }

    /**
     * @param currentResponse
     * @param domTreeToBeUpdated
     * @param change
     * @param newPartialChanges
     * @return
     */
    private String optimizeResponse(String currentResponse, HTMLTag domTreeToBeUpdated, HTMLTag change,
            List<HTMLTag> newPartialChanges) {
        String id = change.getId();
        return optimizeResponse(currentResponse, domTreeToBeUpdated, newPartialChanges, id);
    }

    /**
     * @param currentResponse
     * @param domTreeToBeUpdated
     * @param newPartialChanges
     * @param id
     * @return
     */
    private String optimizeResponse(String currentResponse, HTMLTag domTreeToBeUpdated,
            List<HTMLTag> newPartialChanges, String id) {
        if (!id.contains("javax.faces.ViewState")) {

            int start = currentResponse.indexOf("<update id=\"" + id + "\">");
            int end = currentResponse.indexOf("</update>", start);
            String currentResponseEnd = currentResponse.substring(end + "</update>".length());
            String tmpCurrentResponse = currentResponse.substring(0, start);
            for (HTMLTag changeDefinition : newPartialChanges) {
                tmpCurrentResponse += changeDefinition.toCompactString();
                String idOfCurrentChange = changeDefinition.getId();
                if ((idOfCurrentChange == null) || (idOfCurrentChange.length() == 0)) {
                    LOGGER.severe("Missing HTMLTag ID");
                }
                else if (changeDefinition.getNodeName().equals("update")) {
                    Map<String, HTMLTag> scripts = domTreeToBeUpdated.collectScripts();
                    HTMLTag currentChangeTag = domTreeToBeUpdated.findByID(idOfCurrentChange);
                    if (null != currentChangeTag) {
                        final List<HTMLTag> requiredScripts = currentChangeTag.extractPrimeFacesJavascript(scripts);
                        for (HTMLTag scriptNode : requiredScripts) {
                            tmpCurrentResponse = tmpCurrentResponse.substring(0, tmpCurrentResponse.length()
                                    - "]]></update>".length())
                                    + scriptNode.toCompactString() + "]]></update>";
                        }
                    }
                }

            }
            currentResponse = tmpCurrentResponse + currentResponseEnd;
        }
        return currentResponse;
    }

    /**
     * Retrieves the DOM tree that has been sent by the previous HTTP response (and that ought to be identical to the
     * version displayed in the browser).
     * 
     * @param sessionMap
     * @return
     */
    private HTMLTag retrieveLastKnownHTMLFromSession(Map<String, Object> sessionMap) {
        HTMLTag html = (HTMLTag) sessionMap.get(LAST_KNOWN_HTML_KEY);
        if (html == null) {
            throw new RuntimeException("There's no HTML code stored in the session");
        }
        return html;
    }

    /**
     * Fixes the DOM tree stored in the session by integrating the AJAX updates into the last known DOM tree.
     * 
     * @param domTreeInSession
     * @param typeOfChange
     * @param idToBeUpdated
     */
    private void updateHTMLTag(HTMLTag domTreeInSession, HTMLTag newSubtree, String idToBeUpdated) {

        HTMLTag tagToBeReplaced = domTreeInSession.findByID(idToBeUpdated);
        if (tagToBeReplaced == null) {
            if (!idToBeUpdated.contains("javax.faces.ViewState")) { // JSF omits the view state in AJAX responses
                LOGGER.severe("Wrong ID? Looking for " + idToBeUpdated
                        + ", but couldn't find the ID in the last known HTML tree");
            }
        }
        else {
            if (newSubtree.isCDATANode()) {
                newSubtree = new HTMLTag(newSubtree.getInnerHTML().toString());
            }
            HTMLTag parentHTMLTag = tagToBeReplaced.getParent();
            if (parentHTMLTag == null) {
                LOGGER.severe("parent HTML tag = null: did you try to replace the whole tree?"
                        + tagToBeReplaced.getId());
            }
            else {
                parentHTMLTag.replaceChild(newSubtree, tagToBeReplaced);
            }
        }
    }

    /**
     * Compares the current HTML response with the last known HTML code. If it's a regular HTML response, the HTML code
     * is simply stored in the session. If it's an JSF AJAX response, the method looks at the differences and tries to
     * remove unchanged HTML from the response.
     * 
     * @param rawBuffer
     * @param sessionMap
     * @param isAJAX
     * @return
     */
    public String yieldDifferences(String currentResponse, Map<String, Object> sessionMap, boolean isAJAX) {
        // String boasting = "";
        int originalLength = currentResponse.length(); // differentialEngine=false;
        if (!differentialEngineActive) {
            return currentResponse;
        }
        if (isAJAX) {

            HTMLTag domTreeToBeUpdated = retrieveLastKnownHTMLFromSession(sessionMap);
            List<HTMLTag> listOfChanges = extractChangesFromPartialResponse(currentResponse);
            for (HTMLTag change : listOfChanges) {
                if (change.getNodeName().equals("update")) {
                    if (change.getId().equals("javax.faces.ViewRoot")) {
                        HTMLTag newDom = change;
                        HTMLTag header = newDom.findTag("head");
                        HTMLTag body = newDom.findTag("body");

                        int endOfFirstUpdate = currentResponse.indexOf("</update>");
                        String secondUpdate = currentResponse.substring(endOfFirstUpdate);

                        String s = "<?xml version='1.0' encoding='UTF-8'?>\r\n<partial-response id=\"j_id1\"><changes>";
                        HTMLTag oldHeader = domTreeToBeUpdated.findTag("head");
                        String headerAsString = header.toCompactString();
                        String oldHeaderAsString = oldHeader.toCompactString();
                        if (headerAsString.equals(oldHeaderAsString)) {
                            // nothing to do - just omit the header, it hasn't changed
                        }
                        else {
                            // if everything but the header is identical extract the new header into a Javascript
                            // command
                            int startNew = headerAsString.indexOf("<title>");
                            int endNew = headerAsString.indexOf("</title>");
                            String title = "";
                            if ((startNew >= 0) && (endNew >= 0)) {
                                title = headerAsString.substring(startNew + "<title>".length(), endNew);
                                headerAsString = headerAsString.substring(0, startNew)
                                        + headerAsString.substring(endNew);
                            }
                            int startOld = oldHeaderAsString.indexOf("<title>");
                            int endOld = oldHeaderAsString.indexOf("</title>");
                            if ((startOld >= 0) && (endNew >= 0)) {
                                oldHeaderAsString = oldHeaderAsString.substring(0, startOld)
                                        + oldHeaderAsString.substring(endOld);
                            }
                            if (headerAsString.equals(oldHeaderAsString)) {
                                s += "<eval><![CDATA[" + "window.document.title='::" + title + "';" + "]]></eval>";
                            }
                            else {
                                s += "<update id=\"javax.faces.ViewHead\"><![CDATA[" + headerAsString + "]]></update>";
                            }
                        }
                        s += "<update id=\"javax.faces.ViewBody\"><![CDATA[" + body.toCompactString() + "]]>";
                        s += secondUpdate;
                        currentResponse = s;

                        List<HTMLTag> newBodyChanges = determineNecessaryChanges(body,
                                domTreeToBeUpdated.findTag("body"));
                        currentResponse = optimizeResponse(currentResponse, domTreeToBeUpdated, newBodyChanges,
                                "javax.faces.ViewBody");
                        // boasting = boast(newBodyChanges);
                        domTreeToBeUpdated = newDom;
                        sessionMap.remove(LAST_KNOWN_HTML_KEY);
                        sessionMap.put(LAST_KNOWN_HTML_KEY, newDom);
                    }
                    else if ((!change.getId().contains("javax.faces.ViewState"))
                            && ((!change.getId().contains("javax.faces.ViewHead")))
                            && ((!change.getId().contains("javax.faces.ViewBody")))) {
                        List<HTMLTag> newPartialChanges = determineNecessaryChangeFromResponse(change,
                                domTreeToBeUpdated);
                        if ((null != newPartialChanges)) {
                            currentResponse = optimizeResponse(currentResponse, domTreeToBeUpdated, change,
                                    newPartialChanges);

                            // boasting = boast(newPartialChanges);
                        }
                        updateHTMLTag(domTreeToBeUpdated, change.getFirstChild(), change.getId());
                    }

                }
                else if ((!"error-name".equals(change.getNodeName()))
                        && (!"error-message".equals(change.getNodeName()))) {
                    LOGGER.severe("Unexpected JSF response (" + change.getNodeName() + ")");
                }
            }
        }
        else {
            final HTMLTag responseWithAdditionalIDs = new HTMLTag(currentResponse);
            sessionMap.remove(LAST_KNOWN_HTML_KEY);
            sessionMap.put(LAST_KNOWN_HTML_KEY, responseWithAdditionalIDs);
            String body = responseWithAdditionalIDs.getChildren().get(1).toString();
            int bodyIndex = currentResponse.indexOf("<body>");
            if (bodyIndex < 0) {
                bodyIndex = currentResponse.indexOf("<body ");
            }
            int bodyEndIndex = currentResponse.indexOf("</body>");
            if ((bodyIndex > 0) && (bodyEndIndex > 0)) {
                currentResponse = currentResponse.substring(0, bodyIndex)
                        + "\n<!-- Optimized by BabbageFaces, an AngularFaces subproject -->\n" + body
                        + currentResponse.substring(bodyEndIndex + "</body>".length());
            }
        }
        if (isDeveloperMode) {
            int optimizedLength = currentResponse.length();
            DEBUG_optimizedBytesCumulated += optimizedLength;
            DEBUG_originalBytesCumulated += originalLength;
            String responseMessage;
            if (isAJAX) {
                responseMessage = "AXAX - original response:  " + originalLength + " bytes  Optimized response: "
                        + optimizedLength + " bytes  total original: " + DEBUG_originalBytesCumulated
                        + "  total optimized: " + DEBUG_optimizedBytesCumulated;
                LOGGER.info(responseMessage);
            }
            else {
                responseMessage = "HTML - original response:  " + originalLength + " bytes  Optimized response: "
                        + optimizedLength + " bytes  total original: " + DEBUG_originalBytesCumulated
                        + "  total optimized: " + DEBUG_optimizedBytesCumulated;
                LOGGER.info(responseMessage);

            }
            // if (currentResponse.contains("babbageFacesBoasting")) {
            // boasting = "<div>" + responseMessage + "<br />" + boasting + "</div>";
            // currentResponse = currentResponse.replace("babbageFacesBoasting", boasting);
            // }

        }
        return currentResponse;
    }
}
