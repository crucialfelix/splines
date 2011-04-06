

LinearSpline  : AbstractFunction {

	var <>points,<>isClosed;
	
	*new { |points,isClosed=false|
		^super.newCopyArgs(points.collect(_.asArray),isClosed)
	}
	storeArgs { ^[points,isClosed] }
	simplifyStoreArgs { |args| ^args }
	
	value { arg u;
		^points.intAt(u)
	}
	numDimensions {
		^points.first.size
	}
	interpolate { arg divisions=128;
		// along the spline path
		// actually gives divisions * numPoints 
		^points.interpolate(divisions,this.interpolationKey,isClosed,this.extraArgs)
		// need to change to use this.value
	}
	interpolationKey { ^\linear }
	extraArgs { ^nil }

	bilinearInterpolate { arg divisions,domain=0,fillEnds=true;
		// return y values for evenly spaced intervals along x (eg. steady time increments)
		// interpolate returns a series of points evenly spaced along the spline path
		// this linear interpolates between those known points to find y
		// bicubic would be better

		// domain : dimension along which evenly spaced divisions occur
		// value : the other dimension for which value is sought
		// any higher dimensions are ignored
		var ps,step,feed,t=0.0;
		var value,totalTime;

		value = 1 - domain;
		
		
		ps = this.interpolate(divisions / points.size * 4.0); // oversampled
		
		totalTime = ps.last[domain];
		// TODO
		// interpolates from time 0
		// mathematically obliged to do the pre-0 span
		//if(ps.first[domain] < 0,{
		//	totalTime = totalTime + ps.first[domain].abs
		//});

		step = totalTime / divisions.asFloat;
		feed = Routine({ // arg t;
				var xfrac,after;
				ps.do({ arg p,i;
					if(t == p[domain], {
						p[value].yield
					});
					while({ p[domain] > t },{
						if(i > 0,{
							xfrac = (t - ps[i-1][domain]) / (p[domain] - ps[i-1][domain]);
							blend(ps[i-1][value],p[value],xfrac).yield
						},{
							// first point is already past t
							// nil or fill
							if(fillEnds,{
								p[value].yield
							},{
								nil.yield
							})
						})
					})
				});
				inf.do({
					if(fillEnds,{
						ps.last[value].yield
					},{
						nil.yield
					})
				})
			});
							
		^Array.fill(divisions,{ |i|
			t = i * step;
			feed.next
		})
	}
	
	guiClass { 
		// 2D only
		^SplineGui 
	}	
	xypoints {
		^points.collect({ |p| Point(p[0],p[1]) })
	}
	createPoint { arg p,i;
		points = points.insert(i,p.asArray);
	}
	deletePoint { arg i;
		if(points.size > 1,{
			points.removeAt(i)
		});
	}
	
	minMaxVal { arg dim;
		var maxes,mins,numd;
		numd = this.numDimensions;
		maxes = -inf;
		mins = inf;
		points.do { arg p;
			if(p[dim] < mins,{
				mins = p[dim]
			});
			if(p[dim] > maxes,{
				maxes = p[dim]
			});
		};
		^[mins,maxes]
	}		
	normalizeDim { arg dim,min=0.0,max=1.0;
		// normalize the points
		// not the result which depends on analyzing the interpolation
		// squashing the points by that much may or may not work
		// depending on freakiness of curves
		var maxes,mins,numd;
		# mins, maxes = this.minMaxVal(dim);
		points.do { arg p;
			p[dim] = p[dim].linlin(mins,maxes,min,max)
		};
	}

	
	/*
	normalize { arg ... dimsMax;
		// normalize the points
		// not the result which depends on curve
		var maxes,mins,numd;
		numd = this.numDimensions;
		maxes = Array.fill(numd,-inf);
		mins = Array.fill(numd,inf);
		points.do { arg p;
			numd.do { arg di;
				if(p[di] < mins[di],{
					mins[di] = p[di]
				});
				if(p[di] > maxes[di],{
					maxes[di] = p[di]
				});
			}
		};
		scales		
		(dimsMax ?? {Array.fill(numd,nil)}).do({ arg max,di;
	}
	*/		





	// move this elsewhere
	animate { arg target,selector,frameRate=12,undersampling=1,clock=AppClock;
		//if(selector.isKindOf(Dictionary),{
		// get current values
		Routine({
			var levels,numFrames,t;
			t = frameRate.reciprocal;
			numFrames = (this.duration * frameRate).asInteger;
			levels = this.interpolateAlongX(numFrames);
			levels.do({ arg y,frame;
				target.perform(selector,y);
				t.wait
			})
		}).play(clock)
	}

	//	plot
	//	skew
	//	rotate
	//	moveby
	//	resizeBy
	//	
	//	++	

	// see ScatterView3d for viewing 3D into a 2D plane

	//Catmull-Rom
	//http://en.wikipedia.org/wiki/Cubic_Hermite_spline#Catmull.E2.80.93Rom_spline
	//http://stackoverflow.com/questions/1251438/catmull-rom-splines-in-python
	/* For example, most camera path animations generated from discrete key-frames are handled using Catmull–Rom splines. They are popular mainly for being relatively easy to compute, guaranteeing that each key frame position will be hit exactly, and also guaranteeing that the tangents of the generated curve are continuous over multiple segments.*/
		
}


BSpline : LinearSpline {

	var <>order;
	
	*new { |points,order=2.0,isClosed=false|
		^super.newCopyArgs(points.collect(_.asArray),isClosed).order_(order)
	}
	storeArgs { ^[points,order,isClosed] }
	extraArgs { ^order }
	interpolationKey { ^\bspline }
	*defaultOrder { ^2.0 }
}

/*
Spline : LinearSpline { 
	
	// replace this with explicit Bezier series
	interpolationKey { ^\spline }

}*/


HermiteSpline : BSpline {
	
	interpolationKey { ^\hermite }

}


BezierSpline : LinearSpline {
	
	var <>controlPoints;
	
	*new { arg ... things;
		var isClosed,points,controlPoints,nu;
		if(things.size.odd,{
			isClosed = things.pop;
		},{
			isClosed = false
		});
		points = Array.new(things.size/2);
		controlPoints = Array.new(things.size/2);
		things.do { arg p,i;
			if(i.even,{
				points.add(p.asArray)
			},{
				controlPoints.add(p.collect(_.asArray))
			});
		};
		nu = super.new(points,isClosed);
		nu.controlPoints = controlPoints;
		^nu
	}
	//storeArgs { ^[points,isClosed] }
	interpolate { arg divisions=128;		
		// along the spline path
		// actually gives divisions * numPoints 
		var ps,funcs;
		funcs = #['linear','quadratic','cubic'];
		points.do { arg p,i;
			var cp,pnext;
			cp = controlPoints[i];
			if(isClosed, {
				pnext = points.wrapAt(i+1);
			},{
				pnext = points.at(i+1);
			});
			if(pnext.notNil,{
				// iterate t along tangent from p to pnext
				divisions.do { arg di;
					var t,pt;
					t = divisions.reciprocal * di;
					// choose interpolation
					pt = this.perform(funcs[cp.size] ? \ntic,t,p,pnext,cp);
					ps = ps.add(pt)
				};
			});
		};
		^ps			
	}
	linear { arg t,p1,p2,cps;
		^p1.blend(p2,t)
	}
	quadratic { arg t,p1,p2,cps;
		^(((1.0-t).squared)*p1) + (2*(1.0-t)*t*cps[0]) + (t.squared*p2)
	}
	cubic {  arg t,p1,p2,cps;
		^(((1.0-t).cubed)*p1) +   (3*((1.0-t).squared)*t*cps[0]) +    (3*(1.0-t)*t.squared*cps[1]) + (t.cubed*p2)
	}
	ntic { arg t,p1,p2,cps;
		// gazillionic
		var sum,n;
		n = cps.size;
		sum = (1-t).pow(n) * p1;
		(n-1..1).do { arg ni,i;
			var binomialCoef;
			binomialCoef = n.factorial / (ni.factorial * (n - ni).factorial);
			sum = sum + (binomialCoef * t.pow(n-ni) * (1-t).pow(ni) * cps[i] )
		};			
		^sum	+ (t.pow(n) * p2);
	}
	
	createPoint { arg p,i;
		super.createPoint(p,i);
		controlPoints = controlPoints.insert(i,[]);
		this.changed('points');
	}		
	createControlPoint { arg p,pointi;
		var cps;
		cps = controlPoints[pointi];
		controlPoints[pointi] = cps.add(p);
		this.changed('points');
		^[pointi,controlPoints[pointi].size-1]
	}
	deletePoint { arg i;
		if(points.size > 1,{
			super.deletePoint(i);
			controlPoints.removeAt(i);
		});
	}
	deleteControlPoint { arg pointi,i;
		controlPoints[pointi].removeAt(i)
	}
	guiClass { ^BezierSplineGui }
}



/*
multi

Path(
	t, spline,
	t, spline
)
purpose is to put splines, elastics, functions, complex point driven splines, fade outs all into one object.
and move them around in time without having to change their internal x

space between is always some spline
by default a bezier with no controls (linear)


LoopedSplineEditor	
	by gui by default if loop is set


*/


