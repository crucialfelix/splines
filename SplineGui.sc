

SplineGui : ObjectGui {
	
	// 2D spline editor

	var spec,domainSpec;
	var order,orderSpec,uv,gridLines;

	writeName {}
	
	gui { arg parent, bounds, argSpec,argDomainSpec;
		spec = argSpec;
		domainSpec = argDomainSpec;
		^super.gui(parent, bounds ?? {Rect(0,0,300,200)})
	}
	
	guiBody { arg layout;
		var scalex,scaley,scale,br,bounds;
		var range=7,selected;
		var lastps,mapPoint,grey;
		var sp,ds;
		
		bounds = layout.innerBounds.insetAll(0,0,20,0);
		
		uv = UserView( layout, bounds );//.resize_(5);
		uv.background = GUI.skin.background;
		br = bounds.height;
		
		// this can recalc on rezoom
		sp = this.spec;
		ds = this.domainSpec;
		gridLines = GridLines(uv,bounds,sp,ds);
		
		scalex = bounds.width.asFloat / ds.range;
		scaley = bounds.height.asFloat / sp.range;
		scale = scalex@scaley;
		
		grey = Color.black.alpha_(0.5);
		mapPoint = { arg p;
			p = p * scale;
			Point(p.x,br-p.y)
		};
		uv.drawFunc_({

			gridLines.draw;
				
			grey.set; 
			model.xypoints.do { |point,i|
				Pen.addArc(mapPoint.value(point),range,0,2pi);
				if(i==selected,{
					Color(0.28358208955224, 0.69296375266525, 1.0).set;
					Pen.fill;
					grey.set; 
				},{
					Pen.stroke;
				})
			};
			
			Color.blue.set;
			Pen.moveTo( mapPoint.value( model.points[0].copyRange(0,1).asPoint) );

			model.interpolate(32).do { arg point,i;
				Pen.lineTo( mapPoint.value(point.asPoint) )
			};
			Pen.stroke;
		});
		uv.focusColor = GUI.skin.foreground.alpha_(0.4);
		uv.mouseDownAction = { |uvw, x, y,modifiers, buttonNumber, clickCount|
			var p;
			p = x@(br-y);
			selected = model.xypoints.detectIndex({ |pt|
				(pt * scale).dist(p) <= range
			});
			if(selected.notNil,{
				uv.refresh
			},{
				if(clickCount == 2,{
					model.createPoint(p/scale);
					uv.refresh;
				});
			});
		};
			
		uv.mouseMoveAction = { |uvw, x,y| 
			var p;
			p = x@(br-y);
			if( selected.notNil ) { 
				p = p / scale;
				if(spec.notNil,{
					p.y = spec.constrain(p.y)
				});
				if(domainSpec.notNil,{
					p.x = spec.constrain(p.x)
				});					
				model.points[selected][0] = p.x;
				model.points[selected][1] = p.y;
				model.changed;
			}; 
		};
		// key down action
		// delete selected
		
		uv.refresh;
		if(model.interpolationKey != 'linear',{
			this.curveGui(layout);
		});
	}
	spec {
		var miny,maxy;
		^spec ?? {
			miny = model.xypoints.minValue(_.y); // not sure if 0 is floor, no idea of spec
			maxy = model.xypoints.maxValue(_.y) * 1.25;
			[miny,maxy].asSpec
		}
	}
	domainSpec {
		var minx,maxx;
		^domainSpec ?? {
			minx = 0;
			maxx = model.xypoints.last.x * 1.25;
			// assuming time
			ControlSpec(minx,maxx,units:"sec")
		}
	}			
			
	update {
		uv.refresh
	}

	curveGui { arg layout;
		orderSpec = [2,8].asSpec;
		order = Slider( layout, 17@200 )
			.value_( model.order )
			.action_({
				model.order = orderSpec.map(order.value);
				model.changed
			});
		order.focusColor = GUI.skin.foreground.alpha_(0.4);
	}

}


// LoopSplineEditor
// BezierPathEditor
// SplineMapperGui

SplineMapperGui : SplineGui {
	
	
}


	