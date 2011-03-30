

BSplineGui : ObjectGui {
	
	var order,orderSpec,uv,gridLines;

	writeName {}
	
	gui { arg parent, bounds;
		^super.gui(parent, bounds ?? {Rect(0,0,300,200)})
	}
	
	guiBody { arg layout;
		var maxx,maxy,miny,scalex,scaley,scale,br,bounds;
		var range=4,selected;
		var lastps;
		bounds = layout.innerBounds.insetAll(0,0,20,0);
		
		uv = UserView( layout, bounds );//.resize_(5);
		uv.background = GUI.skin.background;
		br = bounds.height;
		
		// this can recalc on rezoom
		maxx = model.points.last.x * 1.25;
		
		if(model.spec.isNil,{
			maxy = model.points.maxValue(_.y) * 1.25;
			miny = model.points.minValue(_.y); // not sure if 0 is floor, no idea of spec
		},{
			maxy = model.spec.maxval;
			miny = model.spec.minval;
		});
		gridLines = GridLines(uv,bounds,model.spec ?? {[miny,maxy].asSpec},ControlSpec(0,maxx,units:"sec"));
		
		scalex = bounds.width.asFloat / maxx;
		scaley = bounds.height.asFloat / maxy; // ignoring miny for now
		scale = scalex@scaley;
		
		uv.drawFunc_({
			var ps,p,ips;
			gridLines.draw;
			ps = model.points ++ [model.order];
			//if(lastps != ps,{
				lastps = ps;
				
				Color.black.alpha_(0.5).set; 
				model.points.do { |point,i|
					var p;
					p = point * scale;
					p = p.x@(br - p.y);
					Pen.addArc(p,range,0,2pi).stroke
				};
				
				Color.blue.set;
				p = model.points.first;
				Pen.moveTo( p.x@(br-p.y) );
				// wrong,
				// this loop display is dependent on how you intend to use the spline
				if(model.loop,{
					ips = model.wrapxInterpolate(16)
				},{
					ips = model.interpolate(16)
				});
				ips.do { arg point,i;
					var p;
					p = point * scale;
					Pen.lineTo(p.x@(br - p.y))
				};
				Pen.stroke;
			//})
		});
		uv.focusColor = GUI.skin.foreground.alpha_(0.4);
		uv.mouseDownAction = { |uvw, x, y|
			var distances,p;
			p = x@(br-y);
			selected = model.points.detectIndex({ |pt|
				(pt * scale).dist(p) <= range
			});
		};
			
		uv.mouseMoveAction = { |uvw, x,y| 
			var p;
			p = x@(br-y);
			if( selected.notNil ) { 
				model.points[selected] = p / scale;
				model.changed;
			}; 
		};
		uv.refresh;
		this.curveGui(layout);
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


	