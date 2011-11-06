

SplineGui : ObjectGui {
	
	// 2D spline editor

	var <spec,<domainSpec;
	var <>density=128;
	var order,orderSpec,uv,<gridLines;
	var <>range=5;
	var selected;
	var boundsHeight,boundsWidth;
	var fromX,toX,fromXpixels=0,xScale=1.0;
	
	gui { arg parent, bounds, argSpec,argDomainSpec;
		if(argSpec.notNil,{
			spec = argSpec.asSpec
		},{
			spec = spec ?? {this.guessSpec};
		});
		if(argDomainSpec.notNil,{
			domainSpec = argDomainSpec.asSpec
		},{
			domainSpec = domainSpec ?? {this.guessDomainSpec};
		});
		^super.gui(parent, bounds ?? {Rect(0,0,300,200)})
	}
	
	guiBody { arg layout,b;
		var bounds;
		var grey;
		layout.decorator.margin = 0@0;
		layout.decorator.gap = 0@0;
		bounds = layout.decorator.bounds;
		
		uv = UserView( layout, bounds );
		this.background = GUI.skin.background;
		
		// this can recalc on resize
		boundsHeight = bounds.height.asFloat;
		boundsWidth = bounds.width.asFloat;

		gridLines = GridLines(uv,bounds,this.spec,domainSpec);
		this.setZoom(domainSpec.minval,domainSpec.maxval);
		
		grey = Color.black.alpha_(0.5);
		uv.drawFunc_({

			gridLines.draw;
						
			grey.set; 
			// can cache an array of Pen commands
			model.xypoints.do { |point,i|
				var focPoint;
				focPoint = point;
				point = this.map(point);
				Pen.addArc(point,range,0,2pi);
				if(i==selected,{
					Color.blue.set;
					Pen.fill;
					// crosshairs
					Color(0.92537313432836, 1.0, 0.0, 0.41791044776119).set;
					Pen.moveTo(0@point.y);
					Pen.lineTo(Point(bounds.width-1,point.y));
					Pen.moveTo(point.x@0);
					Pen.lineTo(Point(point.x,bounds.height-1));
					Pen.stroke;

					grey.set; 
					// text
					// better to be able to defer to the GridLines for string repr
					Pen.use {
						Pen.translate(point.x,point.y);
						Pen.rotate(0.5pi);
						focPoint.x.asFloat.asStringPrec(4).drawAtPoint(Point(-45,0),nil,grey);
					};
					focPoint.y.asFloat.asStringPrec(4).drawAtPoint(Point(point.x+15,point.y-15),nil,grey);
					
				},{
					Pen.stroke;
				})
			};
			this.drawControlPoints();
			
			Color.blue.set;
			Pen.moveTo( this.map( model.points[0]) );

			model.interpolate(density).do { arg point,i;
				Pen.lineTo( this.map(point) )
			};
			Pen.stroke;
		});
		this.focusColor = GUI.skin.focusColor ?? {GUI.skin.foreground.alpha_(0.4)};
		this.makeMouseActions;
		
		this.update;
		if(model.class !== LinearSpline,{
			this.curveGui(layout);
		});
	}
	map { arg p;
		// map a spline point to pixel point
		// for the upside down userview
		 p = p.asArray;
		^Point(
			domainSpec.unmap(p[0]) * boundsWidth - fromXpixels * xScale,
			boundsHeight - (spec.unmap(p[1]) * boundsHeight)
		)
	}
	rawMapX { arg x;
		// non-zoomed map: spline x to pixel x
		^domainSpec.unmap(x) * boundsWidth
	}
	unmap { arg point;
		// unmap a pixel point to a spline point-array
		var x;
		x = point.x / xScale + fromXpixels;
		^[
			domainSpec.map(x / boundsWidth),
			spec.map((boundsHeight - point.y) / boundsHeight)
		]
	}
	makeMouseActions {
		uv.mouseDownAction = { |uvw, x, y,modifiers, buttonNumber, clickCount|
			var p;
			p = x@y;
			selected = model.xypoints.detectIndex({ |pt|
				(this.map(pt)).dist(p) <= range
			});
			if(selected.notNil,{
				uv.refresh
			},{
				if(clickCount == 2,{
					selected = this.createPoint(this.unmap(p));
					uv.refresh;
				});
			});
		};
			
		uv.mouseMoveAction = { |uvw, x,y| 
			var spoint;
			if( selected.notNil ) { 
				spoint = this.unmap(x@y);
				if(spec.notNil,{
					spoint[1] = spec.constrain(spoint[1])
				});
				if(domainSpec.notNil,{
					spoint[0] = domainSpec.constrain(spoint[0])
				});					
				model.points[selected] = spoint;
				model.changed;
			}; 
		};
		// key down action
		// delete selected
		uv.keyDownAction = { arg view, char, modifiers, unicode, keycode;
			this.keyDownAction(view, char, modifiers, unicode, keycode);
		};
	}
	guessSpec {
		var miny,maxy;
		miny = model.xypoints.minValue(_.y); // not sure if 0 is floor, no idea of spec
		maxy = model.xypoints.maxValue(_.y) * 1.25;
		^ControlSpec(miny,maxy)
	}
	guessDomainSpec {
		var minx,maxx;
		minx = 0;
		maxx = model.xypoints.last.x * 1.25;
		^ControlSpec(minx,maxx)
	}
	spec_ { arg sp;
		spec = sp;
		gridLines.spec = sp;
	}
	domainSpec_ { arg dsp;
		domainSpec = dsp;
		//gridLines.domainSpec = dsp;
	}
	setZoom { arg argFromX,argToX;
		var toXpixels;
		fromX = argFromX.asFloat;
		toX = argToX.asFloat;
		this.domainSpec = ControlSpec(domainSpec.minval,max(toX,domainSpec.maxval));
		gridLines.domainSpec = ControlSpec(fromX,toX);
		if(boundsWidth.notNil,{
			fromXpixels = this.rawMapX(fromX);
			toXpixels = this.rawMapX(toX);
			xScale = boundsWidth / (toXpixels - fromXpixels);
			if(xScale == inf,{
				xScale = 1.0
			});
		});
	}
		
	update {
		uv.refresh
	}
	background_ { arg c; uv.background = c }
	focusColor_ { arg c; 
		uv.focusColor = c;
		if(order.notNil,{
			order.focusColor = c
		})	
	}
	writeName {}
	
	curveGui { arg layout;
		orderSpec = [2,8].asSpec;
		order = Slider( layout, 17@200 )
			.value_( orderSpec.unmap( model.order ) )
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
	keyDownAction { arg view, char, modifiers, unicode, keycode;
		var handled = false;
		if(unicode == 127,{
			if(selected.notNil,{
				model.deletePoint(selected);
				selected = nil;
				model.changed;
				handled = true;
			})
		});
		^handled
	}
}


// LoopSplineEditor
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
			flatpoints = flatpoints.add( [this.map(xy),\point,i] )
		};
		model.controlPoints.do { arg cps,cpi;
			cps.do { arg xy,i;
				flatpoints = flatpoints.add( [this.map(xy),\cp, [cpi,i] ] )
			}
		};
		{
			uv.refresh;
		}.defer
	}

	makeMouseActions {
		uv.mouseDownAction = { |uvw, x, y,modifiers, buttonNumber, clickCount|
			var p,fp,i;
			p = x@y;
			fp = flatpoints.detect({ arg flatpoint;
				flatpoint[0].dist(p) <= range
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
						i = this.createControlPoint(this.unmap(p));
						selected = nil;
						selectedCP = i;
					},{
						i = this.createPoint(this.unmap(p));
						selected = i;
						selectedCP = nil;
					});
				});					
			});
		};
			
		uv.mouseMoveAction = { |uvw, x,y| 
			var p,spoint;
			p = x@y;
			spoint = this.unmap(p);
			if(spec.notNil,{
				spoint[1] = spec.constrain(spoint[1])
			});
			if(domainSpec.notNil,{
				spoint[0] = domainSpec.constrain(spoint[0])
			});
			if( selected.notNil,{
				model.points[selected] = spoint;
				model.changed;
			},{
				if(selectedCP.notNil,{
					model.controlPoints[selectedCP[0]][selectedCP[1]] = spoint;
					model.changed;
				});
			}); 
		};
		uv.keyDownAction = { arg view, char, modifiers, unicode, keycode;
			this.keyDownAction(view, char, modifiers, unicode, keycode);
		};		
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
	keyDownAction { arg view, char, modifiers, unicode, keycode;
		var handled = super.keyDownAction(view, char, modifiers, unicode, keycode);
		if(handled.not,{
			if(unicode == 127,{
				if(selectedCP.notNil,{
					model.deleteControlPoint(selectedCP[0],selectedCP[1]);
					selectedCP = nil;
					model.changed;
					handled = true;
				})
			});
		});
		^handled
	}	
}
	

	