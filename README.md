Splines
=======

BezierSpline
LinearSpline
BSpline
HermiteSpline

SplineGen
SplineOsc

guis for editing


factored out the GridLines from Common:Plot (plot2)
this GridLine class can be used in Plot and is also used here as a background to edit the splines.
could also be used behind sliders, 2d sliders, envelope views etc
can be hooked into any UserView

I'd like to make the Spline editor also portable so you could stick the spline plus editable points and curves on another userview,
for instance to edit several splines together or to edit it on top of a sound file display or in a larger arrange layout where you can also move other objects.

will add option/control drag to adjust curve.


audio classes do not update on the server in real time yet.  

editing waveforms for Osc won't be practical the way its doing it anyway.
it has to reinterpolate with oversampling, resample that for the x domain and send it to the buffer on the server.

but using it to design wavetables and blending those should be quite fun

will also add a .xyKr that plays along the spline path and returns x and y as separate kr
and a .polarKr that returns polar 

the splines don't presuppose any limitation in how they are to be used.
they could move in circles


