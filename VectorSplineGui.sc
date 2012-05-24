

/*
	vectorizing data is a common machine learning technique
	where many parameters are stored as dimensions of a vector.

	a vector-spline is a (time) series of such vectors where the first dimension is interpreted as X and the other dimensions are the data. 

	most commonly X is time, but can also be a series of states or presets.

	the spline can be used to interpolate between those states or to sequence changes between states.  

	and this gui shows each dimension overlaid and allows switching the focused one and editing it.

	it uses an array of 2D splines
	each one a pair of X (global time) and that dimension as Y

*/

VectorSplineGui : AbstractSplineGui {

	var <splineGuis,<focused=0;
	var fade=0.25;

	guiBody { arg layout,bounds;
		splineGuis = { arg di;
			var spline,sg;
			spline = model.sliceDimensions([0,di+1]);
			sg = spline.guiClass.new(spline).gui(userView:uv);
			sg.color = Color.hsv(di * (model.numDimensions.reciprocal),1,0.5);
			sg.showGrid = false;
			sg.alpha = fade;
			sg
		} ! (model.numDimensions - 1);
		if(splineGuis.size > 4,{ fade = 0.1 });
		this.focused = focused;
	}
	focused_ { arg di;
		if(di.inclusivelyBetween(0,splineGuis.size - 1).not,{
			("Focus index out of range:" + di).error;
			^this
		});
		splineGuis[focused].alpha = fade;
		splineGuis[focused].showGrid = false;
		focused = di;
		splineGuis[focused].alpha = 1.0;
		splineGuis[focused].showGrid = true;
		this.refresh;
	}
	refresh {
		splineGuis.do { arg sg,sgi;
			if(sgi != focused, {sg.refresh})
		};
		splineGuis[focused].refresh
	}
}



