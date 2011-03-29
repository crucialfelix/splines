Splines
=======

temporary, just putting the work up here

this uses the interpolation methods in Wouter's wslib
which he is still working on.
some of these methods should at some point move to Common I think.

I made Spline objects that can be edited, saved and loaded.
I think they deserve to be objects

just working with BSpline for the moment


b = BSpline([ Point(0,0), Point(0.58963874282376, 0.4134375), Point(2.2682499386103, 0.826875), Point(4.8180390967671, 0.013125) ], 2.3870967741935);
{
PinkNoise.ar * b.kr
}.play


b.gui

a gui for editing (based on wouter's helpfile)

factored out the GridLines from Common:Plot (plot2)
this GridLine class can be used in Plot and is also used here as a background to edit the splines.
could also be used behind sliders, 2d sliders, envelope views etc
can be hooked into any UserView

I'd like to make the Spline editor also portable so you could stick the spline plus editable points and curves on another userview,
for instance to edit several splines together or to edit it on top of a sound file display or in a larger arrange layout where you can also move other objects.

will add click-to-create new points. option/control drag to adjust curve.
would like to figure out multiple curves. I think this means blending multiple spline sets. 


BSpline can .kr .readKr and .ar 

can be used like an envelope (with a doneAction)

or looped

so you could also make LFOs and waveforms for Osc


doesn't update on the server in real time yet.  

editing waveforms for Osc won't be practical the way its doing it.
it has to reinterpolate with oversampling, resample that for the x domain and send it to the buffer on the server.

but using it to design wavetables and blending those should be quite fun

will also add a .xyKr that plays along the spline path and returns x and y as separate kr
and a .polarKr that returns polar 

the splines don't presuppose any limitation in how they are to be used.
it could move in circles
