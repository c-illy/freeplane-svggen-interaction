package org.apache.batik.svggen;

import java.util.Collection;
import java.util.HashMap;
import java.util.ArrayList;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.link.ConnectorModel;

import org.apache.batik.ext.awt.g2d.GraphicContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

import org.freeplane.features.link.NodeLinks;
import org.freeplane.features.link.NodeLinkModel;
import org.freeplane.core.extension.Configurable;
import org.freeplane.features.attribute.NodeAttributeTableModel;

/**
* Tested only for nodes and connectors. Everything else should be exported the same
* than in the java view.
*
* To export background or not, user can use option : Appearence : white background on print.
*
* To export every MM nodes, user must unfold them all.
* To export some folded/unfoldable, user must add a 'unfolded'='false' MM attribute to 
* each MM node to fold.
*
* Still to do : implement unfold gizmos instead of uggly tmp edges residues.
*
* For algorithm readability, 'unfold' is written 'expand' :
* 'fold = true' would mean 'show = false'
* 'unfold = true' would be disturbing because of the double-negation 'un' - 'fold'
* so instead : 'expand = true' means 'show = true'
*/
public class CustomDOMGroupManager extends DOMGroupManager {

    static final HashMap<String, ConnectorModel> allConnectorModels = new HashMap<String, ConnectorModel>();//similar to Map<String,NodeModel> MapModel.nodes
    static final HashMap<String, Element> allCustomGroups = new HashMap<String, Element>();
    private static final ArrayList<Element> allTokens = new ArrayList<Element>();//"token" as in grammar parsing (elems tags : rect, text, path...)
    static MapModel mapModel = null;
    static Document domDoc = null;
    static Element centralNodefirstToken = null;
    static final String CHILD_OF_ATTR = "childOf";
    static final String PARENT_OF_ATTR = "parentOf";

    public static void prepareCustomSVGGen(MapModel givenMapModel, DOMTreeManager domTreeManager, Configurable component) {
        mapModel = givenMapModel;
        allConnectorModels.clear();
        allCustomGroups.clear();
        allTokens.clear();
        domDoc = domTreeManager.getDOMFactory();
        centralNodefirstToken = null;

        NodeModel centralNode = mapModel.getRootNode();

        Element tmpPendingRoot = domDoc.createElementNS(SVG_NAMESPACE_URI, SVG_G_TAG);
        tmpPendingRoot.setAttribute("id", "cortege");
        allCustomGroups.put("cortege", tmpPendingRoot);

        //DFS to fill allConnectorModels and allCustomGroups
        prepareFromModel(centralNode, tmpPendingRoot, tmpPendingRoot);
    }

    private static void prepareFromModel(NodeModel currentNodeModel, Element parentCustomGroup, Element tmpPendingRoot) {
        int nbChildren = currentNodeModel.getChildCount();
        String currCustomGId = currentNodeModel.getID();
        String parentCustomGId = parentCustomGroup.getAttribute("id");

        //connectors

        // MapLinks links = MapLinks.getLinks(mapModel);//false lead
        Collection<NodeLinkModel> linksOfAllKinds = NodeLinks.getLinks(currentNodeModel);
        for(NodeLinkModel linkOfAnyKind : linksOfAllKinds) {
            if(linkOfAnyKind instanceof ConnectorModel) {
                ConnectorModel c = (ConnectorModel)linkOfAnyKind;
                String cID = makeConnectorID(c);//c.toString();
                if(!(allConnectorModels.containsKey(cID))) {
                    allConnectorModels.put(cID, c);

                    Element currentConnectorGroup = domDoc.createElementNS(SVG_NAMESPACE_URI, SVG_G_TAG);
                    NodeModel targetParent = c.getTarget().getParentNode();
                    String targetParentId;
                    if(targetParent != null) {
                        targetParentId = targetParent.getID();
                    } else {
                        targetParentId = "cortege";
                    }
                    currentConnectorGroup.setAttribute("id", cID);
                    currentConnectorGroup.setAttribute("connectorFrom", parentCustomGId);
                    currentConnectorGroup.setAttribute("connectorTo", targetParentId);
                    currentConnectorGroup.setAttribute("visibility","hidden");
                    allCustomGroups.put(cID, currentConnectorGroup);
                    tmpPendingRoot.insertBefore(currentConnectorGroup, tmpPendingRoot.getFirstChild());
                }
            }
        }

        //tree structure

        if(nbChildren > 0) {
            Element currCustomG = domDoc.createElement(SVG_G_TAG);

            //id="ID_123456" expanded="false" visibility="hidden"
            currCustomG.setAttribute("id", currCustomGId);//id="ID_123456"
            NodeAttributeTableModel attrTable = NodeAttributeTableModel.getModel(currentNodeModel);
            int foldAttrIndex = attrTable.getAttributeIndex("unfolded"); // -1 if none
            String expandedValue;
            if((foldAttrIndex != -1) && 
                (attrTable.getValue(foldAttrIndex).toString().equalsIgnoreCase("false"))) {
                expandedValue = "false";
            } else {
                expandedValue = "true";
            }
            String visValue;
            if(("hidden").equals(parentCustomGroup.getAttribute​("visibility"))) {
                visValue = "hidden";
            } else if(expandedValue.equals("false")) {
                visValue = "hidden";
            } else {
                visValue = "visible";
            }
            currCustomG.setAttribute("expanded", expandedValue);
            currCustomG.setAttribute("visibility", visValue);
            
            parentCustomGroup.appendChild(currCustomG);//put next level group
            allCustomGroups.put(currCustomGId, currCustomG);

            //recurs
            for (int i=0; i < currentNodeModel.getChildCount(); i++) {
                NodeModel child = currentNodeModel.getChildAt(i);
                prepareFromModel(child, currCustomG, tmpPendingRoot);
            }
        }
    }

    public static void finishCustomSVGGen(Element svgRoot) {
        //cortegeGroup = (Element)(svgRoot.getFirstChild()); //wrong
        NodeList gList = svgRoot.getElementsByTagName​(SVG_G_TAG);
        Element cortegeGroup = (Element)(gList.item(0));// 0 : empirically found index

        //add svg interactivity to cortegeGroup
        cortegeGroup.setAttribute("id", "cortege");
        cortegeGroup.setAttribute("visibility", "visible");
        cortegeGroup.setAttribute("onmousedown", JSInteraction.CALL);
        Element scriptElem = domDoc.createElement("script");
        // CDATA stands for Character Data, means to de-specialize xml tokens <,>,&,'," ...
        scriptElem.appendChild(domDoc.createCDATASection(JSInteraction.FUNCTIONS));//big js function
        cortegeGroup.appendChild(scriptElem);
        
        //move customsGroups from tmpPendingRoot to their final place in custom tree
        ArrayList<Element> topCustomGroups = new ArrayList<Element>();
        Element tmpPendingRoot = allCustomGroups.get("cortege");
        NodeList children = tmpPendingRoot.getChildNodes();
        for(int i=0; i<children.getLength();i++) {
            Element child = (Element)(children.item(i));
            topCustomGroups.add(child);

            if(child.hasAttribute​("expanded")) {
                child.setAttribute("cursor", "pointer");
            }
        }
        for(Element topCustGroup : topCustomGroups) {
            tmpPendingRoot.removeChild​(topCustGroup);
            cortegeGroup.appendChild(topCustGroup);
        }

        allCustomGroups.put("cortege", cortegeGroup);//replace old tmp by real one

        //note : for clouds to respect z-order, tokens must be inserted before custom groups

        //move (wrapped) tokens from cortege group to their final place in custom tree
        for(Element token : allTokens) {
            if(token.hasAttribute​(CHILD_OF_ATTR)) {
                String destId = token.getAttribute(CHILD_OF_ATTR);
                Element parentG = allCustomGroups.get(destId);
                Node tokenWrapper = token.getParentNode();
                Node tokenWrapperOldParent = tokenWrapper.getParentNode();
                if(tokenWrapperOldParent != parentG) {
                    tokenWrapperOldParent.removeChild​(tokenWrapper);
                    Node currPassedSibling = parentG.getLastChild();
                    Node prevPassedSibling = null;
                    while((currPassedSibling instanceof Element)
                        && (((Element)(currPassedSibling)).hasAttribute​("expanded"))) {
                        //custom group, continue searching
                        prevPassedSibling = currPassedSibling;
                        currPassedSibling = currPassedSibling.getPreviousSibling();
                    }
                    parentG.insertBefore(tokenWrapper, prevPassedSibling);
                }

                //remove tmp attributes
                token.removeAttribute(CHILD_OF_ATTR);
            }
        }

        if(centralNodefirstToken != null) {
            Element scrollCenterGroup = (Element)(centralNodefirstToken.getParentNode());
            scrollCenterGroup.setAttribute("id", "scrollCenter");
        }
        svgRoot.setAttribute("onload", "rootScroll()");
        svgRoot.setAttribute("cursor", "grab");

        //connectors visibility
        for(String cntrId : allConnectorModels.keySet()) {
            Element cntrGroup = allCustomGroups.get(cntrId);
            String sourceId = cntrGroup.getAttribute("connectorFrom");
            String targetId = cntrGroup.getAttribute("connectorTo");
            String sourceVis = allCustomGroups.get(sourceId).getAttribute("visibility");
            String targetVis = allCustomGroups.get(targetId).getAttribute("visibility");
            if(("visible").equals(sourceVis) && ("visible").equals(targetVis)) {
                cntrGroup.setAttribute("visibility", "visible");
            }
        }

        //reduce future file size by truncating too precise numbers
        //(can half the size of a ~1MB svg file if ctx.setPrecision(12))
        //(ctx.setPrecision(3) reduces almost all numbers precision but 
        // not in fonts nor gradients)
        //reduceNumbersPrecision(svgRoot);//unsignificant if ctx.setPrecision(3): less than 1% difference

        allConnectorModels.clear();
        allCustomGroups.clear();
        allTokens.clear();
        centralNodefirstToken = null;
    }

    private static String makeConnectorID(ConnectorModel c) {
        String full = c.toString();//class name @ adress
        return (full.replaceFirst(".*@", "")) + "ctr";
    }

    private boolean isCutomGroup(Node node) {
        return (node instanceof Element)
            && (((Element)(node)).hasAttribute​("expanded"));
    }

	public CustomDOMGroupManager(GraphicContext gc, DOMTreeManager dtm) {
		super(gc, dtm);
	}

	@Override
    public void addElement(Element element, short method) {
        // Element currCurrG = currentGroup;
        String currMMID = null;
        String mmType = null;
        if(ConnectorModel.connectorBeingPainted != null) {
            currMMID = makeConnectorID(ConnectorModel.connectorBeingPainted);
            element.setAttribute(CHILD_OF_ATTR, currMMID);
            allTokens.add(element);
        } else if(NodeModel.nodesBeingPainted.size() >= 1) {
            currMMID = NodeModel.nodesBeingPainted.peek().getID();
            element.setAttribute(PARENT_OF_ATTR, currMMID);

            if((centralNodefirstToken == null) &&
                    (element.getTagName().equals(SVG_RECT_TAG)) ) {
                // first rect
                centralNodefirstToken = element;
            }
        }

        super.addElement(element, method);

        //prepare future move from cortege to final hierarchized customGroup
        int depth = NodeModel.nodesBeingPainted.size();
        if(depth >= 2) {
            NodeModel parent = NodeModel.nodesBeingPainted.get(depth - 2);
            element.setAttribute(CHILD_OF_ATTR, parent.getID());
            allTokens.add(element);
        }
    }

    // private static void reduceNumbersPrecision(Node root) {
    //     NamedNodeMap attrs = root.getAttributes();
    //     if((attrs != null) && (root instanceof Element)) {
    //         Element elem = (Element)root;
    //         for(int i=0; i<attrs.getLength(); i++) {
    //             Node attr = attrs.item(i);
    //             String attrName = attr.getNodeName();
    //             if(!(attrName.contains(":"))) {//otherwise, bug with xwml:space
    //                 String attrVal = elem.getAttribute(attrName);
    //                 //keep max 3 digits after floating point
    //                 String newAttrVal = attrVal.replaceAll("\\.(\\d\\d\\d)\\d+",".$1");
    //                 elem.setAttribute(attrName, newAttrVal);
    //             }
    //         }
    //     }
    //     NodeList children = root.getChildNodes();
    //     for(int i=0; i<children.getLength();i++) {
    //         reduceNumbersPrecision(children.item(i));
    //     }
    // }

}
