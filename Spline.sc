

LinearSpline  { //: AbstractFunction

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
		// needs to change to use this.value
	}
	interpolationKey { ^\linear }
	extraArgs { ^nil }

	// pretty sure this is correctly named
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
							// point is already past t
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
	
	


	// under construction	
	/*
	
	
	wrong
	
	interpolateDimension { arg domainDimension=0,valueDimension=1,divisions=128;
		// wrong
		var args,minval,maxval,step;
		minval = points.minValue(_[domainDimension]);
		maxval = points.maxValue(_[domainDimension]);
		step = (maxval - minval).asFloat / divisions.asFloat;
		
		^Array.fill(divisions,{ arg i;
			this.interpolateDimensionAt(i * step,valueDimension)
		})
	}
		
	// bilinearIntpAt
	interpolateDimensionAt { arg i,dimension=1;
		// interpolate across values in dimension
		^points.collect(_[dimension]).intAt(i,this.interpolationKey,isClosed)
		// not correct. this interpolates one dimension
		// STILL with equal tangents along the spline
		// my original attempt was correct
		// I think that is bilinear interpolation
		// you need to make points then search and interpolate
		
		// if more than 2D then
		// has to be translated to a 2D view
		// with some definition of a "camera"

		// or use some method of
		// http://en.wikipedia.org/wiki/Multivariate_interpolation
	}
	*/
	
	// see ScatterView3d for viewing 3D into a 2D plane
	
	/*duration { // if looping then this is the duration of one cycle
		^this.points.last.x
	}*/

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

//			var levels,numFrames;
//			numFrames = (this.duration * frameRate / undersampling).asInteger;
//			levels = this.interpolateAlongX(numFrames);
//			(this.duration * frameRate).asInteger.do({ arg frame;
//				var y;
//				if(undersampling != 1,{
//					y = levels.blendAt(frame / undersampling)
				
				
	//	
	//	plot
	//	skew
	//	rotate
	//	moveby
	//	resizeBy
	//	
	//	++	

	//http://www.thirdpartyplugins.com/python/modules/c4d/C4DAtom/GeListNode/BaseList2D/BaseObject/PointObject/SplineObject/index.html#SplineObject.GetInterpolationType
	// c4d terminology:
	// akima
	// bezier
	// isClosed
	
	//Catmull-Rom
	//http://en.wikipedia.org/wiki/Cubic_Hermite_spline#Catmull.E2.80.93Rom_spline
	//http://stackoverflow.com/questions/1251438/catmull-rom-splines-in-python
	/* For example, most camera path animations generated from discrete key-frames are handled using Catmull–Rom splines. They are popular mainly for being relatively easy to compute, guaranteeing that each key frame position will be hit exactly, and also guaranteeing that the tangents of the generated curve are continuous over multiple segments.*/
	
	/*
		don't use Point use .asArray so they can be n-dimensional
		interpolate along x becomes:
		
		interpolate(1,0,1024)
			levels of dimension 1
			along dimension 0, 1024 values
			from min to max
			
		spline class thus defines the method of interpolation
		subclasses could be used for various multivariate tactics
		
		spline interpolation means to have segments
		polynomial interpolation is what these are really doing
		
		Path
		PolyPath
			interpolates using polys
		Bezier
			same thing but uses control points to specify
		Spline
			separate polys or beziers for each segment
			select interpolation method
		
		ie no such thing as LinearSpline
		
		but they are separate classes because args are different
		
		PathGen
		PathOsc
						
	*/


	
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


/*
BezierPath(
	point,
	[ control points], // 0 .. 5
	point2,
	[ control points], // 0 .. 5	
	..,
	pointN,
	[ control points], // 0 .. 5	ignored if not closed/looped
	
	isClosed
)
where each point is joined to the next point using control points.
0 control points means linear
up to quintile bezier


Path(
	t, spline,
	t, spline
)
purpose is to put splines, elastics, functions, complex point driven splines, fade outs all into one object.
and move them around in time without having to change their internal x

space between is always some spline
by default a bezier with no controls (linear)




SplineEditor
	used by gui
LoopedSplineEditor	
	by gui by default if loop is set
	



*/


