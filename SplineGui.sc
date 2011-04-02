

SplineGui : ObjectGui {
	
	// 2D spline editor

	var spec;
	var order,orderSpec,uv,gridLines;

	writeName {}
	
	gui { arg parent, bounds, argSpec;
		spec = argSpec;
		^super.gui(parent, bounds ?? {Rect(0,0,300,200)})
	}
	
	guiBody { arg layout;
		var maxx,maxy,miny,scalex,scaley,scale,br,bounds;
		var range=4,selected;
		var lastps,mapPoint;
		bounds = layout.innerBounds.insetAll(0,0,20,0);
		
		uv = UserView( layout, bounds );//.resize_(5);
		uv.background = GUI.skin.background;
		br = bounds.height;
		
		// this can recalc on rezoom
		maxx = model.xypoints.last.x * 1.25;

		if(spec.isNil,{
			maxy = model.xypoints.maxValue(_.y) * 1.25;
			miny = model.xypoints.minValue(_.y); // not sure if 0 is floor, no idea of spec
		},{
			maxy = spec.maxval;
			miny = spec.minval;
		});
		gridLines = GridLines(uv,bounds,spec ?? {[miny,maxy].asSpec},ControlSpec(0,maxx,units:"sec"));
		
		scalex = bounds.width.asFloat / maxx;
		scaley = bounds.height.asFloat / maxy; // ignoring miny for now
		scale = scalex@scaley;
		
		mapPoint = { arg p;
			p = p * scale;
			Point(p.x,br-p.y)
		};
		uv.drawFunc_({

			gridLines.draw;
				
			Color.black.alpha_(0.5).set; 
			model.xypoints.do { |point,i|
				Pen.addArc(mapPoint.value(point),range,0,2pi).stroke
			};
			
			Color.blue.set;
			Pen.moveTo( mapPoint.value( model.points[0].copyRange(0,1).asPoint) );

			model.interpolate(32).do { arg point,i;
				Pen.lineTo( mapPoint.value(point.asPoint) )
			};
			Pen.stroke;
		});
		uv.focusColor = GUI.skin.foreground.alpha_(0.4);
		uv.mouseDownAction = { |uvw, x, y|
			var distances,p;
			p = x@(br-y);
			selected = model.xypoints.detectIndex({ |pt|
				(pt * scale).dist(p) <= range
			});
		};
			
		uv.mouseMoveAction = { |uvw, x,y| 
			var p;
			p = x@(br-y);
			if( selected.notNil ) { 
				p = p / scale;
				model.points[selected][0] = p.x;
				model.points[selected][1] = p.y;
				model.changed;
			}; 
		};
		uv.refresh;
		if(model.interpolationKey != 'linear',{
			this.curveGui(layout);
		});
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

	