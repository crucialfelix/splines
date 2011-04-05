

SplineGui : ObjectGui {
	
	// 2D spline editor

	var spec,domainSpec;
	var order,orderSpec,uv,gridLines;
	var <>range=5;
	var selected;
	var scale,boundsHeight;

	writeName {}
	
	gui { arg parent, bounds, argSpec,argDomainSpec;
		spec = argSpec;
		domainSpec = argDomainSpec;
		^super.gui(parent, bounds ?? {Rect(0,0,300,200)})
	}
	
	guiBody { arg layout;
		var scalex,scaley,bounds;
		var lastps,grey;
		var sp,ds;
		
		bounds = layout.innerBounds.insetAll(0,0,20,0);
		
		uv = UserView( layout, bounds );//.resize_(5);
		uv.background = GUI.skin.background;
		boundsHeight = bounds.height;
		
		// this can recalc on resize
		sp = this.spec;
		ds = this.domainSpec;
		gridLines = GridLines(uv,bounds,sp,ds);
		
		scalex = bounds.width.asFloat / ds.range;
		scaley = bounds.height.asFloat / sp.range;
		scale = scalex@scaley;
		
		grey = Color.black.alpha_(0.5);
		uv.drawFunc_({

			gridLines.draw;
				
			grey.set; 
			model.xypoints.do { |point,i|
				point = this.map(point);
				Pen.addArc(point,range,0,2pi);
				if(i==selected,{
					Color.blue.set;
					Pen.fill;
					Color.blue(alpha:0.3).set;
					Pen.moveTo(0@point.y);
					Pen.lineTo(Point(bounds.width-1,point.y));
					Pen.moveTo(point.x@0);
					Pen.lineTo(Point(point.x,bounds.height-1));
					Pen.stroke;
					
					grey.set; 
				},{
					Pen.stroke;
				})
			};
			this.drawControlPoints();
			
			Color.blue.set;
			Pen.moveTo( this.map( model.points[0]) );

			model.interpolate(32).do { arg point,i;
				Pen.lineTo( this.map(point) )
			};
			Pen.stroke;
		});
		uv.focusColor = GUI.skin.foreground.alpha_(0.4);
		this.makeMouseActions;
		
		this.update;
		if(model.interpolationKey != 'linear',{
			this.curveGui(layout);
		});
	}
	map { arg p;
		p = p.asPoint * scale;
		^Point(p.x,boundsHeight-p.y)
	}
	makeMouseActions {
		uv.mouseDownAction = { |uvw, x, y,modifiers, buttonNumber, clickCount|
			var p;
			p = x@(boundsHeight-y);
			selected = model.xypoints.detectIndex({ |pt|
				(pt * scale).dist(p) <= range
			});
			if(selected.notNil,{
				uv.refresh
			},{
				if(clickCount == 2,{
					selected = this.createPoint(p/scale);
					uv.refresh;
				});
			});
		};
			
		uv.mouseMoveAction = { |uvw, x,y| 
			var p;
			p = x@(boundsHeight-y);
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
			// let's assume time
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
	drawControlPoints {}
	createPoint { arg p;
		var pnext,pprev,i;

		// non-ideal
		// should be close to a line
		// close to an interpolation point
		if(selected.notNil,{
			// one after current selected
			i = (selected + 1).clip(0,model.points.size-1);
		},{
			// guessing. badly
			p = p.asPoint;
			i = model.points.minIndex({ arg pt,i;
				p.dist(pt.asPoint)
			});
			pprev = model.points[i-1];
			pnext = model.points[i+1];
			if(pprev.notNil,{
				pprev = pprev.asPoint - p;
			});
			if(pnext.notNil,{
				pnext = pnext.asPoint - p;
			},{
			     i = i + 1;	
			});
			if(pnext.notNil and: pprev.notNil,{
				if(pnext.x.sign > pprev.x.sign,{
					i = i + 1; // after
				},{
					if(pnext.x.sign == pprev.x.sign,{
						if(pnext.y.sign > pprev.y.sign,{
							i + i + 1;
						})
					})
				})
			});
		});
		model.createPoint(p.asArray,i);
		^i
	}	

}


// LoopSplineEditor
// BezierPathEditor
// SplineMapperGui

SplineMapperGui : SplineGui {
	
}


BezierSplineGui : SplineGui {
	
	var selectedCP,flatpoints;
	
	drawControlPoints {
		model.controlPoints.do { |cps,cpi|
			var next;
			Color(0.55223880597015, 0.36106502462071, 0.20605925595901).set;			cps.do { arg point,i;
				Pen.addArc(this.map(point),range,0,2pi);
				if(selectedCP == [cpi,i],{
					Pen.fill;
				},{
					Pen.stroke;
				});
			};
			
			Color(0.55223880597015, 0.36106502462071, 0.20605925595901, 0.5).set;			Pen.moveTo(this.map(model.points[cpi]));
			cps.do { arg point,i;
				Pen.lineTo(this.map(point));
			};
			if(model.isClosed,{
				next = model.points.wrapAt(cpi+1)
			},{
				next = model.points.at(cpi+1)
			});
			if(next.notNil,{
				Pen.lineTo(this.map(next))
			});
			Pen.stroke;
		};
	}
	curveGui {}
	update {
		// only on points changed
		flatpoints = [];
		model.xypoints.do { arg xy,i;
			flatpoints = flatpoints.add( [xy * scale,\point,i] )
		};
		model.controlPoints.do { arg cps,cpi;
			cps.do { arg xy,i;
				flatpoints = flatpoints.add( [xy.asPoint * scale,\cp, [cpi,i] ] )
			}
		};
		uv.refresh;
	}

	makeMouseActions {
		uv.mouseDownAction = { |uvw, x, y,modifiers, buttonNumber, clickCount|
			var p,fp,i;
			p = (x@(boundsHeight-y));
			fp = flatpoints.detect({ arg fp;
				fp[0].dist(p) <= range
			});
			if(fp.notNil,{ 
				if(fp[1] == 'point',{
					selected = fp[2];
					selectedCP = nil;
				},{
					selected = nil;
					selectedCP = fp[2];
				});
				uv.refresh;
			},{
				if(clickCount == 2,{
					if(modifiers.isCtrl,{
						i = this.createControlPoint(p/scale);
						selected = nil;
						selectedCP = i;
					},{
						i = this.createPoint(p/scale);
						selected = i;
						selectedCP = nil;
					});
				});					
			});
		};
			
		uv.mouseMoveAction = { |uvw, x,y| 
			var p;
			p = x@(boundsHeight-y);
			p = p / scale;
			if(spec.notNil,{
				p.y = spec.constrain(p.y)
			});
			if(domainSpec.notNil,{
				p.x = spec.constrain(p.x)
			});
			if( selected.notNil,{
				model.points[selected][0] = p.x;
				model.points[selected][1] = p.y;
				model.changed;
			},{
				if(selectedCP.notNil,{
					model.controlPoints[selectedCP[0]][selectedCP[1]][0] = p.x;
					model.controlPoints[selectedCP[0]][selectedCP[1]][1] = p.y;
					model.changed;
				});
			}); 
		};
		// key down action
			// delete selected
			// move by arrows
			// select many, move together
	}
	
	createControlPoint { arg p;
		// select a point then control double click to create a control for it
		var s;
		s = selected ?? { if(selectedCP.notNil,{selectedCP[0]},0) };
		if(s == (model.points.size-1) and: {model.isClosed.not},{
			s = max(s - 1,0);
		});
		model.createControlPoint(p.asArray,s);
		^[s,model.controlPoints[s].size-1]
	}	
			
}
	

	