

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
		this.makeSplineGuis;
		this.focused = focused;
	}
	focused_ { arg di;
		var sg;
		if(di.inclusivelyBetween(0,splineGuis.size - 1).not,{
			("Focus index out of range:" + di).error;
			^this
		});
		sg = splineGuis[focused];
		sg.alpha = fade;
		sg.showGrid = false;
		focused = di;
		sg = splineGuis[focused];
		sg.alpha = 1.0;
		sg.showGrid = true;
		sg.makeMouseActions;
		this.refresh;
	}
	refresh {
		splineGuis.do { arg sg,sgi;
			if(sgi != focused, {sg.refresh})
		};
		splineGuis[focused].refresh
	}
	update { arg spline;
		if(spline === model,{
			// update all

		},{
			splineGuis.do { arg sg,sgi;
				if(sg.model === spline,{
					model.spliceDimensions([0,sgi+1],sg.model)
				})
			}
		})
		// rebuild all
		splineGuis.do { arg sg;
			sg.model.removeDependant(this);
			sg.remove;
		};
		{
			this.makeSplineGuis;
			uv.refresh;
			this.focused = focused;
		}.defer
	}
	updateSplineGuis {
		(model.numDimensions - 1).do { arg di;
			var spline;
			spline = model.sliceDimensions([0,di+1]);
			splineGuis[di].model = spline;
		};
	}
	makeSplineGuis {
		uv.drawFunc = nil;
		splineGuis = { arg di;
			var spline,sg;
			spline = model.sliceDimensions([0,di+1]);
			sg = spline.guiClass.new(spline).gui(userView:uv);
			sg.color = Color.hsv(di * (model.numDimensions.reciprocal),1,0.5);
			sg.showGrid = false;
			sg.alpha = fade;
			spline.addDependant(this);
			sg
		} ! (model.numDimensions - 1);
	}	
	viewDidClose {
		super.viewDidClose;
		splineGuis.do { arg sg;
			sg.model.removeDependant(this)
		}
	}
}



