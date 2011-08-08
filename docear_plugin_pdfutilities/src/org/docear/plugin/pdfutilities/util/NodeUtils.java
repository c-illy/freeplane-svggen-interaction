package org.docear.plugin.pdfutilities.util;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.docear.plugin.pdfutilities.features.PdfAnnotationExtensionModel;
import org.docear.plugin.pdfutilities.features.PdfAnnotationExtensionModel.AnnotationType;
import org.docear.plugin.pdfutilities.pdf.PdfAnnotation;
import org.docear.plugin.pdfutilities.pdf.PdfFileFilter;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.link.NodeLinks;
import org.freeplane.features.link.mindmapmode.MLinkController;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;


public class NodeUtils {
	
	private final MMapController currentMapController;
	
	public NodeUtils(){
		this.currentMapController = (MMapController) Controller.getCurrentModeController().getMapController();
		
	}
	
	public Map<URI, Collection<NodeModel>> getPdfLinkedNodesFromCurrentMap(){
		return getPdfLinkedNodesFrom(this.currentMapController.getRootNode());
	}
	
	private Map<URI, Collection<NodeModel>> getPdfLinkedNodesFrom(NodeModel parent){
		Map<URI, Collection<NodeModel>> result = new HashMap<URI, Collection<NodeModel>>();
		
		if(isPdfLinkedNode(parent)){
			URI uri = Tools.getAbsoluteUri(NodeLinks.getLink(parent));
			if(uri != null){				
				result.put(uri, new ArrayList<NodeModel>());				
				result.get(uri).add(parent);
			}		 
		}
		
		for(NodeModel child : parent.getChildren()){
			Map<URI, Collection<NodeModel>> children = getPdfLinkedNodesFrom(child);
			for(URI uri : children.keySet()){
				if(!result.containsKey(uri)){
					result.put(uri, new ArrayList<NodeModel>());				
				}
				result.get(uri).addAll(children.get(uri));
			}
		}
		
		return result;
	}
	
	public NodeModel insertChildNodesFromPdf(URI uri, List<PdfAnnotation> annotations, boolean isLeft, NodeModel target){
		File file = Tools.getFilefromUri(uri);
		if(file == null){
			return null;
		}
		else{
			return this.insertChildNodesFromPdf(file, annotations, isLeft, target);
		}
	}
	
	public NodeModel insertChildNodesFromPdf(File pdfFile, List<PdfAnnotation> annotations, boolean isLeft, NodeModel target){
		NodeModel node = this.insertChildNodeFrom(pdfFile, isLeft, target, AnnotationType.PDF_FILE);
		this.insertChildNodesFrom(annotations, isLeft, node);
		return node;
	}
	
	public List<NodeModel> insertChildNodesFrom(List<PdfAnnotation> annotations, boolean isLeft, NodeModel target){
		List<NodeModel> nodes = new ArrayList<NodeModel>();
		
		for(PdfAnnotation annotation : annotations){
			NodeModel node = this.insertChildNodeFrom(annotation.getFile(), annotation, isLeft, target);
			this.insertChildNodesFrom(annotation.getChildren(), isLeft, node);
			nodes.add(node);
		}
		
		return nodes;
	}
	
	public NodeModel insertChildNodeFrom(File file, boolean isLeft, NodeModel target, AnnotationType type){
		final NodeModel node = this.currentMapController.newNode(file.getName(), target.getMap());
		this.setLinkFrom(file, node);
		
		if(type != null){
			PdfAnnotationExtensionModel model = new PdfAnnotationExtensionModel();
			model.setAnnotationType(type);
			PdfAnnotationExtensionModel.setModel(node, model);
		}
		
		return this.insertChildNodeFrom(node, isLeft, target);
	}
	
	public NodeModel insertChildNodeFrom(File file, PdfAnnotation annotation, boolean isLeft, NodeModel target){		
		final NodeModel node = this.currentMapController.newNode(annotation.getTitle(), target.getMap());
		PdfAnnotationExtensionModel model = new PdfAnnotationExtensionModel();
		model.setAnnotationType(annotation.getAnnotationType());
		model.setPage(annotation.getPage());
		PdfAnnotationExtensionModel.setModel(node, model);
		
		return insertChildNodeFrom(node, isLeft, target);
	}
	
	public NodeModel setLinkFrom(File file, NodeModel node){
		final URI uri;
		if (ResourceController.getResourceController().getProperty("links").equals("relative")) {
			uri = LinkController.toRelativeURI(node.getMap().getFile(), file);
		}
		else {
			uri = file.getAbsoluteFile().toURI();
		}
		
		((MLinkController) LinkController.getController()).setLink(node, uri, false);
		
		return node;
	}
	
	public NodeModel insertChildNodeFrom(NodeModel node, boolean isLeft, NodeModel target){
		
		this.currentMapController.insertNode(node, target, false, isLeft, isLeft);
		
		return node;
	}
	
	public static boolean isPdfLinkedNode(NodeModel node){
		URI link = NodeLinks.getValidLink(node);		
        return new PdfFileFilter().accept(link);
    }

	public List<NodeModel> insertNewChildNodesFrom(Collection<PdfAnnotation> annotations, boolean isLeft, NodeModel target) {
		List<NodeModel> nodes = new ArrayList<NodeModel>();
		
		for(PdfAnnotation annotation : annotations){
			if(annotation.isNew() || annotation.hasNewChildren()){
				NodeModel equalChild = targetHasEqualChild(target, annotation);
				if(equalChild == null){
					NodeModel node = this.insertChildNodeFrom(annotation.getFile(), annotation, isLeft, target);
					this.insertNewChildNodesFrom(annotation.getChildren(), isLeft, node);
					nodes.add(node);
				}
				else{
					this.insertNewChildNodesFrom(annotation.getChildren(), isLeft, equalChild);
					nodes.add(equalChild);
				}
				
			}		
		}
		
		return nodes;
	}
	
	public NodeModel targetHasEqualChild(NodeModel target, PdfAnnotation annotation){
		for(NodeModel child : target.getChildren()){
			URI uri = NodeLinks.getLink(child);
			uri = Tools.getAbsoluteUri(uri);
			if(child.getText() == null || uri == null) continue;
			if(child.getText().equals(annotation.getTitle()) && uri.equals(annotation.getAbsoluteUri())){
				return child;
			}
		}
		return null;
	}

}
