

AbstractSpline { 

	var <>points,<>order,<>loop,<>spec;
	
	*new { |points,order,loop=false,spec|
		^super.newCopyArgs(points.collect(_.asPoint),order?this.defaultOrder,loop,spec)
	}
	storeArgs { ^[points,order,loop,spec] }
	*defaultOrder { ^2.0 }

	interpolate { arg divisionsPerSegment=128,withLoop=false;
		^points.collect(_.asArray)
			.interpolate(divisionsPerSegment,
						this.interpolationKey,
						withLoop,order)
			.collect(_.asPoint);
	}
	wrapxInterpolate { arg divisionsPerSegment=128;
		// odd: last point is far right edge
		// if first point is at far left edge
		// then they are at same time
		// onscreen you expect to be viewing one cycle
		// not have it varying in wavelength by where last point lies.
		// maybe that is fine. no other way to vary the wavelength without zooming
		var first,ps,li,one,two;
		first = Point(points.last.x + points.first.x,points.first.y);
		ps = (points ++ [first]).collect(_.asArray)
			.interpolate(divisionsPerSegment,
						this.interpolationKey,
						false,order)
			.collect(_.asPoint);
		// chop off after last point
		// move to pre-first
		li = ps.lastIndexForWhich(_.x < points.last.x);
		if(li.notNil,{
			one = ps.copyToEnd(li).collect({ |p| Point(p.x-points.last.x,p.y) });
			two = ps.copyRange(0,li-1);
			ps = one ++ two
		});
		^ps
	}
	
	interpolateAlongX { arg divisions,totalTime,fillEnds=true;
		// return y values where x is divided evenly (eg. steady time increments)
		// interpolate returns a series of points evenly spaced along the spline path
		// but not evenly spaced for X

		// doesn't do loop correctly yet
		var ps,step,feed,t=0.0;
		if(loop,{
			ps = this.wrapxInterpolate(divisions / points.size * 4.0)
		},{
			ps = this.interpolate(divisions / points.size * 4.0); // oversampled
		});
				
		if(totalTime.isNil, {
			totalTime = ps.last.x;
		});
		step = totalTime / divisions;
		feed = Routine({
				var xfrac,after;
				ps.do({ arg p,i;
					//[t,p,i].debug;
					if(t == p.x, {
						p.y.yield
					});
					while({ p.x > t },{
						if(i > 0,{
							xfrac = (t - ps[i-1].x) / (p.x - ps[i-1].x);
							blend(ps[i-1].y,p.y,xfrac).yield
						},{
							// point is already past t
							// if loop then interpolate with last point wrapped with totalTime
							// else either nil or fill with first value (waiting for spline to start)
							if(fillEnds,{
								p.y.yield
							},{
								nil.yield
							})
						})
					})
				});
				inf.do({
					if(fillEnds,{
						ps.last.y.yield
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
	duration { // if looping then this is the duration of one cycle
		^this.points.last.x
	}

	kr { arg timeScale=1.0,doneAction=0,divisions=512;
	    // spline x values are seconds
	    // if timeScale is tempo then x values are beats
		var b,index,levels;
		levels = this.interpolateAlongX(divisions);
		b = LocalBuf.newFrom(levels);
		if(loop,{
			index = VarSaw.kr(this.duration.reciprocal * timeScale,width:1).range(0,levels.size-1)
		},{
			index = Line.kr(0.0,levels.size-1,this.duration * timeScale,doneAction:doneAction);
		});
		^BufRd.kr(1,b,index,loop,4)
	}
	readKr { arg position,timeScale=1.0,divisions=512;
	    // position is in X units (seconds)
	    // if timeScale is tempo then x values are beats
		var b,index,levels;
		levels = this.interpolateAlongX(divisions);
		b = LocalBuf.newFrom(levels);
		if(timeScale != 1.0,{
		    position = position * timeScale;
		});
		index = (position / this.duration) * divisions;
		^BufRd.kr(1,b,index,loop,4)
	}	
	ar { arg freq=440,phase=0,divisions=1024;
	    // plays in cycles (full spline) per second
		var b,index,totalTime=0,levels;
		levels = this.interpolateAlongX(divisions);
		totalTime = this.points.last.x;
		b = LocalBuf.newFrom(levels);
		if(loop,{
			index = VarSaw.ar((totalTime).reciprocal * freq,phase,width:1).range(0,levels.size-1)
		},{
			index = Line.ar(phase / 2pi * (levels.size-1),levels.size-1,totalTime * freq);
		});
		^BufRd.ar(1,b,index,loop,4)
	}
	// a more accurate, efficient and complicated technique
	// would be to use a time to buffer index mapper
	// where more points are allocated when the error/difference between true value and
	// interpolated value is highest
	// so fewer total points, more of them where they are needed.
	// easier to just write a UGen to calculate the splines on the server
	
}


BSpline : AbstractSpline {
	
	interpolationKey { ^\bspline }
	*defaultOrder { ^2.0 }
	guiClass { ^BSplineGui }
}

Spline : AbstractSpline {
	
	interpolationKey { ^\spline }

}

HermiteSpline : AbstractSpline {
	
	interpolationKey { ^\hermite }

}

LinearSpline : AbstractSpline {
	
	interpolationKey { ^\linear }

}



