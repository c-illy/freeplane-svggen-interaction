
package org.apache.batik.svggen;

/**
* Only a quick-and-dirty way to inject javascript 'template' to future svg file
*/
public class JSInteraction{
	public static final String CALL = "mmfold(evt)";

	public static final String FUNCTIONS =
"\n" +

// onload scroll center

"function rootScrollNow(){\n" +
"  var c = document.getElementById('scrollCenter');\n" +
"  c.scrollIntoView({behavior: 'smooth', block: 'center', inline: 'center'});\n" +
"}\n" +
"function rootScroll(){\n" +
"  setTimeout(rootScrollNow, 1);\n" +
"}\n" +
"\n" +

// drag to scroll

"var mmDragScrollStart = { scrollTop: 0, scrollLeft: 0, clientX: 0, clientY: 0 };\n" +
"var svgElem = document.querySelector('svg');\n" +
"\n" +
"function mmDragScrollMouseDown(e){\n" +
"  mmDragScrollStart = { \n" +
"    scrollTop: svgElem.scrollTop,\n" +
"    scrollLeft: svgElem.scrollLeft,\n" +
"    clientX: e.clientX,\n" +
"    clientY: e.clientY\n" +
"  };\n" +
"  svgElem.setAttribute('cursor', 'grabbing');\n" +
"  svgElem.setAttribute('style', 'user-select: none;');\n" +
"  document.addEventListener('mousemove', mmDragScrollMouseMove);\n" +
"  document.addEventListener('mouseup', mmDragScrollMouseUp);\n" +
"}\n" +
"function mmDragScrollMouseMove(e){\n" +
"  var dx = e.clientX - mmDragScrollStart.clientX;\n" +
"  var dy = e.clientY - mmDragScrollStart.clientY;\n" +
"  svgElem.scrollLeft = mmDragScrollStart.scrollLeft - dx;\n" +
"  svgElem.scrollTop = mmDragScrollStart.scrollTop - dy;\n" +
"}\n" +
"function mmDragScrollMouseUp(e){\n" +
"  svgElem.setAttribute('cursor', 'grab');\n" +
"  svgElem.removeAttribute('style');\n" +
"  document.removeEventListener('mousemove', mmDragScrollMouseMove);\n" +
"  document.removeEventListener('mouseup', mmDragScrollMouseUp);\n" +
"}\n" +
"document.onmousedown = mmDragScrollMouseDown;\n" +
"\n" +

// click to fold/unfold
// (for algorithm readability, 'unfold' is written 'expand' to avoid the double-negation 'un' - 'fold')

"function mmfold(evt){\n" +
"  var elem = evt.target;\n" +
"  var targetId = elem.getAttribute('parentOf');\n" +


"  if((targetId == null) || (targetId == '')){\n" +
    //background (or ?), let event call mmDragScrollMouseDown() instead
"    return;\n" +
"  }\n" +

"  var target = document.querySelector('#'+targetId);\n" +
//"  var target = document.querySelector('#'+elem.getAttribute('rootOf'));\n" +
"  if(target == null){\n" +
    //leaf node, no sub-branch to fold
"    evt.stopPropagation();\n" + //prevent mmDragScrollMouseDown() call
"    return;\n" +
"  }\n" +
"  var oldVis = target.getAttribute('visibility');\n" +
"  var newVis;\n" +
"  var newExpanded;\n" +
//"  var targetId = target.getAttribute('id');\n" +
"  if(oldVis == 'hidden'){\n" +
"    newVis = 'visible';\n" +
"    newExpanded = 'true';\n" +
"    updateConnectorsVis(targetId, 'connectorFrom', 'visible', 'hidden', 'visible', 'connectorTo');\n" +
"    updateConnectorsVis(targetId, 'connectorTo', 'visible', 'hidden', 'visible', 'connectorFrom');\n" +
"  }else{\n" +
"    newVis = 'hidden';\n" +
"    newExpanded = 'false';\n" +
"    updateConnectorsVis(targetId, 'connectorFrom', 'visible', 'visible', 'hidden', 'connectorTo');\n" +
"    updateConnectorsVis(targetId, 'connectorTo', 'visible', 'visible', 'hidden', 'connectorFrom');\n" +
"  }\n" +
"\n" +
"  target.setAttribute('visibility', newVis);\n" +
"  target.setAttribute('expanded', newExpanded);\n" +
"\n" +
//"  visibility='...' is overridden by different visibility='...' in deeper nodes\n" +
"  updateBranchVis(newExpanded=='true', target);\n" +
"  evt.stopPropagation();\n" +//prevent mmDragScrollMouseDown() call
"}\n" +
"\n" +
"function updateBranchVis(parentExpand, elem){\n" +
"  var elemId = elem.getAttribute('id');\n" +
"  var oldVis = elem.getAttribute('visibility');\n" +
"  var oldUnfol = elem.getAttribute('expanded');\n" +
"  if((oldVis != null) && (oldUnfol != null)){\n" +
"    oldVis = (oldVis == 'visible');\n" +
"    oldUnfol = (oldUnfol == 'true');\n" +
"    if((!oldVis) && \n" +
"            (oldUnfol) &&\n" +
"            (parentExpand)){\n" +
"      elem.setAttribute('visibility','visible');\n" +
"      updateConnectorsVis(elemId, 'connectorFrom', 'visible', 'hidden', 'visible', 'connectorTo');\n" +
"      updateConnectorsVis(elemId, 'connectorTo', 'visible', 'hidden', 'visible', 'connectorFrom');\n" +
"    }else if((oldVis) && \n" +
"            (oldUnfol) &&\n" +
"            (!parentExpand)){\n" +
"      elem.setAttribute('visibility','hidden');\n" +
"      updateConnectorsVis(elemId, 'connectorFrom', 'visible', 'visible', 'hidden', 'connectorTo');\n" +
"      updateConnectorsVis(elemId, 'connectorTo', 'visible', 'visible', 'hidden', 'connectorFrom');\n" +
"    }\n" +
"    var branchExpand = oldUnfol && parentExpand;\n" +
"    for(var i=0; i<elem.children.length; i++){\n" +
"      updateBranchVis(branchExpand, elem.children[i]);\n" +
"    }\n" +
"  }\n" +
"}\n" +
"\n" +
"function updateConnectorsVis(elemId, directionAttrName, otherVisCheck, cntrOldVisCheck, newVis, oppositeDirection){\n" +
//"  connectors from or to elemId\n" +
"  var connectorsFrom = document.querySelectorAll('g[' + directionAttrName + '=\\'' + elemId + '\\']');\n" +
"  for(var i=0; i<connectorsFrom.length; i++){\n" +
"    var cntrOldVis = connectorsFrom[i].getAttribute('visibility');\n" +
"    var otherEndGId = connectorsFrom[i].getAttribute(oppositeDirection);\n" +
"    var otherVis = document.querySelector('#'+otherEndGId).getAttribute('visibility');\n" +
"    if((otherVis == otherVisCheck) && (cntrOldVis == cntrOldVisCheck)){\n" +
"      connectorsFrom[i].setAttribute('visibility', newVis);\n" +
"    }\n" +
"  }\n" +
"}\n";
}
