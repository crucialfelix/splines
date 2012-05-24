

AbstractSplineGui : ObjectGui {

	var uv,<>alpha=1.0;
	var <>color,<>crossHairsColor,<>textColor;
	var <>pointSize=3,<>font;

	gui { arg parent,bounds,userView;
		if(userView.notNil,{
			parent = userView.parent;
			bounds = bounds ?? {userView.bounds};
		},{
			if(parent.isNil,{
				parent = Window(model.asString,bounds).front;
				bounds = parent.bounds.moveTo(0,0);
			},{
	 			bounds = bounds ?? {
	 				parent.tryPerform('indentedRemaining') ?? {
	 					Rect(0,0,400,300)
	 				}
	 			}
			});
		});
		uv = this.makeView(parent,bounds,userView);
		this.initColors;	
		this.guiBody(parent,uv.bounds.moveTo(0,0));
	}
	initColors {
		// Du gamla, Du fria
		color = Color.blue;
		crossHairsColor = Color(0.92537313432836, 1.0, 0.0, 0.41791044776119);
		textColor = Color.black.alpha_(0.5);
		font = GUI.font.sansSerif(9);
	}
	makeView { arg parent,bounds,userView;
		^userView ?? {
			UserView(parent, bounds)
				.background_(GUI.skin.background)
				.resize_(5);
		};
	}
}


SplineGui : AbstractSplineGui {
	
	// 2D spline editor

	var <spec,<domainSpec;
	var <>density=128;
	var order,orderSpec,<gridLines;
	var selected,<>onSelect;
	var boundsHeight,boundsWidth;
	var fromX,toX,fromXpixels=0,xScale=1.0;
	
	gui { arg parent, bounds, argSpec,argDomainSpec,userView;
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
		^super.gui(parent,bounds,userView)
	}
	
	guiBody { arg layout,bounds;
		var pen;

		// this can recalc on resize
		boundsHeight = bounds.height.asFloat;
		boundsWidth = bounds.width.asFloat;

		if(\GridLines.asClass.notNil,{
			gridLines = DrawGrid(bounds,GridLines(domainSpec),GridLines(this.spec));
		},{
			// this is the old implementation
			// can disappear when this is 3.5 only
			gridLines = GridLines0(uv,bounds,this.spec,domainSpec);
		});
		this.setZoom(domainSpec.minval,domainSpec.maxval);
		
		pen = GUI.pen;
		uv.drawFunc_({
			if(uv.bounds != bounds,{
				this.didResize;
				bounds = uv.bounds;
			});
			pen.use {
				pen.alpha = alpha;
				gridLines.opacity = alpha;
				Pen.font = font;
				gridLines.draw;

				// can cache an array of Pen commands
				model.xypoints.do { |point,i|
					var focPoint;
					focPoint = point;
					point = this.map(point);
					Pen.addArc(point,pointSize,0,2pi);
					if(i==selected,{
						Pen.color = color;
						Pen.fill;
						
						// crosshairs
						Pen.color = crossHairsColor;
						Pen.moveTo(0@point.y);
						Pen.lineTo(Point(bounds.width-1,point.y));
						Pen.moveTo(point.x@0);
						Pen.lineTo(Point(point.x,bounds.height-1));
						Pen.stroke;

						// text
						Pen.color = textColor;
						Pen.use {
							Pen.translate(point.x,point.y);
							Pen.rotate(0.5pi);
							Pen.stringAtPoint(gridLines.x.grid.formatLabel(focPoint.x,4),Point(-45,0));
						};
						Pen.stringAtPoint( gridLines.y.grid.formatLabel(focPoint.y,4), Point(point.x+15,point.y-15) );
					},{
						Pen.stroke;
					})
				};
				this.drawControlPoints();
				
				Pen.color = color;
				Pen.moveTo( this.map( model.points[0]) );

				model.interpolate(density).do { arg point,i;
					Pen.lineTo( this.map(point) )
				};
				Pen.stroke;
			}
		});
		this.focusColor = GUI.skin.focusColor ?? {GUI.skin.foreground.alpha_(0.4)};
		this.makeMouseActions;
		
		this.update;
		// make a BSplineGui to separate this out
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
			var p,newSelect;
			p = x@y;
			newSelect = model.xypoints.detectIndex({ |pt|
				(this.map(pt)).dist(p) <= pointSize
			});
			if(selected.notNil and: newSelect.notNil and: {modifiers.isShift},{
				selected = nil
			},{
				selected = newSelect
			});
			if(selected.notNil,{
				uv.refresh
			},{
				if(clickCount == 2,{
					selected = this.createPoint(this.unmap(p));
					uv.refresh;
				});
			});
			if(selected.notNil,{ onSelect.value(selected,this) });
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
		this.update;
	}
	setDomainSpec { arg dsp,setGridLines=true;
		domainSpec = dsp;
		if(setGridLines,{
			gridLines.x.setZoom(dsp.minval,dsp.maxval);
		});
		this.update;
	}
	setZoom { arg argFromX,argToX;
		var toXpixels;
		fromX = argFromX.asFloat;
		toX = argToX.asFloat;
		domainSpec = ControlSpec(domainSpec.minval,max(toX,domainSpec.maxval));
		gridLines.x.setZoom(fromX,toX);
		if(boundsWidth.notNil,{
			fromXpixels = this.rawMapX(fromX);
			toXpixels = this.rawMapX(toX);
			xScale = boundsWidth / (toXpixels - fromXpixels);
			if(xScale == inf,{
				xScale = 1.0
			});
		});
	}
	didResize {
		var b;
		b = uv.bounds.moveTo(0,0);
		gridLines.bounds = b;
		boundsHeight = b.height;
		boundsWidth = b.width;
	}
	select { arg i;
		selected = i;
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
		var i;
		i = this.detectPointIndex(p);
		model.createPoint(p,i);
		^i
	}
	detectPointIndex { arg p;
		if(model.points.size == 0,{ ^0 });
		if(p[0] < model.points.first[0],{ ^0 });
		if(p[0] > model.points.last[0],{ ^model.points.size + 1 });
		model.points.do { arg j,i;
			var k;
			k = model.points.clipAt(i+1);
			if(p[0].inclusivelyBetween(j[0],k[0]),{
				^i+1
			})
		};
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
			Pen.color = Color(0.55223880597015, 0.36106502462071, 0.20605925595901);			cps.do { arg point,i;
				Pen.addArc(this.map(point),range,0,2pi);
				if(selectedCP == [cpi,i],{
					Pen.fill;
				},{
					Pen.stroke;
				});
			};
			
			Pen.color = Color(0.55223880597015, 0.36106502462071, 0.20605925595901, 0.5);			Pen.moveTo(this.map(model.points[cpi]));
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
				flatpoint[0].dist(p) <= pointSize
			});
			if(fp.notNil,{ 
				if(fp[1] == 'point',{
					if(selected.notNil and: modifiers.isShift,{
						selected = nil
					},{
						selected = fp[2];
					});
					selectedCP = nil;
					onSelect.value(selected,this);
				},{ // control point
					if(selected.notNil,{
						onSelect.value(nil,this)
					});
					selected = nil;
					selectedCP = fp[2];
				});
				uv.refresh;
			},{
				if(clickCount == 2,{
					if(modifiers.isAlt,{ // isCtrl double click not working on Qt
						i = this.createControlPoint(this.unmap(p));
						selected = nil;
						selectedCP = i;
					},{
						i = this.createPoint(this.unmap(p));
						selected = i;
						selectedCP = nil;
					});
					onSelect.value(selected,this);
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
		var i,cps,cpi,return;
		return = { arg i,ci;
			model.createControlPoint(p.asArray,i,ci ?? {model.controlPoints[i].size});
			^[i,model.controlPoints[i].size-1]
		};
		i = (this.detectPointIndex(p) - 1).clip(0,model.points.size-1);
		cps = model.controlPoints[i];
		if(cps.size == 0,{
			return.value(i)
		});
		if(p[0] < cps.first[0],{
			return.value(i,0)
		});
		if(p[0] > cps.last[0],{
			return.value(i)
		});
		cps.do { arg cp,ci;
			if(p[0].inclusivelyBetween(cp[0],cps.clipAt(ci+1)[0]),{
				return.value(i,ci + 1);
			})
		};
		return.value(i)
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
	

	