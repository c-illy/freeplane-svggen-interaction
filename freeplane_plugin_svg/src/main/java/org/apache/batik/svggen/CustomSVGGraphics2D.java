
package org.apache.batik.svggen;

import java.awt.Graphics;
import java.io.Writer;
import org.w3c.dom.Element;
import org.freeplane.core.extension.Configurable;
import org.freeplane.features.map.MapModel;

/**
* Needed only to redirect to CustomDOMGroupManager instead of DOMGroupManager
*/
public class CustomSVGGraphics2D extends SVGGraphics2D {

    public CustomSVGGraphics2D(CustomSVGGraphics2D g){
        super(g);

        this.domTreeManager.removeGroupManager(this.domGroupManager);//wrong class
        this.domGroupManager = new CustomDOMGroupManager(this.gc, this.domTreeManager);//DOMGroupManager
        this.domTreeManager.addGroupManager(domGroupManager);
    }

	public CustomSVGGraphics2D(SVGGeneratorContext generatorCtx,
                         boolean textAsShapes) {
        super(generatorCtx, textAsShapes);
    }

    @Override
    protected void setGeneratorContext(SVGGeneratorContext generatorCtx) {
        super.setGeneratorContext(generatorCtx);
        
        this.domTreeManager.removeGroupManager(this.domGroupManager);//wrong class
        this.domGroupManager = new CustomDOMGroupManager(this.gc, domTreeManager);//CustomDOMGroupManager
        this.domTreeManager.addGroupManager(domGroupManager);
    }
        

    @Override
    public Graphics create() {
        return new CustomSVGGraphics2D(this);
    }

    public void prepareCustomSVGGen(MapModel mapModel, Configurable mapView) {
        CustomDOMGroupManager.prepareCustomSVGGen(mapModel, domTreeManager, mapView);
    }

    @Override
    public void stream(Writer writer, boolean useCss, boolean escaped)
        throws SVGGraphics2DIOException {
        Element svgRoot = getRoot();//getRoot()=>domTreeManager recycles topLevelGroup, new calls will be empty!
        CustomDOMGroupManager.finishCustomSVGGen(svgRoot);
        stream(svgRoot, writer, useCss, escaped);
        // System.out.println("svg gen finished");
    }

    @Override
    public void stream(Writer writer, boolean useCss)
        throws SVGGraphics2DIOException {
        stream(writer, useCss, false);
    }

}
