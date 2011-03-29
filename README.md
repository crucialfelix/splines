Splines
=======

temporary, just putting the work up here

this uses the interpolation methods in Wouter's wslib

I made Spline objects that can be edited, saved and loaded

just working with BSpline for the moment

a gui for editing (based on wouter's helpfile)

factored out the GridLines from Common:Plot (plot2)
this GridLine class can be used in Plot and is also used here as a background to edit the splines.
could also be used behind sliders, 2d sliders, envelope views etc
can be hooked into any UserView

I'd like to make the Spline editor also portable so you could stick the spline plus editable points and curves on another userview,
for instance to edit several splines together or to edit it on top of a sound file display or in a larger arrange layout.


BSpline can .kr .readKr and .ar 

doesn't update in real time yet



